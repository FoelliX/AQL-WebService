package de.foellix.aql.webservice.scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Statistics {
	private static Statistics instance = new Statistics();

	long systemStart;
	long lastTaskStart;
	int queriesAsked;
	int queriesDone;
	int timeConsumed;

	private Statistics() {
		this.systemStart = System.currentTimeMillis();
		this.lastTaskStart = 0;
		this.queriesAsked = 0;
		this.queriesDone = 0;
		this.timeConsumed = 0;
	}

	public static Statistics getInstance() {
		return instance;
	}

	public void asked() {
		if (this.queriesAsked == this.queriesDone) {
			this.lastTaskStart = System.currentTimeMillis();
		}
		this.queriesAsked++;
	}

	public void done() {
		this.queriesDone++;
		this.timeConsumed += Math.max(1, (System.currentTimeMillis() - this.lastTaskStart) / 1000);
		if (this.queriesAsked < this.queriesDone) {
			this.lastTaskStart = System.currentTimeMillis();
		}
	}

	private String formatTimestamp(long timestamp) {
		if (timestamp == 0) {
			return "-";
		}
		final Date date = new Date(timestamp);
		final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy - HH:mm:ss");
		return format.format(date);
	}

	public String getSystemStart() {
		return formatTimestamp(this.systemStart);
	}

	public String getLastTaskStart() {
		return formatTimestamp(this.lastTaskStart);
	}

	public int getQueriesAsked() {
		return this.queriesAsked;
	}

	public int getQueriesDone() {
		return this.queriesDone;
	}

	public int getAverageQueryTimeInSeconds() {
		return this.timeConsumed / Math.max(1, this.queriesDone);
	}
}