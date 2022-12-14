package com.openam.util;

import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class OAuth2Client extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(OAuth2Client.class);

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		final var oauth2Client = new OAuth2Client(id);

		final var acrs = new HashSet<String>();
		if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues"))
			json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));

		OpenAM.getInstance().updateAuthAsPerPolicies(oauth2Client);

		if (!acrs.isEmpty()) {
			oauth2Client.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
			if (acrs.contains("2"))
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_MFA);
			if (acrs.contains("4") || acrs.contains("6"))
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_CERT);

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
		super(id, EntityType.OAUTH2);
	}

}
