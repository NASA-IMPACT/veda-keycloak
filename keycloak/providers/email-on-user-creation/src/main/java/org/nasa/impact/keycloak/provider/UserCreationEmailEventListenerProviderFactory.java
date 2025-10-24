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
        String raw = config.get("email-address");
        log.infof("email-address from Keycloak config scope: '%s'", raw);
        this.stage = config.get("stage");
        log.infof("stage from Keycloak config scope: '%s'", this.stage);
        Map<String, String> mapping = new HashMap<>();
        if (raw != null && !raw.isBlank()) {
            for (String pair : raw.split(",")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    String realm = parts[0].trim();
                    String addr = parts[1].trim();
                    if (!realm.isEmpty() && !addr.isEmpty()) {
                        mapping.put(realm, addr);
                    }
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
     * @return a single property in a list, the email address
     */
    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("email-address")
                .type("string")
                .helpText("Comma-separated realm=email pairs. Example: veda=ops@exa.mple,maap=ops@exa.mple")
                .defaultValue("")
                .add()
                .property()
                .name("stage")
                .type("string")
                .helpText("Deployment stage identifier (e.g., dev, prod)")
                .defaultValue("")
                .add()
                .build();
    }

}