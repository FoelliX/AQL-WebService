package de.foellix.aql.webservice.helper;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.config.Execute;
import de.foellix.aql.config.Priority;
import de.foellix.aql.config.Tool;
import de.foellix.aql.datastructure.handler.QueryHandler;
import de.foellix.aql.datastructure.query.Query;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.system.task.PreprocessorTaskInfo;
import de.foellix.aql.webservice.config.Account;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.scheduler.PreprocessTask;
import de.foellix.aql.webservice.scheduler.QueryTask;

public class Helper {
	public static Map<String, Tool> fakePreprocessorMap = null;
	public static boolean inNewLine = false;

	public static String replaceFileVariablesInQuery(String query, List<File> files) {
		final Query queryObj = QueryHandler.parseQuery(query);
		query = queryObj.toString();
		for (int i = 0; i < files.size(); i++) {
			query = query.replace("%FILE_" + (i + 1) + "%", files.get(i).getAbsolutePath().replace("\\", "/"));
		}
		return query;
	}

	public static long getTimeOutByConfig() {
		long timeout;
		final String readTimeout = Config.getInstance().getProperty(Config.TIMEOUT);
		if (readTimeout.contains("h")) {
			timeout = Integer.parseInt(readTimeout.replaceAll("h", "")) * 3600;
		} else if (readTimeout.contains("m")) {
			timeout = Integer.parseInt(readTimeout.replaceAll("m", "")) * 60;
		} else {
			timeout = Integer.parseInt(readTimeout.replaceAll("s", ""));
		}
		return timeout;
	}

	public static QueryTask interpretQuery(Account account, String query, List<FormDataBodyPart> fileBodies) {
		if (account == null) {
			msg("Async. query accepted: " + query);
		} else {
			msg("Query of \"" + account.getUsername() + "\" accepted: " + query);
		}

		try {
			// Get ID
			final int id = Config.getInstance().increaseID();

			// Extract files
			final List<File> files = new ArrayList<>();
			for (int i = 0; i < fileBodies.size(); i++) {
				final BodyPartEntity bodyPartEntity = (BodyPartEntity) fileBodies.get(i).getEntity();
				final String fileName = fileBodies.get(i).getContentDisposition().getFileName();
				final File fileReceived = new File(Config.getInstance().getProperty(Config.UPLOAD_PATH) + File.separator
						+ id + File.separator + fileName);
				fileReceived.getParentFile().mkdirs();
				try {
					Files.copy(bodyPartEntity.getInputStream(), fileReceived.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					if (!fileReceived.exists()) {
						error("Could not copy " + fileReceived.toPath() + " to server (" + e.getClass().getSimpleName()
								+ "): " + e.getMessage());
					} else {
						warning(fileReceived.toPath() + " already exists (" + e.getClass().getSimpleName() + "): "
								+ e.getMessage());
					}
				}
				files.add(fileReceived);
			}

			// Update query
			query = Helper.replaceFileVariablesInQuery(query, files);

			return new QueryTask(query, id);
		} catch (final Exception e) {
			error("Could not interpret query (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static PreprocessTask interpretPrepossesorTask(String keyword, List<FormDataBodyPart> fileBodies) {
		msg("Preprocessor task accepted: " + keyword);

		// Get ID
		final int id = Config.getInstance().increaseID();

		try {
			// Extract file(s)
			final List<File> filesReceived = new ArrayList<>();
			for (final FormDataBodyPart formBodyPartEntity : fileBodies) {
				// Get filename
				final BodyPartEntity bodyPartEntity = (BodyPartEntity) formBodyPartEntity.getEntity();
				final String fileName = formBodyPartEntity.getContentDisposition().getFileName();

				// Create file
				final File fileReceived = new File(Config.getInstance().getProperty(Config.UPLOAD_PATH) + File.separator
						+ id + File.separator + fileName);
				fileReceived.getParentFile().mkdirs();
				try {
					Files.copy(bodyPartEntity.getInputStream(), fileReceived.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					if (!fileReceived.exists()) {
						error("Could not copy " + fileReceived.toPath() + " to server (" + e.getClass().getSimpleName()
								+ "): " + e.getMessage());
					} else {
						warning(fileReceived.toPath() + " already exists (" + e.getClass().getSimpleName() + "): "
								+ e.getMessage());
					}
				}
				filesReceived.add(fileReceived);
			}

			if (id > 0) {
				return new PreprocessTask(keyword, filesReceived, id);
			}
		} catch (final Exception e) {
			error("Could not interpret preprocessor task (" + e.getClass().getSimpleName() + "): " + e.getMessage());
		}
		return null;
	}

	public static void msg(String msg) {
		msg(msg, true);
	}

	public static void msg(String msg, boolean newLine) {
		if (inNewLine) {
			endLine();
		}
		Log.msg(msg, Log.IMPORTANT);
		if (newLine) {
			newLine();
		}
	}

	public static void warning(String msg) {
		warning(msg, true);
	}

	public static void warning(String msg, boolean newLine) {
		warning(msg, true, false);
	}

	public static void warning(String msg, boolean newLine, boolean repeat) {
		if (inNewLine) {
			endLine();
		}
		if (repeat) {
			Log.msg(ansi().fg(YELLOW).a(msg).reset(), Log.WARNING);
		} else {
			Log.warning(msg);
		}
		if (newLine) {
			newLine();
		}
	}

	public static void error(String msg) {
		error(msg, true);
	}

	public static void error(String msg, boolean newLine) {
		if (inNewLine) {
			endLine();
		}
		Log.error(msg);
		if (newLine) {
			newLine();
		}
	}

	public static void newLine() {
		inNewLine = true;
		Log.msg("> ", Log.NORMAL, false);
	}

	public static void endLine() {
		Log.emptyLine();
		inNewLine = false;
	}

	public static Tool getFakeToolForPreprocessing(String keyword) {
		if (fakePreprocessorMap == null) {
			fakePreprocessorMap = new HashMap<>();
		}
		if (!fakePreprocessorMap.containsKey(keyword)) {
			// Create new tool
			final Tool fakePreprocessor = new Tool();

			// Unify tool name
			String fakePreprocessorName = "FakePreprocessor";
			int unifier = 0;
			while (ConfigHandler.getInstance().getToolByName(fakePreprocessorName) != null) {
				unifier++;
				if (unifier == 1) {
					fakePreprocessorName = fakePreprocessorName + "_unified_";
				}
				fakePreprocessorName = fakePreprocessorName + unifier;
			}
			fakePreprocessor.setName(fakePreprocessorName);

			// Setup execution properties
			final Execute fakeExecute = new Execute();
			fakeExecute.setInstances(0);
			fakeExecute.setMemoryPerInstance(1);
			if (System.getProperty("os.name").startsWith("Windows")) {
				fakeExecute.setRun("cmd /C echo noExecution (" + PreprocessorTaskInfo.APP_APK + ", "
						+ PreprocessorTaskInfo.APP_APK_FILENAME + ", " + PreprocessorTaskInfo.APP_APK_NAME + ", "
						+ PreprocessorTaskInfo.APP_APK_PACKAGE + ", " + keyword + ")");
			} else {
				fakeExecute.setRun("echo noExecution (" + PreprocessorTaskInfo.APP_APK + ", "
						+ PreprocessorTaskInfo.APP_APK_FILENAME + ", " + PreprocessorTaskInfo.APP_APK_NAME + ", "
						+ PreprocessorTaskInfo.APP_APK_PACKAGE + ", " + keyword + ")");
			}
			fakeExecute.setResult(PreprocessorTaskInfo.APP_APK);
			fakePreprocessor.setExecute(fakeExecute);

			// Setup other properties
			fakePreprocessor.setExternal(false);
			fakePreprocessor.setVersion(keyword);
			fakePreprocessor.setPath(".");
			fakePreprocessor.setQuestions(KeywordsAndConstantsHelper.SOI_ARGUMENTS);

			// Setup priority
			final Priority fakePriority = new Priority();
			fakePriority.setValue(ConfigHandler.getInstance().getMaxConfiguredPriority() + 1);
			fakePreprocessor.getPriority().add(fakePriority);

			// Put in map
			fakePreprocessorMap.put(keyword, fakePreprocessor);
		}
		return fakePreprocessorMap.get(keyword);
	}

	public static String getIp() throws IOException {
		final URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			final String ip = in.readLine();
			in.close();
			return ip;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}