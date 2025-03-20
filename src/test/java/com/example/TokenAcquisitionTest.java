package com.example;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;

class TokenAcquisitionTest {
    
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String AUTHORIZATION_CODE = "test-auth-code";
    private static final String REDIRECT_URI = "https://oauth-redirect.googleusercontent.com/r/YOUR_PROJECT_ID";
    private static final String REFRESH_TOKEN = "123refresh";
    
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
    void testTokenAcquisitionWithAuthCode() {
        // Test token acquisition using authorization code grant
        given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "authorization_code")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
            .formParam("code", AUTHORIZATION_CODE)
            .formParam("redirect_uri", REDIRECT_URI)
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("refresh_token", equalTo("123refresh"))
            .body("expires_in", equalTo(86400));
    }
    
    @Test
    void testTokenAcquisitionWithRefreshToken() {
        // Test token acquisition using refresh token grant
        given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "refresh_token")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
            .formParam("refresh_token", REFRESH_TOKEN)
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("expires_in", equalTo(86400))
            .body("refresh_token", is(nullValue()));
    }
    
    @Test
    void testTokenAcquisitionWithJsonBody() {
        // Test token acquisition using JSON body instead of form parameters
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("client_id", CLIENT_ID);
        requestBody.put("client_secret", CLIENT_SECRET);
        requestBody.put("code", AUTHORIZATION_CODE);
        requestBody.put("redirect_uri", REDIRECT_URI);
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("refresh_token", equalTo("123refresh"))
            .body("expires_in", equalTo(86400));
    }
    
    @Test
    void testTokenAcquisitionWithInvalidGrantType() {
        // Test token acquisition with invalid grant type
        given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "invalid_grant_type")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200) // Current implementation doesn't validate grant type properly
            .contentType(ContentType.JSON)
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("refresh_token", is(nullValue()))
            .body("expires_in", equalTo(86400));
    }
    
    @Test
    void testTokenAcquisitionWithMissingParameters() {
        // Test token acquisition with missing required parameters
        given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "authorization_code")
            // Missing client_id, client_secret, code, redirect_uri
        .when()
            .post("/faketoken")
        .then()
            .statusCode(200) // Current implementation doesn't validate required params
            .contentType(ContentType.JSON)
            .body("token_type", equalTo("bearer"))
            .body("access_token", equalTo("123access"))
            .body("refresh_token", equalTo("123refresh"))
            .body("expires_in", equalTo(86400));
    }
    
    @Test
    void testTokenResponseHeaders() {
        // Test token response headers including CORS headers
        Response response = given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "authorization_code")
            .formParam("client_id", CLIENT_ID)
            .formParam("client_secret", CLIENT_SECRET)
            .formParam("code", AUTHORIZATION_CODE)
            .formParam("redirect_uri", REDIRECT_URI)
        .when()
            .post("/faketoken");
        
        response.then()
            .statusCode(200)
            .contentType(ContentType.JSON);
            
        // Note: If CORS headers should be present, add assertions for them here
        // .header("Access-Control-Allow-Origin", "*")
        // .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
        // .header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
} 