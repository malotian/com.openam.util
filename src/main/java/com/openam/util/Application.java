package com.openam.util;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class Application {

	static final HttpClient httpClient = HttpClientBuilder.create().build();
	static final ObjectMapper mapper = new ObjectMapper();

	public static String getOpenamUrl() {
		return openamUrl;
	}

	public static String getOpenamUsername() {
		return openamUsername;
	}

	public static String getOpenamPassword() {
		return openamPassword;
	}

	@Value("${openam.url}")
	private static String openamUrl;

	@Value("${openam.username}")
	private static String openamUsername;

	@Value("${openam.password}")
	private static String openamPassword;

	public static void main(final String[] args) throws IOException {

		final var mapper = new ObjectMapper();

		final var jsonPolicies = mapper.readValue(Paths.get("jsonPolicies.json").toFile(), JsonNode.class);
		Policy.process(jsonPolicies);

		final var jsonSaml2Entities = mapper.readValue(Paths.get("jsonSaml2Entities.json").toFile(), JsonNode.class);
		Saml2.process(jsonSaml2Entities);

		final var jsonWsEntities = mapper.readValue(Paths.get("jsonWsEntities.json").toFile(), JsonNode.class);
		Wsfed.process(jsonWsEntities);

		final var jsonOAuth2Entities = mapper.readValue(Paths.get("jsonOAuth2Entities.json").toFile(), JsonNode.class);
		Wsfed.process(jsonOAuth2Entities);

		final var jsonCircleOfTrust = mapper.readValue(Paths.get("jsonCircleOfTrust.json").toFile(), JsonNode.class);
		CircleOfTrust.process(jsonCircleOfTrust);

		SpringApplication.run(Application.class, args);
	}

	public static void main2(final String[] args) throws IOException {

		final var loginRequest = new HttpPost(getOpenamUrl() + "/openam/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
		loginRequest.addHeader("X-OpenAM-Username", getOpenamUsername());
		loginRequest.addHeader("X-OpenAM-Password", getOpenamPassword());
		Application.httpClient.execute(loginRequest);
		final var mapper = new ObjectMapper();

		final var fetchSaml2EntitiesRequest = new HttpGet(getOpenamUrl() + "/openam/json/realm-config/federation/entityproviders/saml2?_queryFilter=true");
		final var jsonSaml2Entities = mapper.readTree(Application.httpClient.execute(fetchSaml2EntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonSaml2Entities.json").toFile(), jsonSaml2Entities);

		final var fetchWsEntitiesRequest = new HttpGet(getOpenamUrl() + "/openam/json/realm-config/federation/entityproviders/ws?_queryFilter=true");
		final var jsonWsEntities = mapper.readTree(Application.httpClient.execute(fetchWsEntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonWsEntities.json").toFile(), jsonWsEntities);

		final var fetchOAuth2EntitiesRequest = new HttpGet(getOpenamUrl() + "/json/realm-config/agents/OAuth2Client?_queryFilter=true");
		final var jsonOAuth2Entities = mapper.readTree(Application.httpClient.execute(fetchOAuth2EntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonSaml2Entities.json").toFile(), jsonOAuth2Entities);

		final var fetchPoliciesRequest = new HttpGet(getOpenamUrl() + "/json/policies?_queryFilter=true");
		final var jsonPolicies = mapper.readTree(Application.httpClient.execute(fetchPoliciesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonPolicies.json").toFile(), jsonPolicies);

		final var fetchCircleOfTrustRequest = new HttpGet("https://login-stg.pwcinternal.com/openam/json/realm-config/federation/circlesoftrust?_queryFilter=true");
		final var jsonCircleOfTrust = mapper.readTree(Application.httpClient.execute(fetchCircleOfTrustRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonCircleOfTrust.json").toFile(), jsonCircleOfTrust);

	}

}
