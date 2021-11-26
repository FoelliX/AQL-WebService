package de.foellix.aql.webservice.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.webservice.RequestHandler;

public class Accounts {
	private static final File ACCOUNTS_FILE = new File("accounts.csv");

	public static final Account INITIAL_ACCOUNT = new Account("aql", "S3cR3T!", 10, 10);

	private Map<String, Account> accounts = new HashMap<>();

	private static Accounts instance = new Accounts();

	private Accounts() {
		load();
	}

	public static Accounts getInstance() {
		return instance;
	}

	private void load() {
		this.accounts.clear();
		if (!ACCOUNTS_FILE.exists()) {
			this.accounts.put(INITIAL_ACCOUNT.getUsername(), INITIAL_ACCOUNT);
			store();
		} else {
			try {
				for (String line : Files.readAllLines(ACCOUNTS_FILE.toPath())) {
					line = Helper.replaceAllWhiteSpaceChars(line);
					if (line.startsWith("username,password,maxPerDay,maxQuestionsPerQuery,timestamp,counter")) {
						continue;
					}
					final String[] data = line.split(",");
					this.accounts.put(data[0], new Account(data[0], data[1], data[2], data[3], data[4], data[5]));
				}
			} catch (final IOException e) {
				de.foellix.aql.webservice.helper.Helper
						.error("Could not read accounts file!" + Log.getExceptionAppendix(e));
			}
		}
	}

	public void store() {
		try {
			Files.write(ACCOUNTS_FILE.toPath(), toString().getBytes());
		} catch (final IOException e) {
			de.foellix.aql.webservice.helper.Helper
					.error("Could not write accounts file!" + Log.getExceptionAppendix(e));
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(
				"username,password,maxPerDay,maxQuestionsPerQuery,timestamp,counter\n");
		for (final Account account : this.accounts.values()) {
			sb.append(account.toString()).append("\n");
		}
		return sb.toString();
	}

	public Account getAccount(String username, String password, String ip) {
		if (!password.isEmpty()) {
			password = HashHelper.sha256Hash(password);
		}

		// Free Account
		if (username.equals(ConfigHandler.FREE_ACCOUNT) && this.accounts.containsKey(ConfigHandler.FREE_ACCOUNT)
				&& (this.accounts.get(ConfigHandler.FREE_ACCOUNT).getPasswordHash().isEmpty()
						|| this.accounts.get(ConfigHandler.FREE_ACCOUNT).getPasswordHash().equals(password))) {
			username = ConfigHandler.FREE_ACCOUNT + "[" + ip + "]";
			if (!this.accounts.containsKey(username)) {
				this.accounts.put(username,
						new Account(username, this.accounts.get(ConfigHandler.FREE_ACCOUNT).getPasswordHash(),
								this.accounts.get(ConfigHandler.FREE_ACCOUNT).getMaxQueriesPerDay(),
								this.accounts.get(ConfigHandler.FREE_ACCOUNT).getQuestionsPerQuery()));
			}
			de.foellix.aql.webservice.helper.Helper.msg(ip + " associated with \"" + username + "\"");
			return this.accounts.get(username);
		}

		// Non-free Account
		if (!this.accounts.containsKey(username) || !this.accounts.get(username).getPasswordHash().equals(password)) {
			de.foellix.aql.webservice.helper.Helper.warning(
					RequestHandler.ERROR_USERNAME_OR_PASSWORD + " (Username: " + username + ", IP: " + ip + ")", true,
					true);
			return null;
		}
		return this.accounts.get(username);
	}

	public void addAccount(Account account) {
		this.accounts.put(account.getUsername(), account);
	}

	public void removeAccount(Account account) {
		this.accounts.remove(account.getUsername());
	}
}