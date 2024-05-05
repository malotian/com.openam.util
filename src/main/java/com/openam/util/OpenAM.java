package com.openam.util;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openam.util.entity.processor.CircleOfTrustProcessor;
import com.openam.util.entity.processor.OAuth2ClientProcessor;
import com.openam.util.entity.processor.PolicyProcessor;
import com.openam.util.entity.processor.Saml2Processor;
import com.openam.util.entity.processor.WsfedProcessor;
import com.openam.util.rest.RestKlient;

@Component
@EnableConfigurationProperties(value = Konfiguration.class)
public class OpenAM {

	static final Logger logger = LoggerFactory.getLogger(OpenAM.class);

	public static Pattern patternCirceOfTrust2025 = Pattern.compile("25|2025");
	public static Pattern patternCirceOfTrust2031 = Pattern.compile("31|2031");

	@Autowired
	private RestKlient restKlient;

	@Autowired
	private Konfiguration konfiguration;

	@Autowired
	private Kontext kontext;

	private final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	PolicyProcessor policyProcessor;

	@Autowired
	Saml2Processor saml2Processor;

	@Autowired
	OAuth2ClientProcessor oAuth2ClientProcessor;

	@Autowired
	WsfedProcessor wsfedProcessor;

	@Autowired
	CircleOfTrustProcessor circleOfTrustProcessor;

	public OpenAM() {
		System.setProperty("jdk.httpclient.keepalive.timeout", "99999");
	}

	private JsonNode fetchCircleOfTrust() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/circlesoftrust?_queryFilter=true", kontext.file("jsonCircleOfTrust.json"));
	}

	private JsonNode fetchOAuth2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/agents/OAuth2Client?_queryFilter=true", kontext.file("jsonOAuth2Entities.json"));
	}

	private JsonNode fetchPolicies() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/policies?_queryFilter=true", kontext.file("jsonPolicies.json"));
	}

	private JsonNode fetchSaml2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/entityproviders/saml2?_queryFilter=true", kontext.file("jsonSaml2Entities.json"));
	}

	private JsonNode fetchWs() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/entityproviders/ws?_queryFilter=true", kontext.file("jsonWsEntities.json"));
	}

	public Konfiguration getKonfiguration() {
		return konfiguration;
	}

	ObjectMapper getMapper() {
		return mapper;
	}

	public boolean login() throws IOException, ClientProtocolException {
		return restKlient.login("/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
	}

	public JsonNode parseCircleOfTrust() throws IOException, StreamReadException, DatabindException {
		return getMapper().readValue(kontext.file("jsonCircleOfTrust.json"), JsonNode.class);
	}

	public JsonNode parseOAuth2Entities() throws IOException, StreamReadException, DatabindException {
		return getMapper().readValue(kontext.file("jsonOAuth2Entities.json"), JsonNode.class);
	}

	public JsonNode parsePolicies() throws IOException, StreamReadException, DatabindException {
		return getMapper().readValue(kontext.file("jsonPolicies.json"), JsonNode.class);
	}

	public JsonNode parseSaml2Entities() throws IOException, StreamReadException, DatabindException {
		return getMapper().readValue(kontext.file("jsonSaml2Entities.json"), JsonNode.class);
	}

	public JsonNode parseWsEntities() throws IOException, StreamReadException, DatabindException {
		return getMapper().readValue(kontext.file("jsonWsEntities.json"), JsonNode.class);
	}

	public boolean processEntities(final boolean refresh) throws ClientProtocolException, IOException {

		if (refresh && !login()) {
			return false;
		}

		var result = refresh ? fetchPolicies() : parsePolicies();
		policyProcessor.process(result);

		result = refresh ? fetchWs() : parseWsEntities();
		wsfedProcessor.process(result);

		result = refresh ? fetchOAuth2() : parseOAuth2Entities();
		oAuth2ClientProcessor.process(result);

		var resultCOT = refresh ? fetchCircleOfTrust() : parseCircleOfTrust();

		result = refresh ? fetchSaml2() : parseSaml2Entities();
		saml2Processor.process(result);

		circleOfTrustProcessor.process(resultCOT);

		return true;
	}

	public void setKonfiguration(final Konfiguration konfiguration) {
		this.konfiguration = konfiguration;
	}

}
