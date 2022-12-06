package com.openam.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Kontroller {
	@GetMapping("/openam/download")
	void download() throws ClientProtocolException, IOException  {
		OpenAM.getInstance().download();
	}

	@GetMapping("/openam")
	Set<Object> openam() throws ClientProtocolException, IOException {
		OpenAM.getInstance().process();
		return Entity.getAllEntities().values().stream().map(v -> {
			var copy = new HashMap(v.getAttributes());
			copy.put("id", v.getID());
			copy.put("type", v.getEntityType());
			return copy;
		}).collect(Collectors.toSet());
	}
}
