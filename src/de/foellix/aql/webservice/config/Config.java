package de.foellix.aql.webservice.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.foellix.aql.Log;

public class Config {
	public static final String PROPERTIES_FILE = "config.properties";

	public static final String AQL_PATH = "aqlPath";
	public static final String TIMEOUT = "timeout";
	public static final String QUEUE_START = "queueStart";
	public static final String ALLOWED_URLS = "allowedURLs";
	public static final String PORT = "port";
	public static final String BUFFER_TIME = "bufferTime";
	public static final String ASYNC_ENABLED = "asyncEnabled";
	public static final String KEYSTORE_PATH = "keystorePath";
	public static final String KEYSTORE_PASS = "keystorePass";
	public static final String TRUSTSTORE_PATH = "truststorePath";
	public static final String TRUSTSTORE_PASS = "truststorePass";

	// Below are relative to AQL path
	public static final String UPLOAD_PATH = "uploadPath";
	public static final String STORAGE_PATH = "storagePath";
	public static final String ANSWERS_PATH = "answersPath";
	public static final String TEMP_PATH = "tempPath";
	public static final String CONVERTER_PATH = "converterPath";
	public static final String LOG_FILE = "logFile";

	private final Properties properties;
	private int id;

	private Lock lock;

	private static Config instance = new Config();

	private Config() {
		this.properties = new Properties();

		try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
			this.properties.load(input);
			this.id = Integer.valueOf(getProperty(QUEUE_START));
		} catch (final IOException e) {
			init();
			de.foellix.aql.webservice.helper.Helper.warning(
					"Could not find/read " + PROPERTIES_FILE + ". Creating new one now!" + Log.getExceptionAppendix(e));
		}

		this.lock = new ReentrantLock();
	}

	private void init() {
		final File aqlDirectory = new File("aql");
		this.properties.setProperty(AQL_PATH, aqlDirectory.getAbsolutePath());
		this.properties.setProperty(TIMEOUT, "20m");
		this.properties.setProperty(QUEUE_START, "10000");
		this.properties.setProperty(ALLOWED_URLS, "http://localhost,https://localhost");
		this.properties.setProperty(PORT, "8080");
		this.properties.setProperty(BUFFER_TIME, "1000");
		this.properties.setProperty(ASYNC_ENABLED, "false");
		this.properties.setProperty(KEYSTORE_PATH, "");
		this.properties.setProperty(KEYSTORE_PASS, "");
		this.properties.setProperty(TRUSTSTORE_PATH, "");
		this.properties.setProperty(TRUSTSTORE_PASS, "");
		store();

		if (!aqlDirectory.mkdir()) {
			Log.warning("Could not create the AQL-System's directory (" + aqlDirectory.getAbsolutePath()
					+ ") - continuing without. Please consider adapting the \"" + AQL_PATH
					+ "\" in the config.properties file.");
		}
	}

	public static Config getInstance() {
		return instance;
	}

	public String getProperty(String name) {
		if (name.equals(UPLOAD_PATH)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "uploads");
			return temp.getAbsolutePath();
		} else if (name.equals(STORAGE_PATH)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "data/storage");
			return temp.getAbsolutePath();
		} else if (name.equals(ANSWERS_PATH)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "answers");
			return temp.getAbsolutePath();
		} else if (name.equals(TEMP_PATH)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "data/temp");
			return temp.getAbsolutePath();
		} else if (name.equals(CONVERTER_PATH)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "data/converter");
			return temp.getAbsolutePath();
		} else if (name.equals(LOG_FILE)) {
			final File temp = new File(this.properties.getProperty(AQL_PATH), "log.txt");
			return temp.getAbsolutePath();
		} else {
			return this.properties.getProperty(name);
		}
	}

	public Properties getProperties() {
		return this.properties;
	}

	public int increaseID() {
		this.lock.lock();
		this.id++;
		this.lock.unlock();
		this.properties.setProperty(QUEUE_START, String.valueOf(this.id));
		store();
		return this.id;
	}

	public int getID() {
		return this.id;
	}

	public boolean asyncEnabled() {
		return Boolean.valueOf(this.properties.getProperty(ASYNC_ENABLED)).booleanValue();
	}

	public void store() {
		try (OutputStream output = new FileOutputStream(PROPERTIES_FILE)) {
			this.properties.store(output, null);
		} catch (final Exception e) {
			de.foellix.aql.webservice.helper.Helper
					.error("Could not write " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
		}
	}
}