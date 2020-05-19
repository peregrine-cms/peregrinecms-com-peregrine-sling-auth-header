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

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.namepath.NamePathMapper;
import org.apache.jackrabbit.oak.plugins.value.ValueFactoryImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.oak.spi.security.authentication.PreAuthenticatedLogin;
import org.apache.jackrabbit.oak.spi.security.authentication.external.*;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.spi.whiteboard.WhiteboardUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component(
        name = "Header Login Module",
        service = HeaderExternalLoginModule.class,
        immediate = true
)
public class HeaderExternalLoginModule extends AbstractLoginModule
{
    private final Logger logger = LoggerFactory.getLogger(HeaderExternalLoginModule.class);
    private static final int MAX_SYNC_ATTEMPTS = 3;

    static Set<Class> SUPPORTED_CREDENTIALS = new HashSet<Class>();

    static
    {
        SUPPORTED_CREDENTIALS.add(HeaderCredentials.class);
    }

    private ExternalIdentityProvider externalIdentityProvider;

    private SyncHandler syncHandler;

    private ExternalUser externalUser;

    public HeaderExternalLoginModule()
    {
    }

    @Override
    protected Set<Class> getSupportedCredentials()
    {
        return SUPPORTED_CREDENTIALS;
    }

    @Override
    public boolean login() throws LoginException
    {
        Credentials credentials = getCredentials();

        if (credentials instanceof HeaderCredentials)
        {
            final String userId = ((HeaderCredentials) credentials).getUserId();
            if (userId != null)
            {
                sharedState.put(SHARED_KEY_PRE_AUTH_LOGIN, new PreAuthenticatedLogin(userId));
                sharedState.put(SHARED_KEY_CREDENTIALS, new SimpleCredentials(userId, new char[0]));
                sharedState.put(SHARED_KEY_LOGIN_NAME, userId);
                logger.debug("Adding pre-authenticated login user '{}' to shared state.", userId);

                // TODO: Only sync user on first login?
                // TODO: Can we wire the user sync instead of calling manually?
                handleUserSync((HeaderCredentials)credentials);
            }
            else
            {
                logger.warn("Could not extract userId from credentials");
            }
        }

        // subsequent login modules need to succeed and process the 'PreAuthenticatedLogin'
        return false;
    }

    private void handleUserSync(final HeaderCredentials credentials)
    {
        final String userId = credentials.getUserId();

        try
        {

            SyncedIdentity syncedIdentity = null;
            UserManager userMgr = getUserManager();
            externalUser = externalIdentityProvider.getUser(userId);

            if (userId != null && userMgr != null)
            {
                syncedIdentity = syncHandler.findIdentity(userMgr, userId);
                if (syncedIdentity != null)
                {
                    logger.debug("Found identity: '{}' for user: '{}'", syncedIdentity, userId);
                    return;
                }

                // Inject user profile
                ((HeaderExternalIdentityProvider)externalIdentityProvider).setUserProfile(credentials.getProfile());

                Root root = getRoot();
                if (null == root)
                {
                    throw new SyncException("Cannot synchronize user. root == null");
                }

                UserManager userManager = getUserManager();
                if (userManager == null)
                {
                    throw new SyncException("Cannot synchronize user. userManager == null");
                }

                int numAttempt = 0;
                while (numAttempt++ < MAX_SYNC_ATTEMPTS)
                {
                    SyncContext context = null;
                    try
                    {
                        context = syncHandler.createContext(externalIdentityProvider, userManager,
                                new ValueFactoryImpl(root, NamePathMapper.DEFAULT));
                        // Note: The SyncContext will only create a user in the repository if you use the
                        // DefaultSyncContext.sync(ExternalIdentity) method. It will NOT create a user if use
                        // the DefaultSyncContext.sync(String) method.
                        SyncResult syncResult = context.sync(externalUser);
                        logger.debug("Synced user: '{}' wth status: '{}'", externalUser.getId(), syncResult.getStatus());

                        root.commit();
                        return;
                    } catch (CommitFailedException e)
                    {
                        logger.error("Error syncing user: '{}'", userId, e);
                        root.refresh();
                    } finally
                    {
                        if (context != null)
                        {
                            context.close();
                        }
                    }
                }
                throw new SyncException("User synchronization failed during commit after " + MAX_SYNC_ATTEMPTS + " attempts");

            }
        } catch (Exception e)
        {
            logger.error("Error syncing user: '{}'", userId, e);
        }
    }

    @Override
    public boolean commit() throws LoginException
    {
        // This module leaves subject population to the subsequent modules that already handled the login with
        // 'PreAuthenticatedLogin' marker.
        return false;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options)
    {
        // TODO: Refactor implementation to use DS instead of manually wiring with whiteboard.

        super.initialize(subject, callbackHandler, sharedState, options);

        // 1. Get Whiteboard
        Whiteboard whiteboard = getWhiteboard();
        if (null == whiteboard)
        {
            logger.error("Header login module needs Whiteboard.");
            return;
        }

        // 2. Get Identity Provider Manager
        ExternalIdentityProviderManager externalIdentityProviderManager = WhiteboardUtils.getService(whiteboard, ExternalIdentityProviderManager.class);
        if (null == externalIdentityProviderManager)
        {
            logger.error("Header login module needs ExternalIdentityProviderManager.");
            return;
        }

        // 3. get Identity Provider
        externalIdentityProvider = externalIdentityProviderManager.getProvider(HeaderExternalIdentityProvider.NAME);
        if (null == externalIdentityProvider)
        {
            logger.error("Header login module needs ExternalIdentityProvider. Can't get IDP with name: '{}'",
                    HeaderExternalIdentityProvider.NAME);
            return;
        }

        // 4. get Sync Manager
        SyncManager syncManager = WhiteboardUtils.getService(whiteboard, SyncManager.class);
        if (null == syncManager)
        {
            logger.error("Header login module needs SyncManager.");
            return;
        }

        // 5. Get Sync Handler
        String syncHandlerName = "default";
        syncHandler = syncManager.getSyncHandler(syncHandlerName);
        if (null == syncHandler)
        {
            logger.error("Header login module needs SyncHandler. Can't get SyncHandler: '{}'", syncHandlerName);
            return;
        }
    }

    @Override
    public boolean abort() throws LoginException
    {
        // TODO:  Do we need to override this method?
        return super.abort();
    }

    @Override
    protected void clearState()
    {
        // TODO:  Do we need to override this method?
        super.clearState();
    }
}
