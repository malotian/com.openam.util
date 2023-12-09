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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import com.openam.util.Kontext;
import com.openam.util.OpenAM;

@SpringBootApplication
@ComponentScan(basePackages = "com.openam.*")
public class AdhocProcessor implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(AdhocProcessor.class);

	@Autowired
	Kontext kontext;

	@Autowired
	OpenAM openam;

	Gson gsonPrinter = new GsonBuilder().disableHtmlEscaping().create();

	public static void main(String[] args) {
		SpringApplication.run(AdhocProcessor.class, args).close();
	}

	@Override
	public void run(String... args) throws Exception {
		kontext.initilize("prod");
		logger.debug("setting environment: {}", kontext.getEnvironment());

		acr9(args);
		logger.debug("AdhocProcessor.run");
	}

	public void acr9(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		var result = openam.parseOAuth2Entities().get("result");

		for (final var json : result) {
			final var id = json.get("_id").asText();

			final var acrs = new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOpenIDClientConfig").has("defaultAcrValues")) {
				json.get("coreOpenIDClientConfig").get("defaultAcrValues").forEach(h -> acrs.add(h.asText()));
			}

			acrs.removeIf(StringUtils::isEmptyOrWhitespace);

			if (!acrs.isEmpty()) {
				if (acrs.contains("9"))
					AdhocProcessor.logger.debug("OAuth2Entity: {} ", id);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void stepUpHelper(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		String regex = "^([^-]*)-(.*?)[|]";
		Pattern pattern = Pattern.compile(regex);

		JsonObject jsonHelper = new JsonObject();

		var policyNames = Arrays.asList("SAML/WS-Fed/OAuth Internal MFA", "SAML/WS-Fed/OAuth Internal CERT", "External User MFA", "ExternalUsersTrustedDevice");
		var protcols = Arrays.asList("wsfed", "oauth", "saml");
		JsonObject policyMapJson = new JsonObject();
		policyMapJson.addProperty("oauth-External User MFA", "OAuth External MFA");
		policyMapJson.addProperty("oauth-ExternalUsersTrustedDevice", "OAuth Stepup External Trust My device");
		policyMapJson.addProperty("oauth-SAML/WS-Fed/OAuth Internal CERT", "OAuth Internal Cert");
		policyMapJson.addProperty("oauth-SAML/WS-Fed/OAuth Internal MFA", "OAuth Internal MFA");

		policyMapJson.addProperty("wsfed-External User MFA", "WSFed External MFA");
		policyMapJson.addProperty("wsfed-ExternalUsersTrustedDevice", "WSFed Stepup External Trust My device");
		policyMapJson.addProperty("wsfed-SAML/WS-Fed/OAuth Internal CERT", "WSFed Internal CERT");
		policyMapJson.addProperty("wsfed-SAML/WS-Fed/OAuth Internal MFA", "WSFed Internal MFA");

//		policyMappings.put("saml-External User MFA", "Saml External MFA");
//		policyMappings.put("saml-ExternalUsersTrustedDevice", "Saml Stepup External Trust My device");
//		policyMappings.put("saml-SAML/WS-Fed/OAuth Internal CERT", "Saml Internal CERT");
//		policyMappings.put("saml-SAML/WS-Fed/OAuth Internal MFA", "Saml Internal MFA");

		for (final var json : resultPolicies) {
			final var policyname = json.get("_id").asText();

			if (policyNames.contains(policyname)) {

				if (!json.has("resources")) {
					PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
					return;
				}

				if (!json.has("resources")) {
					PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
					return;
				}

				json.get("resources").forEach(resource -> {
					Matcher matcher = pattern.matcher(resource.asText());
					if (!matcher.find()) {
						logger.error("shall never encounter this");
						return;
					}

					if (!jsonHelper.has(policyname)) {
						jsonHelper.add(policyname, new JsonObject());
					}

					var jsonHelperPolicy = jsonHelper.getAsJsonObject(policyname);
					if (!jsonHelperPolicy.has(matcher.group(1))) {
						jsonHelperPolicy.add(matcher.group(1), new JsonArray());
					}

					var jsonHelperPolicyResources = jsonHelperPolicy.getAsJsonArray(matcher.group(1));
					jsonHelperPolicyResources.add(MessageFormat.format("{0}-{1}|domain-*|type-*", matcher.group(1), matcher.group(2)));

				});
			}
		}

		JsonObject targetPolicies = new JsonObject();
		List<String> lines = Files.readAllLines(Path.of("step-up/amster.log"));
		for (String line : lines) {
			if (!line.startsWith("===>"))
				continue;

			var json = JsonParser.parseString(line.substring("===>".length())).getAsJsonObject();
			targetPolicies.add(json.get("_id").getAsString(), json);
		}

		for (String policy : jsonHelper.keySet()) {
			JsonObject policyContents = jsonHelper.getAsJsonObject(policy);
			for (String protocol : policyContents.keySet()) {
				JsonArray resources = policyContents.getAsJsonArray(protocol);
				String targetPolicyKey = MessageFormat.format("{0}-{1}", protocol, policy);

				com.google.common.io.Files.write(gsonPrinter.toJson(resources).getBytes(), new File(targetPolicyKey.replaceAll(" |/", "-")));

				if (policyMapJson.has(targetPolicyKey)) {
					var targetPolicyName = policyMapJson.get(targetPolicyKey).getAsString();
					var tragetPolicy = targetPolicies.get(targetPolicyName).getAsJsonObject();

					tragetPolicy.remove("_rev");
					tragetPolicy.add("resources", resources);

					String command = "update Policies --realm  '/pwc' --id '" + tragetPolicy.get("_id").getAsString() + "' --body '" + tragetPolicy.toString() + "'";

					System.out.println(command);
				}
			}
		}

	}

	public static void main7(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();

		List<String> policyNames = Arrays.asList("SAML/WS-Fed/OAuth Internal MFA", "SAML/WS-Fed/OAuth Internal CERT", "External User MFA", "ExternalUsersTrustedDevice");
		List<String> protocols = Arrays.asList("saml", "wsfed", "oauth");

		for (String p : protocols) {
			for (int i = 0; i < policyNames.size(); ++i) {
				String filenameI = MessageFormat.format("{0}---{1}", p, policyNames.get(i).replaceAll(" |/", "-"));
				var resoucesI = mapper.readValue(Paths.get(filenameI).toFile(), JsonNode.class);
				for (int j = i + 1; j < policyNames.size(); ++j) {
					String filenameJ = MessageFormat.format("{0}---{1}", p, policyNames.get(j).replaceAll(" |/", "-"));
					var resoucesJ = mapper.readValue(Paths.get(filenameJ).toFile(), JsonNode.class);
					var common = commonItems(toListOfStrings(resoucesI), toListOfStrings(resoucesJ));
					logger.debug("{}: [{},{}]", common.size(), policyNames.get(i), policyNames.get(j));
					logger.debug("{}", String.join(",", common));
					break;
				}
				break;
			}

		}
	}

	public static void main9(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "stage";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonPolicies.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		String regex = "^([^-]*)-(.*?)[|]";
		Pattern pattern = Pattern.compile(regex);

		JsonObject jsonHelper = new JsonObject();
		Gson gsonPrettyPrinter = new GsonBuilder().disableHtmlEscaping().create();

		for (final var json : resultPolicies) {
			final var policyname = json.get("_id").asText();

			if (!json.has("resources")) {
				PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
				return;
			}

			if (json.get("resources").has("urn:pwcid:stg:saml:test"))
				logger.debug("{}", policyname);

		}

	}

	public static void main8(final String[] args) throws StreamReadException, DatabindException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		final var mapper = new ObjectMapper();
		final var env = "prod";
		final var jsonSaml2Entities = mapper.readValue(Paths.get(env + "/jsonOAuth2Entities.json").toFile(), JsonNode.class);
		final var resultPolicies = jsonSaml2Entities.get("result");

		CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(env + "-oauth-status.csv")), CSVFormat.DEFAULT.withHeader("ID", "Status"));

		for (final var json : resultPolicies) {
			final var id = json.get("_id").asText();

			final var acrs = new HashSet<String>();
			if (json.has("coreOpenIDClientConfig") && json.get("coreOAuth2ClientConfig").has("status")) {
				csvPrinter.printRecord(id, json.get("coreOAuth2ClientConfig").get("status"));
			}
		}

	}

	private static List<String> toListOfStrings(JsonNode array) {
		ArrayList<String> list = new ArrayList<>();
		for (var item : array) {
			list.add(item.asText());
		}
		return list;
	}

	private static List<String> commonItems(List<String> list1, List<String> list2) {
		ArrayList<String> commonList = new ArrayList<>();
		for (String str : list1) {
			if (list2.contains(str)) {
				// add to the common list if it is not already present
				if (!commonList.contains(str)) {
					commonList.add(str);
				}
			}
		}
		return commonList;
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

}
