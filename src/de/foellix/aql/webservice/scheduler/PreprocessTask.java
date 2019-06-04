package de.foellix.aql.webservice.scheduler;

import java.io.File;

import de.foellix.aql.webservice.helper.Helper;

public class PreprocessTask {
	final int id;
	final String keyword;
	final File target;
	String status;

	public PreprocessTask(String keyword, File target, int id) {
		this.id = id;
		this.keyword = keyword;
		this.target = target;
		this.status = "In Queue";
	}

	public String createQuery() {
		return "Permissions IN App('" + this.target.getAbsolutePath() + "' | '" + this.keyword + "') FEATURING '"
				+ Helper.getFakeToolForPreprocessing().getName() + "' ?";
	}

	public int getId() {
		return this.id;
	}

	public String getKeyword() {
		return this.keyword;
	}

	public File getTarget() {
		return this.target;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
