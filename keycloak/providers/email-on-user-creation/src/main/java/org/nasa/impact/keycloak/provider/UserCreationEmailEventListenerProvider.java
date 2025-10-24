package org.nasa.impact.keycloak.provider;

/**
 * Implementation adapted from:
 * https://github.com/stfc/keycloak-email-on-user-creation
 */

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import java.util.Map;
import java.util.Collections;

public class UserCreationEmailEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UserCreationEmailEventListenerProvider.class);
    private final KeycloakSession session;
    private final Map<String, String> realmToEmail;
    private final String stage;

    /**
     * Init the UserCreationEmailEventListenerProvider with key instance info
     * @param session our current session
     * @param realmToEmail mapping of realm name to email address to send emails to
     * @param stage deployment stage identifier
     */
    public UserCreationEmailEventListenerProvider(KeycloakSession session, Map<String, String> realmToEmail, String stage) {
        this.session = session;
        this.realmToEmail = realmToEmail != null ? realmToEmail : Collections.emptyMap();
        this.stage = stage != null ? stage : "";
    }

    /**
     * When a user registers for the service, send an informational email to the administrators of the service
     * (or any email of your choice, configured via the web UI) with some information like UUID, email and IP address
     * This is to have notifications when new users register.
     * @param event the event that has taken place, we only care about REGISTRATION event types
     */
    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())) {
            DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
            String realmName = session.getContext().getRealm().getName();
            log.infof("Registration event for realm '%s' detected (stage='%s')", realmName, stage);
            String to = realmToEmail.get(realmName);
            if (to == null || to.isBlank()) {
                log.infof("No email mapping for realm '%s'; skipping notification", realmName);
                return;
            }
            log.infof("Sending new-user notification for realm '%s' to '%s' (stage='%s')", realmName, to, stage);
            StringBuilder sbtxt = new StringBuilder();
            sbtxt.append("A new Keycloak user has registered in the %s realm (%s)%n%n".formatted(realmName, stage));
            sbtxt.append("Username: ").append(event.getDetails().get("username")).append("\n");
            sbtxt.append("First name: ").append(event.getDetails().get("firstName")).append("\n");
            sbtxt.append("Last name: ").append(event.getDetails().get("lastName")).append("\n");
            sbtxt.append("Email: ").append(event.getDetails().get("email")).append("\n");

            StringBuilder sbhtml = new StringBuilder();
            sbhtml.append("<p>A new Keycloak user has registered in the %s realm (%s)</p>".formatted(realmName, stage));
            sbhtml.append("<p>Username: ").append(event.getDetails().get("username")).append("</p>");
            sbhtml.append("<p>First name: ").append(event.getDetails().get("firstName")).append("</p>");
            sbhtml.append("<p>Last name: ").append(event.getDetails().get("lastName")).append("</p>");
            sbhtml.append("<p>Email: ").append(event.getDetails().get("email")).append("</p>");
            try {
                senderProvider.send(session.getContext().getRealm().getSmtpConfig(), to, "New User Registration with Keycloak", sbtxt.toString(), sbhtml.toString());
            } catch (EmailException e) {
                log.error("Failed to send email", e);
            }
        }
    }

    /**
     * For admin events - which we don't care about, so we ignore them
     * @param adminEvent ignored
     * @param b ignored
     */
    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        // No-op for admin events
    }

    @Override
    public void close() {

    }
}