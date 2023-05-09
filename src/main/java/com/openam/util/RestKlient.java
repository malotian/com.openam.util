package com.openam.util;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.aspects.Loggable;
import com.jcabi.aspects.RetryOnFailure;

@Component
public class RestKlient {
	static final int SOCKET_TIMEOUT_MILLIS = 60000;

	static final Logger logger = LoggerFactory.getLogger(RestKlient.class);

	static CloseableHttpClient httpClientWithServiceUnavailableRetryStrategy() {
		final var httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(RestKlient.SOCKET_TIMEOUT_MILLIS).build());

		return httpClientBuilder.build();
	}

	@Autowired
	private Konfiguration konfiguration;

	@Autowired
	private Kontext kontext;
	CloseableHttpClient httpClient = RestKlient.httpClientWithServiceUnavailableRetryStrategy();
	private final ObjectMapper mapper = new ObjectMapper();

	@Loggable
	@RetryOnFailure(attempts = 5, delay = 5000, verbose = false)
	public boolean fetch(final String uri, final File saveReponseToFile) throws ClientProtocolException, IOException, SocketTimeoutException {
		final var fetch = new HttpGet(MessageFormat.format("{0}{1}", getKonfiguration().getUrl(), uri));
		RestKlient.logger.info("HttpGet: {}", fetch.getURI());
		final var fetchResponse = httpClient.execute(fetch);
		final var jsonResponse = getMapper().readTree(fetchResponse.getEntity().getContent());
		getMapper().writeValue(saveReponseToFile, jsonResponse);
		RestKlient.logger.info("fetchResponse: {}", fetchResponse.getStatusLine().getStatusCode());

		final var statusLine = fetchResponse.getStatusLine();
		if (statusLine.getStatusCode() != 200) {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

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
	@RetryOnFailure(attempts = 2, delay = 5000, verbose = false)
	public boolean login(final String loginUri) throws IOException, ClientProtocolException {
		RestKlient.logger.info("X-OpenAM-Username: {}", getKonfiguration().getUsername());
		RestKlient.logger.info("X-OpenAM-Password: {}", getKonfiguration().getPassword().replaceAll(".", "*"));
		final var loginRequest = new HttpPost(MessageFormat.format("{0}{1}", getKonfiguration().getUrl(), loginUri));
		RestKlient.logger.info("HttpPost: {}", loginRequest.getURI());
		loginRequest.addHeader("X-OpenAM-Username", getKonfiguration().getUsername());
		loginRequest.addHeader("X-OpenAM-Password", getKonfiguration().getPassword());
		final var loginResponse = httpClient.execute(loginRequest);
		RestKlient.logger.info("loginResponse: {}", loginResponse.getStatusLine().getStatusCode());
		final var statusLine = loginResponse.getStatusLine();
		if (statusLine.getStatusCode() != 200) {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

		return loginResponse.getStatusLine().getStatusCode() == 200;
	}

	public void setKonfiguration(final Konfiguration konfiguration) {
		this.konfiguration = konfiguration;
	}

	public void setKontext(final Kontext kontext) {
		this.kontext = kontext;
	}

}
