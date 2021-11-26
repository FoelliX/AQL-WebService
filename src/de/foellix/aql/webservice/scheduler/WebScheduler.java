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
import de.foellix.aql.system.AQLSystem;
import de.foellix.aql.system.Options;
import de.foellix.aql.ui.cli.OutputWriter;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.helper.Helper;

public class WebScheduler {
	private long TIME_TO_LOAD_PRECOMPUTED_ANSWERS = 60; // in seconds

	private Queue<QueryTask> queue;
	private Map<Integer, QueryTask> mapDone;
	private AQLSystem aqlSystem;
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
		final Options options = new Options();
		options.setStoreAnswers(false);
		options.setTimeout(Helper.getTimeOutByConfig());
		this.aqlSystem = new AQLSystem(options);
	}

	public static WebScheduler getInstance() {
		return instance;
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

	public void queryNextTask(QueryTask task) {
		this.running = true;
		this.mapDone.put(task.getId(), task);

		// Ask AQL-System
		task.setStatus(Task.IN_PROGRESS);
		if (this.outputWriter != null) {
			this.aqlSystem.getAnswerReceivers().remove(this.outputWriter);
		}
		this.outputWriter = new OutputWriter(
				new File(Config.getInstance().getProperty(Config.ANSWERS_PATH), "answer_" + task.getId() + ".xml"));
		this.aqlSystem.getAnswerReceivers().add(this.outputWriter);
		this.aqlSystem.queryAndWait(task.getQuery());
		task.setStatus(Task.DONE);

		// Next task
		if (!this.queue.isEmpty() && !this.aqlSystem.isRunning()) {
			queryNextTask(this.queue.poll());
		} else {
			this.running = false;
		}
	}

	public Object ask(QueryTask task, long timeout) {
		Statistics.getInstance().asked();
		try {
			this.lock.lock();
			final long backupTimeout = this.aqlSystem.getOptions().getTimeout();
			final long currentTimeout = Math.min(timeout, backupTimeout);

			this.queryBuffer.add(task);
			this.bufferTimeout = Math.max(this.bufferTimeout, this.bufferTimeout + currentTimeout);
			this.lock.unlock();

			try {
				Thread.sleep(Integer.valueOf(Config.getInstance().getProperty(Config.BUFFER_TIME)).intValue());
			} catch (final InterruptedException e) {
				de.foellix.aql.webservice.helper.Helper
						.error("Scheduler interrupted while buffering requests: " + e.getMessage());
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
					this.aqlSystem.getOptions().setTimeout(Helper.getTimeOutByConfig());
				} else {
					this.aqlSystem.getOptions().setTimeout(this.bufferTimeout);
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
					de.foellix.aql.webservice.helper.Helper
							.error("Scheduler interrupted while waiting for buffer to be flushed: " + e.getMessage());
				}
			}

			this.lock.lock();
			this.aqlSystem.getOptions().setTimeout(this.TIME_TO_LOAD_PRECOMPUTED_ANSWERS);
			final Object answer;
			Log.setSilence(Log.SILENCE_LEVEL_WARNING);
			final Collection<Object> candidates = this.aqlSystem.queryAndWait(task.getQuery());
			Log.setSilence(false);
			if (candidates != null && candidates.iterator().hasNext()) {
				answer = candidates.iterator().next();
			} else {
				answer = new Answer();
			}
			if (backupTimeout != this.aqlSystem.getOptions().getTimeout()) {
				this.aqlSystem.getOptions().setTimeout(backupTimeout);
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

			de.foellix.aql.webservice.helper.Helper.error("While working on\n" + sb.toString()
					+ "an unexpected exception was thrown (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			Statistics.getInstance().done();
		}
	}

	public File preprocess(PreprocessTask task, long timeout) {
		Statistics.getInstance().asked();
		try {
			this.lock.lock();
			final long backupTimeout = this.aqlSystem.getOptions().getTimeout();
			final long currentTimeout = Math.min(timeout, backupTimeout);

			this.pptBuffer.add(task);
			this.bufferTimeout = Math.max(this.bufferTimeout, this.bufferTimeout + currentTimeout);
			this.lock.unlock();

			try {
				Thread.sleep(Integer.valueOf(Config.getInstance().getProperty(Config.BUFFER_TIME)).intValue());
			} catch (final InterruptedException e) {
				de.foellix.aql.webservice.helper.Helper
						.error("Scheduler interrupted while buffering requests: " + e.getMessage());
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
					this.aqlSystem.getOptions().setTimeout(Helper.getTimeOutByConfig());
				} else {
					this.aqlSystem.getOptions().setTimeout(this.bufferTimeout);
				}
				this.bufferTimeout = 0;
				activateFakeTool(true, task.getKeyword());
				this.aqlSystem.queryAndWait(sb.toString());
				activateFakeTool(false, task.getKeyword());
				this.flushBuffer = true;
			}
			this.lock.unlock();

			while (!this.flushBuffer) {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					de.foellix.aql.webservice.helper.Helper
							.error("Scheduler interrupted while waiting for buffer to be flushed: " + e.getMessage());
				}
			}

			this.lock.lock();
			this.aqlSystem.getOptions().setTimeout(this.TIME_TO_LOAD_PRECOMPUTED_ANSWERS);
			activateFakeTool(true, task.getKeyword());
			final Object answer;
			final Collection<Object> candidates = this.aqlSystem.queryAndWait(task.createQuery());
			if (candidates != null && candidates.iterator().hasNext()) {
				answer = candidates.iterator().next();
			} else {
				answer = new Answer();
			}
			activateFakeTool(false, task.getKeyword());
			if (backupTimeout != this.aqlSystem.getOptions().getTimeout()) {
				this.aqlSystem.getOptions().setTimeout(backupTimeout);
			}
			this.lock.unlock();

			return (File) answer;
		} catch (final Exception e) {
			final StringBuilder sb = new StringBuilder();
			for (final PreprocessTask t : this.pptBuffer) {
				if (t == null) {
					sb.append("null;\n");
				} else {
					sb.append(t.getKeyword() + ";\n");
				}
			}

			de.foellix.aql.webservice.helper.Helper.error("While working on\n" + sb.toString()
					+ "an unexpected exception was thrown (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			Statistics.getInstance().done();
		}
	}

	private void activateFakeTool(boolean value, String keyword) {
		if (value) {
			if (!ConfigHandler.getInstance().getConfig().getTools().getTool()
					.contains(Helper.getFakeToolForPreprocessing(keyword))) {
				ConfigHandler.getInstance().getConfig().getTools().getTool()
						.add(Helper.getFakeToolForPreprocessing(keyword));
			}
		} else {
			if (ConfigHandler.getInstance().getConfig().getTools().getTool()
					.contains(Helper.getFakeToolForPreprocessing(keyword))) {
				ConfigHandler.getInstance().getConfig().getTools().getTool()
						.remove(Helper.getFakeToolForPreprocessing(keyword));
			}
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

	public String getRunningTasks() {
		if (this.aqlSystem == null || this.aqlSystem.getTaskScheduler() == null
				|| this.aqlSystem.getTaskScheduler().getTaskTree() == null) {
			return "None";
		} else {
			return this.aqlSystem.getTaskScheduler().getTaskTree().toString();
		}
	}
}