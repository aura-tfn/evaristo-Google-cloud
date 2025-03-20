# OAuth2 Tests for Evaristo Google Cloud

This document describes the test cases created for testing the OAuth2 login functionality and token retrieval process.

## Test Classes

1. **OAuth2MockTest**: Unit tests using Mockito to test the OAuth2 login and token endpoints.
2. **OAuth2LoginTest**: Integration tests for the login flows using RestAssured.
3. **TokenAcquisitionTest**: Focused tests for token acquisition flows.

## Test Setup

Before running the tests, make sure:

1. The project is built using: `mvn clean install`
2. The application server is running: `mvn tomcat7:run` (for integration tests)
3. For unit tests, no server needs to be running

## Running the Tests

### Running Unit Tests (Mocked Servlets)

```bash
mvn test -Dtest=OAuth2MockTest
```

These tests don't require the server to be running as they use mocks for the servlet API.

### Running Integration Tests

For integration tests, the server needs to be running:

```bash
# First, start the server in one terminal
mvn tomcat7:run

# In another terminal, run the integration tests
mvn test -Dtest=OAuth2LoginTest,TokenAcquisitionTest
```

You can also specify a custom base URI if the server is running on a different URL:

```bash
mvn test -Dtest=OAuth2LoginTest -DrestassuredBaseUri=http://localhost:8080/evaristo-google-cloud
```

## Test Scenarios Covered

### 1. Login Flow Tests

- `testLoginServletGetRendersForm`: Verifies the login page renders with the correct form
- `testLoginServletPostRedirects`: Verifies POST to login redirects to the callback URL
- `testFakeAuthGeneratesCodeAndRedirectsToLogin`: Tests the authorization endpoint redirects correctly
- `testFakeAuthServletPostReturnsErrorMessage`: Tests POST to auth endpoint returns error

### 2. Token Acquisition Tests

- `testFakeTokenServletWithAuthorizationCode`: Tests authorization code grant
- `testFakeTokenServletWithRefreshToken`: Tests refresh token grant
- `testFakeTokenServletGetReturnsErrorMessage`: Tests GET to token endpoint returns error

### 3. End-to-End Flow Test

- `testFullOAuth2Flow`: Tests the complete OAuth2 flow from authorization request to access token

## Expected Test Results

All tests should pass when the application is functioning correctly. Successful test execution will show output like:

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

## Dependencies

The tests use:

1. JUnit 5 for test framework
2. Mockito for mocking servlet API
3. RestAssured for HTTP integration tests
4. GSON for JSON parsing

## Additional Notes

- The FakeAuthServlet generates a fixed authorization code (xxxxxx)
- The FakeTokenServlet returns a fixed access token (123access)
- Refresh tokens are only returned on authorization code grants (123refresh)
- Access tokens have a fixed expiration time (86400 seconds = 1 day)

## OAuth2 Flow Summary

1. **Authorization Request**: Client redirects to `/fakeauth?client_id=...&redirect_uri=...&response_type=code&state=...`
2. **User Authentication**: Server redirects to `/login?responseurl=...` 
3. **Authorization Grant**: User approves and gets redirected to `redirect_uri?code=...&state=...`
4. **Token Request**: Client POSTs to `/faketoken` with the code to exchange for tokens
5. **Access Token Use**: Client uses the access token to call protected resources
6. **Token Refresh**: When the access token expires, client uses refresh token to get a new access token 