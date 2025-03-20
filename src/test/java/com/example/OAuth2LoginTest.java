package com.example;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.response.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class OAuth2LoginTest {
    
    private static final String REDIRECT_URI = "https://oauth-redirect.googleusercontent.com/r/YOUR_PROJECT_ID";
    private static final String ENCODED_REDIRECT_URI = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
    private static final String CLIENT_ID = "test-client-id";
    private static final String STATE = "test-state";
    private static final String RESPONSE_TYPE = "code";
    
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
    void testFakeAuthRedirectsToLogin() {
        // Test that FakeAuthServlet redirects to the login page
        given()
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", REDIRECT_URI)
            .param("state", STATE)
            .param("response_type", RESPONSE_TYPE)
        .when()
            .get("/fakeauth")
        .then()
            .statusCode(302)
            .header("Location", containsString("/login?responseurl="));
    }
    
    @Test
    void testLoginPageRenders() {
        // Test that login page renders with the expected form
        String redirectUrl = "https://example.com/callback?code=123&state=test";
        given()
            .param("responseurl", redirectUrl)
        .when()
            .get("/login")
        .then()
            .statusCode(200)
            .contentType("text/html")
            .body(containsString("Link this service to Google"))
            .body(containsString(redirectUrl));
    }
    
    @Test
    void testLoginPostRedirects() {
        // Test that login POST redirects to the provided responseurl
        String redirectUrl = "https://example.com/callback?code=123&state=test";
        given()
            .param("responseurl", redirectUrl)
            .formParam("responseurl", redirectUrl)
        .when()
            .post("/login")
        .then()
            .statusCode(302)
            .header("Location", equalTo(redirectUrl));
    }
    
    @Test
    void testFullOAuth2Flow() {
        // Step 1: Get auth URL from FakeAuthServlet
        Response authResponse = given()
            .param("client_id", CLIENT_ID)
            .param("redirect_uri", REDIRECT_URI)
            .param("state", STATE)
            .param("response_type", RESPONSE_TYPE)
        .when()
            .get("/fakeauth");
        
        // Extract the login URL
        String loginUrl = authResponse.getHeader("Location");
        assertTrue(loginUrl.startsWith("/login?responseurl="));
        
        // Step 2: Submit the login form
        Response loginResponse = given()
            .formParam("responseurl", loginUrl.substring(loginUrl.indexOf("=") + 1))
        .when()
            .post("/login");
        
        // Extract the redirect URL that contains the authorization code
        String redirectWithCode = loginResponse.getHeader("Location");
        assertTrue(redirectWithCode.contains("code="));
        assertTrue(redirectWithCode.contains("state=" + STATE));
        
        // Extract the code
        String code = redirectWithCode.substring(
            redirectWithCode.indexOf("code=") + 5, 
            redirectWithCode.indexOf("&state=")
        );
        
        // Step 3: Exchange code for tokens
        given()
            .param("grant_type", "authorization_code")
            .param("client_id", CLIENT_ID)
            .param("client_secret", "test-client-secret")
            .param("code", code)
            .param("redirect_uri", REDIRECT_URI)
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("refresh_token", equalTo("123refresh"))
            .body("expires_in", equalTo(86400));
    }
    
    @Test
    void testRefreshTokenFlow() {
        // Test refreshing an access token
        given()
            .param("grant_type", "refresh_token")
            .param("client_id", CLIENT_ID)
            .param("client_secret", "test-client-secret")
            .param("refresh_token", "123refresh")
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("expires_in", equalTo(86400))
            .body("refresh_token", is(nullValue()));
    }
    
    @Test
    void testTokenEndpointGetMethod() {
        // Test that token endpoint GET method returns error message
        given()
        .when()
            .get("/faketoken")
        .then()
            .statusCode(200)
            .contentType("text/plain")
            .body(containsString("/faketoken should be a POST"));
    }
    
    @Test
    void testAuthEndpointPostMethod() {
        // Test that auth endpoint POST method returns error message
        given()
        .when()
            .post("/fakeauth")
        .then()
            .statusCode(200)
            .contentType("text/plain")
            .body(containsString("/fakeauth should be a GET"));
    }
} 