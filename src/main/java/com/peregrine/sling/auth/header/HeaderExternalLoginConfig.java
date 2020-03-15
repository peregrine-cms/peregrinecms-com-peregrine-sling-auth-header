package com.peregrine.sling.auth.header;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Header External Login Configuration",
        description = "The configuration for the HeaderExternalLoginModule."
)
public @interface HeaderExternalLoginConfig
{
    public static final int DEFAULT_JAAS_RANKING = 5000;
    public static final String DEEFAULT_JAAS_CONTROL_FLAG = "sufficient";
    public static final String DEFAULT_JAAS_REALM_NAME = "jackrabbit.oak";

    @AttributeDefinition(type = AttributeType.INTEGER, name = "%jaasRanking.name", description = "%jaasRanking.description") int jaas_ranking() default DEFAULT_JAAS_RANKING;

    @AttributeDefinition(options = {@Option(label = "Optional", value = "optional"),
            @Option(label = "Required", value = "required"), @Option(label = "Requisite", value = "requisite"),
            @Option(label = "Sufficient", value = DEEFAULT_JAAS_CONTROL_FLAG)}, name = "%jaasControlFlag.name", description = "%jaasControlFlag.description") String jaas_controlFlag() default DEEFAULT_JAAS_CONTROL_FLAG;

    @AttributeDefinition(name = "%jaasRealm.name", description = "%jaasRealm.description") String jaas_realmName() default DEFAULT_JAAS_REALM_NAME;

    @AttributeDefinition(name = "Sync Handler Name", description = "Name of the sync handler to be retrieved from the SyncManager") String sync_handlerName() default "default"; // OSGi property: sync.handlerName

    @AttributeDefinition(name = "External IDP Name", description = "Name of the external IDP to be retrieved from the ExternalIdentityProviderManager") String idp_name(); // OSGi property: idp.name
}
