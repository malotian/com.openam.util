package com.openam.util.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.EntityType;
import com.openam.util.entity.Policy;

@Component
public class RestHelper {

	@Autowired
	EntityHelper entityHelper;

	public Stream<Entity> getAppEntititesOnly() {
		return Entity.getAllEntities().values().stream()
				.filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType()) || EntityType.OAUTH2.equals(v.getEntityType()))
						&& (!v.hasAttribute(Entity.HOSTED_REMOTE) || v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE))
						&& (!v.hasAttribute(Entity.SP_IDP) || v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))));
	}

	public Set<Map<String, String>> getEntitiesTable() {
		return Entity.getAllEntities().values().stream().filter(v -> !EntityType.POLICY.equals(v.getEntityType())).map(v -> {
			final var copy = new HashMap<>(v.getAttributes());
			copy.put("ID", v.getID());
			copy.put("TYPE", v.getEntityType().toString());
			return copy;
		}).collect(Collectors.toSet());
	}

//	public Set<Map<String, String>> getEntitiesTable2() {
//		return getAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))).map(v -> {
//			final var copy = new HashMap<String, String>();
//			copy.put("ID", v.getID());
//			copy.put("TYPE", v.getEntityType().toString());
//			copy.put(Entity.HOSTED_REMOTE, v.getAttribute(Entity.HOSTED_REMOTE));
//			copy.put(Entity.SP_IDP, v.getAttribute(Entity.SP_IDP));
//			return copy;
//		}).collect(Collectors.toSet());
//	}

	public Set<Map<String, String>> getStatsTable() {
		return stats().entrySet().stream().map(e -> {
			final var copy = new HashMap<String, String>();
			copy.put("ID", e.getKey() + ": " + e.getValue().toString());
			copy.put("TYPE", "STAT");
			return copy;
		}).collect(Collectors.toSet());
	}

	public HashMap<String, Long> stats() {
		final var result = new HashMap<String, Long>();
		// remove all apps that doesn't have IDP
		// remove hosted
		// https://www.netsparkercloud.com is neither remote nor hosted
		// remove duplicates

		result.put("COUNT_OAUTH", getAppEntititesOnly().filter(v -> EntityType.OAUTH2.equals(v.getEntityType())).count());
		result.put("COUNT_SAML", getAppEntititesOnly().filter(v -> EntityType.SAML2.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE)).count());
		result.put("COUNT_WSFED", getAppEntititesOnly().filter(v -> EntityType.WSFED.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE)).count());

		final var patternCOT2031 = Pattern.compile("31|2031");

		result.put("COUNT_2025", getAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_2025_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2025", getAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_SAML2_2025_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2025", getAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_WSFED_2025_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && !patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		/////////////////////////////////////////////////////////// 2031

		result.put("COUNT_2031", getAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_2031_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2031", getAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_SAML2_2031_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2031", getAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find())).count());

		result.put("COUNT_WSFED_2031_INTERNAL_ONLY", getAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.hasAttribute(Entity.COT) && patternCOT2031.matcher(v.getAttribute(Entity.COT)).find() //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_CLIENTFED_ACTIVE", entityHelper.getPolicies().stream().filter(p -> (Policy.patternClientFedPolicies.matcher(p.getID()).find() && p.hasAttribute(Entity.ACTIVE) //
				&& "true".equalsIgnoreCase(p.getAttribute(Entity.ACTIVE)))).count());

		result.put("COUNT_CLIENTFED_NOT_ACTIVE", entityHelper.getPolicies().stream().filter(p -> (Policy.patternClientFedPolicies.matcher(p.getID()).find() && p.hasAttribute(Entity.ACTIVE) //
				&& "false".equalsIgnoreCase(p.getAttribute(Entity.ACTIVE)))).count());

		return result;
	}

}
