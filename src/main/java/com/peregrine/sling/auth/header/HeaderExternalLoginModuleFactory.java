package com.peregrine.sling.auth.header;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.jackrabbit.oak.osgi.OsgiWhiteboard;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityProviderManager;
import org.apache.jackrabbit.oak.spi.security.authentication.external.SyncManager;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.whiteboard.Registration;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.security.auth.spi.LoginModule;

public class HeaderExternalLoginModuleFactory
//public class HeaderExternalLoginModuleFactory implements LoginModuleFactory
{

    private final Logger logger = LoggerFactory.getLogger(HeaderExternalLoginModuleFactory.class);
    /*

    @Reference
    private SyncManager syncManager;

    @Reference
    private ExternalIdentityProviderManager idpManager;

    @Reference
    private Repository repository;

    private ConfigurationParameters osgiConfig;

    private Registration mbeanRegistration;

    @SuppressWarnings("UnusedDeclaration")
    @Activate
    private void activate(ComponentContext context) {
        logger.warn("GASTON: Activating HeaderExternalLoginModuleFactory");
        osgiConfig = ConfigurationParameters.of(context.getProperties());
        String idpName = osgiConfig.getConfigValue("idp.name", "");
        String sncName = osgiConfig.getConfigValue("sync.handlerName", "");

        logger.warn("GASTON: Getting OSGi configuration: '{}'", osgiConfig);

        Whiteboard whiteboard = new OsgiWhiteboard(context.getBundleContext());

    }

    @SuppressWarnings("UnusedDeclaration")
    @Deactivate
    private void deactivate() {
        logger.info("Deactivating HeaderExternalLoginModuleFactory");
    }

    @Override
    public LoginModule createLoginModule() {
        logger.warn("GASTON: Creating login module with OSGi configuration: '{}'", osgiConfig);
        return new HeaderExternalLoginModule(osgiConfig);
    }
    */

}
