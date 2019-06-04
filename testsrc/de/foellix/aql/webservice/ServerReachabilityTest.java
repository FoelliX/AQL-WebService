package de.foellix.aql.webservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ServerReachabilityTest {
	private static HttpServer server;
	private static WebTarget target;

	@BeforeAll
	public static void startServer() {
		server = WebService.startServer();
		final Client c = ClientBuilder.newClient();
		target = c.target(WebService.URL);
	}

	@AfterAll
	public static void stopServer() {
		server.stop();
	}

	@Test
	public void test() {
		final String responseMsg = target.path("index.html").request().get(String.class);
		assertEquals(Index.htmlString(), responseMsg);
	}
}
