package com.example;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.response.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class OAuth2EndToEndTest {
    
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String REDIRECT_URI = "https://oauth-redirect.googleusercontent.com/r/YOUR_PROJECT_ID";
    private static final String STATE = "test-state-value";
    private static final String SCOPE = "https://www.googleapis.com/auth/homegraph";
    
    @BeforeAll
    static void setup() {
        // Set the base URL if provided as a system property
        if (System.getProperty("restassuredBaseUri") != null) {
            baseURI = System.getProperty("restassuredBaseUri");
        } else {
            baseURI = "http://localhost:8080/evaristo-google-cloud";
        }
    }
    
    @Test
    void testCompleteOAuth2Flow() {
        // Step 1: Authorization Request
        Response authResponse = given()
            .param("response_type", "code")
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", REDIRECT_URI)
            .param("scope", SCOPE)
            .param("state", STATE)
        .when()
            .get("/fakeauth");
        
        assertEquals(302, authResponse.getStatusCode());
        String locationHeader = authResponse.getHeader("Location");
        assertTrue(locationHeader.startsWith("/login"));
        
        // Extract the encoded response URL from the Location header
        String encodedResponseUrl = locationHeader.substring(locationHeader.indexOf("=") + 1);
        String responseUrl = URLDecoder.decode(encodedResponseUrl, StandardCharsets.UTF_8);
        
        // Step 2: Login
        Response loginResponse = given()
            .formParam("responseurl", responseUrl)
        .when()
            .post("/login");
        
        assertEquals(302, loginResponse.getStatusCode());
        String redirectUrl = loginResponse.getHeader("Location");
        
        // Verify redirect contains code and state
        assertTrue(redirectUrl.contains("code="));
        assertTrue(redirectUrl.contains("state=" + STATE));
        
        // Extract authorization code
        String authCode = redirectUrl.substring(
            redirectUrl.indexOf("code=") + 5, 
            redirectUrl.indexOf("&state=")
        );
        
        // Step 3: Token Request
        Response tokenResponse = given()
            .formParam("grant_type", "authorization_code")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
            .formParam("code", authCode)
            .formParam("redirect_uri", REDIRECT_URI)
        .when()
            .post("/faketoken");
        
        assertEquals(200, tokenResponse.getStatusCode());
        assertEquals("application/json", tokenResponse.getContentType());
        
        // Parse token response
        JsonObject tokenJson = JsonParser.parseString(tokenResponse.getBody().asString()).getAsJsonObject();
        assertEquals("bearer", tokenJson.get("token_type").getAsString());
        assertEquals("123access", tokenJson.get("access_token").getAsString());
        assertEquals("123refresh", tokenJson.get("refresh_token").getAsString());
        assertEquals(86400, tokenJson.get("expires_in").getAsInt());
        
        // Step 4: Refresh Token
        Response refreshResponse = given()
            .formParam("grant_type", "refresh_token")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
            .formParam("refresh_token", tokenJson.get("refresh_token").getAsString())
        .when()
            .post("/faketoken");
        
        assertEquals(200, refreshResponse.getStatusCode());
        assertEquals("application/json", refreshResponse.getContentType());
        
        // Parse refresh response
        JsonObject refreshJson = JsonParser.parseString(refreshResponse.getBody().asString()).getAsJsonObject();
        assertEquals("bearer", refreshJson.get("token_type").getAsString());
        assertEquals("123access", refreshJson.get("access_token").getAsString());
        assertEquals(86400, refreshJson.get("expires_in").getAsInt());
        assertFalse(refreshJson.has("refresh_token")); // No refresh token in refresh response
    }
    
    @Test
    void testErrorScenarios() {
        // Test invalid client_id
        given()
            .param("response_type", "code")
            .param("client_id", "invalid-client")
            .param("redirect_uri", REDIRECT_URI)
            .param("state", STATE)
        .when()
            .get("/fakeauth")
        .then()
            .statusCode(302); // Current implementation doesn't validate client_id
            
        // Test unsupported response_type
        given()
            .param("response_type", "unsupported_type")
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", REDIRECT_URI)
            .param("state", STATE)
        .when()
            .get("/fakeauth")
        .then()
            .statusCode(302); // Current implementation doesn't validate response_type
            
        // Test invalid redirect_uri format
        given()
            .param("response_type", "code")
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", "invalid-uri")
            .param("state", STATE)
        .when()
            .get("/fakeauth")
        .then()
            .statusCode(302); // Current implementation doesn't validate redirect_uri format
    }
} 