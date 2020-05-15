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

    /**
     * Checks the request for a header named {@value #HTTP_HEADER_REMOTE_USER}. If the header contains
     * a non-empty remote user value, then the value is assumed to be a pre-authenitcated user from
     * an upstream system (i.e. web server) and a HeaderCredentials is created with the username. 
     * <code>null</code> is returned otherwise.  
     */
    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response)
    {
        final String uid = request.getHeader(remoteUserHeader);

        if (StringUtils.isNotBlank(uid))
        {
            logger.debug("Found '{}={}' header.", remoteUserHeader, uid);
            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(AUTH_TYPE, uid);
            authenticationInfo.put("user.jcr.credentials", new HeaderCredentials(uid));
            return authenticationInfo;
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
    }
}
