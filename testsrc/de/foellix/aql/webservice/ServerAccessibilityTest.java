package de.foellix.aql.webservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.MediaType;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.webservice.config.Account;
import de.foellix.aql.webservice.config.Accounts;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class ServerAccessibilityTest {
	private static final String TEST_ACCOUNT_USERNAME = "test";
	private static final String TEST_ACCOUNT_PASSWORD = "password123";

	private static HttpServer server;
	private static WebTarget target;
	private static Account testAccount;

	@BeforeAll
	public static void startServer() {
		server = WebService.startServer();
		ClientBuilder.newBuilder();
		final Client c = ClientBuilder.newClient();
		target = c.target(WebService.url);

		testAccount = new Account(TEST_ACCOUNT_USERNAME, TEST_ACCOUNT_PASSWORD, 0, 0);
		Accounts.getInstance().addAccount(testAccount);
	}

	@AfterAll
	public static void stopServer() {
		Accounts.getInstance().removeAccount(testAccount);

		server.shutdown();
	}

	@Test
	public void test01() {
		final String responseMsg = target.path("index.html").request().get(String.class);
		assertEquals(Pages.getIndex(), responseMsg);
	}

	@Test
	public void test02() {
		boolean noException = true;

		if (ConfigHandler.getInstance().getConfig() != null) {
			try {
				Unirest.config().connectTimeout(10000).socketTimeout(30000).setDefaultHeader("Accept",
						MediaType.TEXT_XML);
				final HttpRequestWithBody request = Unirest.post("http://localhost:8080/AQL-WebService/config")
						.queryString("username", testAccount.getUsername())
						.queryString("password", TEST_ACCOUNT_PASSWORD);
				final HttpResponse<String> responseString = request.asString();
				assertEquals(200, responseString.getStatus());
				assertEquals(
						ConfigHandler.toXML(
								new ConfigExporter(true, testAccount.getUsername(), TEST_ACCOUNT_PASSWORD).getConfig()),
						responseString.getBody());
				Unirest.shutDown();
			} catch (final Exception e) {
				noException = false;
			}
		}

		assertTrue(noException);
	}

	@Test
	public void test03() {
		boolean noException = true;

		try {
			Unirest.config().connectTimeout(10000).socketTimeout(30000).setDefaultHeader("Accept", MediaType.TEXT_XML);
			final HttpRequestWithBody request = Unirest.post("http://localhost:8080/AQL-WebService/config")
					.queryString("username", testAccount.getUsername()).queryString("password", "wrong_password");
			final HttpResponse<String> responseString = request.asString();
			assertEquals(200, responseString.getStatus());
			assertTrue(responseString.getBody().contains("<!-- " + RequestHandler.ERROR_MSG + " -->"));
			Unirest.shutDown();
		} catch (final Exception e) {
			noException = false;
		}

		assertTrue(noException);
	}
}