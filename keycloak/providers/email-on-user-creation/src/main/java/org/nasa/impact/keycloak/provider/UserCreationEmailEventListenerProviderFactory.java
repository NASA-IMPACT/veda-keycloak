package org.nasa.impact.keycloak.provider;

/**
 * Implementation adapted from:
 * https://github.com/stfc/keycloak-email-on-user-creation
 */

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class UserCreationEmailEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(UserCreationEmailEventListenerProviderFactory.class);
    private Map<String, String> realmToEmail = Collections.emptyMap();
    private String stage = "";

    /**
     * Create the EventListenerProvider
     * @param keycloakSession the current keycloak session
     * @return a UserCreationEmailEventListenerProvider instance, with the current session and email address to send to
     */
    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UserCreationEmailEventListenerProvider(keycloakSession, this.realmToEmail, this.stage);
    }

    /**
     * Initialise this factory with the email address config
     * @param config our config options set via the web UI
     */
    @Override
    public void init(Config.Scope config) {
        this.stage = config.get("stage");
        log.infof("stage from Keycloak config scope: '%s'", this.stage);
        
        Map<String, String> mapping = new HashMap<>();
        
        // Dynamically discover realm email configurations from environment variables
        // Pattern: KEYCLOAK_EMAIL_ADDRESS_<REALM>
        String envPrefix = "KEYCLOAK_EMAIL_ADDRESS_";
        
        Map<String, String> envVars = System.getenv();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String envKey = entry.getKey();
            if (envKey.startsWith(envPrefix)) {
                // Extract realm name from environment variable
                String realmUpper = envKey.substring(envPrefix.length());
                String realm = realmUpper.toLowerCase();
                String email = entry.getValue();
                
                if (!realm.isEmpty() && email != null && !email.isBlank()) {
                    mapping.put(realm, email);
                    log.infof("Found %s: configured email notification(s) for realm '%s' to '%s'", envKey, realm, email);
                }
            }
        }
        
        this.realmToEmail = mapping;
        log.infof("User creation listener configured with realm-to-email map: %s", this.realmToEmail);
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
        return "email-on-user-creation";
    }

    /**
     * Build up the list of configuration properties this provider supports
     * @return the configuration properties for stage (realm emails are read from env vars)
     */
    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("stage")
                .type("string")
                .helpText("Deployment stage identifier (e.g., dev, prod). Realm-specific emails are configured via environment variables: KEYCLOAK_EMAIL_ADDRESS_<REALM> (e.g., KEYCLOAK_EMAIL_ADDRESS_VEDA)")
                .defaultValue("")
                .add()
                .build();
    }

}