package de.foellix.aql.webservice;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.jansi.AnsiConsole;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.BackupAndReset;
import de.foellix.aql.webservice.config.Config;

public class WebService {
	public static String URL = "http://0.0.0.0:8080/AQL-WebService/";

	private static final String CMD_EXIT = "exit";
	private static final String CMD_QUIT = "quit";
	private static final String CMD_HELP = "help";
	private static final String CMD_INFO = "info";
	private static final String CMD_SAVE = "save";
	private static final String CMD_CHANGE_PORT = "change port";
	private static final String CMD_CHANGE_USERNAME = "change username";
	private static final String CMD_CHANGE_PASSWORD = "change password";
	private static final String CMD_BACKUP = "backup";
	private static final String CMD_RESET = "reset";
	private static final String CMD_TOOLS = "tools";

	public static void main(String[] args) throws IOException {
		// Initialize
		URL = "http://0.0.0.0:" + Config.getInstance().getProperty(Config.PORT) + "/AQL-WebService/";
		Properties.info().ABBREVIATION = "AQL-WebService";
		Log.setDifferentLogFile(new File(Config.getInstance().getProperty(Config.LOG_FILE)));
		Log.setShorten(true);

		AnsiConsole.systemInstall();
		final String authorStr = "Author: " + Properties.info().AUTHOR + " (" + Properties.info().AUTHOR_EMAIL + ")";
		final String space = "               ".substring(Math.min(Properties.info().VERSION.length() + 3, 15));
		final String centerspace = "                                        "
				.substring(Math.min(32, authorStr.length() / 2));
		Log.msg(ansi().bold().fg(GREEN).a("          ____  _      __          __  _     _____" + space).reset()
				.a(" v. " + Properties.info().VERSION.substring(0, Math.min(Properties.info().VERSION.length(), 12)))
				.bold().fg(GREEN)
				.a(" _          \r\n"
						+ "    /\\   / __ \\| |     \\ \\        / / | |   / ____|               (_)         \r\n"
						+ "   /  \\ | |  | | |  ____\\ \\  /\\  / /__| |__| (___   ___ _ ____   ___  ___ ___ \r\n"
						+ "  / /\\ \\| |  | | | |_____\\ \\/  \\/ / _ \\ '_ \\\\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\\r\n"
						+ " / ____ \\ |__| | |____    \\  /\\  /  __/ |_) |___) |  __/ |   \\ V /| | (_|  __/\r\n"
						+ "/_/    \\_\\___\\_\\______|    \\/  \\/ \\___|_.__/_____/ \\___|_|    \\_/ |_|\\___\\___|\r\n")
				.reset().a("\r\n" + centerspace + authorStr + "\r\n"), Log.NORMAL);

		// Parse launch parameters
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-b") || args[i].equals("-backup")) {
				BackupAndReset.backup(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
			}
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c") || args[i].equals("-cfg") || args[i].equals("-config")) {
				ConfigHandler.getInstance().setConfig(new File(args[i + 1]));
				i++;
			} else if (args[i].equals("-r") || args[i].equals("-re") || args[i].equals("-reset")) {
				BackupAndReset.reset(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				reset();
			} else if (args[i].equals("-d") || args[i].equals("-debug") || args[i].equals("-reset")) {
				final String debug = args[i + 1];
				if (debug.equals("normal")) {
					Log.setLogLevel(Log.NORMAL);
				} else if (debug.equals("short")) {
					Log.setLogLevel(Log.NORMAL);
					Log.setShorten(true);
				} else if (debug.equals("warning")) {
					Log.setLogLevel(Log.WARNING);
				} else if (debug.equals("error")) {
					Log.setLogLevel(Log.ERROR);
				} else if (debug.equals("debug")) {
					Log.setLogLevel(Log.DEBUG);
				} else if (debug.equals("detailed")) {
					Log.setLogLevel(Log.DEBUG_DETAILED);
				} else if (debug.equals("special")) {
					Log.setLogLevel(Log.DEBUG_SPECIAL);
				} else {
					Log.setLogLevel(Integer.valueOf(debug).intValue());
				}
				i++;
			}
		}

		// Start server
		HttpServer server = startServer();
		Log.msg("\nWebservice started!\n" + info(), Log.NORMAL);

		// Setup
		try (Scanner sc = new Scanner(System.in)) {
			String read;
			do {
				Log.msg("> ", Log.NORMAL, false);
				read = sc.nextLine();
				if (read.equals(CMD_HELP)) {
					Log.msg("\nAvailable commands:\n0. " + CMD_EXIT + " or " + CMD_QUIT
							+ " (to stop the web service)\n1. " + CMD_CHANGE_PORT
							+ " %PORT% (e.g. \"change port 8081\")\n2. " + CMD_CHANGE_USERNAME
							+ " %USERNAME% (e.g. \"change username aqlUser\")\n3. " + CMD_CHANGE_PASSWORD
							+ " %PASSWORD% (e.g. \"change password S3cR3T!\")\n4. " + CMD_SAVE
							+ " (stores the current settings in the \"config.properties\" file)\n5. " + CMD_INFO
							+ " (shows some basic information)\n6. " + CMD_BACKUP
							+ " (backups the underlying AQL-System's storage)\n7. " + CMD_RESET
							+ " (resets the underlying AQL-System)\n8. " + CMD_TOOLS
							+ " (shows a list of all tools in the current config)\n", Log.NORMAL);
				} else if (read.equals(CMD_INFO)) {
					Log.msg(info(), Log.NORMAL);
				} else if (read.equals(CMD_SAVE)) {
					Config.getInstance().store();
					Log.msg("Current settings saved!", Log.NORMAL);
				} else if (read.startsWith(CMD_CHANGE_PORT)) {
					server.shutdownNow();
					final String port = read.replaceAll(CMD_CHANGE_PORT, "").replaceAll(" ", "");
					Config.getInstance().getProperties().setProperty(Config.PORT, port);
					server = startServer(port);
					Log.msg("Port changed to: " + port, Log.NORMAL);
				} else if (read.startsWith(CMD_CHANGE_USERNAME)) {
					final String username = read.replaceAll(CMD_CHANGE_USERNAME, "").replaceAll(" ", "");
					Config.getInstance().getProperties().setProperty(Config.USERNAME, username);
					Log.msg("Username changed to: " + username, Log.NORMAL);
				} else if (read.startsWith(CMD_CHANGE_PASSWORD)) {
					final String password = read.replaceAll(CMD_CHANGE_PASSWORD, "").replaceAll(" ", "");
					Config.getInstance().getProperties().setProperty(Config.PASSWORD, password);
					Log.msg("Password changed to: " + password, Log.NORMAL);
				} else if (read.equals(CMD_BACKUP)) {
					BackupAndReset.backup(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				} else if (read.equals(CMD_RESET)) {
					BackupAndReset.reset(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				} else if (read.equals(CMD_TOOLS)) {
					Log.msg(tools(), Log.NORMAL);
				} else if (!(read.equals(CMD_EXIT) || read.equals(CMD_QUIT))) {
					Log.msg("Unknown command: " + read + " (Type \"" + CMD_HELP
							+ "\" to see a list of available commands)", Log.NORMAL);
				}
			} while (!(read.equals(CMD_EXIT) || read.equals(CMD_QUIT)));
		} catch (final Exception e) {
			Log.error("Web service unexpectedly stopped! (" + e.getMessage() + ")");
		}

		// Stop server
		server.shutdownNow();
		Log.msg("Webservice stopped!", Log.NORMAL);
	}

	private static void reset() {
		final File uploadFolder = new File(Config.getInstance().getProperty(Config.UPLOAD_PATH));
		final boolean failed = de.foellix.aql.webservice.helper.Helper.deleteFolder(uploadFolder);
		if (failed) {
			Log.msg("Successfully deleted upload directory!", Log.NORMAL);
		} else {
			Log.warning("Could not delete everything inside the upload directory!");
		}
		uploadFolder.mkdir();

		Config.getInstance().getProperties().setProperty(Config.QUEUE_START, "10000");
		Config.getInstance().store();
	}

	private static String info() {
		return "\nListening on port " + Config.getInstance().getProperty(Config.PORT) + "\n(" + URL + ")\n\nUsername: "
				+ Config.getInstance().getProperty(Config.USERNAME) + "\nPassword: "
				+ Config.getInstance().getProperty(Config.PASSWORD) + "\n\nConfig: "
				+ ConfigHandler.getInstance().getConfigFile().getAbsolutePath() + "\n";
	}

	private static String tools() {
		return (ConfigHandler.getInstance().getConfig().getTools() != null
				? "\nToollists: " + Helper.toString(ConfigHandler.getInstance().getConfig().getTools().getTool()) + "\n"
				: "")
				+ (ConfigHandler.getInstance().getConfig().getPreprocessors() != null ? "\nProprocessors: "
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getPreprocessors().getTool()) + "\n"
						: "")
				+ (ConfigHandler.getInstance().getConfig().getOperators() != null ? "\nOperators: "
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getOperators().getTool()) + "\n" : "")
				+ (ConfigHandler.getInstance().getConfig().getConverters() != null ? "\nConverters: "
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getConverters().getTool()) + "\n"
						: "");
	}

	public static HttpServer startServer() {
		return startServer(null);
	}

	public static HttpServer startServer(String port) {
		if (port != null) {
			URL = "http://0.0.0.0:" + port + "/AQL-WebService/";
		}

		final ResourceConfig rc = new ResourceConfig();
		final Map<String, Object> initParams = new HashMap<>();
		initParams.put(ServerProperties.PROVIDER_PACKAGES, "de.foellix.aql.webservice");
		initParams.put(ServerProperties.PROVIDER_CLASSNAMES, "org.glassfish.jersey.media.multipart.MultiPartFeature");
		rc.addProperties(initParams);
		if (!Log.logIt(Log.DEBUG)) {
			Logger.getLogger("org.glassfish").setLevel(Level.OFF);
		}

		final HttpServer grizzly = GrizzlyHttpServerFactory.createHttpServer(URI.create(URL), rc, false);

		final TCPNIOTransport transport = grizzly.getListener("grizzly").getTransport();
		final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().setCorePoolSize(128).setMaxPoolSize(128)
				.setQueueLimit(-1);
		transport.setWorkerThreadPoolConfig(config);

		try {
			grizzly.start();
		} catch (final IOException e) {
			Log.error("Error occured while starting Grizzly webservice: " + e.getMessage());
		}

		return grizzly;
	}
}