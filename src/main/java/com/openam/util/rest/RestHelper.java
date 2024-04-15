package com.openam.util.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.EntityType;
import com.openam.util.entity.Policy;

@Component
public class RestHelper {

	private static final Logger logger = LoggerFactory.getLogger(RestHelper.class);

	@Autowired
	EntityHelper entityHelper;

	@Autowired
	StatsCalculator statsCalculator;

	public Set<Map<String, String>> getEntitiesTable() {
		return Entity.getAllEntities().values().stream().filter(v -> !EntityType.POLICY.equals(v.getEntityType())).map(v -> {
			final var copy = new HashMap<>(v.getAttributes());
			copy.put("ID", v.getID());
			copy.put("TYPE", v.getEntityType().toString());
			return copy;
		}).collect(Collectors.toSet());
	}

	public Set<Map<String, String>> getStatsTable() {
		return statsCalculator.stats().entrySet().stream().map(e -> {
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
		final Set<String> setOf2025CertAlias = Set.of("PwCIdentitySigning_Stg_exp_2035", "pwcidentitysigning_stg", "PwCIdentitySigning_Prod_exp_2035", "pwcidentitysigning_prd");
		final Set<String> setOf2031CertAlias = Set.of("pwcidentitysigning_stg_exp_2031", "pwcidentitysigning_prd_exp_2031");

		result.put("COUNT_OAUTH", entityHelper.getStreamOfAppEntititesOnly().filter(v -> EntityType.OAUTH2.equals(v.getEntityType())).count());
		result.put("COUNT_SAML", entityHelper.getStreamOfAppEntititesOnly().filter(v -> EntityType.SAML2.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE)).count());
		result.put("COUNT_WSFED", entityHelper.getStreamOfAppEntititesOnly().filter(v -> EntityType.WSFED.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE)).count());

		result.put("COUNT_2025", entityHelper.getStreamOfAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias))).count());

		result.put("COUNT_2025_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2025", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias))).count());

		result.put("COUNT_SAML2_2025_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2025", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias))).count());

		result.put("COUNT_WSFED_2025_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2025CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		/////////////////////////////////////////////////////////// 2031

		result.put("COUNT_2031", entityHelper.getStreamOfAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias))).count());

		result.put("COUNT_2031_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_SAML2_2031", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias))).count());

		result.put("COUNT_SAML2_2031_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_WSFED_2031", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias))).count());

		result.put("COUNT_WSFED_2031_INTERNAL_ONLY", entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.WSFED.equals(v.getEntityType()) //
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) //
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, setOf2031CertAlias) //
				&& v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)))).count());

		result.put("COUNT_CLIENTFED_ACTIVE", entityHelper.getPolicies().stream().filter(p -> (Policy.patternClientFedPolicies.matcher(p.getID()).find() && p.hasAttribute(Entity.ACTIVE) //
				&& "true".equalsIgnoreCase(p.getAttribute(Entity.ACTIVE)))).count());

		result.put("COUNT_CLIENTFED_NOT_ACTIVE", entityHelper.getPolicies().stream().filter(p -> (Policy.patternClientFedPolicies.matcher(p.getID()).find() && p.hasAttribute(Entity.ACTIVE) //
				&& "false".equalsIgnoreCase(p.getAttribute(Entity.ACTIVE)))).count());

		return result;
	}

}
