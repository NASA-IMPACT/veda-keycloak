package org.nasa.impact.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for {@link LinkShorteningEmailTemplateProvider}.
 *
 * Configuration (via KC_SPI_EMAIL_TEMPLATE_LINK_SHORTENER_* environment variables):
 *   - service-url (KC_SPI_EMAIL_TEMPLATE_LINK_SHORTENER_SERVICE_URL):
 *       shortener endpoint, defaults to https://openveda.cloud/service/link/shorten
 *   - realms (KC_SPI_EMAIL_TEMPLATE_LINK_SHORTENER_REALMS):
 *       comma-separated realm names whose verification emails get short links,
 *       defaults to "eie"
 *   - shorten (KC_SPI_EMAIL_TEMPLATE_LINK_SHORTENER_SHORTEN):
 *       set to false to skip shortening and send the original links. This is a
 *       runtime option; it must NOT be named with an "-enabled" suffix, because
 *       Keycloak treats every "spi-*-enabled" key as a build-time option and a
 *       server started with --optimized refuses to boot when one differs from
 *       the value persisted at image build.
 *
 * The {@link #order()} above zero makes this the default emailTemplate provider,
 * overriding Keycloak's built-in "freemarker" provider (order 0).
 */
public class LinkShorteningEmailTemplateProviderFactory implements EmailTemplateProviderFactory {

    public static final String ID = "link-shortener";

    private static final Logger log = Logger.getLogger(LinkShorteningEmailTemplateProviderFactory.class);

    private static final String DEFAULT_SERVICE_URL = "https://openveda.cloud/service/link/shorten";
    private static final String DEFAULT_REALMS = "eie";

    private String serviceUrl = DEFAULT_SERVICE_URL;
    private Set<String> realms = Set.of(DEFAULT_REALMS);
    private boolean shortenEnabled = true;

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        // A null serviceUrl makes the provider a pass-through (stock behavior)
        return new LinkShorteningEmailTemplateProvider(session, shortenEnabled ? serviceUrl : null, realms);
    }

    @Override
    public void init(Config.Scope config) {
        this.shortenEnabled = config.getBoolean("shorten", true);

        String url = config.get("service-url");
        if (url != null && !url.isBlank()) {
            this.serviceUrl = url.trim();
        }

        String realmsConfig = config.get("realms");
        if (realmsConfig != null && !realmsConfig.isBlank()) {
            this.realms = Arrays.stream(realmsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }

        if (shortenEnabled) {
            log.infof("Verification-email link shortening enabled for realms %s via %s", this.realms, this.serviceUrl);
        } else {
            log.info("Verification-email link shortening disabled; original links will be sent");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        // Must be higher than the built-in "freemarker" provider (0) so that
        // Keycloak selects this provider as the default for the emailTemplate SPI.
        return 100;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("service-url")
                .type("string")
                .helpText("URL-shortener endpoint called with ?url=<long link>; must return the short link as JSON.")
                .defaultValue(DEFAULT_SERVICE_URL)
                .add()
                .property()
                .name("realms")
                .type("string")
                .helpText("Comma-separated realm names whose email-verification links are shortened.")
                .defaultValue(DEFAULT_REALMS)
                .add()
                .property()
                .name("shorten")
                .type("boolean")
                .helpText("Set to false to send the original long links (runtime option, safe with --optimized).")
                .defaultValue("true")
                .add()
                .build();
    }
}
