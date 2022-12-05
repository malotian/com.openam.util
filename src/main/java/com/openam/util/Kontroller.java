package com.openam.util;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Kontroller {
	@GetMapping("/openam")
	Set<Object> openam() {
		return Entity.getAllEntities().values().stream().map(v -> {
			var copy = new HashMap(v.getAttributes());
			copy.put("id", v.getID());
			copy.put("type", v.getEntityType());
			return copy;
		}).collect(Collectors.toSet());
	}
}
