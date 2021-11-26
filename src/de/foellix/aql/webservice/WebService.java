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

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.FileHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.BackupAndReset;
import de.foellix.aql.system.storage.Storage;
import de.foellix.aql.webservice.config.Account;
import de.foellix.aql.webservice.config.Accounts;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.scheduler.Statistics;
import de.foellix.aql.webservice.scheduler.WebScheduler;

public class WebService {
	public static String url;

	private static final String CMD_EXIT = "exit";
	private static final String CMD_QUIT = "quit";
	private static final String CMD_HELP = "help";
	private static final String CMD_INFO = "info";
	private static final String CMD_USER = "user";
	private static final String CMD_PORT = "port";
	private static final String CMD_BACKUP = "backup";
	private static final String CMD_RESET = "reset";
	private static final String CMD_TOOLS = "tools";
	private static final String CMD_ACCOUNTS = "accounts";
	private static final String CMD_TASKS = "tasks";

	public static void main(String[] args) {
		// Initialize
		Properties.info().ABBREVIATION = "AQL-WebService";
		Log.setDifferentLogFile(new File(Config.getInstance().getProperty(Config.LOG_FILE)));
		Log.setShorten(true);

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

		// Initialize Statistics
		Statistics.getInstance();

		// Setup file system
		FileHelper.setTempDirectory(new File(Config.getInstance().getProperty(Config.TEMP_PATH)));
		FileHelper.setConverterDirectory(new File(Config.getInstance().getProperty(Config.CONVERTER_PATH)));
		FileHelper.setAnswersDirectory(new File(Config.getInstance().getProperty(Config.ANSWERS_PATH)));
		final File newStorage = new File(Config.getInstance().getProperty(Config.STORAGE_PATH));
		newStorage.mkdirs();
		Storage.getInstance().setStorageDirectory(newStorage);

		// Parse launch parameters
		boolean firstConfig = true;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-b") || args[i].equals("-backup")) {
				BackupAndReset.backup(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
			}
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c") || args[i].equals("-cfg") || args[i].equals("-config")) {
				if (firstConfig) {
					CLIHelper.evaluateConfig(args[++i]);
					firstConfig = false;
				} else {
					final File oldConfig = ConfigHandler.getInstance().getConfigFile();
					CLIHelper.evaluateConfig(args[++i]);
					ConfigHandler.getInstance().mergeWith(oldConfig);
				}
			} else if (args[i].equals("-rules")) {
				CLIHelper.evaluateRules(args[i + 1]);
			} else if (args[i].equals("-r") || args[i].equals("-re") || args[i].equals("-reset")) {
				BackupAndReset.reset(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				reset();
			} else if (args[i].equals("-d") || args[i].equals("-debug") || args[i].equals("-reset")) {
				CLIHelper.evaluateLogLevel(args[++i]);
			}
		}

		// Start server
		HttpServer server = startServer();
		Log.msg("\nWebservice started!\n" + info(), Log.NORMAL);

		// Setup
		try (Scanner sc = new Scanner(System.in)) {
			String read;
			do {
				de.foellix.aql.webservice.helper.Helper.newLine();
				read = sc.nextLine();
				if (read.equals(CMD_HELP)) {
					Log.msg("\nAvailable commands:\n- " + CMD_EXIT + " or " + CMD_QUIT
							+ " (to stop the web service)\n- " + CMD_USER
							+ " %NAME% %PASSWORD% (e.g. \"user FoelliX password123\")\n- " + CMD_PORT
							+ " %PORT% (e.g. \"port 8081\")\n- " + CMD_INFO + " (shows some basic information)\n- "
							+ CMD_BACKUP + " (backups the underlying AQL-System's storage)\n- " + CMD_RESET
							+ " (resets the underlying AQL-System)\n- " + CMD_TOOLS
							+ " (shows a list of all tools in the current config)\n- " + CMD_ACCOUNTS
							+ " (shows a table of all accounts)\n- " + CMD_TASKS
							+ " (shows the currently running tasks)", Log.NORMAL);
				} else if (read.equals(CMD_INFO)) {
					Log.msg(info(), Log.NORMAL);
				} else if (read.startsWith(CMD_USER)) {
					final String[] parts = read.split(" ");
					if (parts.length == 3) {
						Accounts.getInstance().addAccount(new Account(parts[1], parts[2]));
						Accounts.getInstance().store();
						Log.msg("Account created! Please finish configuring it in the \"accounts.csv\" file.",
								Log.NORMAL);
					} else {
						Log.warning("User not created! Invalid command!");
					}
				} else if (read.startsWith(CMD_PORT)) {
					server.shutdownNow();
					final String port = read.replaceAll(CMD_PORT, "").replaceAll(" ", "");
					Config.getInstance().getProperties().setProperty(Config.PORT, port);
					server = startServer();
					Log.msg("Port changed to: " + port, Log.NORMAL);
				} else if (read.equals(CMD_BACKUP)) {
					BackupAndReset.backup(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				} else if (read.equals(CMD_RESET)) {
					BackupAndReset.reset(new File(Config.getInstance().getProperty(Config.STORAGE_PATH)));
				} else if (read.equals(CMD_TOOLS)) {
					Log.msg(tools(), Log.NORMAL);
				} else if (read.equals(CMD_ACCOUNTS)) {
					Log.msg(Accounts.getInstance().toString().replace(",", ", "), Log.NORMAL);
				} else if (read.equals(CMD_TASKS)) {
					Log.msg(WebScheduler.getInstance().getRunningTasks(), Log.NORMAL);
				} else if (!(read.equals(CMD_EXIT) || read.equals(CMD_QUIT))) {
					Log.msg("Unknown command: " + read + " (Type \"" + CMD_HELP
							+ "\" to see a list of available commands)", Log.NORMAL);
				}
			} while (!(read.equals(CMD_EXIT) || read.equals(CMD_QUIT)));
		} catch (final Exception e) {
			de.foellix.aql.webservice.helper.Helper.error("Web service unexpectedly stopped! (" + e.getMessage() + ")");
		}

		// Stop server
		server.shutdown();
		Log.msg("Webservice stopped!", Log.NORMAL);
	}

	private static void initUrl(String port) {
		url = "http://0.0.0.0:" + port + "/AQL-WebService";
	}

	private static void reset() {
		final File uploadFolder = new File(Config.getInstance().getProperty(Config.UPLOAD_PATH));
		final boolean failed = FileHelper.deleteDir(uploadFolder);
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
		String remoteUrl;
		try {
			final String ip = de.foellix.aql.webservice.helper.Helper.getIp();
			remoteUrl = "http://" + ip + ":" + Config.getInstance().getProperty(Config.PORT) + "/AQL-WebService";
		} catch (final IOException e) {
			remoteUrl = "Could not be fetched";
		}
		final String localUrl = "http://localhost:" + Config.getInstance().getProperty(Config.PORT) + "/AQL-WebService";

		return "\nListening on port " + Config.getInstance().getProperty(Config.PORT) + "\nURLs:\n\t- Local: "
				+ localUrl + ",\n\t- Remote: " + remoteUrl + "\n\nConfig: "
				+ ConfigHandler.getInstance().getConfigFile().getAbsolutePath() + "\n";
	}

	private static String tools() {
		return (ConfigHandler.getInstance().getConfig().getTools() != null
				? "\n*** Tools ***\n" + Helper.toString(ConfigHandler.getInstance().getConfig().getTools().getTool())
						+ "\n"
				: "")
				+ (ConfigHandler.getInstance().getConfig().getPreprocessors() != null ? "\n*** Proprocessors ***\n"
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getPreprocessors().getTool()) + "\n"
						: "")
				+ (ConfigHandler.getInstance().getConfig().getOperators() != null ? "\n*** Operators ***\n"
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getOperators().getTool()) + "\n" : "")
				+ (ConfigHandler.getInstance().getConfig().getConverters() != null ? "\n*** Converters ***\n"
						+ Helper.toString(ConfigHandler.getInstance().getConfig().getConverters().getTool()) + "\n"
						: "");
	}

	public static HttpServer startServer() {
		initUrl(Config.getInstance().getProperty(Config.PORT));

		final ResourceConfig rc = new ResourceConfig();
		final Map<String, Object> initParams = new HashMap<>();
		initParams.put(ServerProperties.PROVIDER_PACKAGES, "de.foellix.aql.webservice");
		initParams.put(ServerProperties.PROVIDER_CLASSNAMES, "org.glassfish.jersey.media.multipart.MultiPartFeature");
		rc.addProperties(initParams);
		if (!Log.logIt(Log.DEBUG)) {
			Logger.getLogger("org.glassfish").setLevel(Level.OFF);
		}

		final HttpServer grizzly;
		if (sslActive()) {
			final SSLContextConfigurator ssl = new SSLContextConfigurator();
			ssl.setKeyStoreFile(Config.getInstance().getProperty(Config.KEYSTORE_PATH));
			ssl.setKeyStorePass(Config.getInstance().getProperty(Config.KEYSTORE_PASS));
			ssl.setTrustStoreFile(Config.getInstance().getProperty(Config.TRUSTSTORE_PATH));
			ssl.setTrustStorePass(Config.getInstance().getProperty(Config.TRUSTSTORE_PASS));
			grizzly = GrizzlyHttpServerFactory.createHttpServer(URI.create(url), rc, true,
					new SSLEngineConfigurator(ssl).setClientMode(false).setNeedClientAuth(false), false);
		} else {
			grizzly = GrizzlyHttpServerFactory.createHttpServer(URI.create(url), rc, false);
		}

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

	private static boolean sslActive() {
		if (Config.getInstance().getProperty(Config.KEYSTORE_PATH) != null
				&& !Config.getInstance().getProperty(Config.KEYSTORE_PATH).isEmpty()
				&& Config.getInstance().getProperty(Config.KEYSTORE_PASS) != null
				&& Config.getInstance().getProperty(Config.TRUSTSTORE_PATH) != null
				&& !Config.getInstance().getProperty(Config.TRUSTSTORE_PATH).isEmpty()
				&& Config.getInstance().getProperty(Config.TRUSTSTORE_PASS) != null) {
			final File keystore = new File(Config.getInstance().getProperty(Config.KEYSTORE_PATH));
			final File truststore = new File(Config.getInstance().getProperty(Config.TRUSTSTORE_PATH));
			if (keystore.exists() && truststore.exists()) {
				return true;
			}
		}
		return false;
	}
}