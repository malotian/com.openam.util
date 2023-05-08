package com.openam.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import com.jcabi.aspects.Loggable;

@Component
public class RestKlient {

	static final int MAX_RETRIES = 5;

	static final int RETRY_INTERVAL = 5000;

	static List<Integer> RETRYABLE_500_HTTP_STATUS_CODES = Arrays.asList(HttpStatus.SC_SERVICE_UNAVAILABLE, HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpStatus.SC_BAD_GATEWAY,
			HttpStatus.SC_GATEWAY_TIMEOUT);

	static final int TIMEOUT_MILLIS = 60000;

	static final Logger logger = LoggerFactory.getLogger(RestKlient.class);

	static CloseableHttpClient httpClientWithServiceUnavailableRetryStrategy() {
		final var httpClientBuilder = HttpClients.custom().setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
			@Override
			public long getRetryInterval() {
				return RestKlient.RETRY_INTERVAL;
			}

			@Override
			public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
				final var statusCode = response.getStatusLine().getStatusCode();
				final var currentReq = (HttpUriRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
				OpenAM.logger.info("Response code {} for {}", statusCode, currentReq.getURI());

				if (statusCode >= HttpStatus.SC_OK && statusCode <= 299) {
					return false;
				}

				final var shouldRetry = (statusCode == 429 || RestKlient.RETRYABLE_500_HTTP_STATUS_CODES.contains(statusCode)) && executionCount <= RestKlient.MAX_RETRIES;
				if (!shouldRetry) {
					throw new RuntimeException(String.format("Not retrying %s. Count %d, Max %d", currentReq.getURI(), executionCount, RestKlient.MAX_RETRIES));
				}

				OpenAM.logger.error("Retrying request on response status {}. Count {} Max is {}", statusCode, executionCount, RestKlient.MAX_RETRIES);
				return true;
			}
		});

		httpClientBuilder.setRetryHandler((exception, executionCount, context) -> {
			final var currentReq = (HttpUriRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
			Uninterruptibles.sleepUninterruptibly(RestKlient.RETRY_INTERVAL, TimeUnit.SECONDS);
			OpenAM.logger.info("Encountered network error. Retrying request {},  Count {} Max is {}", currentReq.getURI(), executionCount, RestKlient.MAX_RETRIES);
			return executionCount <= RestKlient.MAX_RETRIES;
		});

		httpClientBuilder.setDefaultRequestConfig(
				RequestConfig.custom().setConnectionRequestTimeout(RestKlient.TIMEOUT_MILLIS).setConnectTimeout(RestKlient.TIMEOUT_MILLIS).setSocketTimeout(RestKlient.TIMEOUT_MILLIS).build());

		return httpClientBuilder.build();
	}

	@Autowired
	private Konfiguration konfiguration;

	@Autowired
	private Kontext kontext;
	CloseableHttpClient httpClient = RestKlient.httpClientWithServiceUnavailableRetryStrategy();
	private final ObjectMapper mapper = new ObjectMapper();

	@Loggable
	public boolean fetch(final String uri, final File saveReponseToFile) throws ClientProtocolException, IOException {
		final var fetch = new HttpGet(MessageFormat.format("{0}{1}", getKonfiguration().getUrl(), uri));
		logger.info("HttpGet: {}", fetch.getURI());
		final var fetchResponse = httpClient.execute(fetch);
		final var jsonResponse = getMapper().readTree(fetchResponse.getEntity().getContent());
		getMapper().writeValue(saveReponseToFile, jsonResponse);
		RestKlient.logger.info("fetchResponse: {}", fetchResponse.getStatusLine().getStatusCode());
		return fetchResponse.getStatusLine().getStatusCode() == 200;
	}

	public Konfiguration getKonfiguration() {
		return konfiguration;
	}

	public Kontext getKontext() {
		return kontext;
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	@Loggable
	public boolean login(final String loginUri) throws IOException, ClientProtocolException {
		RestKlient.logger.info("X-OpenAM-Username: {}", getKonfiguration().getUsername());
		RestKlient.logger.info("X-OpenAM-Password: {}", getKonfiguration().getPassword().replaceAll(".", "*"));
		final var loginRequest = new HttpPost(MessageFormat.format("{0}{1}", getKonfiguration().getUrl(), loginUri));
		logger.info("HttpPost: {}", loginRequest.getURI());
		loginRequest.addHeader("X-OpenAM-Username", getKonfiguration().getUsername());
		loginRequest.addHeader("X-OpenAM-Password", getKonfiguration().getPassword());
		final var loginResponse = httpClient.execute(loginRequest);
		RestKlient.logger.info("loginResponse: {}", loginResponse.getStatusLine().getStatusCode());
		return loginResponse.getStatusLine().getStatusCode() == 200;
	}

	public void setKonfiguration(final Konfiguration konfiguration) {
		this.konfiguration = konfiguration;
	}

	public void setKontext(final Kontext kontext) {
		this.kontext = kontext;
	}

}
