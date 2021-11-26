package de.foellix.aql.webservice;

import de.foellix.aql.webservice.config.Config;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

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