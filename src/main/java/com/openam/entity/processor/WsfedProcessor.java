package com.openam.entity.processor;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.entity.Entity;
import com.openam.entity.EntityHelper;
import com.openam.entity.Wsfed;

@Component
public class WsfedProcessor {

	static final Logger logger = LoggerFactory.getLogger(WsfedProcessor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig")) {
			return;
		}

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		final var wsfed = new Wsfed(id);
		helper.updateAuthAsPerPolicies(wsfed);

		if (entityConfig.contains("IDPSSOConfig")) {
			wsfed.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);
			final var attributeMappers = new ArrayList<String>();

			if (Entity.patternDefaultIDPAttributeMapper.matcher(entityConfig).find()) {
				attributeMappers.add("DefaultIDPAttributeMapper");
			}
			if (Entity.patternPwCIdentityIDPAttributeMapper.matcher(entityConfig).find()) {
				attributeMappers.add("PwCIdentityIDPAttributeMapper");
			}
			if (Entity.patternPwCIdentityWSFedIDPAttributeMapper.matcher(entityConfig).find()) {
				attributeMappers.add("PwCIdentityWSFedIDPAttributeMapper");
			}

			wsfed.addAttribute(Entity.ATTRIBUTE_MAPPER, String.join(",", attributeMappers));

			final var accountMappers = new ArrayList<String>();
			if (Entity.patternDefaultIDPAccountMapper.matcher(entityConfig).find()) {
				accountMappers.add("DefaultIDPAccountMapper");
			}
			if (Entity.patternPwCIdentityMultipleNameIDAccountMapper.matcher(entityConfig).find()) {
				accountMappers.add("PwCIdentityMultipleNameIDAccountMapper");
			}
			if (Entity.patternPwCIdentityWsfedIDPAccountMapper.matcher(entityConfig).find()) {
				accountMappers.add("PwCIdentityWsfedIDPAccountMapper");
			}

			wsfed.addAttribute(Entity.ACCOUNT_MAPPER, String.join(",", accountMappers));
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

	public void process(final JsonNode saml2Entities) {
		final var result = saml2Entities.get("result");
		result.forEach(se -> {
			try {
				_process(se);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}
}