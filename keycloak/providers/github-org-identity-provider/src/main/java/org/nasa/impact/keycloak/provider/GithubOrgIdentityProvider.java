package org.nasa.impact.keycloak.provider;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.social.github.GitHubIdentityProvider;
import org.keycloak.events.EventBuilder;


/**
 * @author <a href="mailto:alukach@developmentseed.org">Anthony Lukach</a>
 */
public class GithubOrgIdentityProvider extends GitHubIdentityProvider {
    
    private final String apiUrl;
    private final String organization;

    private static final String DEFAULT_SCOPE = "user:email read:org";

    public GithubOrgIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);

        organization = config.getConfig().get("organization");
        apiUrl = super.getUrlFromConfig(config, super.API_URL_KEY, super.DEFAULT_API_URL);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        BrokeredIdentityContext user = super.doGetFederatedIdentity(accessToken);

        // String organization = getConfig().get("organization");
        if (organization == null || organization.isEmpty()) {
            throw new IdentityBrokerException("User is not a member of the required organization.");
        }
        String username = user.getUsername();
        boolean isMember = checkOrganizationMembership(accessToken, organization, username);
        if (!isMember) {
            logger.warn(String.format("User '%s' is NOT a member of the required organization '%s.", username, organization));
            throw new IdentityBrokerException("User is not a member of the required organization.");
        }

        logger.warn(String.format("User '%s' is a member of the required organization '%s.", username, organization));
        return user;
	}

    private boolean checkOrganizationMembership(String accessToken, String organization, String username) {
        // https://docs.github.com/en/rest/orgs/members?apiVersion=2022-11-28#check-organization-membership-for-a-user
        String orgUrl = apiUrl + String.format("/orgs/%s/members/%s", organization, username);
        try {
            SimpleHttp.Response response = SimpleHttp.doGet(orgUrl, session)
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Accept", "application/json")
                                .asResponse();
            int statusCode = response.getStatus();
            return statusCode == 204;
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not verify organization membership", e);
        }
    }

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}