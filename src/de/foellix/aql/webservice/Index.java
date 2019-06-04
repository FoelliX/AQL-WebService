package de.foellix.aql.webservice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import de.foellix.aql.Log;

public class Index {
	private static String indexStr = null;
	private static File indexFile = null;

	public static String htmlString() {
		if (indexStr == null) {
			final File f = new File("index.html");
			try {
				final byte[] bytes = Files.readAllBytes(f.toPath());
				indexStr = new String(bytes, StandardCharsets.UTF_8);
			} catch (final IOException e) {
				Log.error("Could not find/read " + f.getAbsolutePath() + ". (" + e.getMessage() + ")");
			}
		}
		return indexStr;
	}

	public static File htmlFile() {
		if (indexFile == null) {
			indexFile = new File("index.html");
		}
		return indexFile;
	}
}
