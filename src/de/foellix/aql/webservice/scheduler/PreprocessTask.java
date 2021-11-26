package de.foellix.aql.webservice.scheduler;

import java.io.File;
import java.util.List;

import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.webservice.helper.Helper;

public class PreprocessTask extends Task {
	private final String keyword;
	private final List<File> target;

	public PreprocessTask(String keyword, List<File> target, int id) {
		super(id);
		this.keyword = keyword;
		this.target = target;
	}

	public String createQuery() {
		final StringBuilder sb = new StringBuilder();

		for (final File app : this.target) {
			if (!sb.isEmpty()) {
				sb.append(", ");
			}
			sb.append(app.getAbsolutePath());
		}

		return KeywordsAndConstantsHelper.SOI_ARGUMENTS + " IN App('" + sb.toString() + "' | '" + this.keyword
				+ "') USES '" + Helper.getFakeToolForPreprocessing(this.keyword).getName() + "-"
				+ Helper.getFakeToolForPreprocessing(this.keyword).getVersion() + "' !";
	}

	public String getKeyword() {
		return this.keyword;
	}

	public List<File> getTarget() {
		return this.target;
	}
}