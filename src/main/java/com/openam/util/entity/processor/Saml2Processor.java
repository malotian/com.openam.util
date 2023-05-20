package com.openam.util.entity.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openam.util.Util;
import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.Saml2;

@Component
public class Saml2Processor {

	static final Logger logger = LoggerFactory.getLogger(Saml2Processor.class);

	private final static ObjectMapper mapper = new ObjectMapper();

	public static void main(final String[] args) throws StreamReadException, DatabindException, IOException, XPathExpressionException, ParserConfigurationException, SAXException {
		final var processor = new Saml2Processor();
		final var jsonPolicies = Saml2Processor.mapper.readValue(new File("test.json"), JsonNode.class);
		processor._process(jsonPolicies);
	}

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		final var id = json.get("_id").asText();

		if (!json.has("entityConfig")) {
			Saml2Processor.logger.warn("skipping, entityConfig missing : {} ", json.get("_id").asText());
			return;
		}

		final var entityConfig = json.get("entityConfig").asText();

		final var saml2 = new Saml2(id);

		final var builderFactory = DocumentBuilderFactory.newInstance();
		final var builder = builderFactory.newDocumentBuilder();

		final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

		final var xPath = XPathFactory.newInstance().newXPath();

		final var nodeListIDPSSODescriptor = (NodeList) xPath.compile("//IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var isIDP = 0 != nodeListIDPSSODescriptor.getLength();
		Saml2Processor.logger.debug("isIDP: {}", isIDP);

		final var nodeListSPSSODescriptor = (NodeList) xPath.compile("//SPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var isSP = 0 != nodeListSPSSODescriptor.getLength();
		Saml2Processor.logger.debug("isSP: {}", isSP);

		final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
		Saml2Processor.logger.debug("hosted: {}", "true".equalsIgnoreCase(hosted));
		if ("true".equalsIgnoreCase(hosted)) {
			saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
		} else if ("false".equalsIgnoreCase(hosted)) {
			saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
		}

		// assume default
		saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);

		if (isIDP) {
			_processIDP(id, saml2, xmlEntityConfig, xPath);
		}
		if (isSP) {
			helper.updateAuthAsPerPolicies(saml2);
			if (!json.has("metadata")) {
				Saml2Processor.logger.warn("skipping, metadata missing : {} ", json.get("_id").asText());
				return;
			}
			final var metadata = json.get("metadata").asText();
			final var xmlMetadata = builder.parse(new InputSource(new StringReader(metadata)));
			_processSP(saml2, xmlMetadata, xmlEntityConfig, xPath, isSP);
		}

	}

	private void _processIDP(final String id, final Saml2 saml2, final Document xmlEntityConfig, final XPath xPath) throws XPathExpressionException {
		saml2.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);
		final var idpAuthncontextClassrefMappings = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAuthncontextClassrefMapping']/Value/text()").evaluate(xmlEntityConfig,
				XPathConstants.NODESET);
		for (var i = 0; i < idpAuthncontextClassrefMappings.getLength(); i++) {
			Saml2Processor.logger.debug("idpAuthncontextClassrefMappings: {}", idpAuthncontextClassrefMappings.item(i).getTextContent());
			final var matcher = Entity.patternPasswordProtectedTransportServiceCertMfa.matcher(idpAuthncontextClassrefMappings.item(i).getTextContent());

			if (matcher.find()) {
				Saml2Processor.logger.debug("INTERNAL_AUTH: {}", matcher.group(1));
				saml2.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
				final var remarks1 = MessageFormat.format("INTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.INTERNAL_AUTH), matcher.group(1));
				saml2.addRemarks(remarks1);

				saml2.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
				final var remarks2 = MessageFormat.format("EXTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.EXTERNAL_AUTH), matcher.group(1));
				saml2.addRemarks(remarks2);
			}
		}

		final var idpAccountMappers = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAccountMapper']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var accountMappers = IntStream.range(0, idpAccountMappers.getLength()).mapToObj(idpAccountMappers::item).map(iam -> {
			Saml2Processor.logger.debug("idpAccountMapper: {}", iam.getTextContent());
			if (Entity.patternDefaultIDPAccountMapper.matcher(iam.getTextContent()).find()) {
				return "DefaultIDPAccountMapper";
			}
			if (Entity.patternPwCIdentityMultipleNameIDAccountMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityMultipleNameIDAccountMapper";
			}
			if (Entity.patternPwCIdentityWsfedIDPAccountMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityWsfedIDPAccountMapper";
			} else {
				Saml2Processor.logger.warn("invalid idpAccountMapper: {} for saml2: {}", iam.getTextContent(), id);
				return null;
			}
		}).collect(Collectors.toList());

		saml2.addAttribute(Entity.ACCOUNT_MAPPER, Util.json(accountMappers));

		final var idpAttributeMappers = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAttributeMapper']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

		final var attributeMappers = IntStream.range(0, idpAttributeMappers.getLength()).mapToObj(idpAttributeMappers::item).map(iam -> {
			Saml2Processor.logger.debug("idpAttributeMapper: {}", iam.getTextContent());
			if (Entity.patternDefaultIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "DefaultIDPAttributeMapper";
			}
			if (Entity.patternPwCIdentityIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityIDPAttributeMapper";
			}
			if (Entity.patternPwCIdentityWSFedIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityWSFedIDPAttributeMapper";
			} else {
				Saml2Processor.logger.warn("invalid idpAttributeMapper: {} for saml2: {}", iam.getTextContent(), id);
				return null;
			}
		}).collect(Collectors.toList());

		saml2.addAttribute(Entity.ATTRIBUTE_MAPPER, Util.json(attributeMappers));
	}

	private void _processSP(final Saml2 saml2, final Document xmlMetadata, final Document xmlEntityConfig, final XPath xPath, final boolean isSP) throws XPathExpressionException {
		saml2.addAttribute(Entity.SP_IDP, Entity.SERVICE_PROVIDER);
		Saml2Processor.logger.debug("isSP: {}", isSP);

		final var assertionConsumerServices = (NodeList) xPath.compile("//EntityDescriptor/SPSSODescriptor/AssertionConsumerService/@Location").evaluate(xmlMetadata, XPathConstants.NODESET);
		final var redirectUrls = IntStream.range(0, assertionConsumerServices.getLength()).mapToObj(assertionConsumerServices::item).map(acs -> {
			Saml2Processor.logger.debug("assertionConsumerService: {}", acs);
			return acs.getTextContent();
		}).collect(Collectors.toList());

		saml2.addAttribute(Entity.REDIRECT_URLS, Util.json(redirectUrls));

		final var attributeMaps = (NodeList) xPath.compile("//SPSSOConfig/Attribute[@name='attributeMap']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

		final var claims = IntStream.range(0, attributeMaps.getLength()).mapToObj(attributeMaps::item).map(claim -> {
			Saml2Processor.logger.debug("claim: {}", claim.getTextContent());
			return claim.getTextContent();
		}).collect(Collectors.toList());

		saml2.addAttribute(Entity.CLAIMS, Util.json(claims));
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