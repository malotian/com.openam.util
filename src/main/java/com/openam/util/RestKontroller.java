package com.openam.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.ClientProtocolException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestKontroller {

	private Stream<Entity> apps() {
		return Entity.getAllEntities().values().stream()
				.filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType()) || EntityType.OAUTH2.equals(v.getEntityType()))
						&& (!v.hasAttribute(Entity.HOSTED_REMOTE) || v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE))
						&& (!v.hasAttribute(Entity.SP_IDP) || v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))));
	}

	@GetMapping("/openam/json")
	public ResponseEntity<?> downloadJson(@RequestParam(name = "env") final String env) throws ClientProtocolException, IOException {
		Configuration.getInstance().useEnvironment(env);

		OpenAM.getInstance().download();
		OpenAM.getInstance().process();
		final var result = entitiesTable();
		result.addAll(statsTable());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	private Set<Map<String, String>> entitiesTable() {
		return Entity.getAllEntities().values().stream().map(v -> {
			final var copy = new HashMap<>(v.getAttributes());
			copy.put("ID", v.getID());
			copy.put("TYPE", v.getEntityType().toString());
			return copy;
		}).collect(Collectors.toSet());
	}

	private HashMap<String, Long> stats() {
		final var result = new HashMap<String, Long>();
		// remove all apps that doesn't have IDP
		// remove hosted
		// https://www.netsparkercloud.com is neither remote nor hosted
		// remove duplicates

		result.put("COUNT_OAUTH", apps().filter(v -> EntityType.OAUTH2.equals(v.getEntityType())).count());
		result.put("COUNT_SAML", apps().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))).count());
		result.put("COUNT_WSFED", apps().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))).count());

		final var patternCOT2031 = Pattern.compile("31|2031");

		result.put("COUNT_2025", apps().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_2025_INTERNAL_ONLY", apps().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2025", apps().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_SAML2_2025_INTERNAL_ONLY", apps().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2025", apps().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_WSFED_2025_INTERNAL_ONLY", apps().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		/////////////////////////////////////////////////////////// 2031

		result.put("COUNT_2031", apps().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_2031_INTERNAL_ONLY", apps().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2031", apps().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_SAML2_2031_INTERNAL_ONLY", apps().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2031", apps().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_WSFED_2031_INTERNAL_ONLY", apps().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());
		return result;
	}

	private Set<Map<String, String>> statsTable() {
		return stats().entrySet().stream().map(e -> {
			final var copy = new HashMap<String, String>();
			copy.put("ID", e.getKey() + ": " + e.getValue().toString());
			copy.put("TYPE", "STAT");
			return copy;
		}).collect(Collectors.toSet());
	}
}