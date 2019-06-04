package de.foellix.aql.webservice.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.system.Storage;
import de.foellix.aql.system.task.ITaskHook;
import de.foellix.aql.system.task.OperatorTask;
import de.foellix.aql.ui.cli.OutputWriter;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.helper.Helper;

public class WebScheduler {
	private long TIME_TO_LOAD_PRECOMPUTED_ANSWERS = 60; // in seconds

	private Queue<QueryTask> queue;
	private Map<Integer, QueryTask> mapDone;
	private de.foellix.aql.system.System aqlSystem;
	private OutputWriter outputWriter;

	List<QueryTask> queryBuffer;
	List<PreprocessTask> pptBuffer;
	long bufferTimeout;

	private Lock lock;
	private boolean flushBuffer;
	private boolean running;

	private static WebScheduler instance = new WebScheduler();

	private WebScheduler() {
		this.queryBuffer = new ArrayList<>();
		this.pptBuffer = new ArrayList<>();
		this.bufferTimeout = 0;

		this.lock = new ReentrantLock();
		this.flushBuffer = true;
		this.running = false;

		// Setup Queue
		this.queue = new LinkedList<>();
		this.mapDone = new HashMap<>();

		// Setup AQL-System
		OperatorTask.setTempDirectory(new File(Config.getInstance().getProperty(Config.TEMP_PATH)));
		new File(Config.getInstance().getProperty(Config.STORAGE_PATH)).mkdirs();
		Storage.getInstance().setDifferentStorageLocation(Config.getInstance().getProperty(Config.STORAGE_PATH));
		File configFile = ConfigHandler.getInstance().getConfigFile();
		if (!configFile.exists()) {
			if (ConfigHandler.getInstance().getConfigFile() != null
					&& !ConfigHandler.getInstance().getConfigFile().equals("")
					&& !ConfigHandler.getInstance().getConfigFile().equals(" ")) {
				Log.warning("Could not find/read desired config file: " + configFile.getAbsolutePath());
			}
			try {
				configFile = new File("config.xml");
			} catch (final Exception e) {
				Log.error("Could not find/read default config file.");
				return;
			}
		}
		ConfigHandler.getInstance().setConfig(configFile);
		this.aqlSystem = new de.foellix.aql.system.System();
		this.aqlSystem.setStoreAnswers(false);
		new File(Config.getInstance().getProperty(Config.ANSWERS_PATH)).mkdirs();
		this.aqlSystem.getScheduler().setTimeout(Helper.getTimeOutByConfig());
		this.aqlSystem.getScheduler()
				.setAlwaysPreferLoading(Boolean.valueOf(Config.getInstance().getProperty(Config.PREFER_LOADING)));
	}

	public static WebScheduler getInstance() {
		return instance;
	}

	public void queryNextTask(QueryTask task) {
		this.running = true;
		this.mapDone.put(task.getId(), task);

		// Ask AQL-System
		task.setStatus("In Progress");
		if (this.outputWriter != null) {
			this.aqlSystem.getAnswerReceivers().remove(this.outputWriter);
		}
		this.outputWriter = new OutputWriter(
				new File(Config.getInstance().getProperty(Config.ANSWERS_PATH), "answer_" + task.getId() + ".xml"));
		this.aqlSystem.getAnswerReceivers().add(this.outputWriter);
		this.aqlSystem.queryAndWait(task.getQuery()).iterator().next();
		task.setStatus("Done");

		// Next task
		if (!this.queue.isEmpty() && this.aqlSystem.getScheduler().getWaiting() <= 0) {
			queryNextTask(this.queue.poll());
		} else {
			this.running = false;
		}
	}

	public Answer ask(QueryTask task, long timeout) {
		try {
			this.lock.lock();
			final long backupTimeout = this.aqlSystem.getScheduler().getTimeout();
			final long currentTimeout = Math.min(timeout, backupTimeout);

			this.queryBuffer.add(task);
			this.bufferTimeout = Math.max(this.bufferTimeout, this.bufferTimeout + currentTimeout);
			this.lock.unlock();

			try {
				Thread.sleep(Integer.valueOf(Config.getInstance().getProperty(Config.BUFFER_TIME)).intValue());
			} catch (final InterruptedException e) {
				Log.error("Scheduler interrupted while buffering requests: " + e.getMessage());
			}

			this.lock.lock();
			if (this.flushBuffer && !this.queryBuffer.isEmpty()) {
				this.flushBuffer = false;
				final List<QueryTask> removeList = new ArrayList<>();
				final StringBuilder sb = new StringBuilder();
				for (final QueryTask t : this.queryBuffer) {
					sb.append(t.getQuery() + " \n");
					removeList.add(t);
				}
				this.queryBuffer.removeAll(removeList);
				if (this.bufferTimeout <= 0) {
					this.aqlSystem.getScheduler().setTimeout(Helper.getTimeOutByConfig());
				} else {
					this.aqlSystem.getScheduler().setTimeout(this.bufferTimeout);
				}
				this.bufferTimeout = 0;
				this.aqlSystem.queryAndWait(sb.toString());
				this.flushBuffer = true;
			}
			this.lock.unlock();

			while (!this.flushBuffer) {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					Log.error("Scheduler interrupted while waiting for buffer to be flushed: " + e.getMessage());
				}
			}

			this.lock.lock();
			this.aqlSystem.getScheduler().setTimeout(this.TIME_TO_LOAD_PRECOMPUTED_ANSWERS);
			final Answer answer;
			final Collection<Answer> candidates = this.aqlSystem.queryAndWait(task.getQuery());
			if (candidates != null && candidates.iterator().hasNext()) {
				answer = candidates.iterator().next();
			} else {
				answer = new Answer();
			}
			if (backupTimeout != this.aqlSystem.getScheduler().getTimeout()) {
				this.aqlSystem.getScheduler().setTimeout(backupTimeout);
			}
			this.lock.unlock();

			return answer;
		} catch (final Exception e) {
			final StringBuilder sb = new StringBuilder();
			for (final QueryTask t : this.queryBuffer) {
				if (t == null) {
					sb.append("null;\n");
				} else {
					sb.append(t.getQuery() + ";\n");
				}
			}

			Log.error("While working on\n" + sb.toString() + "an unexpected exception was thrown ("
					+ e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public File preprocess(PreprocessTask task, long timeout) {
		try {
			this.lock.lock();
			final long backupTimeout = this.aqlSystem.getScheduler().getTimeout();
			final long currentTimeout = Math.min(timeout, backupTimeout);

			this.pptBuffer.add(task);
			this.bufferTimeout = Math.max(this.bufferTimeout, this.bufferTimeout + currentTimeout);
			this.lock.unlock();

			try {
				Thread.sleep(Integer.valueOf(Config.getInstance().getProperty(Config.BUFFER_TIME)).intValue());
			} catch (final InterruptedException e) {
				Log.error("Scheduler interrupted while buffering requests: " + e.getMessage());
			}

			this.lock.lock();
			if (this.flushBuffer && !this.pptBuffer.isEmpty()) {
				this.flushBuffer = false;
				final List<PreprocessTask> removeList = new ArrayList<>();
				final StringBuilder sb = new StringBuilder();
				for (final PreprocessTask t : this.pptBuffer) {
					sb.append(t.createQuery() + " \n");
					removeList.add(t);
				}
				this.pptBuffer.removeAll(removeList);
				if (this.bufferTimeout <= 0) {
					this.aqlSystem.getScheduler().setTimeout(Helper.getTimeOutByConfig());
				} else {
					this.aqlSystem.getScheduler().setTimeout(this.bufferTimeout);
				}
				this.bufferTimeout = 0;
				activateFakeTool(true);
				this.aqlSystem.queryAndWait(sb.toString());
				activateFakeTool(false);
				this.flushBuffer = true;
			}
			this.lock.unlock();

			while (!this.flushBuffer) {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					Log.error("Scheduler interrupted while waiting for buffer to be flushed: " + e.getMessage());
				}
			}

			this.lock.lock();
			this.aqlSystem.getScheduler().setTimeout(this.TIME_TO_LOAD_PRECOMPUTED_ANSWERS);
			activateFakeTool(true);
			final Answer answer;
			final Collection<Answer> candidates = this.aqlSystem.queryAndWait(task.createQuery());
			if (candidates != null && candidates.iterator().hasNext()) {
				answer = candidates.iterator().next();
			} else {
				answer = new Answer();
			}
			activateFakeTool(false);
			if (backupTimeout != this.aqlSystem.getScheduler().getTimeout()) {
				this.aqlSystem.getScheduler().setTimeout(backupTimeout);
			}
			this.lock.unlock();

			return new File(
					answer.getPermissions().getPermission().iterator().next().getReference().getApp().getFile());
		} catch (final Exception e) {
			final StringBuilder sb = new StringBuilder();
			for (final PreprocessTask t : this.pptBuffer) {
				if (t == null) {
					sb.append("null;\n");
				} else {
					sb.append(t.getKeyword() + ";\n");
				}
			}

			Log.error("While working on\n" + sb.toString() + "an unexpected exception was thrown ("
					+ e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private void activateFakeTool(boolean value) {
		if (value) {
			if (!ConfigHandler.getInstance().getConfig().getTools().getTool()
					.contains(Helper.getFakeToolForPreprocessing())) {
				ConfigHandler.getInstance().getConfig().getTools().getTool().add(Helper.getFakeToolForPreprocessing());

				if (this.aqlSystem.getTaskHooksBefore().getHooks().get(Helper.getFakeToolForPreprocessing()) == null) {
					final List<ITaskHook> temp = new ArrayList<>();
					temp.add(new CreateAnswerFileHook());
					this.aqlSystem.getTaskHooksBefore().getHooks().put(Helper.getFakeToolForPreprocessing(), temp);
				}
			}
		} else {
			if (ConfigHandler.getInstance().getConfig().getTools().getTool()
					.contains(Helper.getFakeToolForPreprocessing())) {
				ConfigHandler.getInstance().getConfig().getTools().getTool()
						.remove(Helper.getFakeToolForPreprocessing());
			}
		}
	}

	public void scheduleTask(QueryTask task) {
		if (!this.running) {
			new Thread(() -> {
				queryNextTask(task);
			}).start();
		} else {
			this.queue.add(task);
		}
	}

	public QueryTask getTask(int id) {
		if (this.mapDone.get(id) != null) {
			return this.mapDone.get(id);
		}
		for (final QueryTask task : this.queue) {
			if (task.getId() == id) {
				return task;
			}
		}
		return null;
	}
}
