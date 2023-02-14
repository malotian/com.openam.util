package com.openam.util;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class Wsfed extends Entity {

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig")) {
			return;
		}

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		final var wsfed = new Wsfed(id);
		OpenAM.getInstance().updateAuthAsPerPolicies(wsfed);

		if (entityConfig.contains("IDPSSOConfig")) {
			wsfed.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);
		}

		if (entityConfig.contains("SPSSOConfig")) {
			wsfed.addAttribute(Entity.SP_IDP, Entity.SERVICE_PROVIDER);
		}

		if (entityConfig.contains("hosted=\"true\"")) {
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
		} else if (entityConfig.contains("hosted=\"false\"")) {
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
		}

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
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).equals(Entity.IDENTITY_PROVIDER);
	}

	public boolean isNotIDP() {
		return !isIDP();
	}

	public boolean isNotSP() {
		return !isSP();
	}

	public boolean isSP() {
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER);
	}
}
