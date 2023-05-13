package com.openam.entity.processor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
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

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig"))
			return;

		final var entityConfig = json.get("entityConfig").asText().replace("\r", "").replace("\n", "");

		final var wsfed = new Wsfed(id);

		if (!entityConfig.contains("IDPSSOConfig"))
			helper.updateAuthAsPerPolicies(wsfed);

		if (entityConfig.contains("IDPSSOConfig")) {
			wsfed.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);
			final var attributeMappers = new ArrayList<String>();

			if (Entity.patternDefaultIDPAttributeMapper.matcher(entityConfig).find())
				attributeMappers.add("DefaultIDPAttributeMapper");
			if (Entity.patternPwCIdentityIDPAttributeMapper.matcher(entityConfig).find())
				attributeMappers.add("PwCIdentityIDPAttributeMapper");
			if (Entity.patternPwCIdentityWSFedIDPAttributeMapper.matcher(entityConfig).find())
				attributeMappers.add("PwCIdentityWSFedIDPAttributeMapper");

			wsfed.addAttribute(Entity.ATTRIBUTE_MAPPER, String.join(",", attributeMappers));

			final var accountMappers = new ArrayList<String>();
			if (Entity.patternDefaultIDPAccountMapper.matcher(entityConfig).find())
				accountMappers.add("DefaultIDPAccountMapper");
			if (Entity.patternPwCIdentityMultipleNameIDAccountMapper.matcher(entityConfig).find())
				accountMappers.add("PwCIdentityMultipleNameIDAccountMapper");
			if (Entity.patternPwCIdentityWsfedIDPAccountMapper.matcher(entityConfig).find())
				accountMappers.add("PwCIdentityWsfedIDPAccountMapper");

			wsfed.addAttribute(Entity.ACCOUNT_MAPPER, String.join(",", accountMappers));
		}

		if (entityConfig.contains("SPSSOConfig"))
			wsfed.addAttribute(Entity.SP_IDP, Entity.SERVICE_PROVIDER);

		final var metaData = json.get("metadata").asText();
		final var builderFactory = DocumentBuilderFactory.newInstance();
		final var builder = builderFactory.newDocumentBuilder();
		final var xmlDocument = builder.parse(new InputSource(new StringReader(metaData)));

		final var xPath = XPathFactory.newInstance().newXPath();
		final var xpathAddress = "//*[local-name() = 'TokenIssuerEndpoint']/*[local-name() = 'Address']";
		final var nodeListAddress = (NodeList) xPath.compile(xpathAddress).evaluate(xmlDocument, XPathConstants.NODESET);

		final var length = nodeListAddress.getLength();

		final var redirectUrls = new ArrayList<>();
		for (var i = 0; i < length; i++)
			if (nodeListAddress.item(i).getNodeType() == Node.ELEMENT_NODE)
				redirectUrls.add(nodeListAddress.item(i).getAttributes().getNamedItem("Location").getNodeValue());

		wsfed.addAttribute(Entity.REDIRECT_URLS, redirectUrls.stream().collect(Collectors.joining(",", "\"", "\"")));

		if (entityConfig.contains("hosted=\"true\""))
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
		else if (entityConfig.contains("hosted=\"false\""))
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);

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