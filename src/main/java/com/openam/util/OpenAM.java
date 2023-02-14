package com.openam.util;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;

@Component
public class OpenAM {

	private static HttpClient httpClient = OpenAM.httpClientWithServiceUnavailableRetryStrategy();
	private static OpenAM instance = new OpenAM();

	private static final Logger logger = LoggerFactory.getLogger(OpenAM.class);
	private static final int MAX_RETRIES = 5;

	public static Pattern patternCirceOfTrust2025 = Pattern.compile("25|2025");
	public static Pattern patternCirceOfTrust2031 = Pattern.compile("31|2031");
	private static final int RETRY_INTERVAL = 5000;
	private static List<Integer> RETRYABLE_500_HTTP_STATUS_CODES = Arrays.asList(HttpStatus.SC_SERVICE_UNAVAILABLE, HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpStatus.SC_BAD_GATEWAY,
			HttpStatus.SC_GATEWAY_TIMEOUT);

	private static final int TIMEOUT_MILLIS = 30000;

	public static OpenAM getInstance() {
		return OpenAM.instance;
	}

	private static CloseableHttpClient httpClientWithServiceUnavailableRetryStrategy() {
		final var httpClientBuilder = HttpClients.custom().setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
			@Override
			public long getRetryInterval() {
				return OpenAM.RETRY_INTERVAL;
			}

			@Override
			public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
				final var statusCode = response.getStatusLine().getStatusCode();
				final var currentReq = (HttpUriRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
				OpenAM.logger.info("Response code {} for {}", statusCode, currentReq.getURI());

				if (statusCode >= HttpStatus.SC_OK && statusCode <= 299) {
					return false;
				}

				final var shouldRetry = (statusCode == 429 || OpenAM.RETRYABLE_500_HTTP_STATUS_CODES.contains(statusCode)) && executionCount <= OpenAM.MAX_RETRIES;
				if (!shouldRetry) {
					throw new RuntimeException(String.format("Not retrying %s. Count %d, Max %d", currentReq.getURI(), executionCount, OpenAM.MAX_RETRIES));
				}

				OpenAM.logger.error("Retrying request on response status {}. Count {} Max is {}", statusCode, executionCount, OpenAM.MAX_RETRIES);
				return true;
			}
		});

		httpClientBuilder.setRetryHandler((exception, executionCount, context) -> {
			final var currentReq = (HttpUriRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
			Uninterruptibles.sleepUninterruptibly(OpenAM.RETRY_INTERVAL, TimeUnit.MILLISECONDS);
			OpenAM.logger.info("Encountered network error. Retrying request {},  Count {} Max is {}", currentReq.getURI(), executionCount, OpenAM.MAX_RETRIES);
			return executionCount <= OpenAM.MAX_RETRIES;
		});

		httpClientBuilder.setDefaultRequestConfig(
				RequestConfig.custom().setConnectionRequestTimeout(OpenAM.TIMEOUT_MILLIS).setConnectTimeout(OpenAM.TIMEOUT_MILLIS).setSocketTimeout(OpenAM.TIMEOUT_MILLIS).build());

		return httpClientBuilder.build();
	}

	private final ObjectMapper mapper = new ObjectMapper();

	public OpenAM() {
		System.setProperty("jdk.httpclient.keepalive.timeout", "99999");
	}

	void download() {
		var retry = false;
		do {
			try {
				retry = !login() || !fetch();
			} catch (final Exception e) {
				// retry = true;
			}
		} while (retry);
	}

	boolean fetch() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		return fetchSaml2() && fetchWs() && fetchOAuth2() && fetchPolicies() && fetchCircleOfTrust();
	}

	private boolean fetchCircleOfTrust() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		final var mapper = new ObjectMapper();
		final var fetchCircleOfTrustRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/circlesoftrust?_queryFilter=true");
		fetchCircleOfTrustRequest.addHeader("Accept-Encoding", "gzip, deflate, br");
		final var fetchCircleOfTrustResponse = OpenAM.httpClient.execute(fetchCircleOfTrustRequest);
		final var jsonCircleOfTrust = mapper.readTree(fetchCircleOfTrustResponse.getEntity().getContent());
		mapper.writeValue(Paths.get("jsonCircleOfTrust.json").toFile(), jsonCircleOfTrust);
		OpenAM.logger.debug("fetchCircleOfTrustResponse: {}", fetchCircleOfTrustResponse.getStatusLine().getStatusCode());
		return fetchCircleOfTrustResponse.getStatusLine().getStatusCode() == 200;
	}

	private boolean fetchOAuth2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		final var mapper = new ObjectMapper();
		final var fetchOAuth2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/agents/OAuth2Client?_queryFilter=true");
		final var fetchOAuth2EntitiesResponse = OpenAM.httpClient.execute(fetchOAuth2EntitiesRequest);
		final var jsonOAuth2Entities = mapper.readTree(fetchOAuth2EntitiesResponse.getEntity().getContent());
		mapper.writeValue(Paths.get("jsonOAuth2Entities.json").toFile(), jsonOAuth2Entities);
		OpenAM.logger.debug("fetchOAuth2EntitiesResponse: {}", fetchOAuth2EntitiesResponse.getStatusLine().getStatusCode());
		return fetchOAuth2EntitiesResponse.getStatusLine().getStatusCode() == 200;
	}

	private boolean fetchPolicies() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		final var mapper = new ObjectMapper();
		final var fetchPoliciesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/policies?_queryFilter=true");
		final var fetchPoliciesResponse = OpenAM.httpClient.execute(fetchPoliciesRequest);
		final var jsonPolicies = mapper.readTree(fetchPoliciesResponse.getEntity().getContent());
		mapper.writeValue(Paths.get("jsonPolicies.json").toFile(), jsonPolicies);
		OpenAM.logger.debug("fetchPoliciesResponse: {}", fetchPoliciesResponse.getStatusLine().getStatusCode());
		return fetchPoliciesResponse.getStatusLine().getStatusCode() == 200;
	}

	private boolean fetchSaml2() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {

		final var mapper = new ObjectMapper();
		final var fetchSaml2EntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/saml2?_queryFilter=true");
		final var fetchSaml2EntitiesResponse = OpenAM.httpClient.execute(fetchSaml2EntitiesRequest);
		final var jsonSaml2Entities = mapper.readTree(fetchSaml2EntitiesResponse.getEntity().getContent());
		mapper.writeValue(Paths.get("jsonSaml2Entities.json").toFile(), jsonSaml2Entities);
		OpenAM.logger.debug("fetchSaml2EntitiesResponse: {}", fetchSaml2EntitiesResponse.getStatusLine().getStatusCode());
		return fetchSaml2EntitiesResponse.getStatusLine().getStatusCode() == 200;
	}

	private boolean fetchWs() throws IOException, ClientProtocolException, StreamWriteException, DatabindException {
		final var mapper = new ObjectMapper();
		final var fetchWsEntitiesRequest = new HttpGet(Configuration.getInstance().getOpenamUrl() + "/json/realm-config/federation/entityproviders/ws?_queryFilter=true");
		final var fetchWsEntitiesResponse = OpenAM.httpClient.execute(fetchWsEntitiesRequest);
		final var jsonWsEntities = mapper.readTree(fetchWsEntitiesResponse.getEntity().getContent());
		mapper.writeValue(Paths.get("jsonWsEntities.json").toFile(), jsonWsEntities);
		OpenAM.logger.debug("fetchWsEntitiesResponse: {}", fetchWsEntitiesResponse.getStatusLine().getStatusCode());
		return fetchWsEntitiesResponse.getStatusLine().getStatusCode() == 200;
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

	private boolean login() throws IOException, ClientProtocolException {

		final var loginRequest = new HttpPost(Configuration.getInstance().getOpenamUrl() + "/json/realms/root/authenticate?authIndexType=service&authIndexValue=ldapService");
		loginRequest.addHeader("X-OpenAM-Username", Configuration.getInstance().getOpenamUsername());
		loginRequest.addHeader("X-OpenAM-Password", Configuration.getInstance().getOpenamPassword());
		final var loginResponse = OpenAM.httpClient.execute(loginRequest);
		OpenAM.logger.debug("loginResponse: {}", loginResponse.getStatusLine().getStatusCode());
		return loginResponse.getStatusLine().getStatusCode() == 200;
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
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (OpenAM.getInstance().getResourcesForInternalCERTPolicies().contains(entity)) {
			final var policies = OpenAM.getInstance().getInternalCERTPoliciesApplicable(entity);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_CERT);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else {
			entity.addAttribute(Entity.INTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.INTERNAL_AUTH));
			entity.addRemarks(remarks);
		}

		if (OpenAM.getInstance().getResourcesForExternalMFAPolices().contains(entity)) {
			final var policies = OpenAM.getInstance().getExternalMFAPoliciesApplicable(entity);
			entity.addAttribute(Entity.EXTERNAL_AUTH, "MFA");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.EXTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (OpenAM.getInstance().getResourcesForInternalOnlyPolicies().contains(entity)) {
			entity.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
		} else {
			entity.addAttribute(Entity.EXTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.EXTERNAL_AUTH));
			entity.addRemarks(remarks);
		}
	}

}
