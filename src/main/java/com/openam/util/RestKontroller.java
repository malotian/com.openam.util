package com.openam.util;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableConfigurationProperties(value = Konfiguration.class)
public class RestKontroller {

	public static Pattern patternPROD = Pattern.compile("prd|prod|production");

	@Autowired
	OpenAM oam;

	@Autowired
	protected RestHelper helper;

	@GetMapping("/rest/local/json")
	public ResponseEntity<?> fetchFromLocal(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {
		oam.processEntities();
		final var result = helper.getEntitiesTable();
		result.addAll(helper.getStatsTable());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	@GetMapping("/rest/openam/json")
	public ResponseEntity<?> fetchFromOpenAM(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {
		oam.loginAndFetchEntities();
		oam.processEntities();
		final var result = helper.getEntitiesTable();
		result.addAll(helper.getStatsTable());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	@GetMapping("/openam/test")
	public ResponseEntity<String> test() {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(oam.getKonfiguration().getUsername());

	}
}