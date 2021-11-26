package de.foellix.aql.webservice.scheduler;

public class QueryTask extends Task {
	private final String query;

	public QueryTask(String query, int id) {
		super(id);
		this.query = query;
	}

	public String getQuery() {
		return this.query;
	}
}