package com.openam.util.entity.processor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.util.Util;
import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.OAuth2Client;

@Component
public class OAuth2ClientProcessor {

	static final Logger logger = LoggerFactory.getLogger(OAuth2ClientProcessor.class);

	protected static Map<String, String> acrMap = new HashMap<>() {
		private static final long serialVersionUID = 1L;

		{
			put("2", "MFA");
			put("3", "test_entrust");
			put("4", "CERT");
			put("5", "internalChain");
			put("6", "pwc-cert");
			put("7", "ldapDJChain");
			put("8", "AdminMFA");
			put("100", "internal_mfa");
			put("101", "internal_bypass");
			put("102", "zt_device_registration");
			put("103", "zt_change_device");
			put("104", "mfa_oath");
			put("105", "internal");
			put("106", "internal_no_fallback");
			put("107", "internal_mfa_trusted");
			put("108", "admin");
			put("109", "cyberark");
		}
	};

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		final var oauth2Client = new OAuth2Client(id);

		final var acrs = new HashSet<String>();
		if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues")) {
			json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));
		}

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
			acrs.forEach(acr -> {
				if (OAuth2ClientProcessor.acrMap.containsKey(acr)) {
					oauth2Client.addAttribute(Entity.INTERNAL_AUTH, OAuth2ClientProcessor.acrMap.get(acr));
					final var remarks2 = MessageFormat.format("INTERNAL_AUTH: {0}, acr: {1}", oauth2Client.getAttribute(Entity.INTERNAL_AUTH), Util.json(acr));
					oauth2Client.addRemarks(remarks2);
				}
			});
		}

		final var redirectUrls = new ArrayList<String>();
		if (json.has("coreOAuth2ClientConfig") && json.get("coreOAuth2ClientConfig").has("redirectionUris")) {
			json.get("coreOAuth2ClientConfig").get("redirectionUris").forEach(h -> redirectUrls.add(h.asText()));
		}

		oauth2Client.addAttribute(Entity.REDIRECT_URLS, Util.json(redirectUrls.stream().limit(200).toList()));
		if (json.has("coreOAuth2ClientConfig") && json.get("coreOAuth2ClientConfig").has("status")) {
			oauth2Client.addAttribute(Entity.STATUS, json.get("coreOAuth2ClientConfig").get("status").asText().equalsIgnoreCase("active") ? Entity.STATUS_ACTIVE : Entity.STATUS_INACTIVE);
		}

		final var claims = new ArrayList<String>();
		if (json.has("coreOAuth2ClientConfig") && json.get("coreOAuth2ClientConfig").has("scopes")) {
			json.get("coreOAuth2ClientConfig").get("scopes").forEach(s -> claims.add(s.asText()));
		}

		oauth2Client.addAttribute(Entity.CLAIMS, Util.json(claims));

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