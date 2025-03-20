package com.example;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class OAuth2UnitTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private StringWriter stringWriter;
    private PrintWriter writer;
    
    @BeforeEach
    void setUp() throws IOException {
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
    }
    
    @Test
    void testLoginServletGet() throws IOException {
        // Set up mock request
        String redirectURL = "https://example.com/callback?code=123&state=test";
        when(request.getParameter("responseurl")).thenReturn(redirectURL);
        
        // Create servlet and call doGet
        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doGet(request, response);
        
        // Verify response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/html");
        
        // Verify content contains form and redirection URL
        String content = stringWriter.toString();
        assertTrue(content.contains("<form action='/login' method='post'>"));
        assertTrue(content.contains("<input type='hidden' name='responseurl' value='" + redirectURL + "'/>"));
        assertTrue(content.contains("<button type='submit'"));
    }
    
    @Test
    void testLoginServletPost() throws IOException {
        // Set up mock request
        String redirectURL = "https://example.com/callback?code=123&state=test";
        when(request.getParameter("responseurl")).thenReturn(redirectURL);
        
        // Create servlet and call doPost
        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);
        
        // Verify redirect
        verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        verify(response).setHeader("Location", redirectURL);
    }
    
    @Test
    void testFakeAuthServlet() throws IOException {
        // Set up mock request
        String redirectUri = "https://example.com/callback";
        String state = "state-123";
        
        when(request.getParameter("redirect_uri")).thenReturn(redirectUri);
        when(request.getParameter("state")).thenReturn(state);
        
        // Mock response.encodeRedirectURL
        String mockEncodedUrl = "/login?responseurl=https://example.com/callback?code=xxxxxx&state=state-123";
        when(response.encodeRedirectURL(anyString())).thenReturn(mockEncodedUrl);
        
        // Create servlet and call doGet
        FakeAuthServlet authServlet = new FakeAuthServlet();
        authServlet.doGet(request, response);
        
        // Verify response
        verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        verify(response).setHeader("Location", mockEncodedUrl);
    }
    
    @Test
    void testFakeAuthServletPost() throws IOException {
        // Create servlet and call doPost
        FakeAuthServlet authServlet = new FakeAuthServlet();
        authServlet.doPost(request, response);
        
        // Verify response
        verify(response).setContentType("text/plain");
        
        // Verify content
        String content = stringWriter.toString();
        assertTrue(content.trim().equals("/fakeauth should be a GET".trim()));
    }
    
    @Test
    void testFakeTokenServletAuthorizationCodeGrant() throws IOException {
        // Set up mock request for authorization code grant
        when(request.getParameter("grant_type")).thenReturn("authorization_code");
        
        // Create servlet and call doPost
        FakeTokenServlet tokenServlet = new FakeTokenServlet();
        tokenServlet.doPost(request, response);
        
        // Verify response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        
        // Parse and verify JSON response
        JsonObject jsonResponse = JsonParser.parseString(stringWriter.toString()).getAsJsonObject();
        assertEquals("bearer", jsonResponse.get("token_type").getAsString());
        assertEquals("123access", jsonResponse.get("access_token").getAsString());
        assertEquals(86400, jsonResponse.get("expires_in").getAsInt());
        assertEquals("123refresh", jsonResponse.get("refresh_token").getAsString());
    }
    
    @Test
    void testFakeTokenServletRefreshTokenGrant() throws IOException {
        // Set up mock request for refresh token grant
        when(request.getParameter("grant_type")).thenReturn("refresh_token");
        
        // Create servlet and call doPost
        FakeTokenServlet tokenServlet = new FakeTokenServlet();
        tokenServlet.doPost(request, response);
        
        // Verify response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        
        // Parse and verify JSON response
        JsonObject jsonResponse = JsonParser.parseString(stringWriter.toString()).getAsJsonObject();
        assertEquals("bearer", jsonResponse.get("token_type").getAsString());
        assertEquals("123access", jsonResponse.get("access_token").getAsString());
        assertEquals(86400, jsonResponse.get("expires_in").getAsInt());
        assertFalse(jsonResponse.has("refresh_token"));
    }
    
    @Test
    void testFakeTokenServletGet() throws IOException {
        // Create servlet and call doGet
        FakeTokenServlet tokenServlet = new FakeTokenServlet();
        tokenServlet.doGet(request, response);
        
        // Verify response
        verify(response).setContentType("text/plain");
        
        // Verify content
        String content = stringWriter.toString();
        assertTrue(content.trim().equals("/faketoken should be a POST".trim()));
    }
    
    // Helper method to check if a string starts with a given prefix
    private String startsWith(String prefix) {
        return argThat(arg -> arg != null && arg.startsWith(prefix));
    }
    
    // Helper method to check if a string equals a given value
    private String eq(String value) {
        return argThat(arg -> arg != null && arg.equals(value));
    }
} 