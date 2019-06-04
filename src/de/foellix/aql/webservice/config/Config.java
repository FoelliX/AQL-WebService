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
	public static final String UPLOAD_PATH = "uploadPath";
	public static final String STORAGE_PATH = "storagePath";
	public static final String ANSWERS_PATH = "answersPath";
	public static final String TEMP_PATH = "tempPath";
	public static final String LOG_FILE = "logFile";
	public static final String TIMEOUT = "timeout";
	public static final String PREFER_LOADING = "preferLoading";
	public static final String QUEUE_START = "queueStart";
	public static final String ALLOWED_URLS = "allowedURLs";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String PORT = "port";
	public static final String BUFFER_TIME = "bufferTime";

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
			Log.error("Could not find/read " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
		}

		this.lock = new ReentrantLock();
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

	public void store() {
		try (OutputStream output = new FileOutputStream(PROPERTIES_FILE)) {
			this.properties.store(output, null);
		} catch (final Exception e) {
			Log.error("Could not write " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
		}
	}
}
