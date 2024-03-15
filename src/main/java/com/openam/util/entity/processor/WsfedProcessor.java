package com.openam.util.entity.processor;

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
			WsfedProcessor.logger.warn("skipping, entityConfig missing : {} ", json.get("_id").asText());
			return;
		}

		final var entityConfig = json.get("entityConfig").asText();

		final var wsfed = new Wsfed(id);

		final var builderFactory = DocumentBuilderFactory.newInstance();
		final var builder = builderFactory.newDocumentBuilder();

		final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

		final var xPath = XPathFactory.newInstance().newXPath();

		final var nodeListIDPSSOConfig = (NodeList) xPath.compile("//FederationConfig/IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var isIDP = 0 != nodeListIDPSSOConfig.getLength();
		// WsfedProcessor.logger.debug("isIDP: {}", isIDP);

		final var nodeListSPSSOConfig = (NodeList) xPath.compile("//FederationConfig/SPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var isSP = 0 != nodeListSPSSOConfig.getLength();
		// WsfedProcessor.logger.debug("isSP: {}", isSP);

		final var hosted = (String) xPath.compile("//FederationConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
		// WsfedProcessor.logger.debug("hosted: {}", "true".equalsIgnoreCase(hosted));
		if ("true".equalsIgnoreCase(hosted)) {
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
		} else if ("false".equalsIgnoreCase(hosted)) {
			wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
		}

		// assume default
		wsfed.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);

		if (isIDP) {
			_processIDP(id, wsfed, xmlEntityConfig, xPath);
		}
		if (isSP) {
			helper.updateAuthAsPerPolicies(wsfed);
			if (!json.has("metadata")) {
				WsfedProcessor.logger.warn("skipping, metadata missing : {} ", json.get("_id").asText());
				return;
			}
			final var metadata = json.get("metadata").asText();
			final var xmlMetadata = builder.parse(new InputSource(new StringReader(metadata)));
			_processSP(wsfed, xmlMetadata, xmlEntityConfig, xPath);
		}
	}

	private void _processIDP(final String id, final Wsfed wsfed, final Document xmlEntityConfig, final XPath xPath) throws XPathExpressionException {
		wsfed.addAttribute(Entity.SP_IDP, Entity.IDENTITY_PROVIDER);
		final var idpAuthncontextClassrefMappings = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAuthncontextClassrefMapping']/Value/text()").evaluate(xmlEntityConfig,
				XPathConstants.NODESET);
		for (var i = 0; i < idpAuthncontextClassrefMappings.getLength(); i++) {
			// WsfedProcessor.logger.debug("idpAuthncontextClassrefMappings: {}",
			// idpAuthncontextClassrefMappings.item(i).getTextContent());
			final var matcher = Entity.patternPasswordProtectedTransportServiceCertMfa.matcher(idpAuthncontextClassrefMappings.item(i).getTextContent());

			if (matcher.find() && !matcher.group(1).isBlank()) {
				// WsfedProcessor.logger.debug("INTERNAL_AUTH: {}", matcher.group(1));
				wsfed.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
				final var remarks1 = MessageFormat.format("INTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", wsfed.getAttribute(Entity.INTERNAL_AUTH), matcher.group(1));
				wsfed.addRemarks(remarks1);

				wsfed.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
				final var remarks2 = MessageFormat.format("EXTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", wsfed.getAttribute(Entity.EXTERNAL_AUTH), matcher.group(1));
				wsfed.addRemarks(remarks2);
			}
		}

		final var idpAccountMappers = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAccountMapper']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var accountMappers = IntStream.range(0, idpAccountMappers.getLength()).mapToObj(idpAccountMappers::item).map(iam -> {
			// WsfedProcessor.logger.debug("idpAccountMapper: {}", iam.getTextContent());
			if (Entity.patternWsfedDefaultIDPAccountMapper.matcher(iam.getTextContent()).find()) {
				return "DefaultIDPAccountMapper";
			}
			if (Entity.patternPwCIdentityMultipleNameIDAccountMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityMultipleNameIDAccountMapper";
			}
			if (Entity.patternPwCIdentityWsfedIDPAccountMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityWsfedIDPAccountMapper";
			}
			WsfedProcessor.logger.warn("invalid idpAccountMapper: {} for wsfed: {}", iam.getTextContent(), id);
			return null;
		}).collect(Collectors.toList());

		accountMappers.forEach(am -> wsfed.addRemarks(MessageFormat.format("ACCOUNT_MAPPER: {0}", am)));

		final var idpAttributeMappers = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAttributeMapper']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

		final var attributeMappers = IntStream.range(0, idpAttributeMappers.getLength()).mapToObj(idpAttributeMappers::item).map(iam -> {
			// WsfedProcessor.logger.debug("idpAttributeMapper: {}", iam.getTextContent());
			if (Entity.patternWsfedDefaultIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "DefaultIDPAttributeMapper";
			}
			if (Entity.patternPwCIdentityIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityIDPAttributeMapper";
			}
			if (Entity.patternPwCIdentityWSFedIDPAttributeMapper.matcher(iam.getTextContent()).find()) {
				return "PwCIdentityWSFedIDPAttributeMapper";
			}
			WsfedProcessor.logger.warn("invalid idpAttributeMapper: {} for wsfed: {}", iam.getTextContent(), id);
			return null;
		}).collect(Collectors.toList());

		attributeMappers.forEach(am -> wsfed.addRemarks(MessageFormat.format("ATTRIBUTE_MAPPER: {0}", am)));
		
		final var idpSigningCertAlias = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='signingCertAlias']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);
		final var signingCertAlias = IntStream.range(0, idpSigningCertAlias.getLength()).mapToObj(idpSigningCertAlias::item).map(Node::getTextContent).collect(Collectors.toList());
		wsfed.addAttribute(Entity.SIGNING_CERT_ALIAS, Util.json(signingCertAlias));
		
		
	}

	private void _processSP(final Wsfed wsfed, final Document xmlMetadata, final Document xmlEntityConfig, final XPath xPath) throws XPathExpressionException {
		wsfed.addAttribute(Entity.SP_IDP, Entity.SERVICE_PROVIDER);
		// WsfedProcessor.logger.debug("isSP: {}", isSP);

		final var tokenIssuerEndpoints = (NodeList) xPath.compile("//*[local-name() = 'TokenIssuerEndpoint']/*[local-name() = 'Address']").evaluate(xmlMetadata, XPathConstants.NODESET);
		final var redirectUrls = IntStream.range(0, tokenIssuerEndpoints.getLength()).mapToObj(tokenIssuerEndpoints::item).map(Node::getTextContent).collect(Collectors.toList());

		wsfed.addAttribute(Entity.REDIRECT_URLS, Util.json(redirectUrls.stream().limit(200).toList()));

		final var attributeMaps = (NodeList) xPath.compile("//SPSSOConfig/Attribute[@name='attributeMap']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

		final var claims = IntStream.range(0, attributeMaps.getLength()).mapToObj(attributeMaps::item).map(Node::getTextContent).collect(Collectors.toList());

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