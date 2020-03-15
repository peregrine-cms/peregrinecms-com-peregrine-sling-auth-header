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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
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
@Designate(
        ocd = HeaderExternalLoginConfig.class
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
        logger.debug("Initializing default constructor");
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
            if (userId == null)
            {
                logger.warn("Could not extract userId from credentials");
            } else
            {
                sharedState.put(SHARED_KEY_PRE_AUTH_LOGIN, new PreAuthenticatedLogin(userId));
                sharedState.put(SHARED_KEY_CREDENTIALS, new SimpleCredentials(userId, new char[0]));
                sharedState.put(SHARED_KEY_LOGIN_NAME, userId);
                logger.info("Adding pre-authenticated login user '{}' to shared state.", userId);

                handleUserSync(userId);
            }
        }

        // subsequent login modules need to succeed and process the 'PreAuthenticatedLogin'
        return false;
    }

    private void handleUserSync(final String userId)
    {
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
                    logger.info("Found identity: '{}' for user: '{}'", syncedIdentity, userId);
                    // TODO: Should we sync existing users?
                    return;
                }

                logger.info("Could not find existing identity: '{}'", userId);

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
                logger.debug("Got UserManager");


                userManager.createUser(userId, (new char[0]).toString());
                logger.info("Created user: '{}'", userId);

                int numAttempt = 0;
                while (numAttempt++ < MAX_SYNC_ATTEMPTS)
                {
                    SyncContext context = null;
                    try
                    {
                        context = syncHandler.createContext(externalIdentityProvider, userManager,
                                new ValueFactoryImpl(root, NamePathMapper.DEFAULT));
                        context.sync(userId);
                        root.commit();
                        logger.info("Synced user: '{}'", externalUser.getId());
                        return;
                    } catch (CommitFailedException e)
                    {
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
        logger.debug("initialize() called");
        super.initialize(subject, callbackHandler, sharedState, options);

        // 1. Get Whiteboard
        Whiteboard whiteboard = getWhiteboard();
        if (null == whiteboard)
        {
            logger.error("Header login module needs Whiteboard.");
            return;
        }
        logger.debug("Got Whiteboard");

        // 2. Get Identity Provider Manager
        ExternalIdentityProviderManager externalIdentityProviderManager = WhiteboardUtils.getService(whiteboard, ExternalIdentityProviderManager.class);
        if (null == externalIdentityProviderManager)
        {
            logger.error("Header login module needs ExternalIdentityProviderManager.");
            return;
        }
        logger.debug("Got ExternalIdentityProviderManager");

        // 3. get Identity Provider
        externalIdentityProvider = externalIdentityProviderManager.getProvider(HeaderExternalIdentityProvider.NAME);
        if (null == externalIdentityProvider)
        {
            logger.error("Header login module needs ExternalIdentityProvider. Can't get IDP with name: '{}'",
                    HeaderExternalIdentityProvider.NAME);
            return;
        }
        logger.debug("Got ExternalIdentityProvider");


        // 4. get Sync Manager
        SyncManager syncMgr = WhiteboardUtils.getService(whiteboard, SyncManager.class);
        if (null == syncMgr)
        {
            logger.error("Header login module needs SyncManager.");
            return;
        }
        logger.debug("Got SyncManager");

        // 5. Get Syn Handler
        // TODO: Remember to create the OSGi configuration
        String syncHandlerName = "default";
        syncHandler = syncMgr.getSyncHandler(syncHandlerName);
        if (null == syncHandler)
        {
            logger.error("Header login module needs SyncHandler. Can't get SyncHandler: '{}'", syncHandlerName);
            return;
        }
        logger.debug("Got SyncHandler: '{}'", syncHandler);
    }

    @Override
    public boolean abort() throws LoginException
    {
        // TODO:  Do we need to override this method?
        logger.debug("abort() called");
        return super.abort();
    }

    @Override
    protected void clearState()
    {
        // TODO:  Do we need to override this method?
        logger.debug("clearState() called");
        super.clearState();
    }
}
