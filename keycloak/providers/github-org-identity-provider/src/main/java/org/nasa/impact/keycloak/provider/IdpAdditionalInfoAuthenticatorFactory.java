package org.nasa.impact.keycloak.provider;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class IdpAdditionalInfoAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "idp-additional-info-authenticator";

    @Override
    public String getId() { return PROVIDER_ID; }

    @Override
    public String getDisplayType() { return "IdP Additional Info Form"; }

    @Override
    public String getHelpText() { return "Shows a custom form to new IdP users."; }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IdpAdditionalInfoAuthenticator();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {}
    @Override
    public void postInit(KeycloakSessionFactory factory) {}
    @Override
    public void close() {}
    @Override
    public boolean isConfigurable() { return false; }
    @Override
    public List<ProviderConfigProperty> getConfigProperties() { return null; }
    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[] { Requirement.REQUIRED, Requirement.DISABLED };
    }
} 