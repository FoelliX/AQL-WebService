package de.foellix.aql.webservice.config;

import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.webservice.RequestHandler;

public class Account {
	private String username;
	private String passwordHash;
	private int maxQueriesPerDay;
	private int questionsPerQuery;
	private long dailyQueryTimestamp;
	private int dailyQueryCounter;

	public Account(String username, String password) {
		this(username, password, 10, 10);
	}

	public Account(String username, String password, int maxQueriesPerDay, int questionsPerQuery) {
		this.username = username;
		this.passwordHash = HashHelper.sha256Hash(password);
		this.maxQueriesPerDay = maxQueriesPerDay;
		this.questionsPerQuery = questionsPerQuery;
		this.dailyQueryTimestamp = 0;
		this.dailyQueryCounter = 0;
	}

	public Account(String username, String passwordHash, String maxQueriesPerDay, String questionsPerQuery,
			String timestamp, String counter) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.maxQueriesPerDay = Integer.valueOf(maxQueriesPerDay).intValue();
		this.questionsPerQuery = Integer.valueOf(questionsPerQuery).intValue();
		this.dailyQueryTimestamp = Long.valueOf(timestamp).longValue();
		this.dailyQueryCounter = Integer.valueOf(counter).intValue();
	}

	public boolean allowedToQuery(String ip) {
		if (this.maxQueriesPerDay <= 0) {
			return true;
		} else {
			if ((System.currentTimeMillis() - 86400000l) > this.dailyQueryTimestamp) {
				this.dailyQueryTimestamp = System.currentTimeMillis();
				this.dailyQueryCounter = 0;
			}
			if (this.dailyQueryCounter < this.maxQueriesPerDay) {
				return true;
			} else {
				de.foellix.aql.webservice.helper.Helper.warning(
						RequestHandler.ERROR_QUERY_MAX + " (Username: " + this.username + ", IP: " + ip + ")", true,
						true);
				return false;
			}
		}
	}

	public boolean queryAllowed(String query, String ip) {
		if (this.questionsPerQuery <= 0 || query.replaceAll("[^.!?]+", "").length() <= this.questionsPerQuery) {
			this.dailyQueryCounter++;
			Accounts.getInstance().store();
			return true;
		} else {
			de.foellix.aql.webservice.helper.Helper.warning(RequestHandler.ERROR_QUESTION_MAX + " (Query: " + query
					+ ", Username: " + this.username + ", IP: " + ip + ")", true, true);
			return false;
		}
	}

	public String getUsername() {
		return this.username;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public int getMaxQueriesPerDay() {
		return this.maxQueriesPerDay;
	}

	public int getQuestionsPerQuery() {
		return this.questionsPerQuery;
	}

	@Override
	public String toString() {
		return this.username + "," + this.passwordHash + "," + this.maxQueriesPerDay + "," + this.questionsPerQuery
				+ "," + this.dailyQueryTimestamp + "," + this.dailyQueryCounter;
	}
}