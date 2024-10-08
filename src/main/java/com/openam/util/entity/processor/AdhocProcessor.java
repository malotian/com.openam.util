package com.openam.util.entity.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.thymeleaf.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.openam.util.Kontext;
import com.openam.util.OpenAM;
import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityID;
import com.openam.util.entity.Policy;
import com.openam.util.entity.Saml2;

@SpringBootApplication
@ComponentScan(basePackages = "com.openam.*")
public class AdhocProcessor implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(AdhocProcessor.class);

	private static List<String> commonItems(final List<String> list1, final List<String> list2) {
		final var commonList = new ArrayList<String>();
		for (final String str : list1) {
			// add to the common list if it is not already present
			if (list2.contains(str) && !commonList.contains(str)) {
				commonList.add(str);
			}
		}
		return commonList;
	}

	public static void idpMapperAdapterServiceCsv(final String[] args)
			throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		final String[] columns = { "ID", "SERVICE", "ADAPTER", "HOSTED-REMOTE" };

		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get(env + "-idp-mapper.service.csv"));
		final var printer = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		for (final var json : resultSaml) {
			final var id = json.get("_id").asText();

			if (!json.has("entityConfig")) {
				continue;
			}

			final var entityConfig = json.get("entityConfig").asText();
			// AdhocProcessor.logger.debug("id: {}", id);
			final var saml2 = new Saml2(id);

			final var builderFactory = DocumentBuilderFactory.newInstance();
			final var builder = builderFactory.newDocumentBuilder();

			final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

			final var xPath = XPathFactory.newInstance().newXPath();

			final var nodeListIDPSSODescriptor = (NodeList) xPath.compile("//IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var isIDP = 0 != nodeListIDPSSODescriptor.getLength();
			// Saml2Processor.logger.debug("isIDP: {}", isIDP);

			final var nodeListSPSSODescriptor = (NodeList) xPath.compile("//SPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			nodeListSPSSODescriptor.getLength();

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			// Saml2Processor.logger.debug("hosted: {}", "true".equalsIgnoreCase(hosted));
			if ("true".equalsIgnoreCase(hosted)) {
				saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
			} else if ("false".equalsIgnoreCase(hosted)) {
				saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
			} else {
				continue;
			}

			if (!isIDP) {
				// AdhocProcessor.logger.debug("isIDP: {}", isIDP);
				continue;
			}
			final var idpAuthncontextClassrefMappings = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAuthncontextClassrefMapping']/Value/text()").evaluate(xmlEntityConfig,
					XPathConstants.NODESET);
			for (var i = 0; i < idpAuthncontextClassrefMappings.getLength(); i++) {
				// Saml2Processor.logger.debug("idpAuthncontextClassrefMappings: {}",
				// idpAuthncontextClassrefMappings.item(i).getTextContent());
				final var matcher = Entity.patternPasswordProtectedTransportServiceCertMfa.matcher(idpAuthncontextClassrefMappings.item(i).getTextContent());

				if (matcher.find() && !matcher.group(1).isBlank()) {
					Saml2Processor.logger.debug("INTERNAL_AUTH: {}", matcher.group(1));
					saml2.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
					final var remarks1 = MessageFormat.format("INTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.INTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks1);

					saml2.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
					final var remarks2 = MessageFormat.format("EXTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.EXTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks2);
				}
			}

			final var idpAdapter = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAdapter']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

			final var idpAdapters = IntStream.range(0, idpAdapter.getLength()).mapToObj(idpAdapter::item).map(ia -> {
				return ia.getTextContent();
			}).collect(Collectors.toList());

			logger.debug("idpAdapters: {}", String.join("#", idpAdapters));

			printer.printRecord(id, saml2.getAttribute(Entity.INTERNAL_AUTH), String.join("#", idpAdapters), saml2.getAttribute(Entity.HOSTED_REMOTE));

		}
		printer.close();
	}

	public static void idpNameID(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		final String[] columns = { "ID", "SERVICE", "ADAPTER", "HOSTED-REMOTE" };

		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get(env + "-idp-mapper.service.csv"));
		final var printer = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		for (final var json : resultSaml) {
			final var id = json.get("_id").asText();

			if (!json.has("entityConfig")) {
				continue;
			}

			final var entityConfig = json.get("entityConfig").asText();
			// AdhocProcessor.logger.debug("id: {}", id);
			final var saml2 = new Saml2(id);

			final var builderFactory = DocumentBuilderFactory.newInstance();
			final var builder = builderFactory.newDocumentBuilder();

			final var xmlEntityConfig = builder.parse(new InputSource(new StringReader(entityConfig)));

			final var xPath = XPathFactory.newInstance().newXPath();

			final var nodeListIDPSSODescriptor = (NodeList) xPath.compile("//IDPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var isIDP = 0 != nodeListIDPSSODescriptor.getLength();
			// Saml2Processor.logger.debug("isIDP: {}", isIDP);

			final var nodeListSPSSODescriptor = (NodeList) xPath.compile("//SPSSOConfig").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			nodeListSPSSODescriptor.getLength();

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			// Saml2Processor.logger.debug("hosted: {}", "true".equalsIgnoreCase(hosted));
			if ("true".equalsIgnoreCase(hosted)) {
				saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.HOSTED);
			} else if ("false".equalsIgnoreCase(hosted)) {
				saml2.addAttribute(Entity.HOSTED_REMOTE, Entity.REMOTE);
			} else {
				continue;
			}

			if (!isIDP) {
				// AdhocProcessor.logger.debug("isIDP: {}", isIDP);
				continue;
			}
			final var idpAuthncontextClassrefMappings = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAuthncontextClassrefMapping']/Value/text()").evaluate(xmlEntityConfig,
					XPathConstants.NODESET);
			for (var i = 0; i < idpAuthncontextClassrefMappings.getLength(); i++) {
				// Saml2Processor.logger.debug("idpAuthncontextClassrefMappings: {}",
				// idpAuthncontextClassrefMappings.item(i).getTextContent());
				final var matcher = Entity.patternPasswordProtectedTransportServiceCertMfa.matcher(idpAuthncontextClassrefMappings.item(i).getTextContent());

				if (matcher.find() && !matcher.group(1).isBlank()) {
					Saml2Processor.logger.debug("INTERNAL_AUTH: {}", matcher.group(1));
					saml2.addAttribute(Entity.INTERNAL_AUTH, matcher.group(1));
					final var remarks1 = MessageFormat.format("INTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.INTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks1);

					saml2.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
					final var remarks2 = MessageFormat.format("EXTERNAL_AUTH: {0}, PasswordProtectedTransport: {1}", saml2.getAttribute(Entity.EXTERNAL_AUTH), matcher.group(1));
					saml2.addRemarks(remarks2);
				}
			}

			final var idpAdapter = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='idpAdapter']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);

			final var idpAdapters = IntStream.range(0, idpAdapter.getLength()).mapToObj(idpAdapter::item).map(ia -> {
				return ia.getTextContent();
			}).collect(Collectors.toList());

			logger.debug("idpAdapters: {}", String.join("#", idpAdapters));

			printer.printRecord(id, saml2.getAttribute(Entity.INTERNAL_AUTH), String.join("#", idpAdapters), saml2.getAttribute(Entity.HOSTED_REMOTE));

		}
		printer.close();
	}

	public static void redirectionUris(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonOAuth2Entities.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		final var csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(env + "-oauth-client-type.csv")), CSVFormat.DEFAULT.withHeader("ID", "Status"));

		for (final var json : resultPolicies) {
			final var id = json.get("_id").asText();

			new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOAuth2ClientConfig").has("clientType")) {
				csvPrinter.printRecord(id, json.get("coreOAuth2ClientConfig").get("clientType"));
			}
		}
	}

	public static void listPublicConfidentialClient(final String[] args)
			throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonOAuth2Entities.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		final var csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(env + "-oauth-client-type.csv")), CSVFormat.DEFAULT.withHeader("ID", "Type", "RedirectURls"));

		for (final var json : resultPolicies) {
			final var id = json.get("_id").asText();

			new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOAuth2ClientConfig").has("clientType")) {
				csvPrinter.printRecord(id, json.get("coreOAuth2ClientConfig").get("clientType"), json.get("coreOAuth2ClientConfig").get("redirectionUris"));
			}
		}

	}

	public static void main(final String[] args) {
		SpringApplication.run(AdhocProcessor.class, args).close();
	}

	public static void main1(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		for (final var json : resultSaml) {
			final var id = json.get("_id").asText();
			// logger.debug("id: {}", id);

			if (!json.has("metadata") || !json.has("entityConfig")) {
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
			if (!isIDP) {
				continue;
			}

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			if ("true".equalsIgnoreCase(hosted)) {
				continue;
			}

			final var metadata = json.get("metadata").asText();
			final var xmlMetadata = builder.parse(new InputSource(new StringReader(metadata)));

			final var wantAuthnRequestsSigned = (NodeList) xPath.compile("//IDPSSODescriptor/@WantAuthnRequestsSigned").evaluate(xmlMetadata, XPathConstants.NODESET);

			final var length = wantAuthnRequestsSigned.getLength();
			for (var i = 0; i < length; i++) {
				wantAuthnRequestsSigned.item(i).getTextContent();
				if ("true".equalsIgnoreCase(wantAuthnRequestsSigned.item(i).getTextContent())) {
					AdhocProcessor.logger.info(id);
				}
			}
		}
	}

	public static void main4(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonSaml2Entities.json").toFile(), JsonNode.class);
		final var resultSaml = jsonSaml2Entities.get("result");

		for (final var json : resultSaml) {
			json.get("_id").asText();

			if (!json.has("metadata") || !json.has("entityConfig")) {
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
			if (!isIDP) {
				continue;
			}

			final var hosted = (String) xPath.compile("//EntityConfig/@hosted").evaluate(xmlEntityConfig, XPathConstants.STRING);
			if (!"true".equalsIgnoreCase(hosted)) {
				continue;
			}

			final var signingCertAliases = (NodeList) xPath.compile("//IDPSSOConfig/Attribute[@name='signingCertAlias']/Value/text()").evaluate(xmlEntityConfig, XPathConstants.NODESET);
			final var certAlaises = IntStream.range(0, signingCertAliases.getLength()).mapToObj(signingCertAliases::item).map(Node::getTextContent).collect(Collectors.toList());

			AdhocProcessor.logger.info("{},\"{}\"", json.get("_id"), String.join(",", certAlaises));

		}
	}

	public static void main7(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();

		final List<String> policyNames = Arrays.asList("SAML/WS-Fed/OAuth Internal MFA", "SAML/WS-Fed/OAuth Internal CERT", "External User MFA", "ExternalUsersTrustedDevice");
		final List<String> protocols = Arrays.asList("saml", "wsfed", "oauth");

		for (final String p : protocols) {
			for (var i = 0; i < policyNames.size(); ++i) {
				final var filenameI = MessageFormat.format("{0}---{1}", p, policyNames.get(i).replaceAll(" |/", "-"));
				final var resoucesI = mapper.readValue(Paths.get(filenameI).toFile(), JsonNode.class);
				for (var j = i + 1; j < policyNames.size(); ++j) {
					final var filenameJ = MessageFormat.format("{0}---{1}", p, policyNames.get(j).replaceAll(" |/", "-"));
					final var resoucesJ = mapper.readValue(Paths.get(filenameJ).toFile(), JsonNode.class);
					final var common = AdhocProcessor.commonItems(AdhocProcessor.toListOfStrings(resoucesI), AdhocProcessor.toListOfStrings(resoucesJ));
					AdhocProcessor.logger.debug("{}: [{},{}]", common.size(), policyNames.get(i), policyNames.get(j));
					AdhocProcessor.logger.debug("{}", String.join(",", common));
					break;
				}
				break;
			}

		}
	}

	public static void main8(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonOAuth2Entities.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		final var csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(env + "-oauth-status.csv")), CSVFormat.DEFAULT.withHeader("ID", "Status"));

		for (final var json : resultPolicies) {
			final var id = json.get("_id").asText();

			new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOAuth2ClientConfig").has("status")) {
				csvPrinter.printRecord(id, json.get("coreOAuth2ClientConfig").get("status"));
			}
		}

	}

	public static void mainApplicationsAccessPolicies(final String[] args)
			throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonPolicies = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonPolicies.get("result");

		final String[] columns = { "POLICY", "RESOURCES" };

		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get(env + "-ApplicationsAccess.csv"));
		final var csvPrinter = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		for (var json : resultPolicies) {

			final var policyname = json.get("_id").asText();
			if (!json.has("applicationName"))
				continue;

			if (!json.get("applicationName").asText().equals("ApplicationsAccess"))
				continue;

			System.out.println(policyname);

			if (!json.has("resources")) {
				PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
				return;
			}

			final var reources = new HashSet<String>();
			json.get("resources").forEach(h -> reources.add(h.asText()));

			csvPrinter.printRecord(policyname, String.join(", ", reources));
		}
		csvPrinter.close();

	}

	public static void main9(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		final var regex = "^([^-]*)-(.*?)[|]";
		Pattern.compile(regex);

		new JsonObject();
		new GsonBuilder().disableHtmlEscaping().create();

		for (final var json : resultPolicies) {
			final var policyname = json.get("_id").asText();

			if (!json.has("resources")) {
				PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
				return;
			}

			if (json.get("resources").has("urn:pwcid:stg:saml:test")) {
				AdhocProcessor.logger.debug("{}", policyname);
			}

		}

	}

	private static List<String> toListOfStrings(final JsonNode array) {
		final var list = new ArrayList<String>();
		for (final var item : array) {
			list.add(item.asText());
		}
		return list;
	}

	@Autowired
	Kontext kontext;

	@Autowired
	OpenAM openam;

	Gson gsonPrinter = new GsonBuilder().disableHtmlEscaping().create();

	ObjectMapper mapper = new ObjectMapper();

	public void acr9(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var result = openam.parseOAuth2Entities().get("result");

		for (final var json : result) {
			final var id = json.get("_id").asText();

			final var acrs = new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues")) {
				json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));
			}

			acrs.removeIf(StringUtils::isEmptyOrWhitespace);

			if (!acrs.isEmpty() && acrs.contains("9")) {
				AdhocProcessor.logger.debug("OAuth2Entity: {} ", id);
			}
		}
	}

	public void acrList() throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var env = "prod";
		final String[] columns = { "ID", "ACR" };

		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get(env + "-id-acr.csv"));
		final var csvPrinter = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		final var result = mapper.readValue(Paths.get(env + "/jsonOAuth2Entities.json").toFile(), JsonNode.class).get("result");
		for (final var json : result) {
			final var id = json.get("_id").asText();

			final var acrs = new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues")) {
				json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));
				csvPrinter.printRecord(id, String.join(",", acrs));
			}

			acrs.removeIf(StringUtils::isEmptyOrWhitespace);

			if (!acrs.isEmpty() && acrs.contains("9")) {
				AdhocProcessor.logger.debug("OAuth2Entity: {} ", id);
			}
		}
	}

	public void cotDiff(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var past = mapper.readValue(Paths.get("stage-21-jan/jsonCircleOfTrust.json").toFile(), JsonNode.class).get("result");
		final var pastTable = new HashMap<String, HashSet<String>>();
		for (final var json : past) {
			if (!json.has("trustedProviders")) {
				continue;
			}

			final var id = json.get("_id").asText();
			pastTable.put(id, new HashSet<>());
			final var trustedProviders = json.get("trustedProviders");
			for (final var tp : trustedProviders) {
				pastTable.get(id).add(tp.asText());
			}
		}

		kontext.initilize("stage");
		final var today = mapper.readValue(Paths.get("stage/jsonCircleOfTrust.json").toFile(), JsonNode.class).get("result");
		final var todayTable = new HashMap<String, HashSet<String>>();
		for (final var json : today) {
			if (!json.has("trustedProviders")) {
				continue;
			}

			final var id = json.get("_id").asText();
			todayTable.put(id, new HashSet<>());
			final var trustedProviders = json.get("trustedProviders");
			for (final var tp : trustedProviders) {
				todayTable.get(id).add(tp.asText());
			}
		}

		for (final var p : pastTable.keySet()) {
			if (!todayTable.containsKey(p)) {
				AdhocProcessor.logger.error("COT: {} missing", p);
			} else {
				pastTable.get(p).removeAll(todayTable.get(p));

				if (!pastTable.get(p).isEmpty()) {
					AdhocProcessor.logger.error("COT: {}, missing enteries: {}", p, String.join(",", pastTable.get(p)));
				}

			}
		}

	}

	public void invaliEntities(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var entities = new HashSet<>();
		Files.list(Paths.get("C:\\Users\\hdhanjal005\\Downloads\\am_AmsterexportConfig-20240120\\realms\\root-pwc\\WsEntity")).forEach(f -> {
			try {
				final var w = mapper.readValue(f.toFile(), JsonNode.class).get("data");
				entities.add(w.get("_id").asText());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});

		Files.list(Paths.get("C:\\Users\\hdhanjal005\\Downloads\\am_AmsterexportConfig-20240120\\realms\\root-pwc\\Saml2Entity")).forEach(f -> {
			try {
				final var s = mapper.readValue(f.toFile(), JsonNode.class).get("data");
				entities.add(s.get("_id").asText());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});

		Files.list(Paths.get("C:\\Users\\hdhanjal005\\Downloads\\am_AmsterexportConfig-20240120\\realms\\root-pwc\\CircleOfTrust")).forEach(f -> {
			try {
				final var cot = mapper.readValue(f.toFile(), JsonNode.class).get("data");
				final var trustedProviders = new HashSet<String>();
				cot.get("trustedProviders").forEach(provider -> {
					final var eid = EntityID.ParseProviderEntry(provider.asText()).getID();
					trustedProviders.add(eid);
				});
				entities.removeAll(trustedProviders);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});

		// AdhocProcessor.logger.info(String.join(", ", entities));

	}

	public void invaliEntitiesInMultipleCOT(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var rps = new HashMap<String, Set<String>>();
		final var cots = openam.parseCircleOfTrust().get("result");
		final var saml2Entities = openam.parseSaml2Entities().get("result");
		final var wsEntities = openam.parseWsEntities().get("result");

		for (final var entry : wsEntities) {
			rps.put(entry.get("_id").asText(), new HashSet<String>());
		}

		for (final var entry : saml2Entities) {
			rps.put(entry.get("_id").asText(), new HashSet<String>());
		}

		for (final var cot : cots) {
			if (!cot.has("trustedProviders")) {
				continue;
			}
			cot.get("trustedProviders").forEach(provider -> {
				final var id = EntityID.ParseProviderEntry(provider.asText()).getID();
				if (rps.containsKey(id)) {
					rps.get(id).add(cot.get("_id").asText());
				}
			});
		}

		rps.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
			AdhocProcessor.logger.info("{}: [{}]", e.getKey(), String.join(", ", e.getValue()));
		});

	}

	public void listPolicies(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final List<String> rps = Arrays.asList("https://alt-xdp-se.pwcinternal.com/aas", "urn:insightsds.pwc.com", "learningrequests.pwc.com", "urn:eia:prd", "urn:ivd:prd", "urn:labs-hosted-apps",
				"urn:p2p:web:prd", "urn:personalindependence.pwc.com", "urn:pwcusayp:prd", "https://pwc.sdelements.com", "https://pwc.sdelements.com/sso/saml2/metadata/",
				"https://pwc.tfaforms.net/authenticator_saml/metadata", "https://aire-pm.pwc.com");

		final var policiies = openam.parsePolicies().get("result");

		final Map<String, HashSet<String>> rpPolicies = new HashMap<>();
		rps.forEach(r -> rpPolicies.put(r, new HashSet<>()));

		openam.parsePolicies().get("result");

		for (final var policy : policiies) {
			if (!policy.has("resources")) {
				continue;
			}

			new HashSet<>();
			policy.get("resources").forEach(resource -> {
				final var id = EntityID.ParseResourceEntry(resource.asText());
				if (rpPolicies.containsKey(id.getID())) {
					rpPolicies.get(id.getID()).add(policy.get("_id").asText());
				}
			});
		}

		rpPolicies.entrySet().stream().forEach(r -> {
			AdhocProcessor.logger.info("{}: [{}]", r.getKey(), String.join(", ", r.getValue()));
		});
	}

	public void policiesDiff(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var past = mapper.readValue(Paths.get("stage-21-jan/jsonPolicies.json").toFile(), JsonNode.class).get("result");
		final var pastTable = new HashMap<String, HashSet<String>>();
		for (final var json : past) {
			if (!json.has("resources")) {
				continue;
			}

			final var id = json.get("_id").asText();
			pastTable.put(id, new HashSet<>());
			final var resources = json.get("resources");
			for (final var r : resources) {
				pastTable.get(id).add(r.asText());
			}
		}

		kontext.initilize("stage");
		final var today = mapper.readValue(Paths.get("stage/jsonPolicies.json").toFile(), JsonNode.class).get("result");
		final var todayTable = new HashMap<String, HashSet<String>>();
		for (final var json : today) {
			if (!json.has("resources")) {
				continue;
			}

			final var id = json.get("_id").asText();
			todayTable.put(id, new HashSet<>());
			final var resources = json.get("resources");
			for (final var r : resources) {
				todayTable.get(id).add(r.asText());
			}
		}

		for (final var p : pastTable.keySet()) {
			if (!todayTable.containsKey(p)) {
				AdhocProcessor.logger.error("POLICY: {} missing", p);
			} else {
				pastTable.get(p).removeAll(todayTable.get(p));

				if (!pastTable.get(p).isEmpty()) {
					AdhocProcessor.logger.error("POLICY: {}, missing enteries: {}", p, String.join(",", pastTable.get(p)));
				}

			}
		}

	}

	public void policiesDiff2(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var table20 = new HashMap<String, JsonNode>();
		Files.list(Paths.get("C:\\Users\\hdhanjal005\\Downloads\\am_AmsterexportConfig-20240120\\realms\\root-pwc\\Policies")).forEach(f -> {
			try {
				final var policy = mapper.readValue(f.toFile(), JsonNode.class).get("data");
				table20.put(policy.get("_id").asText(), policy);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});

		final var table28 = new HashMap<String, JsonNode>();
		Files.list(Paths.get("C:\\Users\\hdhanjal005\\Downloads\\am_AmsterexportConfig-20240128\\realms\\root-pwc\\Policies")).forEach(f -> {
			try {
				final var policy = mapper.readValue(f.toFile(), JsonNode.class).get("data");
				table28.put(policy.get("_id").asText(), policy);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});

		final var policiesOfConcerns = Arrays.asList("OAuth Internal_no_fallback Tree", "ExternalUsersTrustedDevice", "SAML/WS-Fed/OAuth Internal CERT", "OAuth Internal Cert", "OAuth External MFA",
				"External User MFA");

		for (final String p : policiesOfConcerns) {
			if (!table20.containsKey(p)) {
				AdhocProcessor.logger.error("not available in 20240120: {}", p);
			}
			if (!table28.containsKey(p)) {
				AdhocProcessor.logger.error("not available in 20240128: {}", p);
			}

			final var policy20 = table20.get(p);
			final var resources20Set = new HashSet<String>();
			final var resources20 = policy20.get("resources");
			for (final var r : resources20) {
				resources20Set.add(r.asText());
			}

			final var policy28 = table28.get(p);
			final var resources28Set = new HashSet<String>();
			final var resources28 = policy28.get("resources");
			for (final var r : resources28) {
				resources28Set.add(r.asText());
			}
			resources20Set.removeAll(resources28Set);
		}
	}

	@Override
	public void run(final String... args) throws Exception {
		try {
			kontext.initilize("prod");
			AdhocProcessor.logger.debug("setting environment: {}", kontext.getEnvironment());
			AdhocProcessor processor = new AdhocProcessor();
			// processor.acrList();
			// processor.mainApplicationsAccessPolicies(args);
			processor.authModulesHelper(args);
			AdhocProcessor.logger.debug("AdhocProcessor.run");
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void clienfedProcessior(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");
		Pattern _patternClientFedPolicies = Pattern.compile("PwCIDClientFed", Pattern.CASE_INSENSITIVE);

		final String[] columns = { "POLICY", "STATUS" };
		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get(env + "-client-fed-policies.csv"));
		final var printer = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		for (final var json : resultPolicies) {
			final var policyname = json.get("_id").asText();
			// logger.debug("processing policy: {}", policyname);
			final var policy = new Policy(policyname);

			if (policyname.contains("PwCIDClientFed")) {
				policy.addAttribute(Entity.STATUS, "true".equalsIgnoreCase(json.get("active").asText()) ? Entity.STATUS_ACTIVE : Entity.STATUS_INACTIVE);

				printer.printRecord(policyname, json.get("active").asText());
				// boolean selected = _patternClientFedPolicies.matcher(policy.getID()).find()
				// && policy.hasAttribute(Entity.STATUS) && true ==
				// Boolean.valueOf(policy.getAttribute(Entity.STATUS));
			}

		}
		printer.close();

	}

	private void reformatAccessPolicies(String[] args) throws IOException {
		final var targetPolicies = new JsonObject();
		final var lines = Files.readAllLines(Path.of("input/policies-reformat/amster.log"));

		for (final String line : lines) {
			if (!line.startsWith("===>")) {
				continue;
			}

			final var json = JsonParser.parseString(line.substring("===>".length())).getAsJsonObject();
			System.out.println("------------------\npolicy-id:" + json.get("_id").getAsString());
			json.remove("_rev");

			JsonArray updatedResources = json.getAsJsonArray("resources").deepCopy();
			Set<String> resourceSet = new HashSet<>();
			for (var h : updatedResources) {
				resourceSet.add(h.getAsJsonPrimitive().getAsString());
			}

			boolean dirty = false;

			for (var h : json.getAsJsonArray("resources")) {
				String original = h.getAsJsonPrimitive().getAsString();
				// Replace "http:/" with "http/"
				var modified = original.replaceAll("http:/", "http/");
				// Replace "https:/" with "https/"
				modified = modified.replaceAll("https:/", "https/");

				if (!original.equals(modified) && !resourceSet.contains(modified)) {
					dirty = true;
					resourceSet.add(modified);
					updatedResources.add(new JsonPrimitive(modified));
				}
			}

			if (dirty) {
				System.out.println("changed: yes");
				for (var h : updatedResources) {
					System.out.println(h);
				}
			} else {
				System.out.println("changed: no");
				for (var h : updatedResources) {
					System.out.println(h);
				}
			}

			json.add("resources", updatedResources);
			if (dirty)
				System.out.println("update Policies --realm '/pwc' --id \"" + json.get("_id").getAsString() + "\" --body '" + json + "'");

		}
	}

	private void reformatStepUpPolicies(String[] args) throws IOException {
		// "oauth-https://pwccshnldev.service-now.com/|domain-*|type-*"
		// "oauth-https://pwccshnldev.service-now.com/|domain-*"

		final var targetPolicies = new JsonObject();
		final var lines = Files.readAllLines(Path.of("input/policies-reformat/amster.log"));

		for (final String line : lines) {
			if (!line.startsWith("===>")) {
				continue;
			}

			final var json = JsonParser.parseString(line.substring("===>".length())).getAsJsonObject();
			System.out.println("------------------\npolicy-id:" + json.get("_id").getAsString());
			json.remove("_rev");

			JsonArray updatedResources = new JsonArray();
			Set<String> resourceSet = new HashSet<>();

			boolean dirty = false;

			for (var h : json.getAsJsonArray("resources")) {
				String original = h.getAsJsonPrimitive().getAsString();
				// Replace "http:/" with "http/"

				var modified = original;
				if (original.endsWith("|type-*")) {
					modified = original.substring(0, original.length() - "|type-*".length());
					// System.out.println(modified);
				}

				if (!resourceSet.contains(modified)) {
					dirty = true;
					resourceSet.add(modified);
					updatedResources.add(new JsonPrimitive(modified));
				}
			}

			if (dirty) {
				System.out.println("changed: yes");
				for (var h : updatedResources) {
					// System.out.println(h);
				}
			} else {
				System.out.println("changed: no");
				for (var h : updatedResources) {
					// System.out.println(h);
				}
			}

			json.add("resources", updatedResources);
			if (dirty)
				System.out.println("update Policies --realm '/pwc' --id \"" + json.get("_id").getAsString() + "\" --body '" + json + "'");

		}
	}

	public void authModulesHelper(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		//String directoryPath = "C:\\Users\\hdhanjal005\\Downloads\\stg_am_exportConfig-20240814155754\\realms\\root-pwc\\authSamlPwci";
		String directoryPath = "C:\\Users\\hdhanjal005\\Downloads\\prod_am_exportConfig-20240814161113\\realms\\root-pwc\\authSamlPwci";

		// Create a list to hold the loaded objects
		List<JsonNode> objects = new ArrayList<>();

		try {
			// Traverse the directory
			Stream<Path> paths = Files.walk(Paths.get(directoryPath));

			// Filter JSON files and load them
			paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).forEach(path -> {
				try {
					// Load JSON file into object
					ObjectMapper mapper = new ObjectMapper();
					JsonNode obj = mapper.readValue(path.toFile(), JsonNode.class);
					objects.add(obj);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		final String[] columns = { "ID", "entityName" };

		CSVFormat.DEFAULT.builder().setHeader(columns).setSkipHeaderRecord(true).build();
		final var outputCsv = Files.newBufferedWriter(Paths.get("prod-chains.csv"));
		final var printer = new CSVPrinter(outputCsv, CSVFormat.DEFAULT.builder().setHeader(columns).build());

		for (var json : objects) {
			final var id = json.get("data").get("_id").asText();
			final var entityName = json.get("data").get("entityName").asText();
			printer.printRecord(id, entityName);
		}

		printer.close();

	}

	@SuppressWarnings("deprecation")
	public void stepUpHelper(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		final var regex = "^([^-]*)-(.*?)[|]";
		final var pattern = Pattern.compile(regex);

		final var jsonHelper = new JsonObject();

		final var policyNames = Arrays.asList("SAML/WS-Fed/OAuth Internal MFA", "SAML/WS-Fed/OAuth Internal CERT", "External User MFA", "ExternalUsersTrustedDevice");
		Arrays.asList("wsfed", "oauth", "saml");
		final var policyMapJson = new JsonObject();
		policyMapJson.addProperty("oauth-External User MFA", "OAuth External MFA");
		policyMapJson.addProperty("oauth-ExternalUsersTrustedDevice", "OAuth Stepup External Trust My device");
		policyMapJson.addProperty("oauth-SAML/WS-Fed/OAuth Internal CERT", "OAuth Internal Cert");
		policyMapJson.addProperty("oauth-SAML/WS-Fed/OAuth Internal MFA", "OAuth Internal MFA");

		policyMapJson.addProperty("wsfed-External User MFA", "WSFed External MFA");
		policyMapJson.addProperty("wsfed-ExternalUsersTrustedDevice", "WSFed Stepup External Trust My device");
		policyMapJson.addProperty("wsfed-SAML/WS-Fed/OAuth Internal CERT", "WSFed Internal CERT");
		policyMapJson.addProperty("wsfed-SAML/WS-Fed/OAuth Internal MFA", "WSFed Internal MFA");

		policyMapJson.addProperty("saml-External User MFA", "SAML External MFA");
		policyMapJson.addProperty("saml-ExternalUsersTrustedDevice", "SAML Stepup External Trust My device");
		policyMapJson.addProperty("saml-SAML/WS-Fed/OAuth Internal CERT", "SAML Internal CERT");
		policyMapJson.addProperty("saml-SAML/WS-Fed/OAuth Internal MFA", "SAML Internal MFA");

		for (final var json : resultPolicies) {
			final var policyname = json.get("_id").asText();

			if (policyNames.contains(policyname)) {

				if (!json.has("resources") || !json.has("resources")) {
					PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
					return;
				}

				json.get("resources").forEach(resource -> {
					final var matcher = pattern.matcher(resource.asText());
					if (!matcher.find()) {
						AdhocProcessor.logger.error("shall never encounter this");
						return;
					}

					if (!jsonHelper.has(policyname)) {
						jsonHelper.add(policyname, new JsonObject());
					}

					final var jsonHelperPolicy = jsonHelper.getAsJsonObject(policyname);
					if (!jsonHelperPolicy.has(matcher.group(1))) {
						jsonHelperPolicy.add(matcher.group(1), new JsonArray());
					}

					final var jsonHelperPolicyResources = jsonHelperPolicy.getAsJsonArray(matcher.group(1));
					jsonHelperPolicyResources.add(MessageFormat.format("{0}-{1}|domain-*|type-*", matcher.group(1), matcher.group(2)));

				});
			}
		}

		final var targetPolicies = new JsonObject();
		final var lines = Files.readAllLines(Path.of("step-up/amster.log"));
		for (final String line : lines) {
			if (!line.startsWith("===>")) {
				continue;
			}

			final var json = JsonParser.parseString(line.substring("===>".length())).getAsJsonObject();
			targetPolicies.add(json.get("_id").getAsString(), json);
		}

		for (final String policy : jsonHelper.keySet()) {
			final var policyContents = jsonHelper.getAsJsonObject(policy);
			for (final String protocol : policyContents.keySet()) {
				final var resources = policyContents.getAsJsonArray(protocol);
				final var targetPolicyKey = MessageFormat.format("{0}-{1}", protocol, policy);

				com.google.common.io.Files.write(gsonPrinter.toJson(resources).getBytes(), new File(targetPolicyKey.replaceAll(" |/", "-")));

				if (policyMapJson.has(targetPolicyKey)) {
					final var targetPolicyName = policyMapJson.get(targetPolicyKey).getAsString();
					logger.debug(targetPolicyName);
					final var tragetPolicy = targetPolicies.get(targetPolicyName).getAsJsonObject();

					tragetPolicy.remove("_rev");
					tragetPolicy.add("resources", resources);

					final var command = "update Policies --realm  '/pwc' --id '" + tragetPolicy.get("_id").getAsString() + "' --body '" + tragetPolicy.toString() + "'";

					System.out.println(command);
				}
			}
		}

	}

}
