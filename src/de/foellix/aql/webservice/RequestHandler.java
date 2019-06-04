package de.foellix.aql.webservice;

import java.io.File;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.ui.gui.viewer.web.WebRepresentation;
import de.foellix.aql.webservice.config.Config;
import de.foellix.aql.webservice.helper.Helper;
import de.foellix.aql.webservice.scheduler.PreprocessTask;
import de.foellix.aql.webservice.scheduler.QueryTask;
import de.foellix.aql.webservice.scheduler.WebScheduler;

@Path("/")
public class RequestHandler {
	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public String indexString(@Context Request request) {
		Helper.log(request.getRemoteAddr() + " requested the index page!", Log.NORMAL);

		return Index.htmlString();
	}

	@GET
	@Path("index.html")
	@Produces(MediaType.TEXT_HTML)
	public String indexHTML(@Context Request request) {
		return indexString(request);
	}

	@POST
	@Path("ask")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_XML)
	public String ask(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> appBodies,
			@FormDataParam("files") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password) {
		Helper.log(request.getRemoteAddr() + " asked question: " + query, Log.NORMAL);

		// Ask directly
		System.out.println("");
		String answer;
		if (Config.getInstance().getProperty(Config.USERNAME).equals(username)
				&& Config.getInstance().getProperty(Config.PASSWORD).equals(password)) {
			final QueryTask queryTask = Helper.interpretQuery(query, appBodies);
			answer = AnswerHandler.createXMLString(WebScheduler.getInstance().ask(queryTask, timeout));
		} else {
			answer = "Username and/or password incorrect.";
		}
		Log.msg("> ", Log.NORMAL, false);
		return answer;
	}

	@POST
	@Path("ask/{keyword}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("application/vnd.android.package-archive")
	public Response preprocess(@Context Request request, @FormDataParam("app") List<FormDataBodyPart> appBodies,
			@FormDataParam("app") FormDataContentDisposition appInfo, @FormDataParam("timeout") long timeout,
			@FormDataParam("username") String username, @FormDataParam("password") String password,
			@PathParam("keyword") String keyword) {
		Helper.log(request.getRemoteAddr() + " issues preprocessor: " + keyword, Log.NORMAL);

		// Ask directly
		System.out.println("");
		File preprocessedFile;
		if (Config.getInstance().getProperty(Config.USERNAME).equals(username)
				&& Config.getInstance().getProperty(Config.PASSWORD).equals(password)) {
			final PreprocessTask preprocessTask = Helper.interpretPrepossesorTask(keyword, appBodies);
			preprocessedFile = WebScheduler.getInstance().preprocess(preprocessTask, timeout);
		} else {
			preprocessedFile = null;
		}
		Log.msg("> ", Log.NORMAL, false);
		return Response.ok(preprocessedFile)
				.header("Content-Disposition", "attachment; filename=" + preprocessedFile.getName()).build();
	}

	@POST
	@Path("query")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public String query(@Context Request request, @FormDataParam("query") String query,
			@FormDataParam("files") List<FormDataBodyPart> fileBodies,
			@FormDataParam("files") FormDataContentDisposition fileInfo) {
		Helper.log(request.getRemoteAddr() + " sent query: " + query, Log.NORMAL);

		// Put task into queue
		System.out.println("");
		WebScheduler.getInstance().scheduleTask(Helper.interpretQuery(query, fileBodies));
		Log.msg("> ", Log.NORMAL, false);
		return String.valueOf(Config.getInstance().getID());
	}

	@GET
	@Path("answer/{id}")
	public Response answer(@Context Request request, @PathParam("id") int id) {
		Helper.log(request.getRemoteAddr() + " answer requested: " + id, Log.NORMAL);

		// Get answer and reply
		final File answer = new File(Config.getInstance().getProperty(Config.ANSWERS_PATH), "answer_" + id + ".xml");
		if (answer.exists()) {
			return Response.ok(answer, MediaType.APPLICATION_XML).build();
		} else {
			return Response.ok("Not available!", MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	@Path("webanswer/{id}")
	public Response webanswer(@Context Request request, @PathParam("id") int id) {
		Helper.log(request.getRemoteAddr() + " webanswer requested: " + id, Log.NORMAL);

		// Get web-answer and reply
		final File answer = new File(Config.getInstance().getProperty(Config.ANSWERS_PATH), "answer_" + id + ".xml");
		if (answer.exists()) {
			final WebRepresentation webAnswer = new WebRepresentation();
			final String webAnswerString = webAnswer.toJson(AnswerHandler.parseXML(answer));
			return Response.ok(webAnswerString, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.ok("Not available!", MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	@Path("status/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public String status(@Context Request request, @PathParam("id") int id) {
		Helper.log(request.getRemoteAddr() + " status requested: " + id, Log.NORMAL);

		// Get status and reply
		final QueryTask task = WebScheduler.getInstance().getTask(id);
		if (task == null) {
			return "Not available!";
		}
		return task.getStatus();
	}
}