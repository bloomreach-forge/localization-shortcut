package org.bloomreach.forge.localization.shortcut;

import org.hippoecm.frontend.util.WebApplicationHelper;

import jakarta.servlet.http.Cookie;

public final class CookieHelper {

    public void setCookieValue(final String cookieName, final String cookieValue, final int maxAge) {
        final Cookie localeCookie = new Cookie(cookieName, cookieValue);
        localeCookie.setMaxAge(maxAge);
        localeCookie.setHttpOnly(true);
        WebApplicationHelper.retrieveWebResponse().addCookie(localeCookie);
    }

    public String getCookieValue(final String cookieName) {
        final Cookie[] cookies = WebApplicationHelper.retrieveWebRequest().getContainerRequest().getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
