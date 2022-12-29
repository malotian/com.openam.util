package com.openam.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestKontroller {

	@GetMapping("/openam/json")
	public ResponseEntity<?> downloadJson() throws ClientProtocolException, IOException {
		// OpenAM.getInstance().download();
		OpenAM.getInstance().process();
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(entitiesTable());
	}

	@GetMapping("/openam/download")
	public ResponseEntity<?> downloadJsonFile() throws ClientProtocolException, IOException {
		OpenAM.getInstance().download();
		OpenAM.getInstance().process();
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=openam.json").contentType(MediaType.APPLICATION_JSON).body(entitiesTable());
	}

	private Set<?> entitiesTable() {
		return Entity.getAllEntities().values().stream().map(v -> {
			final var copy = new HashMap<>(v.getAttributes());
			copy.put("ID", v.getID());
			copy.put("TYPE", v.getEntityType().toString());
			return copy;
		}).collect(Collectors.toSet());
	}
}
