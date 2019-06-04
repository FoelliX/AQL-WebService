package de.foellix.aql.webservice;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.foellix.aql.webservice.config.Config;

@Provider
public class CORSFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		// Find match
		final String[] targets = Config.getInstance().getProperty(Config.ALLOWED_URLS).replaceAll(" ", "").split(",");
		String match = null;
		if (request.getHeaders().get("Origin") == null) {
			return;
		}
		for (final String origin : request.getHeaders().get("Origin")) {
			for (final String target : targets) {
				if (origin.equals(target)) {
					match = target;
					break;
				}
			}
			if (match != null) {
				break;
			}
		}

		// Assign Headers
		if (match != null) {
			response.getHeaders().add("Access-Control-Allow-Origin", match);
			response.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
			response.getHeaders().add("Access-Control-Allow-Credentials", "true");
			response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
		}
	}
}