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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        service = AuthenticationHandler.class,
        name = "Header Authentication Handler",
        property = {
                AuthenticationHandler.PATH_PROPERTY + "=/"
        },
        immediate = true
)
public class HeaderAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler
{
    private final Logger logger = LoggerFactory.getLogger(HeaderAuthenticationHandler.class);

    public static final String AUTH_TYPE = "HEADER";

    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response)
    {
        // TODO push header into OSGi configuration
        final String uid = request.getHeader("REMOTE_USER");

        if (StringUtils.isNotBlank(uid))
        {
            logger.info("Found 'REMOTE_USER={}' header.", uid);
            final AuthenticationInfo authenticationInfo = new AuthenticationInfo(AUTH_TYPE, uid);
            authenticationInfo.put("user.jcr.credentials", new HeaderCredentials(uid));
            return authenticationInfo;
        }

        return null;
    }

    @Override
    public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        logger.info("requestCredentials() - not yet implemented");
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        logger.info("dropCredentials() - not yet implemented");
    }

    @Override
    public void authenticationFailed(javax.servlet.http.HttpServletRequest request,
                                     javax.servlet.http.HttpServletResponse response,
                                     AuthenticationInfo authInfo)
    {
        logger.warn("authenticationFailed() called: AuthenticationInfo={}", authInfo);
    }
}
