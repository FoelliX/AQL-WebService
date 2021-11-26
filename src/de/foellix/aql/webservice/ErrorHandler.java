package de.foellix.aql.webservice;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorHandler implements ExceptionMapper<Throwable> {
	@Override
	public Response toResponse(Throwable e) {
		if (e instanceof WebApplicationException) {
			return Response.status(((WebApplicationException) e).getResponse().getStatus())
					.entity(Pages.getError(e.getMessage())).type(MediaType.TEXT_HTML).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Pages.getError(e.getMessage()))
					.type(MediaType.TEXT_HTML).build();
		}
	}
}