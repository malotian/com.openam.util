package com.openam.util;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openam.entity.processor.CircleOfTrustProcessor;
import com.openam.entity.processor.OAuth2ClientProcessor;
import com.openam.entity.processor.PolicyProcessor;
import com.openam.entity.processor.Saml2Processor;
import com.openam.entity.processor.WsfedProcessor;

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

	private boolean fetchCircleOfTrust() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/circlesoftrust?_queryFilter=true", kontext.file("jsonCircleOfTrust.json"));
	}

	private boolean fetchOAuth2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/agents/OAuth2Client?_queryFilter=true", kontext.file("jsonOAuth2Entities.json"));
	}

	private boolean fetchPolicies() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/policies?_queryFilter=true", kontext.file("jsonPolicies.json"));
	}

	private boolean fetchSaml2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/entityproviders/saml2?_queryFilter=true", kontext.file("jsonSaml2Entities.json"));
	}

	private boolean fetchWs() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return restKlient.fetch("/json/realm-config/federation/entityproviders/ws?_queryFilter=true", kontext.file("jsonWsEntities.json"));
	}

	public Konfiguration getKonfiguration() {
		return konfiguration;
	}

	ObjectMapper getMapper() {
		return mapper;
	}

	private boolean login() throws IOException, ClientProtocolException {
		return restKlient.login("/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
	}

	boolean loginAndFetchEntities() throws ClientProtocolException, IOException {
		if (!login())
			return false;
		return fetchWs() && fetchOAuth2() && fetchPolicies() && fetchCircleOfTrust() && fetchSaml2();
	}

	void processEntities() throws ClientProtocolException, IOException {

		final var jsonPolicies = getMapper().readValue(kontext.file("jsonPolicies.json"), JsonNode.class);
		policyProcessor.process(jsonPolicies);

		final var jsonWsEntities = getMapper().readValue(kontext.file("jsonWsEntities.json"), JsonNode.class);
		wsfedProcessor.process(jsonWsEntities);

		final var jsonOAuth2Entities = getMapper().readValue(kontext.file("jsonOAuth2Entities.json"), JsonNode.class);
		oAuth2ClientProcessor.process(jsonOAuth2Entities);

		final var jsonSaml2Entities = getMapper().readValue(kontext.file("jsonSaml2Entities.json"), JsonNode.class);
		saml2Processor.process(jsonSaml2Entities);

		final var jsonCircleOfTrust = getMapper().readValue(kontext.file("jsonCircleOfTrust.json"), JsonNode.class);
		circleOfTrustProcessor.process(jsonCircleOfTrust);
	}

	public void setKonfiguration(final Konfiguration konfiguration) {
		this.konfiguration = konfiguration;
	}

}
