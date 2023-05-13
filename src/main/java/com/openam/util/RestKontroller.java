package com.openam.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@EnableConfigurationProperties(value = Konfiguration.class)
public class RestKontroller {

	public static Pattern patternPROD = Pattern.compile("prd|prod|production");

	private static ObjectMapper mapper = new ObjectMapper();

	@Autowired
	OpenAM oam;

	@Autowired
	private Kontext kontext;

	@Autowired
	protected RestHelper helper;

	@GetMapping("/rest/local/json")
	public ResponseEntity<?> fetchFromLocal(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {
		if (!kontext.file("latest.json").exists())
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("[]");

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

	@GetMapping("/openam/test")
	public ResponseEntity<String> test() {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(oam.getKonfiguration().getUsername());

	}
}