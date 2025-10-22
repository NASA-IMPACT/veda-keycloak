package uk.ac.stfc.auth.emailonusercreation;

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public class UserCreationEmailEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(UserCreationEmailEventListenerProvider.class);
    private final KeycloakSession session;
    private final String emailAddress;

    /**
     * Init the UserCreationEmailEventListenerProvider with key instance info
     * @param session our current session
     * @param emailAddress the email address to send emails to
     */
    public UserCreationEmailEventListenerProvider(KeycloakSession session, String emailAddress) {
        this.session = session;
        this.emailAddress = emailAddress;
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
            StringBuilder sbtxt = new StringBuilder();
            sbtxt.append("A new user has registered for SCARF access via Keycloak\n\n");
            sbtxt.append("User UUID: ").append(event.getUserId()).append("\n");
            sbtxt.append("IP Address: ").append(event.getIpAddress()).append("\n");
            sbtxt.append("Email from IdP: ").append(event.getDetails().get("email")).append("\n");

            StringBuilder sbhtml = new StringBuilder();
            sbhtml.append("<p>A new user has registered for SCARF access via Keycloak</p>");
            sbhtml.append("<p>User UUID: ").append(event.getUserId()).append("</p>");
            sbhtml.append("<p>IP Address: ").append(event.getIpAddress()).append("</p>");
            sbhtml.append("<p>Email from IdP: ").append(event.getDetails().get("email")).append("</p>");
            try {
                senderProvider.send(session.getContext().getRealm().getSmtpConfig(), emailAddress, "New user on Keycloak", sbtxt.toString(), sbhtml.toString());
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

    }

    @Override
    public void close() {

    }
}