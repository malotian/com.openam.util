package com.openam.util;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAM {

	public static Pattern patternCirceOfTrust2025 = Pattern.compile("25|2025");
	public static Pattern patternCirceOfTrust2031 = Pattern.compile("31|2031");

	private final HttpClient httpClient = HttpClientBuilder.create().build();
	private final ObjectMapper mapper = new ObjectMapper();

	HttpClient getHttpclient() {
		return httpClient;
	}

	ObjectMapper getMapper() {
		return mapper;
	}

	private static OpenAM instance = new OpenAM();

	public OpenAM() {
	}

	public static OpenAM getInstance() {
		return instance;
	}

	void download() throws ClientProtocolException, IOException {
		System.out.println(Configuration.getInstance().getOpenamUrl());

		final var loginRequest = new HttpPost(Configuration.getInstance().getOpenamUrl() + "/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
		loginRequest.addHeader("X-OpenAM-Username", Configuration.getInstance().getOpenamUsername());
		loginRequest.addHeader("X-OpenAM-Password", Configuration.getInstance().getOpenamPassword());
		getHttpclient().execute(loginRequest);
		final var mapper = new ObjectMapper();
		
		final var fetchSaml2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/saml2?_queryFilter=true");
		final var jsonSaml2Entities = mapper.readTree(getHttpclient().execute(fetchSaml2EntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonSaml2Entities.json").toFile(), jsonSaml2Entities);

		final var fetchWsEntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/ws?_queryFilter=true");
		final var jsonWsEntities = mapper.readTree(getHttpclient().execute(fetchWsEntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonWsEntities.json").toFile(), jsonWsEntities);

		final var fetchOAuth2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/agents/OAuth2Client?_queryFilter=true");
		final var jsonOAuth2Entities = mapper.readTree(getHttpclient().execute(fetchOAuth2EntitiesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonOAuth2Entities.json").toFile(), jsonOAuth2Entities);

		final var fetchPoliciesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/policies?_queryFilter=true");
		final var jsonPolicies = mapper.readTree(getHttpclient().execute(fetchPoliciesRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonPolicies.json").toFile(), jsonPolicies);

		final var fetchCircleOfTrustRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/circlesoftrust?_queryFilter=true");
		final var jsonCircleOfTrust = mapper.readTree(getHttpclient().execute(fetchCircleOfTrustRequest).getEntity().getContent());
		mapper.writeValue(Paths.get("jsonCircleOfTrust.json").toFile(), jsonCircleOfTrust);
	}

	void process() throws ClientProtocolException, IOException {
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
	}

	public Set<CircleOfTrust> getCircleOfTrusts() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof CircleOfTrust).map(e -> (CircleOfTrust) e).collect(Collectors.toSet());
	}

	public Set<CircleOfTrust> getCircleOfTrusts2025() {
		return getCircleOfTrusts().stream().filter(e -> OpenAM.patternCirceOfTrust2025.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Entity> getCircleOfTrusts2031() {
		return getCircleOfTrusts().stream().filter(e -> OpenAM.patternCirceOfTrust2031.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getExternalMFAPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternExternalMFAPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalCERTPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalCERTPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalMFAPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalMFAPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalOnlyPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalOnlyPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getPolicies() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Policy).map(e -> (Policy) e).collect(Collectors.toSet());
	}

	public Set<EntityID> getResourcesForExternalMFAPolices() {
		final var reources = new HashSet<EntityID>();
		getExternalMFAPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalCERTPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalCERTPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalMFAPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalMFAPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalOnlyPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalOnlyPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<Saml2> getSaml2Entities() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Saml2).map(e -> (Saml2) e).collect(Collectors.toSet());
	}

	public Set<OAuth2Client> getOAuth2Clients() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof OAuth2Client).map(e -> (OAuth2Client) e).collect(Collectors.toSet());
	}

	public Set<Saml2> getSaml2IdPEntities() {
		return getSaml2Entities().stream().filter(e -> e.hasAttribute(Entity.IDENTITY_PROVIDER)).collect(Collectors.toSet());
	}

	public Set<Saml2> getSaml2SpEntities() {
		return getSaml2Entities().stream().filter(e -> e.hasAttribute(Entity.SERVICE_PROVIDER)).collect(Collectors.toSet());
	}

	public Set<Wsfed> getWsfedEntities() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Wsfed).map(e -> (Wsfed) e).collect(Collectors.toSet());
	}

	public Set<Wsfed> getWsfedIdPEntities() {
		return getWsfedEntities().stream().filter(e -> e.hasAttribute(Entity.IDENTITY_PROVIDER)).collect(Collectors.toSet());
	}

	public Set<Wsfed> getWsfedSpEntities() {
		return getWsfedEntities().stream().filter(e -> e.hasAttribute(Entity.SERVICE_PROVIDER)).collect(Collectors.toSet());
	}

}
