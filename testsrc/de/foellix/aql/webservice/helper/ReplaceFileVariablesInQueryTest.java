package de.foellix.aql.webservice.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.foellix.aql.datastructure.handler.QueryHandler;

class ReplaceFileVariablesInQueryTest {

	@Test
	void test1() {
		// Original query
		final String query = "CONNECT [ Flows IN App('%FILE_1%') FEATURING 'FlowDroid' ?, Flows IN App('%FILE_2%') FEATURING 'FlowDroid' ?, MATCH [ IntentSources IN App('%FILE_1%') ?, IntentSinks IN App('%FILE_1%') ?, IntentSources IN App('%FILE_2%') ?, IntentSinks IN App('%FILE_2%') ? ] ? ] ?";

		// Fake files
		final List<File> files = new ArrayList<>();
		final File file1 = new File("/somewhere/File1.apk");
		files.add(file1);
		final File file2 = new File("/somewhere/else/File2.apk");
		files.add(file2);

		// Expected final query
		String expected = "CONNECT [ Flows IN App('" + file1.getAbsolutePath()
				+ "') FEATURING 'FlowDroid' ?, Flows IN App('" + file2.getAbsolutePath()
				+ "') FEATURING 'FlowDroid' ?, MATCH [ IntentSources IN App('" + file1.getAbsolutePath()
				+ "') ?, IntentSinks IN App('" + file1.getAbsolutePath() + "') ?, IntentSources IN App('"
				+ file2.getAbsolutePath() + "') ?, IntentSinks IN App('" + file2.getAbsolutePath() + "') ? ] ? ] ?";
		expected = QueryHandler.parseQuery(expected).toString().replaceAll("\\\\", "/");

		// Final query check
		assertEquals(expected, Helper.replaceFileVariablesInQuery(query, files).replaceAll("\\\\", "/"));
	}
}
