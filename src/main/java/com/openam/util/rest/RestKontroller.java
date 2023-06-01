package com.openam.util.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openam.util.Konfiguration;
import com.openam.util.Kontext;
import com.openam.util.OpenAM;

@RestController
@EnableConfigurationProperties(value = Konfiguration.class)
public class RestKontroller {

	static final Logger logger = LoggerFactory.getLogger(RestKontroller.class);

	public static Pattern patternPROD = Pattern.compile("prd|prod|production");

	private static ObjectMapper mapper = new ObjectMapper();

	@Autowired
	OpenAM oam;

	@Autowired
	private Kontext kontext;

	@Autowired
	private Konfiguration konfiguration;

	@Autowired
	protected RestHelper helper;

	@GetMapping("/rest/local/json")
	public ResponseEntity<?> fetchFromLocal(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {

		if (!kontext.file("latest.json").exists()) {
			oam.processEntities();
			final var result = helper.getEntitiesTable();
			result.addAll(helper.getStatsTable());
			RestKontroller.mapper.writeValue(kontext.file("latest.json"), result);
		}

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new InputStreamResource(new FileInputStream(kontext.file("latest.json"))));
	}

	@GetMapping("/rest/openam/json")
	public ResponseEntity<?> fetchFromOpenAM(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {
		oam.loginAndFetchEntities();
		oam.processEntities();
		final var result = helper.getEntitiesTable();
		result.addAll(helper.getStatsTable());
		RestKontroller.mapper.writeValue(kontext.file("latest.json"), result);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	@GetMapping("/rest/table/column/visible")
	public ResponseEntity<?> fetchTableColumnsVisibility() {
		RestKontroller.logger.debug(konfiguration.getTableColumns());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(List.of(StringUtils.split(konfiguration.getTableColumns(), ",")));
	}

	@GetMapping("/shutdown")
	public String shutdown(HttpServletRequest request) {
		String requestURL = request.getRequestURL().substring(0, request.getRequestURL().indexOf(request.getRequestURI()));
		String uri = requestURL + "/actuator/shutdown";

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = new HttpEntity<>(null, headers);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.postForLocation(uri, entity);
		return "OK";
	}

//	@GetMapping("/rest/test")
//	public ResponseEntity<?> test() throws ClientProtocolException, IOException {
//		oam.processEntities();
//		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(helper.getEntitiesTable2());
//
//	}
}