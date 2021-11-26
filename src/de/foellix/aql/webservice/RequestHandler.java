package de.foellix.aql.webservice;

import java.io.File;
import java.util.List;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.ui.gui.viewer.web.WebRepresentation;
import de.foellix.aql.webservice.config.Account;
import de.foellix.aql.webservice.config.Accounts;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.helper.Helper;
import de.foellix.aql.webservice.scheduler.PreprocessTask;
import de.foellix.aql.webservice.scheduler.QueryTask;
import de.foellix.aql.webservice.scheduler.WebScheduler;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class RequestHandler {
	public static final String ERROR_USERNAME_OR_PASSWORD = "Username and/or password incorrect!";
	public static final String ERROR_QUERY_MAX = "No more queries allowed today for this account!";
	public static final String ERROR_QUESTION_MAX = "The query exceeds the number of questions allowed per query for this account!";
	public static final String ERROR_MSG = KeywordsAndConstantsHelper.FEEDBACK_ANSWER_STRING + "\n- "
			+ ERROR_USERNAME_OR_PASSWORD + "\n- " + ERROR_QUERY_MAX + "\n- " + ERROR_QUESTION_MAX;
	private static final String ERROR_ASYNC_DISABLED = "Forbidden! (AQL-WebService asynchronous mode is disabled)";
	private static final String READY_ASYNC_ENABLED = "Ready! (AQL-WebService asynchronous mode is enabled)";

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String indexString(@Context Request request) {
		Helper.msg(request.getRemoteAddr() + " requested the index page!");

		return Pages.getIndex();
	}

	@GET
	@Path("index.html")
	@Produces(MediaType.TEXT_HTML)
	public String indexHTML(@Context Request request) {
		return indexString(request);
	}

	/*
	 * Sync. mode
	 */
	@POST
	@Path("askAQL")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public String askAQL(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> appBodies,
			@FormDataParam("files") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password) {
		Helper.msg(request.getRemoteAddr() + " asked question (for AQL-Answer): " + query);

		// Ask directly
		Log.emptyLine();
		String answer;
		final Account account = Accounts.getInstance().getAccount(username, password, request.getRemoteAddr());
		if (account != null && account.allowedToQuery(request.getRemoteAddr())
				&& account.queryAllowed(query, request.getRemoteAddr())) {
			final QueryTask queryTask = Helper.interpretQuery(account, query, appBodies);
			answer = AnswerHandler.createXMLString(WebScheduler.getInstance().ask(queryTask, timeout));
		} else {
			answer = ERROR_MSG;
		}
		Helper.newLine();
		return answer;
	}

	@POST
	@Path("askFile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.WILDCARD)
	public Response askFile(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> appBodies,
			@FormDataParam("files") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password) {
		Helper.msg(request.getRemoteAddr() + " asked question (for File): " + query);

		// Ask directly
		Log.emptyLine();
		File answer;
		final Account account = Accounts.getInstance().getAccount(username, password, request.getRemoteAddr());
		if (account != null && account.allowedToQuery(request.getRemoteAddr())
				&& account.queryAllowed(query, request.getRemoteAddr())) {
			final QueryTask queryTask = Helper.interpretQuery(account, query, appBodies);
			answer = (File) WebScheduler.getInstance().ask(queryTask, timeout);
		} else {
			answer = null;
		}
		Helper.newLine();
		if (answer == null) {
			return Response.status(403, ERROR_MSG).build();
		} else {
			return Response.ok(answer).header("Content-Disposition", "attachment; filename=" + answer.getName())
					.build();
		}
	}

	@POST
	@Path("askRAW")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public String askRAW(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> appBodies,
			@FormDataParam("files") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password) {
		Helper.msg(request.getRemoteAddr() + " asked question (for RAW data): " + query);

		// Ask directly
		Log.emptyLine();
		String answer;
		final Account account = Accounts.getInstance().getAccount(username, password, request.getRemoteAddr());
		if (account != null && account.allowedToQuery(request.getRemoteAddr())
				&& account.queryAllowed(query, request.getRemoteAddr())) {
			final QueryTask queryTask = Helper.interpretQuery(account, query, appBodies);
			answer = (String) WebScheduler.getInstance().ask(queryTask, timeout);
		} else {
			answer = ERROR_MSG;
		}
		Helper.newLine();
		return answer;
	}

	@POST
	@Path("ask/{keyword}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.WILDCARD)
	public Response preprocess(@Context Request request, @FormDataParam("files") List<FormDataBodyPart> appBodies,
			@FormDataParam("files") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password,
			@PathParam("keyword") String keyword) {
		Helper.msg(request.getRemoteAddr() + " issues preprocessor: " + keyword);

		// Ask directly
		Log.emptyLine();
		File preprocessedFile;
		final Account account = Accounts.getInstance().getAccount(username, password, request.getRemoteAddr());
		if (account != null && account.allowedToQuery(request.getRemoteAddr())) {
			final PreprocessTask preprocessTask = Helper.interpretPrepossesorTask(keyword, appBodies);
			preprocessedFile = WebScheduler.getInstance().preprocess(preprocessTask, timeout);
		} else {
			preprocessedFile = null;
		}
		Helper.newLine();
		if (preprocessedFile == null) {
			return Response.status(403, ERROR_MSG).build();
		} else {
			return Response.ok(preprocessedFile)
					.header("Content-Disposition", "attachment; filename=" + preprocessedFile.getName()).build();
		}
	}

	/*
	 * Async. mode
	 */
	@GET
	@Path("ready")
	@Produces(MediaType.TEXT_PLAIN)
	public Response ready(@Context Request request) {
		if (Config.getInstance().asyncEnabled()) {
			return Response.ok(READY_ASYNC_ENABLED, MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(403, ERROR_ASYNC_DISABLED).build();
		}
	}

	@POST
	@Path("query")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public String query(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> fileBodies,
			@FormDataParam("files") FormDataContentDisposition fileInfo) {
		if (Config.getInstance().asyncEnabled()) {
			Helper.msg(request.getRemoteAddr() + " sent query: " + query);

			// Put task into queue
			Log.emptyLine();
			WebScheduler.getInstance().scheduleTask(Helper.interpretQuery(null, query, fileBodies));
			Helper.newLine();
			return String.valueOf(Config.getInstance().getID());
		} else {
			return ERROR_ASYNC_DISABLED;
		}
	}

	@GET
	@Path("answer/{id}")
	public Response answer(@Context Request request, @PathParam("id") int id) {
		if (Config.getInstance().asyncEnabled()) {
			Helper.msg(request.getRemoteAddr() + " answer requested: " + id);

			// Get answer and reply
			final File answer = new File(Config.getInstance().getProperty(Config.ANSWERS_PATH),
					"answer_" + id + ".xml");
			if (answer.exists()) {
				return Response.ok(answer, MediaType.APPLICATION_XML).build();
			} else {
				return Response.ok("Not available!", MediaType.TEXT_PLAIN).build();
			}
		} else {
			return Response.status(403, ERROR_ASYNC_DISABLED).build();
		}
	}

	@GET
	@Path("webanswer/{id}")
	public Response webanswer(@Context Request request, @PathParam("id") int id) {
		if (Config.getInstance().asyncEnabled()) {
			Helper.msg(request.getRemoteAddr() + " webanswer requested: " + id);

			// Get web-answer and reply
			final File answer = new File(Config.getInstance().getProperty(Config.ANSWERS_PATH),
					"answer_" + id + ".xml");
			if (answer.exists()) {
				final WebRepresentation webAnswer = new WebRepresentation();
				final String webAnswerString = webAnswer.toJson(AnswerHandler.parseXML(answer));
				return Response.ok(webAnswerString, MediaType.APPLICATION_JSON).build();
			} else {
				return Response.ok("Not available!", MediaType.TEXT_PLAIN).build();
			}
		} else {
			return Response.status(403, ERROR_ASYNC_DISABLED).build();
		}
	}

	@GET
	@Path("status/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public String status(@Context Request request, @PathParam("id") int id) {
		if (Config.getInstance().asyncEnabled()) {
			Helper.msg(request.getRemoteAddr() + " status requested: " + id);

			// Get status and reply
			final QueryTask task = WebScheduler.getInstance().getTask(id);
			if (task == null) {
				return "Not available!";
			}
			return task.getStatus();
		} else {
			return ERROR_ASYNC_DISABLED;
		}
	}

	/*
	 * Misc
	 */
	@POST
	@Path("config")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public String getConfig(@Context Request request, @QueryParam("username") String username,
			@QueryParam("password") String password) {
		Helper.msg(request.getRemoteAddr() + " requested configuration!");

		String configStr;
		final Account account = Accounts.getInstance().getAccount(username, password, request.getRemoteAddr());
		if (account != null && account.allowedToQuery(request.getRemoteAddr())) {
			configStr = ConfigHandler
					.toXML(new ConfigExporter(local(request), account.getUsername(), password).getConfig());
		} else {
			configStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<!-- " + ERROR_MSG
					+ " -->\n<config />";
		}
		return configStr;
	}

	private boolean local(Request request) {
		return (request.getRemoteAddr().equals("127.0.0.1") || request.getRemoteAddr().equals("0.0.0.0"));
	}
}