package com.peregrine.sling.auth.header;

import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.oak.spi.security.authentication.PreAuthenticatedLogin;
import org.osgi.service.component.annotations.Component;
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

    static Set<Class> SUPPORTED_CREDENTIALS = new HashSet<Class>();
    static {
        SUPPORTED_CREDENTIALS.add(HeaderCredentials.class);
    }

    private ConfigurationParameters osgiConfig;


    public HeaderExternalLoginModule() {
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

        if (credentials instanceof HeaderCredentials) {
            final String userId = ((HeaderCredentials) credentials).getUserId();
            if (userId == null) {
                logger.warn("Could not extract userId from credentials");
            } else {
                sharedState.put(SHARED_KEY_PRE_AUTH_LOGIN, new PreAuthenticatedLogin(userId));
                sharedState.put(SHARED_KEY_CREDENTIALS, new SimpleCredentials(userId, new char[0]));
                sharedState.put(SHARED_KEY_LOGIN_NAME, userId);
                logger.info("Login succeeded with trusted user: {}", userId);
            }
        }

        // subsequent login modules need to succeed and process the 'PreAuthenticatedLogin'
        return false;
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
        // TODO:  Do we need to override this method?
        logger.debug("initialize() called");
        super.initialize(subject, callbackHandler, sharedState, options);
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
