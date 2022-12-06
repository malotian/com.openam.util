package com.openam.util;

import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class OAuth2Client extends Entity {

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		final var oauth2Client = new OAuth2Client(id);

		final var acrs = new HashSet<String>();
		if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues")) {
			json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));
		}

		if (acrs.isEmpty()) {
			oauth2Client.addAttribute(Entity.EXTERNAL_AUTH_LEVEL, "N/A");
			if (acrs.contains("2")) {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_MFA);
			}
			if (acrs.contains("4") || acrs.contains("6")) {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_CERT);
			}

		} else {
			if (OpenAM.getInstance().getResourcesForInternalMFAPolicies().contains(oauth2Client)) {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_MFA);
			} else if (OpenAM.getInstance().getResourcesForInternalCERTPolicies().contains(oauth2Client)) {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH_LEVEL, Entity.AUTH_LEVEL_CERT);
			} else {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH_LEVEL, "PWD");
			}
			oauth2Client.addAttribute(Entity.EXTERNAL_AUTH_LEVEL, OpenAM.getInstance().getResourcesForExternalMFAPolices().contains(oauth2Client) ? "MFA" : "PWD");
		}

	}

	public static void process(final JsonNode oauth2Clients) {
		final var result = oauth2Clients.get("result");
		result.forEach(oa -> {
			try {
				OAuth2Client._process(oa);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}

	protected OAuth2Client(final String id) {
		super(id, EntityType.WSFED);
	}

}
