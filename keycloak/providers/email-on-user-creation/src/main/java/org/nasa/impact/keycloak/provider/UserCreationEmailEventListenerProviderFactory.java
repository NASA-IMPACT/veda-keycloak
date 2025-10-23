package org.nasa.impact.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class UserCreationEmailEventListenerProviderFactory implements EventListenerProviderFactory {

    private String emailAddress = "";

    /**
     * Create the EventListenerProvider
     * @param keycloakSession the current keycloak session
     * @return a UserCreationEmailEventListenerProvider instance, with the current session and email address to send to
     */
    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new UserCreationEmailEventListenerProvider(keycloakSession, this.emailAddress);
    }

    /**
     * Initialise this factory with the email address config
     * @param config our config options set via the web UI
     */
    @Override
    public void init(Config.Scope config) {
        this.emailAddress = config.get("email-address");
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
                .helpText("The email address to send registration notifications to")
                .defaultValue("example@example.com")
                .add()
                .build();
    }

}