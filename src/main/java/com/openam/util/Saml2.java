package com.openam.util;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class Saml2 extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(Saml2.class);
	public static Pattern patternCertMfa = Pattern.compile("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\|\\d+\\|service=(CERT|MFA)\\|default");

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();

		if (!json.has("metadata")) {
			Saml2.logger.warn("skipping, metadata missing : {} ", json.get("_id").asText());
			return;
		}

		final var metaData = json.get("metadata").asText();

		final var saml2 = new Saml2(id);
		OpenAM.getInstance().updateAuthAsPerPolicies(saml2);

		if (metaData.contains("IDPSSODescriptor")) {
			saml2.addAttribute(Entity.IDENTITY_PROVIDER);

			if (json.has("entityConfig")) {
				final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");
				final var matcher = Saml2.patternCertMfa.matcher(entityConfig);

				if (matcher.find()) {
					saml2.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
					saml2.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
				}
			}

		}
		if (metaData.contains("SPSSODescriptor"))
			saml2.addAttribute(Entity.SERVICE_PROVIDER);

		if (!json.has("entityConfig"))
			return;
		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		if (entityConfig.contains("hosted=\"true\""))
			saml2.addAttribute(Entity.HOSTED);
		else if (entityConfig.contains("hosted=\"false\""))
			saml2.addAttribute(Entity.REMOTE);

	}

	public static void process(final JsonNode saml2Entities) {
		final var result = saml2Entities.get("result");
		result.forEach(se -> {
			try {
				Saml2._process(se);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}

	protected Saml2(final String id) {
		super(id, EntityType.SAML2);
	}

	public boolean isIDP() {
		return hasAttribute(Entity.IDENTITY_PROVIDER);
	}

	public boolean isNotIDP() {
		return !hasAttribute(Entity.IDENTITY_PROVIDER);
	}

	public boolean isNotSP() {
		return !hasAttribute(Entity.SERVICE_PROVIDER);
	}

	public boolean isSP() {
		return hasAttribute(Entity.SERVICE_PROVIDER);
	}

}
