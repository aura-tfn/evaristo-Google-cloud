package com.example;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ExtendWith(MockitoExtension.class)
class OAuth2MockTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Captor
    private ArgumentCaptor<String> stringCaptor;
    
    private StringWriter stringWriter;
    private PrintWriter writer;
    
    private static final String REDIRECT_URI = "https://oauth-redirect.googleusercontent.com/r/YOUR_PROJECT_ID";
    private static final String STATE = "test-state-value";
    private static final String CLIENT_ID = "test-client-id";
    
    @BeforeEach
    void setUp() throws IOException {
        // Set up response writer
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        Mockito.when(response.getWriter()).thenReturn(writer);
    }
    
    @Test
    void testLoginServletGetRendersForm() throws IOException {
        // Setup
        String redirectURL = "https://example.com/callback?code=123&state=test";
        Mockito.when(request.getParameter("responseurl")).thenReturn(redirectURL);
        
        // Execute
        LoginServlet servlet = new LoginServlet();
        servlet.doGet(request, response);
        
        // Verify
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("text/html");
        writer.flush(); // Ensure all content is written
        
        String content = stringWriter.toString();
        assertTrue(content.contains("<form action='/login' method='post'>"));
        assertTrue(content.contains("<input type='hidden' name='responseurl' value='" + redirectURL + "'/>"));
        assertTrue(content.contains("Link this service to Google"));
    }
    
    @Test
    void testLoginServletPostRedirects() throws IOException {
        // Setup
        String redirectURL = "https://example.com/callback?code=123&state=test";
        Mockito.when(request.getParameter("responseurl")).thenReturn(redirectURL);
        
        // Execute
        LoginServlet servlet = new LoginServlet();
        servlet.doPost(request, response);
        
        // Verify
        Mockito.verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        Mockito.verify(response).setHeader("Location", redirectURL);
    }
    
    @Test
    void testFakeAuthGeneratesCodeAndRedirectsToLogin() throws IOException {
        // Setup
        Mockito.when(request.getParameter("client_id")).thenReturn(CLIENT_ID);
        Mockito.when(request.getParameter("redirect_uri")).thenReturn(REDIRECT_URI);
        Mockito.when(request.getParameter("state")).thenReturn(STATE);
        Mockito.when(request.getParameter("response_type")).thenReturn("code");
        
        // Execute
        FakeAuthServlet servlet = new FakeAuthServlet();
        servlet.doGet(request, response);
        
        // Verify
        Mockito.verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        Mockito.verify(response).setHeader(Mockito.eq("Location"), stringCaptor.capture());
        
        String location = stringCaptor.getValue();
        assertTrue(location.startsWith("/login?responseurl="));
        
        // Decode the URL to check the code and state
        String encodedRedirectUrl = location.substring(location.indexOf("=") + 1);
        String decodedRedirectUrl = URLDecoder.decode(encodedRedirectUrl, StandardCharsets.UTF_8);
        
        assertTrue(decodedRedirectUrl.contains("code="));
        assertTrue(decodedRedirectUrl.contains("state=" + STATE));
    }
    
    @Test
    void testFakeAuthServletPostReturnsErrorMessage() throws IOException {
        // Execute
        FakeAuthServlet servlet = new FakeAuthServlet();
        servlet.doPost(request, response);
        
        // Verify
        Mockito.verify(response).setContentType("text/plain");
        writer.flush();
        
        String content = stringWriter.toString();
        assertEquals("/fakeauth should be a GET\n", content);
    }
    
    @Test
    void testFakeTokenServletWithAuthorizationCode() throws IOException {
        // Setup
        Mockito.when(request.getParameter("grant_type")).thenReturn("authorization_code");
        
        // Execute
        FakeTokenServlet servlet = new FakeTokenServlet();
        servlet.doPost(request, response);
        
        // Verify
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("application/json");
        writer.flush();
        
        JsonObject json = JsonParser.parseString(stringWriter.toString()).getAsJsonObject();
        assertEquals("bearer", json.get("token_type").getAsString());
        assertEquals("123access", json.get("access_token").getAsString());
        assertEquals("123refresh", json.get("refresh_token").getAsString());
        assertEquals(86400, json.get("expires_in").getAsInt());
    }
    
    @Test
    void testFakeTokenServletWithRefreshToken() throws IOException {
        // Setup
        Mockito.when(request.getParameter("grant_type")).thenReturn("refresh_token");
        
        // Execute
        FakeTokenServlet servlet = new FakeTokenServlet();
        servlet.doPost(request, response);
        
        // Verify
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("application/json");
        writer.flush();
        
        JsonObject json = JsonParser.parseString(stringWriter.toString()).getAsJsonObject();
        assertEquals("bearer", json.get("token_type").getAsString());
        assertEquals("123access", json.get("access_token").getAsString());
        assertEquals(86400, json.get("expires_in").getAsInt());
        assertFalse(json.has("refresh_token"));
    }
    
    @Test
    void testFakeTokenServletGetReturnsErrorMessage() throws IOException {
        // Execute
        FakeTokenServlet servlet = new FakeTokenServlet();
        servlet.doGet(request, response);
        
        // Verify
        Mockito.verify(response).setContentType("text/plain");
        writer.flush();
        
        String content = stringWriter.toString();
        assertEquals("/faketoken should be a POST\n", content);
    }
    
    @Test
    void testFullOAuth2Flow() throws IOException {
        // Step 1: Auth endpoint redirects to login
        Mockito.when(request.getParameter("client_id")).thenReturn(CLIENT_ID);
        Mockito.when(request.getParameter("redirect_uri")).thenReturn(REDIRECT_URI);
        Mockito.when(request.getParameter("state")).thenReturn(STATE);
        Mockito.when(request.getParameter("response_type")).thenReturn("code");
        
        FakeAuthServlet authServlet = new FakeAuthServlet();
        authServlet.doGet(request, response);
        
        Mockito.verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        Mockito.verify(response).setHeader(Mockito.eq("Location"), stringCaptor.capture());
        
        String loginUrl = stringCaptor.getValue();
        assertTrue(loginUrl.startsWith("/login?responseurl="));
        
        // Extract the encoded redirect URL
        String encodedRedirectUrl = loginUrl.substring(loginUrl.indexOf("=") + 1);
        String decodedRedirectUrl = URLDecoder.decode(encodedRedirectUrl, StandardCharsets.UTF_8);
        
        // Step 2: User submits login form
        // Reset mocks for the next request
        Mockito.reset(response);
        StringWriter loginWriter = new StringWriter();
        PrintWriter loginPrintWriter = new PrintWriter(loginWriter);
        Mockito.when(response.getWriter()).thenReturn(loginPrintWriter);
        
        // Setup login request with the redirect URL from auth
        Mockito.when(request.getParameter("responseurl")).thenReturn(decodedRedirectUrl);
        
        LoginServlet loginServlet = new LoginServlet();
        loginServlet.doPost(request, response);
        
        Mockito.verify(response).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        Mockito.verify(response).setHeader(Mockito.eq("Location"), stringCaptor.capture());
        
        String redirectWithCode = stringCaptor.getValue();
        assertTrue(redirectWithCode.contains("code="));
        assertTrue(redirectWithCode.contains("state=" + STATE));
        
        // Extract code from the redirect URL
        String code = redirectWithCode.substring(
            redirectWithCode.indexOf("code=") + 5, 
            redirectWithCode.indexOf("&state=")
        );
        
        // Step 3: Exchange code for token
        // Reset mocks for the next request
        Mockito.reset(response);
        StringWriter tokenWriter = new StringWriter();
        PrintWriter tokenPrintWriter = new PrintWriter(tokenWriter);
        Mockito.when(response.getWriter()).thenReturn(tokenPrintWriter);
        
        // Setup token request
        Mockito.when(request.getParameter("grant_type")).thenReturn("authorization_code");
        Mockito.when(request.getParameter("code")).thenReturn(code);
        Mockito.when(request.getParameter("client_id")).thenReturn(CLIENT_ID);
        Mockito.when(request.getParameter("redirect_uri")).thenReturn(REDIRECT_URI);
        
        FakeTokenServlet tokenServlet = new FakeTokenServlet();
        tokenServlet.doPost(request, response);
        
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("application/json");
        tokenPrintWriter.flush();
        
        JsonObject tokenJson = JsonParser.parseString(tokenWriter.toString()).getAsJsonObject();
        assertEquals("bearer", tokenJson.get("token_type").getAsString());
        assertEquals("123access", tokenJson.get("access_token").getAsString());
        assertEquals("123refresh", tokenJson.get("refresh_token").getAsString());
        assertEquals(86400, tokenJson.get("expires_in").getAsInt());
        
        // Step 4: Use refresh token to get new access token
        // Reset mocks for the next request
        Mockito.reset(response);
        StringWriter refreshWriter = new StringWriter();
        PrintWriter refreshPrintWriter = new PrintWriter(refreshWriter);
        Mockito.when(response.getWriter()).thenReturn(refreshPrintWriter);
        
        // Setup refresh token request
        Mockito.when(request.getParameter("grant_type")).thenReturn("refresh_token");
        Mockito.when(request.getParameter("refresh_token")).thenReturn(tokenJson.get("refresh_token").getAsString());
        
        tokenServlet.doPost(request, response);
        
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("application/json");
        refreshPrintWriter.flush();
        
        JsonObject refreshJson = JsonParser.parseString(refreshWriter.toString()).getAsJsonObject();
        assertEquals("bearer", refreshJson.get("token_type").getAsString());
        assertEquals("123access", refreshJson.get("access_token").getAsString());
        assertEquals(86400, refreshJson.get("expires_in").getAsInt());
        assertFalse(refreshJson.has("refresh_token"));
    }
} 