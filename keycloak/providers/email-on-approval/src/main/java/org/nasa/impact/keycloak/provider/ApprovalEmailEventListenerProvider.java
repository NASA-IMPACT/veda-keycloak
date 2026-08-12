package org.nasa.impact.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.urls.UrlType;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Emails a user when an administrator grants them the realm's approval role.
 *
 * Approval in these realms is expressed as a realm role (by default
 * {@code <realm>-approved}). Keycloak has no built-in notification for a role
 * being granted, so without this listener an approved user is never told they
 * can sign in.
 *
 * The listener reacts to realm role mapping admin events rather than to the
 * event's representation, and re-reads the user's effective roles from the
 * session. That keeps it correct regardless of whether admin event details are
 * enabled, and whether the role arrived directly or through a composite role.
 */
public class ApprovalEmailEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(ApprovalEmailEventListenerProvider.class);

    /** Template resolved from the realm's email theme (needs html/ and text/ variants). */
    private static final String EMAIL_TEMPLATE = "approval-notification.ftl";
    /** Subject key, resolved from the email theme's message bundle. */
    private static final String SUBJECT_KEY = "approvalNotificationSubject";
    /** Marks a user as already notified, so re-saving role mappings does not re-send. */
    private static final String NOTIFIED_ATTRIBUTE = "approvalNotifiedAt";

    /** Role mapping resource paths look like {@code users/<id>/role-mappings/realm}. */
    private static final Pattern USER_ROLE_MAPPING_PATH =
            Pattern.compile("^users/([^/]+)/role-mappings(/.*)?$");

    private final KeycloakSession session;
    private final Map<String, String> realmToApprovalRole;
    private final String defaultRoleSuffix;

    /**
     * @param session the current Keycloak session
     * @param realmToApprovalRole per-realm approval role name overrides
     * @param defaultRoleSuffix suffix used to derive the role name for realms without an override
     */
    public ApprovalEmailEventListenerProvider(
            KeycloakSession session,
            Map<String, String> realmToApprovalRole,
            String defaultRoleSuffix) {
        this.session = session;
        this.realmToApprovalRole = realmToApprovalRole != null ? realmToApprovalRole : Collections.emptyMap();
        this.defaultRoleSuffix = defaultRoleSuffix;
    }

    /**
     * User events are not relevant here: approval is always an admin action.
     * @param event ignored
     */
    @Override
    public void onEvent(Event event) {
        // No-op for user events
    }

    /**
     * Notify the user when a realm role mapping change leaves them holding the
     * approval role.
     * @param adminEvent the admin action that took place
     * @param includeRepresentation unused, we read the user's roles instead of the representation
     */
    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        if (!ResourceType.REALM_ROLE_MAPPING.equals(adminEvent.getResourceType())) {
            return;
        }

        OperationType operation = adminEvent.getOperationType();
        if (!OperationType.CREATE.equals(operation) && !OperationType.DELETE.equals(operation)) {
            return;
        }

        try {
            handleRoleMappingChange(adminEvent, operation);
        } catch (Exception e) {
            // A failed notification must never fail the role assignment itself.
            log.errorf(e, "Failed to handle approval notification for '%s'", adminEvent.getResourcePath());
        }
    }

    private void handleRoleMappingChange(AdminEvent adminEvent, OperationType operation) {
        String userId = userIdFromResourcePath(adminEvent.getResourcePath());
        if (userId == null) {
            // Role mappings can also target groups or clients, which we ignore.
            return;
        }

        RealmModel realm = resolveRealm(adminEvent);
        if (realm == null) {
            log.warnf("Could not resolve realm for admin event on '%s'; skipping", adminEvent.getResourcePath());
            return;
        }

        String roleName = approvalRoleName(realm.getName());
        RoleModel approvalRole = realm.getRole(roleName);
        if (approvalRole == null) {
            log.debugf("Realm '%s' has no '%s' role; skipping approval notification", realm.getName(), roleName);
            return;
        }

        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            log.warnf("User '%s' not found in realm '%s'; skipping approval notification", userId, realm.getName());
            return;
        }

        boolean approved = user.hasRole(approvalRole);

        if (OperationType.DELETE.equals(operation)) {
            // Approval was revoked: forget that we notified, so a later
            // re-approval sends a fresh email.
            if (!approved && user.getFirstAttribute(NOTIFIED_ATTRIBUTE) != null) {
                user.removeAttribute(NOTIFIED_ATTRIBUTE);
                log.infof("Approval role '%s' removed from user '%s' in realm '%s'; cleared %s",
                        roleName, user.getUsername(), realm.getName(), NOTIFIED_ATTRIBUTE);
            }
            return;
        }

        if (!approved) {
            // Some other role was granted.
            return;
        }

        if (user.getFirstAttribute(NOTIFIED_ATTRIBUTE) != null) {
            log.debugf("User '%s' in realm '%s' was already notified of approval; skipping",
                    user.getUsername(), realm.getName());
            return;
        }

        String address = user.getEmail();
        if (address == null || address.isBlank()) {
            log.warnf("User '%s' in realm '%s' has no email address; cannot send approval notification",
                    user.getUsername(), realm.getName());
            return;
        }

        try {
            sendApprovalEmail(realm, user);
        } catch (EmailException e) {
            // Leave the attribute unset so a retry (re-granting the role) can send again.
            log.errorf(e, "Failed to send approval notification to '%s' in realm '%s'", address, realm.getName());
            return;
        }

        user.setSingleAttribute(NOTIFIED_ATTRIBUTE, Instant.now().toString());
        log.infof("Sent approval notification to '%s' in realm '%s'", address, realm.getName());
    }

    private void sendApprovalEmail(RealmModel realm, UserModel user) throws EmailException {
        Map<String, Object> attributes = new HashMap<>();
        String link = signInLink(realm);
        if (link != null) {
            attributes.put("link", link);
        }

        // The email theme is resolved from the realm on the session context, which
        // during an admin request is not guaranteed to be the realm being edited.
        KeycloakContext context = session.getContext();
        RealmModel contextRealm = context.getRealm();
        boolean swapped = contextRealm == null || !realm.getId().equals(contextRealm.getId());
        if (swapped) {
            context.setRealm(realm);
        }
        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .send(SUBJECT_KEY, EMAIL_TEMPLATE, attributes);
        } finally {
            if (swapped && contextRealm != null) {
                context.setRealm(contextRealm);
            }
        }
    }

    /**
     * The URL the email invites the user to sign in at: the home URL of the
     * realm's application client, otherwise the realm's account console.
     */
    private String signInLink(RealmModel realm) {
        ClientModel client = applicationClient(realm);
        if (client != null) {
            String baseUrl = client.getBaseUrl();
            String url = baseUrl != null && !baseUrl.isBlank()
                    ? ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), baseUrl)
                    : ResolveRelative.resolveRootUrl(session, client.getRootUrl());
            if (url != null && !url.isBlank()) {
                log.debugf("Approval email for realm '%s' will link to client '%s' at %s",
                        realm.getName(), client.getClientId(), url);
                return url;
            }
        }

        try {
            String baseUri = session.getContext().getUri(UrlType.FRONTEND).getBaseUri().toString();
            if (!baseUri.endsWith("/")) {
                baseUri += "/";
            }
            return baseUri + "realms/" + realm.getName() + "/account";
        } catch (Exception e) {
            log.debugf("Could not determine a sign-in URL for realm '%s'; sending email without a link",
                    realm.getName());
            return null;
        }
    }

    /**
     * The client the approved user signs in to. Realms here have a single
     * browser-facing client carrying the application URL; when more than one
     * qualifies, the <realm>-api naming convention decides.
     */
    private ClientModel applicationClient(RealmModel realm) {
        List<ClientModel> candidates = realm.getClientsStream()
                .filter(ClientModel::isEnabled)
                .filter(ClientModel::isStandardFlowEnabled)
                .filter(client -> !client.isBearerOnly())
                .filter(client -> isSet(client.getRootUrl()) || isSet(client.getBaseUrl()))
                .sorted(Comparator.comparing(ClientModel::getClientId))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        String preferred = realm.getName() + "-api";
        return candidates.stream()
                .filter(client -> preferred.equals(client.getClientId()))
                .findFirst()
                .orElse(candidates.get(0));
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private String approvalRoleName(String realmName) {
        String configured = realmToApprovalRole.get(realmName);
        return configured != null && !configured.isBlank()
                ? configured.trim()
                : realmName + "-" + defaultRoleSuffix;
    }

    /**
     * Resolves the realm the admin action was performed against, which is not
     * necessarily the realm the administrator is authenticated to.
     */
    private RealmModel resolveRealm(AdminEvent adminEvent) {
        String realmId = adminEvent.getRealmId();
        if (realmId != null) {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm != null) {
                return realm;
            }
        }
        return session.getContext().getRealm();
    }

    private String userIdFromResourcePath(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        Matcher matcher = USER_ROLE_MAPPING_PATH.matcher(resourcePath);
        return matcher.matches() ? matcher.group(1) : null;
    }

    @Override
    public void close() {

    }
}
