package de.foellix.aql.webservice.scheduler;

public class QueryTask {
	private final int id;
	private final String query;
	private String status;

	public QueryTask(String query, int id) {
		this.id = id;
		this.query = query;
		this.status = "In Queue";
	}

	public int getId() {
		return this.id;
	}

	public String getQuery() {
		return this.query;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
