package com.openam.entity.processor;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
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
import com.openam.entity.Entity;
import com.openam.entity.EntityHelper;
import com.openam.entity.Saml2;
import com.openam.util.Util;

@Component
public class Saml2Processor {

	static final Logger logger = LoggerFactory.getLogger(Saml2Processor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		final var id = json.get("_id").asText();

		if (!json.has("metadata")) {
			Saml2Processor.logger.warn("skipping, metadata missing : {} ", json.get("_id").asText());
			return;
		}

		final var metaData = json.get("metadata").asText();

		final var saml2 = new Saml2(id);

		if (!metaData.contains("IDPSSODescriptor")) {
			helper.updateAuthAsPerPolicies(saml2);
		}

		if (metaData.contains("IDPSSODescriptor")) {
			saml2.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);

			if (json.has("entityConfig")) {
				final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");
				final var matcher = Entity.patternPasswordProtectedTransportServiceCertMfa.matcher(entityConfig);

				if (matcher.find()) {
					saml2.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
					final var remarks1 = MessageFormat.format("INTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.INTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks1);

					saml2.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
					final var remarks2 = MessageFormat.format("EXTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.EXTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks2);
				}

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

				saml2.addAttribute(Entity.ATTRIBUTE_MAPPER, String.join(",", attributeMappers));

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

				saml2.addAttribute(Entity.ACCOUNT_MAPPER, String.join(",", accountMappers));

			}

		}
		if (metaData.contains("SPSSODescriptor")) {
			saml2.addAttribute(Entity.SP_IDP, Entity.SERVICE_PROVIDER);
		}

		final var builderFactory = DocumentBuilderFactory.newInstance();
		final var builder = builderFactory.newDocumentBuilder();
		final var xmlDocument = builder.parse(new InputSource(new StringReader(metaData)));

		final var xPath = XPathFactory.newInstance().newXPath();
		final var xpathAssertionConsumerService = "//AssertionConsumerService";
		final var nodeListAssertionConsumerService = (NodeList) xPath.compile(xpathAssertionConsumerService).evaluate(xmlDocument, XPathConstants.NODESET);

		final var redirectUrls = new ArrayList<String>();

		for (var i = 0; i < nodeListAssertionConsumerService.getLength(); i++) {
			if (nodeListAssertionConsumerService.item(i).getNodeType() == Node.ELEMENT_NODE) {
				redirectUrls.add(nodeListAssertionConsumerService.item(i).getAttributes().getNamedItem("Location").getNodeValue());
			}
		}

		saml2.addAttribute(Entity.REDIRECT_URLS, Util.json(redirectUrls));

		if (!json.has("entityConfig")) {
			return;
		}

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

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

		saml2.addAttribute(Entity.CLAIMS, Util.json(claims));

		if (entityConfig.contains("hosted=\"true\"")) {
			saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
		} else if (entityConfig.contains("hosted=\"false\"")) {
			saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
		}

	}

	public void process(final JsonNode saml2Entities) {
		final var result = saml2Entities.get("result");
		result.forEach(se -> {
			try {
				_process(se);
			} catch (final ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
				e.printStackTrace();
			}
		});

	}
}