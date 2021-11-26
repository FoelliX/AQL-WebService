package de.foellix.aql.webservice.scheduler;

public class Task {
	public static final String IN_QUEUE = "In Queue";
	public static final String IN_PROGRESS = "In Progress";
	public static final String DONE = "Done";

	int id;
	String status;

	public Task(int id) {
		this.id = id;
		this.status = Task.IN_QUEUE;
	}

	public int getId() {
		return this.id;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}