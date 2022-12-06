package com.openam.util;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class Wsfed extends Entity {

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig"))
			return;

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		final var wsfed = new Wsfed(id);

		if (entityConfig.contains("IDPSSOConfig")) {
			wsfed.addAttribute(Entity.IDENTITY_PROVIDER);
		}

		if (entityConfig.contains("SPSSOConfig")) {
			wsfed.addAttribute(Entity.SERVICE_PROVIDER);
		}

		if (entityConfig.contains("hosted=\"true\"")) {
			wsfed.addAttribute(Entity.HOSTED);
		}

		else if (entityConfig.contains("hosted=\"false\"")) {
			wsfed.addAttribute(Entity.REMOTE);
		}

		if (OpenAM.getInstance().getResourcesForInternalMFAPolicies().contains(wsfed)) {
			wsfed.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_MFA);
		} else if (OpenAM.getInstance().getResourcesForInternalCERTPolicies().contains(wsfed)) {
			wsfed.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_CERT);
		} else {
			wsfed.addAttribute(Entity.INTERNAL_AUTH_LEVEL, "PWD");
		}
		wsfed.addAttribute(Entity.EXTERNAL_AUTH_LEVEL, OpenAM.getInstance().getResourcesForExternalMFAPolices().contains(wsfed) ? "MFA" : "PWD");

	}

	public static void main(final String[] args) {
		System.out.println("aaurn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|2|service=CERT|defaultaa"
				.matches(".*urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\|\\d+\\|service=(CERT|MFA)\\|default.*"));
	}

	public static void process(final JsonNode saml2Entities) {
		final var result = saml2Entities.get("result");
		result.forEach(se -> {
			try {
				Wsfed._process(se);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}

	protected Wsfed(final String id) {
		super(id, EntityType.WSFED);
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
