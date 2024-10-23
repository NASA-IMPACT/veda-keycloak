package org.nasa.impact.keycloak.provider;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * @author Anthony Lukach
 */
public class GithubOrgIdentityProviderFactory extends AbstractIdentityProviderFactory<GithubOrgIdentityProvider> implements SocialIdentityProviderFactory<GithubOrgIdentityProvider> {

    public static final String PROVIDER_ID = "github-org";

    @Override
    public String getName() {
        return "GitHub with Organization Check";
    }

    @Override
    public GithubOrgIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GithubOrgIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }

    @Override
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property()
                .name("organization")
                .label("Required GitHub Organization")
                .helpText("GitHub organization to check for membership.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .build();
    }
}
