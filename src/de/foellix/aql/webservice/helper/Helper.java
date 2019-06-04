package de.foellix.aql.webservice.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.config.Execute;
import de.foellix.aql.config.Priority;
import de.foellix.aql.config.Tool;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.IQuestionNode;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.handler.QueryHandler;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.scheduler.PreprocessTask;
import de.foellix.aql.webservice.scheduler.QueryTask;

public class Helper {
	public static Tool fakePreprocessor = null;

	public static String replaceFilesInQuery(String query, List<File> files) {
		final IQuestionNode queryNode = QueryHandler.parseQuery(query);
		if (query.contains("?")) {
			for (final App app : queryNode.getAllApps(true)) {
				final String temp = app.getFile().replaceAll("\\\\", "/").substring(app.getFile().lastIndexOf("/") + 1);
				for (final File file : files) {
					if (file.getName().equals(temp)) {
						app.setFile(file.getAbsolutePath());
					}
				}
			}
		}
		query = queryNode.toString();

		for (int i = 0; i < files.size(); i++) {
			query = query.replaceAll("%FILE_" + (i + 1) + "%", files.get(i).getAbsolutePath().replaceAll("\\\\", "/"));
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

	public static QueryTask interpretQuery(String query, List<FormDataBodyPart> fileBodies) {
		Log.msg("Query accepted: " + query, Log.NORMAL);

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
						Log.error("Could not copy " + fileReceived.toPath() + " to server ("
								+ e.getClass().getSimpleName() + "): " + e.getMessage());
					} else {
						Log.warning(fileReceived.toPath() + " already exists (" + e.getClass().getSimpleName() + "): "
								+ e.getMessage());
					}
				}
				files.add(fileReceived);
			}

			// Update query
			query = Helper.replaceFilesInQuery(query, files);

			return new QueryTask(query, id);
		} catch (final Exception e) {
			Log.error("Could not interpret query (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static PreprocessTask interpretPrepossesorTask(String keyword, List<FormDataBodyPart> fileBodies) {
		Log.msg("Preprocessor task accepted: " + keyword, Log.NORMAL);

		try {
			// Get ID
			final int id = Config.getInstance().increaseID();

			// Extract file
			final BodyPartEntity bodyPartEntity = (BodyPartEntity) fileBodies.iterator().next().getEntity();
			final String fileName = fileBodies.iterator().next().getContentDisposition().getFileName();
			final File fileReceived = new File(Config.getInstance().getProperty(Config.UPLOAD_PATH) + File.separator
					+ id + File.separator + fileName);
			fileReceived.getParentFile().mkdirs();
			try {
				Files.copy(bodyPartEntity.getInputStream(), fileReceived.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				if (!fileReceived.exists()) {
					Log.error("Could not copy " + fileReceived.toPath() + " to server (" + e.getClass().getSimpleName()
							+ "): " + e.getMessage());
				} else {
					Log.warning(fileReceived.toPath() + " already exists (" + e.getClass().getSimpleName() + "): "
							+ e.getMessage());
				}
			}

			return new PreprocessTask(keyword, fileReceived, id);
		} catch (final Exception e) {
			Log.error(
					"Could not interpret preprocessor task (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static void log(String msg, int loglevel) {
		System.out.println("");
		Log.msg(msg, Log.IMPORTANT);
		Log.msg("> ", Log.NORMAL, false);
	}

	public static Tool getFakeToolForPreprocessing() {
		if (fakePreprocessor == null) {
			// Create new tool
			fakePreprocessor = new Tool();

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
			fakeExecute.setRun("echo noExecution");
			fakeExecute.setResult(
					new File(Config.getInstance().getProperty(Config.TEMP_PATH), "%APP_APK_FILENAME%_fakeForPP.xml")
							.getAbsolutePath());
			fakePreprocessor.setExecute(fakeExecute);

			// Setup other properties
			fakePreprocessor.setExternal(false);
			fakePreprocessor.setVersion("fake");
			fakePreprocessor.setPath("/");
			fakePreprocessor.setQuestions(KeywordsAndConstants.SOI_PERMISSIONS);

			// Setup priority
			final Priority fakePriority = new Priority();
			fakePriority.setValue(Integer.MAX_VALUE);
			fakePreprocessor.getPriority().add(fakePriority);
		}
		return fakePreprocessor;
	}

	public static boolean deleteFolder(File folder) {
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				deleteFolder(file);
			} else {
				file.delete();
			}
		}
		return folder.delete();
	}
}
