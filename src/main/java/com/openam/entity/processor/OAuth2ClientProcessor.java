package com.openam.entity.processor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.entity.Entity;
import com.openam.entity.EntityHelper;
import com.openam.entity.OAuth2Client;
import com.openam.util.Util;

@Component
public class OAuth2ClientProcessor {

	static final Logger logger = LoggerFactory.getLogger(OAuth2ClientProcessor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		final var oauth2Client = new OAuth2Client(id);

		final var acrs = new HashSet<String>();
		if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues"))
			json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));

		helper.updateAuthAsPerPolicies(oauth2Client);

		// remove invalid data like
		// "defaultAcrValues": [
		// ""
		// ],
		acrs.removeIf(StringUtils::isEmptyOrWhitespace);

		if (!acrs.isEmpty()) {
			oauth2Client.addAttribute(Entity.EXTERNAL_AUTH, "N/A");

			final var remarks1 = MessageFormat.format("EXTERNAL_AUTH: {0}, acrs: {1}", oauth2Client.getAttribute(Entity.EXTERNAL_AUTH), Util.json(acrs));
			oauth2Client.addRemarks(remarks1);
			if (acrs.contains("2")) {

				oauth2Client.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_MFA);
				final var remarks2 = MessageFormat.format("INTERNAL_AUTH: {0}, acrs: {1}", oauth2Client.getAttribute(Entity.INTERNAL_AUTH), Util.json(acrs));
				oauth2Client.addRemarks(remarks2);
			}
			if (acrs.contains("4") || acrs.contains("6")) {
				oauth2Client.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_CERT);
				final var remarks2 = MessageFormat.format("INTERNAL_AUTH: {0}, acrs: {1}", oauth2Client.getAttribute(Entity.INTERNAL_AUTH), Util.json(acrs));
				oauth2Client.addRemarks(remarks2);
			}
		}

		final var redirectUrls = new ArrayList<String>();
		if (json.has("coreOAuth2ClientConfig") && json.get("coreOAuth2ClientConfig").has("redirectionUris"))
			json.get("coreOAuth2ClientConfig").get("redirectionUris").forEach(h -> redirectUrls.add(h.asText()));

		oauth2Client.addAttribute(Entity.REDIRECT_URLS, redirectUrls.stream().collect(Collectors.joining(",", "\"", "\"")));
	}

	public void process(final JsonNode oauth2Clients) {
		final var result = oauth2Clients.get("result");
		result.forEach(oa -> {
			try {
				_process(oa);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}
}