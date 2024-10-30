package org.nasa.impact.keycloak.provider;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.KeycloakSession;

public class CILogonIdentityProvider extends OIDCIdentityProvider {

    public CILogonIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl("https://cilogon.org/authorize");
        config.setTokenUrl("https://cilogon.org/oauth2/token");
        config.setUserInfoUrl("https://cilogon.org/oauth2/userinfo");
        config.setIssuer("https://cilogon.org");
    }

    @Override
    protected String getDefaultScopes() {
        return "openid profile email org.cilogon.userinfo";
    }
}
