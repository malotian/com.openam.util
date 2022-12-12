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
public class Kontroller {

	public static String HOSTED_REMOTE = "HOSTED/REMOTE";
	public static String SP_IDP = "SP/IDP";

	@GetMapping("/openam/download")
	public ResponseEntity<Set<HashMap>> downloadJsonFile() throws ClientProtocolException, IOException {
		OpenAM.getInstance().download();
		OpenAM.getInstance().process();
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=openam.json").contentType(MediaType.APPLICATION_JSON)
				.body(Entity.getAllEntities().values().stream().map(v -> {
					var copy = new HashMap(v.getAttributes());
					copy.put("id", v.getID());
					copy.put("type", v.getEntityType());

					if (copy.containsKey(Entity.HOSTED)) {
						copy.put(HOSTED_REMOTE, Entity.HOSTED);
					} else if (copy.containsKey(Entity.REMOTE)) {
						copy.put(HOSTED_REMOTE, Entity.REMOTE);
					} else {
						copy.put(HOSTED_REMOTE, "N/A");
					}
					copy.remove(Entity.HOSTED);
					copy.remove(Entity.REMOTE);

					if (copy.containsKey(Entity.SERVICE_PROVIDER)) {
						copy.put(SP_IDP, Entity.SERVICE_PROVIDER);
					} else if (copy.containsKey(Entity.IDENTITY_PROVIDER)) {
						copy.put(SP_IDP, Entity.IDENTITY_PROVIDER);
					} else {
						copy.put(SP_IDP, "N/A");
					}
					copy.remove(Entity.SERVICE_PROVIDER);
					copy.remove(Entity.IDENTITY_PROVIDER);

					return copy;
				}).collect(Collectors.toSet()));
	}
}
