package org.nasa.impact.keycloak.provider;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class CILogonIdentityProviderFactory extends AbstractIdentityProviderFactory<CILogonIdentityProvider> {

    public static final String PROVIDER_ID = "cilogon";

    @Override
    public String getName() {
        return "CILogon";
    }

    @Override
    public CILogonIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(model);
        return new CILogonIdentityProvider(session, config);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("clientId")
                .label("Client ID")
                .helpText("Client ID for CILogon.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name("clientSecret")
                .label("Client Secret")
                .helpText("Client Secret for CILogon.")
                .type(ProviderConfigProperty.PASSWORD)
                .add()
                .build();
    }
}
