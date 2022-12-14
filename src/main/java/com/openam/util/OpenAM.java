package com.openam.util;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAM {

	private static OpenAM instance = new OpenAM();
	private static final Logger logger = LoggerFactory.getLogger(OpenAM.class);

	public static Pattern patternCirceOfTrust2025 = Pattern.compile("25|2025");
	public static Pattern patternCirceOfTrust2031 = Pattern.compile("31|2031");

	public static OpenAM getInstance() {
		return OpenAM.instance;
	}

	private HttpClient httpClient = HttpClientBuilder.create().build();

	private final ObjectMapper mapper = new ObjectMapper();

	public OpenAM() {
	}

	void download() throws ClientProtocolException, IOException {

		initializetHttpclient();
		System.out.println(Configuration.getInstance().getOpenamUrl());

		final var loginRequest = new HttpPost(Configuration.getInstance().getOpenamUrl() + "/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
		loginRequest.addHeader("X-OpenAM-Username", Configuration.getInstance().getOpenamUsername());
		loginRequest.addHeader("X-OpenAM-Password", Configuration.getInstance().getOpenamPassword());
		getHttpclient().execute(loginRequest);
		final var mapper = new ObjectMapper();

		var retry = false;

		do {
			final var fetchSaml2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/saml2?_queryFilter=true");
			final var fetchSaml2EntitiesResponse = getHttpclient().execute(fetchSaml2EntitiesRequest);
			final var jsonSaml2Entities = mapper.readTree(fetchSaml2EntitiesResponse.getEntity().getContent());
			mapper.writeValue(Paths.get("jsonSaml2Entities.json").toFile(), jsonSaml2Entities);
			OpenAM.logger.debug("fetchSaml2EntitiesResponse: {}", fetchSaml2EntitiesResponse.getStatusLine().getStatusCode());
			retry = fetchSaml2EntitiesResponse.getStatusLine().getStatusCode() != 200;
		} while (retry);

		do {
			final var fetchWsEntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/ws?_queryFilter=true");
			final var fetchWsEntitiesResponse = getHttpclient().execute(fetchWsEntitiesRequest);
			final var jsonWsEntities = mapper.readTree(fetchWsEntitiesResponse.getEntity().getContent());
			mapper.writeValue(Paths.get("jsonWsEntities.json").toFile(), jsonWsEntities);
			OpenAM.logger.debug("fetchWsEntitiesResponse: {}", fetchWsEntitiesResponse.getStatusLine().getStatusCode());
			retry = fetchWsEntitiesResponse.getStatusLine().getStatusCode() != 200;
		} while (retry);

		do {
			final var fetchOAuth2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/agents/OAuth2Client?_queryFilter=true");
			final var fetchOAuth2EntitiesResponse = getHttpclient().execute(fetchOAuth2EntitiesRequest);
			final var jsonOAuth2Entities = mapper.readTree(fetchOAuth2EntitiesResponse.getEntity().getContent());
			mapper.writeValue(Paths.get("jsonOAuth2Entities.json").toFile(), jsonOAuth2Entities);
			OpenAM.logger.debug("fetchOAuth2EntitiesResponse: {}", fetchOAuth2EntitiesResponse.getStatusLine().getStatusCode());
			retry = fetchOAuth2EntitiesResponse.getStatusLine().getStatusCode() != 200;
		} while (retry);

		do {
			final var fetchPoliciesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/policies?_queryFilter=true");
			final var fetchPoliciesResponse = getHttpclient().execute(fetchPoliciesRequest);
			final var jsonPolicies = mapper.readTree(fetchPoliciesResponse.getEntity().getContent());
			mapper.writeValue(Paths.get("jsonPolicies.json").toFile(), jsonPolicies);
			OpenAM.logger.debug("fetchPoliciesResponse: {}", fetchPoliciesResponse.getStatusLine().getStatusCode());
			retry = fetchPoliciesResponse.getStatusLine().getStatusCode() != 200;
		} while (retry);

		do {
			final var fetchCircleOfTrustRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/circlesoftrust?_queryFilter=true");
			final var fetchCircleOfTrustResponse = getHttpclient().execute(fetchCircleOfTrustRequest);
			final var jsonCircleOfTrust = mapper.readTree(fetchCircleOfTrustResponse.getEntity().getContent());
			mapper.writeValue(Paths.get("jsonCircleOfTrust.json").toFile(), jsonCircleOfTrust);
			OpenAM.logger.debug("fetchCircleOfTrustResponse: {}", fetchCircleOfTrustResponse.getStatusLine().getStatusCode());
			retry = fetchCircleOfTrustResponse.getStatusLine().getStatusCode() != 200;
		} while (retry);
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

	public Set<Policy> getExternalMFAPoliciesApplicable(final EntityID id) {
		return getExternalMFAPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	HttpClient getHttpclient() {
		return httpClient;
	}

	public Set<Policy> getInternalCERTPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalCERTPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalCERTPoliciesApplicable(final EntityID id) {
		return getInternalCERTPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalMFAPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalMFAPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalMFAPoliciesApplicable(final EntityID id) {
		return getInternalMFAPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalOnlyPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalOnlyPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalOnlyPoliciesApplicable(final EntityID id) {
		return getInternalOnlyPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	ObjectMapper getMapper() {
		return mapper;
	}

	public Set<OAuth2Client> getOAuth2Clients() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof OAuth2Client).map(e -> (OAuth2Client) e).collect(Collectors.toSet());
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

	HttpClient initializetHttpclient() {
		return httpClient = HttpClientBuilder.create().build();
	}

	void process() throws ClientProtocolException, IOException {

		final var jsonPolicies = mapper.readValue(Paths.get("jsonPolicies.json").toFile(), JsonNode.class);
		Policy.process(jsonPolicies);

		final var jsonSaml2Entities = mapper.readValue(Paths.get("jsonSaml2Entities.json").toFile(), JsonNode.class);
		Saml2.process(jsonSaml2Entities);

		final var jsonWsEntities = mapper.readValue(Paths.get("jsonWsEntities.json").toFile(), JsonNode.class);
		Wsfed.process(jsonWsEntities);

		final var jsonOAuth2Entities = mapper.readValue(Paths.get("jsonOAuth2Entities.json").toFile(), JsonNode.class);
		OAuth2Client.process(jsonOAuth2Entities);

		final var jsonCircleOfTrust = mapper.readValue(Paths.get("jsonCircleOfTrust.json").toFile(), JsonNode.class);
		CircleOfTrust.process(jsonCircleOfTrust);
	}

	public void updateAuthAsPerPolicies(final Entity entity) {
		if (OpenAM.getInstance().getResourcesForInternalMFAPolicies().contains(entity)) {
			final var policies = OpenAM.getInstance().getInternalMFAPoliciesApplicable(entity);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_MFA);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH),
					Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
			OpenAM.logger.debug(remarks);

		} else if (OpenAM.getInstance().getResourcesForInternalCERTPolicies().contains(entity)) {
			final var policies = OpenAM.getInstance().getInternalCERTPoliciesApplicable(entity);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_CERT);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH),
					Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
			OpenAM.logger.debug(remarks);
		} else {
			entity.addAttribute(Entity.INTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.INTERNAL_AUTH));
			entity.addRemarks(remarks);
			OpenAM.logger.debug(remarks);
		}

		if (OpenAM.getInstance().getResourcesForExternalMFAPolices().contains(entity)) {
			final var policies = OpenAM.getInstance().getExternalMFAPoliciesApplicable(entity);
			entity.addAttribute(Entity.EXTERNAL_AUTH, "MFA");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.EXTERNAL_AUTH),
					Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
			OpenAM.logger.debug(remarks);
		} else {
			entity.addAttribute(Entity.EXTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.EXTERNAL_AUTH));
			entity.addRemarks(remarks);
			OpenAM.logger.debug(remarks);
		}
	}

}
