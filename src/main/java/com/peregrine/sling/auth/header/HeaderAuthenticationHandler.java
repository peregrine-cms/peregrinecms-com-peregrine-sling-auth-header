/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peregrine.sling.auth.header;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.peregrine.sling.auth.header.HeaderAuthenticationHandlerConfig.HEADER_AUTH_SHARED_SECRET_HEADER;

@Component(
        service = AuthenticationHandler.class,
        name = "Header Authentication Handler",
        property = {
                AuthenticationHandler.PATH_PROPERTY + "=/"
        },
        immediate = true
)
@Designate(ocd = HeaderAuthenticationHandlerConfig.class)
public class HeaderAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler
{
    private final Logger logger = LoggerFactory.getLogger(HeaderAuthenticationHandler.class);

    public static final String AUTH_TYPE = "HEADER";
 
    private String loginCookie;
    private String remoteUserHeader;
    private String sharedSecret;
    private Pattern usernameWhitelist;
    private Pattern userProfileHeaderWhitelist;

    /**
     * Checks the request for the presence of two request headers: the remote user and shared secret. If either are
     * missing, the request is likely not intended for this authentication handler and credential extraction is
     * ignored by this handler. Any downstream handlers are then executed by Sling.
     *
     * If the shared keys match and there is a non-empty remote user in the request, this handler assumes that
     * that the upstream system (i.e. Apache) has already authenticated the user and is passing the remote user
     * to us. When this occurs, this handler creates a custom credential using the remote user name and sets the
     * pre-authentication marker.
     *
     * @return A valid AuthenticationInfo object with a credential object set and pre-authentication marker set.
     */
    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response)
    {
        if (handleAuthRequest(request))
        {
            final String username = request.getHeader(remoteUserHeader);
            final String sharedSecret = request.getHeader(HEADER_AUTH_SHARED_SECRET_HEADER);

            if (isValidSharedSecret(sharedSecret) && isValidUsername(username))
            {
                logger.debug("Creating credentials and setting pre-authentication marker for user: '{}'", username);
                final AuthenticationInfo authenticationInfo = new AuthenticationInfo(AUTH_TYPE, username);
                authenticationInfo.put("user.jcr.credentials", new HeaderCredentials(username, getUserProfileFromHeader(request)));
                return authenticationInfo;
            }
            else
            {
                logger.warn("Invalid shared secret or username for remote user: '{}'", username);
            }
        }

        return null;
    }

    /**
     * This implementation does not require support for requesting credentials. It will always return
     # <code>false</code>. 
     */
    @Override
    public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // If there is a login cookie specified, delete it.
        if (StringUtils.isNotBlank(loginCookie))
        {
            Cookie cookie = new Cookie(loginCookie, "");
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
         }
    }

    @Override
    public void authenticationFailed(HttpServletRequest request, HttpServletResponse response, AuthenticationInfo authInfo)
    {
        logger.warn("Authentication failed for: {}", authInfo);
    }


    @Activate
    protected void activate(HeaderAuthenticationHandlerConfig config, ComponentContext componentContext)
    {
        this.loginCookie = config.header_auth_login_cookie();
        this.remoteUserHeader = config.header_auth_remote_user_header();
        this.sharedSecret = config.header_auth_shared_secret();
        this.usernameWhitelist = Pattern.compile(config.header_auth_username_whitelist());
        this.userProfileHeaderWhitelist = Pattern.compile(config.header_auth_user_profile_header_whitelist());
    }

    /**
     * Determines whether this handler should attempt to extract the credentials from the request. At a minimum this
     * handlers needs two request headers: a header for the shared secret and a header for the remote user.
     *
     * @param request
     * @return <code>true</code> if it should handle the credentials extraction and <code>false</code> otherwise.
     */
    private boolean handleAuthRequest(final HttpServletRequest request)
    {
        return (request != null) &&
                StringUtils.isNotEmpty(request.getHeader(HEADER_AUTH_SHARED_SECRET_HEADER)) &&
                StringUtils.isNotEmpty(request.getHeader(remoteUserHeader));
    }

    /**
     * Validates the shared secret between the client and Sling.
     *
     * @param clientSharedSecret Shared key from client request
     * @return <code>true</code> if keys match and <code>false</code> otherwise.
     */
    private boolean isValidSharedSecret(final String clientSharedSecret)
    {
        if (clientSharedSecret != null && clientSharedSecret.equals(this.sharedSecret)) {
            return true;
        }

        logger.warn("Shared keys do not match. This is either a configuration error or a malicious request");
        return false;
    }

    /**
     * Determines if the username is valid.
     *
     * @param username
     * @return <code>true</code> if valid and <code>false</code> otherwise.
     */
    private boolean isValidUsername(final String username)
    {
        return StringUtils.isNotBlank(username) &&
            !"admin".equalsIgnoreCase(username) &&
            usernameWhitelist.matcher(username).matches();
    }

    /**
     * Gets user profile information from request headers.
     *
     * @param request
     * @return A map containing each profile data point as a key in the map.
     */
    private Map<String, Object> getUserProfileFromHeader(final HttpServletRequest request)
    {
        Map<String, Object> userProfile = new HashMap<String, Object>();

        Enumeration headers = request.getHeaderNames();
        while(headers.hasMoreElements())
        {
            String header = (String) headers.nextElement();
            if (StringUtils.isNoneBlank(header) && userProfileHeaderWhitelist.matcher(header).matches())
            {
                userProfile.put(header, request.getHeader(header));
            }
        }

        return userProfile;
    }
}
