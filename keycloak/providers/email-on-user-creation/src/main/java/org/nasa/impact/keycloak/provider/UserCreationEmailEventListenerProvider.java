package org.nasa.impact.keycloak.provider;

/**
 * Implementation adapted from:
 * https://github.com/stfc/keycloak-email-on-user-creation
 */

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
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
            EmailSenderProvider senderProvider = session.getProvider(EmailSenderProvider.class);
            String realmName = session.getContext().getRealm().getName();

            log.infof("Registration event for realm '%s' detected (stage='%s')", realmName, stage);
            
            String toAddresses = realmToEmail.get(realmName);
            if (toAddresses == null || toAddresses.isBlank()) {
                log.infof("No email mapping for realm '%s'; skipping notification", realmName);
                return;
            }
            toAddresses = toAddresses.trim();  // Clean up any leading/trailing whitespace
            
            // Split comma-separated addresses
            String[] recipients = toAddresses.split(",");
            log.infof("Sending new-user notification for realm '%s' to %d recipient(s) (stage='%s')", realmName, recipients.length, stage);

            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());

            String username = user != null ? user.getUsername() : event.getDetails().get("username");
            String email = user != null ? user.getEmail() : event.getDetails().get("email");
            String firstName = user != null ? user.getFirstName() : null;
            String lastName = user != null ? user.getLastName() : null;
            String institution = user != null ? user.getFirstAttribute("institution") : null;
            String project = user != null ? user.getFirstAttribute("project") : null;
            String sponsor = user != null ? user.getFirstAttribute("sponsor") : null;
            String funding = user != null ? user.getFirstAttribute("funding") : null;
            String additionalDetails = user != null ? user.getFirstAttribute("additional_details") : null;
            
            StringBuilder sbtxt = new StringBuilder();
            sbtxt.append("A new Keycloak user has registered in the %s realm (%s)%n%n".formatted(realmName, stage));
            sbtxt.append("Username: ").append(username).append("\n");
            sbtxt.append("First name: ").append(firstName).append("\n");
            sbtxt.append("Last name: ").append(lastName).append("\n");
            sbtxt.append("Email: ").append(email).append("\n");
            if (institution != null) sbtxt.append("Institution: ").append(institution).append("\n");
            if (project != null) sbtxt.append("Project: ").append(project).append("\n");
            if (sponsor != null) sbtxt.append("Sponsor: ").append(sponsor).append("\n");
            if (funding != null) sbtxt.append("Funding: ").append(funding).append("\n");
            if (additionalDetails != null) sbtxt.append("Additional Details: ").append(additionalDetails).append("\n");

            StringBuilder sbhtml = new StringBuilder();
            sbhtml.append("<p>A new Keycloak user has registered in the %s realm (%s)</p>".formatted(realmName, stage));
            sbhtml.append("<p>Username: ").append(username).append("</p>");
            sbhtml.append("<p>First name: ").append(firstName).append("</p>");
            sbhtml.append("<p>Last name: ").append(lastName).append("</p>");
            sbhtml.append("<p>Email: ").append(email).append("</p>");
            if (institution != null) sbhtml.append("<p>Institution: ").append(institution).append("</p>");
            if (project != null) sbhtml.append("<p>Project: ").append(project).append("</p>");
            if (sponsor != null) sbhtml.append("<p>Sponsor: ").append(sponsor).append("</p>");
            if (funding != null) sbhtml.append("<p>Funding: ").append(funding).append("</p>");
            if (additionalDetails != null) sbhtml.append("<p>Additional Details: ").append(additionalDetails).append("</p>");
            
            // Send to each recipient individually
            for (String recipient : recipients) {
                String trimmedRecipient = recipient.trim();
                try {
                    log.infof("Sending email to: %s", trimmedRecipient);
                    senderProvider.send(session.getContext().getRealm().getSmtpConfig(), trimmedRecipient, "New User Registration with Keycloak", sbtxt.toString(), sbhtml.toString());
                } catch (EmailException e) {
                    log.errorf("Failed to send email to %s: %s", trimmedRecipient, e.getMessage());
                }
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