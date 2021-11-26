package de.foellix.aql.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.foellix.aql.config.Config;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.config.Converters;
import de.foellix.aql.config.Execute;
import de.foellix.aql.config.Operators;
import de.foellix.aql.config.Preprocessors;
import de.foellix.aql.config.Tool;
import de.foellix.aql.config.Tools;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.system.ToolSelector;
import de.foellix.aql.webservice.helper.Helper;

public class ConfigExporter {
	private static final int TYPE_ANALYSIS_TOOL = 1;
	private static final int TYPE_PREPROCESSOR = 2;
	private static final int TYPE_OPERATOR = 3;
	private static final int TYPE_CONVERTER = 4;

	private final String username;
	private final String password;
	private final String port;
	private String ip;

	public ConfigExporter(boolean local, String username, String password) {
		this.username = username;
		this.password = password;
		this.port = de.foellix.aql.webservice.config.Config.getInstance()
				.getProperty(de.foellix.aql.webservice.config.Config.PORT);
		if (local) {
			this.ip = "0.0.0.0";
		} else {
			try {
				this.ip = Helper.getIp();
			} catch (final IOException e) {
				this.ip = "0.0.0.0";
				de.foellix.aql.webservice.helper.Helper
						.error("IP could not be fetched (Continuing with " + this.ip + ").");
			}
		}
	}

	public Config getConfig() {
		final Config config = ConfigHandler.getInstance().getConfig();
		final Config exportConfig = new Config();

		// General
		if (config.getAndroidBuildTools() != null && !config.getAndroidBuildTools().isBlank()) {
			exportConfig.setAndroidBuildTools("/path/to/Android/build-tools");
		}
		if (config.getAndroidPlatforms() != null && !config.getAndroidPlatforms().isBlank()) {
			exportConfig.setAndroidPlatforms("/path/to/Android/platforms");
		}
		exportConfig.setMaxMemory(config.getMaxMemory());

		// Analysis Tools
		if (config.getTools() != null && !config.getTools().getTool().isEmpty()) {
			final Tools tools = new Tools();
			tools.getTool().addAll(getTools(config.getTools().getTool(), TYPE_ANALYSIS_TOOL));
			exportConfig.setTools(tools);
		}

		// Preprocessors
		if (config.getPreprocessors() != null && !config.getPreprocessors().getTool().isEmpty()) {
			final Preprocessors preprocessors = new Preprocessors();
			preprocessors.getTool().addAll(getTools(config.getPreprocessors().getTool(), TYPE_PREPROCESSOR));
			exportConfig.setPreprocessors(preprocessors);
		}

		// Operators
		if (config.getOperators() != null && !config.getOperators().getTool().isEmpty()) {
			final Operators operators = new Operators();
			operators.getTool().addAll(getTools(config.getOperators().getTool(), TYPE_OPERATOR));
			exportConfig.setOperators(operators);
		}

		// Converters
		if (config.getConverters() != null && !config.getConverters().getTool().isEmpty()) {
			final Converters converters = new Converters();
			converters.getTool().addAll(getTools(config.getConverters().getTool(), TYPE_CONVERTER));
			exportConfig.setConverters(converters);
		}

		Helper.newLine();

		return exportConfig;
	}

	private Collection<Tool> getTools(List<Tool> tools, int type) {
		final List<Tool> exportTools = new ArrayList<>();
		for (final Tool tool : tools) {
			// Basics
			final Tool exportTool = new Tool();
			exportTool.setName(tool.getName());
			exportTool.setVersion(tool.getVersion());
			exportTool.setExternal(true);
			exportTool.setPath(".");
			exportTool.setQuestions(tool.getQuestions());
			exportTool.setTimeout(tool.getTimeout());

			// Execute
			final Execute execute = new Execute();
			if (type == TYPE_PREPROCESSOR) {
				execute.setUrl(getURLPreprocessor(tool));
			} else if (type == TYPE_OPERATOR) {
				execute.setUrl(getURLOperator(tool));
			} else if (type == TYPE_CONVERTER) {
				execute.setUrl(getURLConverter(tool));
			} else {
				execute.setUrl(getURLAnalysisTool(tool));
			}
			execute.setUsername(this.username);
			execute.setPassword(this.password);
			exportTool.setExecute(execute);

			// Hooks
			reportUnexportableHooks(tool);

			// Priority
			exportTool.getPriority().addAll(tool.getPriority());

			// Add tool
			exportTools.add(exportTool);
		}

		// Remove redundant tools
		final List<Tool> toRemove = new ArrayList<>();
		for (int i = 0; i < exportTools.size() - 1; i++) {
			final Tool tool1 = exportTools.get(i);
			for (int j = i + 1; j < exportTools.size(); j++) {
				final Tool tool2 = exportTools.get(j);
				if (EqualsHelper.equals(tool1, tool2)) {
					toRemove.add(tool1);
				}
			}
		}
		if (!toRemove.isEmpty()) {
			exportTools.removeAll(toRemove);
			de.foellix.aql.webservice.helper.Helper.warning(
					toRemove.size() + " tools were looking identical when exporting. Exporting each tool only once.",
					false);
		}

		return exportTools;
	}

	private String getURLConverter(Tool tool) {
		return getURLTool(tool);
	}

	private String getURLOperator(Tool tool) {
		return getURLTool(tool);
	}

	private String getURLPreprocessor(Tool tool) {
		return "http://" + this.ip + ":" + this.port + "/AQL-WebService/ask/" + tool.getQuestions();
	}

	private String getURLAnalysisTool(Tool tool) {
		return getURLTool(tool);
	}

	private String getURLTool(Tool tool) {
		String url = "http://" + this.ip + ":" + this.port + "/AQL-WebService/ask";
		if (ToolSelector.selectConverter(tool) != null) {
			url += "AQL";
		} else if (!tool.getExecute().getResult().contains(".")
				|| tool.getExecute().getResult().toLowerCase().endsWith(".raw")) {
			url += "RAW";
		} else if (tool.getExecute().getResult().toLowerCase().endsWith(".apk")
				|| tool.getExecute().getResult().toLowerCase().endsWith(".txt")) {
			url += "File";
		} else if (tool.getExecute().getResult().toLowerCase().endsWith(".xml")) {
			url += "AQL";
		} else {
			url += "AQL";
			de.foellix.aql.webservice.helper.Helper.warning("Unknown answer type for "
					+ de.foellix.aql.helper.Helper.getQualifiedName(tool) + ". Assuming AQL-Answer!");
		}
		return url;
	}

	private void reportUnexportableHooks(Tool tool) {
		final StringBuilder sb = new StringBuilder("Cannot export \"");
		final int length = sb.length();
		final String qualifiedName = de.foellix.aql.helper.Helper.getQualifiedName(tool);
		if (tool.getRunOnAbort() != null && !tool.getRunOnAbort().isEmpty()) {
			sb.append("RunOnAbort");
		}
		if (tool.getRunOnEntry() != null && !tool.getRunOnEntry().isEmpty()) {
			if (sb.length() > length) {
				sb.append(", ");
			}
			sb.append("RunOnEntry");
		}
		if (tool.getRunOnExit() != null && !tool.getRunOnExit().isEmpty()) {

			if (sb.length() > length) {
				sb.append(", ");
			}
			sb.append("RunOnExit");
		}
		if (tool.getRunOnFail() != null && !tool.getRunOnFail().isEmpty()) {
			if (sb.length() > length) {
				sb.append(", ");
			}
			sb.append("RunOnFail");
		}
		if (tool.getRunOnSuccess() != null && !tool.getRunOnSuccess().isEmpty()) {
			if (sb.length() > length) {
				sb.append(", ");
			}
			sb.append("RunOnSuccess");
		}
		if (sb.length() > length) {
			sb.append("\" for " + qualifiedName);
			de.foellix.aql.webservice.helper.Helper.msg(sb.toString(), false);
		}
	}
}