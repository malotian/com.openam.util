package com.openam.util.entity.processor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.util.Util;
import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.Wsfed;

@Component
public class WsfedProcessor {

	static final Logger logger = LoggerFactory.getLogger(WsfedProcessor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig")) {
			return;
		}

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		final var wsfed = new Wsfed(id);

		if (!entityConfig.contains("IDPSSOConfig")) {
			helper.updateAuthAsPerPolicies(wsfed);
		}

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

		final var metaData = json.get("metadata").asText();
		final var builderFactory = DocumentBuilderFactory.newInstance();
		final var builder = builderFactory.newDocumentBuilder();
		final var xmlDocument = builder.parse(new InputSource(new StringReader(metaData)));

		final var xPath = XPathFactory.newInstance().newXPath();
		final var xpathAddress = "//*[local-name() = 'TokenIssuerEndpoint']/*[local-name() = 'Address']";
		final var nodeListAddress = (NodeList) xPath.compile(xpathAddress).evaluate(xmlDocument, XPathConstants.NODESET);

		final var redirectUrls = new ArrayList<String>();

		for (var i = 0; i < nodeListAddress.getLength(); i++) {
			if (nodeListAddress.item(i).getNodeType() == Node.ELEMENT_NODE) {
				redirectUrls.add(nodeListAddress.item(i).getTextContent());
			}
		}
		wsfed.addAttribute(Entity.REDIRECT_URLS, Util.json(redirectUrls));

		final var builder2 = builderFactory.newDocumentBuilder();
		final var xmlDocument2 = builder2.parse(new InputSource(new StringReader(entityConfig)));

		final var xPath2 = XPathFactory.newInstance().newXPath();
		final var xpathAttributeMap = "//Attribute[@name='attributeMap']/Value";
		final var nodeListAttributeMap = (NodeList) xPath2.compile(xpathAttributeMap).evaluate(xmlDocument2, XPathConstants.NODESET);

		final var claims = new ArrayList<String>();
		for (var i = 0; i < nodeListAttributeMap.getLength(); i++) {
			if (nodeListAttributeMap.item(i).getNodeType() == Node.ELEMENT_NODE) {
				claims.add(nodeListAttributeMap.item(i).getTextContent());
			}
		}

		wsfed.addAttribute(Entity.CLAIMS, Util.json(claims));

	}

	public void process(final JsonNode wsfedEntities) {
		final var result = wsfedEntities.get("result");
		result.forEach(se -> {
			try {
				_process(se);
			} catch (final ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
				e.printStackTrace();
			}
		});

	}
}