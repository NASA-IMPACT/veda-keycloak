package org.nasa.impact.keycloak.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Set;

/**
 * Email template provider that replaces the email-verification and password-reset
 * action links with short links from the openveda.cloud URL-shortener service
 * before the email is rendered. Long action-token URLs are rejected by some
 * receiving mail gateways (notably nasa.gov); a short first-party link avoids that.
 *
 * Only realms listed in the factory configuration are affected. Every failure
 * mode (shortener down, non-200 response, unexpected response body) falls back
 * to sending the original long link, so email delivery never depends on the
 * shortener being available.
 */
public class LinkShorteningEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    private static final Logger log = Logger.getLogger(LinkShorteningEmailTemplateProvider.class);

    /** Fields checked, in order, for the short link in the shortener's JSON response. */
    private static final String[] RESPONSE_FIELDS = {
            "short_url", "shortUrl", "short_link", "shortLink", "shortened_url", "link"
    };

    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int SOCKET_TIMEOUT_MILLIS = 5000;

    private final String serviceUrl;
    private final Set<String> realms;

    public LinkShorteningEmailTemplateProvider(KeycloakSession session, String serviceUrl, Set<String> realms) {
        super(session);
        this.serviceUrl = serviceUrl;
        this.realms = realms;
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        super.sendVerifyEmail(maybeShorten(link), expirationInMinutes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        super.sendPasswordReset(maybeShorten(link), expirationInMinutes);
    }

    /**
     * Shortens the link via the shortener service, returning the original link
     * when the realm is not opted in or the service call fails in any way.
     */
    private String maybeShorten(String link) {
        if (serviceUrl == null || realm == null || !realms.contains(realm.getName())) {
            return link;
        }
        try (SimpleHttp.Response response = SimpleHttp.doGet(serviceUrl, session)
                .param("url", link)
                .header("Accept", "application/json, text/plain")
                .connectTimeoutMillis(CONNECT_TIMEOUT_MILLIS)
                .socketTimeOutMillis(SOCKET_TIMEOUT_MILLIS)
                .asResponse()) {
            if (response.getStatus() != 200) {
                log.warnf("Link shortener returned HTTP %d for realm '%s'; sending the original link",
                        response.getStatus(), realm.getName());
                return link;
            }
            String shortLink = extractShortLink(response.asString());
            if (shortLink == null) {
                log.warnf("No short link found in the shortener response for realm '%s'; sending the original link",
                        realm.getName());
                return link;
            }
            log.debugf("Shortened link for realm '%s': %s", realm.getName(), shortLink);
            return shortLink;
        } catch (Exception e) {
            log.warnf(e, "Link shortener call failed for realm '%s'; sending the original link", realm.getName());
            return link;
        }
    }

    /**
     * Pulls the short link out of a response body, accepting either a JSON object
     * (any of {@link #RESPONSE_FIELDS}), a JSON string, or a bare text/plain URL.
     */
    private static String extractShortLink(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode node;
        try {
            node = JsonSerialization.readValue(body, JsonNode.class);
        } catch (IOException e) {
            // Not JSON: services such as tinyurl.com return the URL as plain text
            return validated(body);
        }
        if (node.isTextual()) {
            return validated(node.asText());
        }
        for (String field : RESPONSE_FIELDS) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual()) {
                String candidate = validated(value.asText());
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String validated(String candidate) {
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.trim();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }
}
