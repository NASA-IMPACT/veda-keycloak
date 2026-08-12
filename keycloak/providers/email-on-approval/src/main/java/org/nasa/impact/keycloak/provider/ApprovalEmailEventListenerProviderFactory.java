package org.nasa.impact.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalEmailEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(ApprovalEmailEventListenerProviderFactory.class);

    /** Overrides the approval role name for a realm, e.g. KEYCLOAK_APPROVAL_ROLE_EIE=eie-approved */
    private static final String ROLE_ENV_PREFIX = "KEYCLOAK_APPROVAL_ROLE_";
    private static final String DEFAULT_ROLE_SUFFIX = "approved";

    private Map<String, String> realmToApprovalRole = Collections.emptyMap();
    private String defaultRoleSuffix = DEFAULT_ROLE_SUFFIX;

    /**
     * Create the EventListenerProvider
     * @param keycloakSession the current keycloak session
     * @return an ApprovalEmailEventListenerProvider instance with the resolved configuration
     */
    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new ApprovalEmailEventListenerProvider(
                keycloakSession, this.realmToApprovalRole, this.defaultRoleSuffix);
    }

    /**
     * Initialise this factory. Per-realm settings are read from environment
     * variables, following the same convention as the email-on-user-creation
     * listener; realms without an override use the <realm>-<suffix> role.
     * @param config our config options set via the web UI or KC_SPI_ variables
     */
    @Override
    public void init(Config.Scope config) {
        String suffix = config.get("roleSuffix");
        if (suffix != null && !suffix.isBlank()) {
            this.defaultRoleSuffix = suffix.trim();
        }

        this.realmToApprovalRole = realmMappingFromEnv(ROLE_ENV_PREFIX);

        log.infof("Approval email listener configured with default role name '<realm>-%s', role overrides: %s",
                this.defaultRoleSuffix, this.realmToApprovalRole);
    }

    /**
     * Collects environment variables of the form {@code <prefix><REALM>} into a
     * realm name to value map.
     */
    private Map<String, String> realmMappingFromEnv(String envPrefix) {
        Map<String, String> mapping = new HashMap<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String envKey = entry.getKey();
            if (!envKey.startsWith(envPrefix)) {
                continue;
            }

            String realm = envKey.substring(envPrefix.length()).toLowerCase();
            String value = entry.getValue();
            if (!realm.isEmpty() && value != null && !value.isBlank()) {
                mapping.put(realm, value.trim());
                log.infof("Found %s: approval notification setting for realm '%s'", envKey, realm);
            }
        }
        return mapping;
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    /**
     * Get the ID of this provider
     * @return ID of provider
     */
    @Override
    public String getId() {
        return "email-on-approval";
    }

    /**
     * Build up the list of configuration properties this provider supports
     * @return the configuration properties for the default role suffix
     */
    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("roleSuffix")
                .type("string")
                .helpText("Suffix of the realm role that marks a user as approved, appended to the realm "
                        + "name (e.g. 'approved' means the 'eie-approved' role in the 'eie' realm). Per-realm "
                        + "overrides are configured via environment variables: KEYCLOAK_APPROVAL_ROLE_<REALM>.")
                .defaultValue(DEFAULT_ROLE_SUFFIX)
                .add()
                .build();
    }
}
