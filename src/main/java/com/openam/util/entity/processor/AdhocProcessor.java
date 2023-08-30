package com.openam.util.entity.processor;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openam.util.Util;
import com.openam.util.entity.Saml2;

public class AdhocProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AdhocProcessor.class);

	public static void main(String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		String env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		for (var json : resultSaml) {
			final var id = json.get("_id").asText();
			// logger.debug("id: {}", id);

			if (!json.has("metadata")) {
				// AdhocProcessor.logger.warn("skipping, metadata missing : {} ",
				// json.get("_id").asText());
				continue;
			}

			if (!json.has("entityConfig")) {
				// AdhocProcessor.logger.warn("skipping, entityConfig missing : {} ",
				// json.get("_id").asText());
				continue;
			}

			final var entityConfig = json.get("entityConfig").asText();

			final var builderFactory = DocumentBuilderFactory.newInstance();
			final var builder = builderFactory.newDocumentBuilder();

			final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

			final var xPath = XPathFactory.newInstance().newXPath();

			final var nodeListIDPSSODescriptor = (NodeList) xPath.compile("//IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var isIDP = 0 != nodeListIDPSSODescriptor.getLength();
			if (false == isIDP)
				continue;

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			if (!"true".equalsIgnoreCase(hosted))
				continue;

			final var signingCertAliases = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='signingCertAlias']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var certAlaises = IntStream.range(0, signingCertAliases.getLength()).mapToObj(signingCertAliases::item).map(ca -> {
				return ca.getTextContent();
			}).collect(Collectors.toList());

			logger.info("{},\"{}\"", json.get("_id"), String.join(",", certAlaises));

		}
	}

	public static void main1(String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		String env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		for (var json : resultSaml) {
			final var id = json.get("_id").asText();
			// logger.debug("id: {}", id);

			if (!json.has("metadata")) {
				// AdhocProcessor.logger.warn("skipping, metadata missing : {} ",
				// json.get("_id").asText());
				continue;
			}

			if (!json.has("entityConfig")) {
				// AdhocProcessor.logger.warn("skipping, entityConfig missing : {} ",
				// json.get("_id").asText());
				continue;
			}

			final var entityConfig = json.get("entityConfig").asText();

			final var builderFactory = DocumentBuilderFactory.newInstance();
			final var builder = builderFactory.newDocumentBuilder();

			final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

			final var xPath = XPathFactory.newInstance().newXPath();

			final var nodeListIDPSSODescriptor = (NodeList) xPath.compile("//IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var isIDP = 0 != nodeListIDPSSODescriptor.getLength();
			if (false == isIDP)
				continue;

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			if ("true".equalsIgnoreCase(hosted))
				continue;

			final var metadata = json.get("metadata").asText();
			final var xmlMetadata = builder.parse(new InputSource(new StringReader(metadata)));

			final var wantAuthnRequestsSigned = (NodeList) xPath.compile("//IDPSSODescriptor/@WantAuthnRequestsSigned").evaluate(xmlMetadata, XPathConstants.NODESET);

			int length = wantAuthnRequestsSigned.getLength();
			for (int i = 0; i < length; i++) {
				var wantAuthnRequestsSignedValue = wantAuthnRequestsSigned.item(i).getTextContent();
				if (wantAuthnRequestsSigned.item(i).getTextContent().equalsIgnoreCase("true"))
					logger.info(id);
			}
		}
	}

}
