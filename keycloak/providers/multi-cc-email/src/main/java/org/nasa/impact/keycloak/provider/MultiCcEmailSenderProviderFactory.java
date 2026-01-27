package org.nasa.impact.keycloak.email;

import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Map;

public class MultiCcEmailSenderProviderFactory implements EmailSenderProviderFactory {

    public static final String ID = "multi-cc-email";

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        // pass authenticator map if you need it, or null
        return new MultiCcEmailSenderProvider(session, Map.of());
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }
}