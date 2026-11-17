
package dev.uptime.monitor;

import com.sun.net.httpserver.HttpServer;
import dev.uptime.monitor.dto.SiteCheckResponse;
import dev.uptime.monitor.service.MonitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class UptimeMonitorApplicationTests {

	private HttpServer server;
	private MonitorService monitorService;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(
				new InetSocketAddress("127.0.0.1", 0), 0
		);

		server.createContext("/ok", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});

		server.createContext("/not-found", exchange -> {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});

		server.start();

		monitorService = new MonitorService();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	private String getBaseUrl() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@Test
	void shouldReturnOnlineForStatus200() {
		String url = getBaseUrl() + "/ok";

		SiteCheckResponse result =
				monitorService.checkSite(url);

		assertTrue(result.online());
		assertEquals(200, result.statusCode());
		assertEquals("OK", result.message());
	}

	@Test
	void shouldReturnOfflineForStatus404() {
		String url = getBaseUrl() + "/not-found";

		SiteCheckResponse result =
				monitorService.checkSite(url);

		assertFalse(result.online());
		assertEquals(404, result.statusCode());
		assertEquals("HTTP Error", result.message());
	}

	@Test
	void shouldReturnInvalidUrlForIncorrectAddress() {
		SiteCheckResponse result =
				monitorService.checkSite("not-a-url");

		assertFalse(result.online());
		assertEquals(0, result.statusCode());
		assertEquals("Invalid URL", result.message());
	}

	@Test
	void shouldReturnConnectionErrorWhenServerIsUnavailable() {
		int port = server.getAddress().getPort();

		server.stop(0);

		String url = "http://127.0.0.1:" + port;

		SiteCheckResponse result =
				monitorService.checkSite(url);

		assertFalse(result.online());
		assertEquals(0, result.statusCode());
		assertEquals("Connection error", result.message());
	}
}
