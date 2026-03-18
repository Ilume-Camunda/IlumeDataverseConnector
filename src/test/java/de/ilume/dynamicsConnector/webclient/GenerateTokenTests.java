package de.ilume.dynamicsConnector.webclient;

import de.ilume.dynamicsConnector.service.GenerateTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.springframework.http.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;

class GenerateTokenTests {

    private GenerateTokenService generateTokenService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws IOException {
        // Start the MockWebServer
        generateTokenService = new GenerateTokenService();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Shut down the MockWebServer after tests
        mockWebServer.shutdown();
    }

    @Test
    void testGetToken() throws Exception {
        // Prepare test data
        String baseUrl = "https://api.example.com/.";
        String clientId = "clientId";
        String clientSecret = "clientSecret";
        String scope = "default";
        String accessTokenUrl = mockWebServer.url("/token").toString();

        // Prepare mock response
        String mockResponseBody = "{\"access_token\":\"mocked-access-token\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockResponseBody));

        // Call the method under test
        Mono<String> tokenMono = generateTokenService.getToken(baseUrl, clientId, clientSecret, scope, accessTokenUrl);

        // Verify the result
        StepVerifier.create(tokenMono)
                .assertNext(token -> {
                    assertNotNull(token);
                    assertEquals("mocked-access-token", token);
                })
                .verifyComplete();

        // Verify the request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/token", recordedRequest.getPath());
        assertTrue(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE)
                .startsWith("application/x-www-form-urlencoded"));
        assertEquals("grant_type=client_credentials&" +
                "client_id=clientId&" +
                "client_secret=clientSecret&" +
                "scope=https%3A%2F%2Fapi.example.com%2F.default", recordedRequest.getBody().readUtf8());
    }
}
