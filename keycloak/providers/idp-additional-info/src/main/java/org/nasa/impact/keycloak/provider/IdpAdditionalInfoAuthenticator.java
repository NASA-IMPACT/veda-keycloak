package org.nasa.impact.keycloak.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class IdpAdditionalInfoAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        String isNewUser = session.getAuthNote("IS_NEW_USER");
        String idpAlias = session.getAuthNote("IDENTITY_PROVIDER");
        if ("true".equals(isNewUser) && idpAlias != null) {
            context.challenge(context.form().createForm("idp-additional-info.ftl"));
        } else {
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // For now, just continue (process form fields here later if needed)
        context.success();
    }

    @Override
    public boolean requiresUser() { return false; }
    @Override
    public boolean configuredFor(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) { return true; }
    @Override
    public void setRequiredActions(org.keycloak.models.KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user) {}
    @Override
    public void close() {}
} 