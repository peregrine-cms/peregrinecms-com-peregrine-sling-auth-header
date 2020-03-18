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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.external.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.security.auth.login.LoginException;
import java.util.*;

@Component(
        name = "Header External Identity Provider",
        service = ExternalIdentityProvider.class,
        immediate = true
)
/**
 * The implementation is based on @see org.apache.jackrabbit.oak.exercise.security.authentication.external.CustomExternalIdentityProvider.class
 *
 * @author <a href="mailto:gg@headwire.com">Gaston Gonzalez</a>
 */
public class HeaderExternalIdentityProvider implements ExternalIdentityProvider
{
    private final Logger logger = LoggerFactory.getLogger(HeaderExternalIdentityProvider.class);

    public static final String NAME = "HeaderExternalIdentityProvider";

    private Map<String, Set<String>> userGroupMap = new HashMap<String, Set<String>>();
    private Set<String> groupIds = new HashSet<String>();

    public HeaderExternalIdentityProvider()
    {
        // Default constructor
    }

    @Activate
    public void activate(Map<String, Object> properties)
    {
        ConfigurationParameters config = ConfigurationParameters.of(properties);
        logger.info("Activated IDP: '{}' with config: '{}'", getName(), config);
    }

    @Modified
    public void modified(Map<String, Object> properties)
    {
        ConfigurationParameters config = ConfigurationParameters.of(properties);
        logger.info("Modified IDP: '{}' with config: '{}'", getName(), config);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public ExternalIdentity getIdentity(ExternalIdentityRef externalIdentityRef) throws ExternalIdentityException
    {
        if (getName().equals(externalIdentityRef.getProviderName()))
        {
            String id = externalIdentityRef.getId();
            return getUser(id);
        } else
        {
            return null;
        }
    }

    @Override
    public ExternalUser getUser(final String userId) throws ExternalIdentityException
    {
        return new ExternalUser()
        {

            @Override
            public ExternalIdentityRef getExternalId()
            {
                return new ExternalIdentityRef(userId, getName());
            }

            @Override
            public String getId()
            {
                return userId;
            }

            @Override
            public String getPrincipalName()
            {
                return "p_" + getExternalId().getString();
            }

            @Override
            public String getIntermediatePath()
            {
                return null;
            }

            @Override
            public Iterable<ExternalIdentityRef> getDeclaredGroups() throws ExternalIdentityException
            {
                return ImmutableSet.of();
            }

            @Override
            public Map<String, ?> getProperties()
            {
                return ImmutableMap.of();
            }
        };
    }

    @Override
    public ExternalUser authenticate(Credentials credentials) throws ExternalIdentityException, LoginException
    {
        if (credentials instanceof HeaderCredentials)
        {
            String userId = ((HeaderCredentials) credentials).getUserId();
            return getUser(userId);
        } else
        {
            throw new LoginException("Unsupported credentials");
        }
    }

    @Override
    public ExternalGroup getGroup(String s) throws ExternalIdentityException
    {
        return null;
    }

    @Override
    public Iterator<ExternalUser> listUsers() throws ExternalIdentityException
    {
        throw new UnsupportedOperationException("listUsers");
    }

    @Override
    public Iterator<ExternalGroup> listGroups() throws ExternalIdentityException
    {
        throw new UnsupportedOperationException("listGroups");
    }
}
