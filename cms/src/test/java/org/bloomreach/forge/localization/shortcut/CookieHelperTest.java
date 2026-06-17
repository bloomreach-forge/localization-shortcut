package org.bloomreach.forge.localization.shortcut;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CookieHelper.
 * WebApplicationHelper static methods are mocked via Mockito's MockedStatic.
 */
class CookieHelperTest {

    // ---------------------------------------------------------------------------
    // getCookieValue
    // ---------------------------------------------------------------------------

    @Test
    void getCookieValue_whenCookiesIsNull_returnsNull() {
        CookieHelper cookieHelper = new CookieHelper();
        ServletWebRequest mockRequest = mock(ServletWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContainerRequest()).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getCookies()).thenReturn(null);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebRequest).thenReturn(mockRequest);
            assertNull(cookieHelper.getCookieValue("loc"));
        }
    }

    @Test
    void getCookieValue_whenNoCookiesPresent_returnsNull() {
        CookieHelper cookieHelper = new CookieHelper();
        ServletWebRequest mockRequest = mock(ServletWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContainerRequest()).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getCookies()).thenReturn(new Cookie[0]);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebRequest).thenReturn(mockRequest);
            assertNull(cookieHelper.getCookieValue("loc"));
        }
    }

    @Test
    void getCookieValue_whenMatchingCookiePresent_returnsValue() {
        CookieHelper cookieHelper = new CookieHelper();
        Cookie[] cookies = {new Cookie("loc", "nl")};
        ServletWebRequest mockRequest = mock(ServletWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContainerRequest()).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getCookies()).thenReturn(cookies);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebRequest).thenReturn(mockRequest);
            assertEquals("nl", cookieHelper.getCookieValue("loc"));
        }
    }

    @Test
    void getCookieValue_whenMultipleCookies_returnsCorrectValue() {
        CookieHelper cookieHelper = new CookieHelper();
        Cookie[] cookies = {
                new Cookie("other", "otherValue"),
                new Cookie("tzcookie", "America/New_York"),
                new Cookie("loc", "de")
        };
        ServletWebRequest mockRequest = mock(ServletWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContainerRequest()).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getCookies()).thenReturn(cookies);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebRequest).thenReturn(mockRequest);
            assertEquals("de", cookieHelper.getCookieValue("loc"));
            assertEquals("America/New_York", cookieHelper.getCookieValue("tzcookie"));
            assertEquals("otherValue", cookieHelper.getCookieValue("other"));
        }
    }

    @Test
    void getCookieValue_whenCookieNameNotFound_returnsNull() {
        CookieHelper cookieHelper = new CookieHelper();
        Cookie[] cookies = {new Cookie("loc", "en")};
        ServletWebRequest mockRequest = mock(ServletWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockRequest.getContainerRequest()).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getCookies()).thenReturn(cookies);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebRequest).thenReturn(mockRequest);
            assertNull(cookieHelper.getCookieValue("tzcookie"));
        }
    }

    // ---------------------------------------------------------------------------
    // setCookieValue
    // ---------------------------------------------------------------------------

    @Test
    void setCookieValue_addsCorrectCookieToResponse() {
        CookieHelper cookieHelper = new CookieHelper();
        WebResponse mockResponse = mock(WebResponse.class);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebResponse).thenReturn(mockResponse);
            // Should not throw; the cookie is passed to the response
            cookieHelper.setCookieValue("loc", "nl", 3600);
            verify(mockResponse).addCookie(argThat(cookie ->
                    "loc".equals(cookie.getName())
                            && "nl".equals(cookie.getValue())
                            && cookie.getMaxAge() == 3600
                            && cookie.isHttpOnly()
            ));
        }
    }

    @Test
    void setCookieValue_setsHttpOnlyFlag() {
        CookieHelper cookieHelper = new CookieHelper();
        WebResponse mockResponse = mock(WebResponse.class);

        try (MockedStatic<WebApplicationHelper> staticMock = mockStatic(WebApplicationHelper.class)) {
            staticMock.when(WebApplicationHelper::retrieveWebResponse).thenReturn(mockResponse);
            cookieHelper.setCookieValue("tzcookie", "UTC", 86400);
            verify(mockResponse).addCookie(argThat(Cookie::isHttpOnly));
        }
    }
}
