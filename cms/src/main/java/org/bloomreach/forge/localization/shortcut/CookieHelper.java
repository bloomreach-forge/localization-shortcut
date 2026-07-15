/*
 *  Copyright 2026 Bloomreach (https://www.bloomreach.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
