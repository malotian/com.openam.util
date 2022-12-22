package com.openam.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class Policy extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(Policy.class);
	public static Pattern patternExternalMFAPolicies = Pattern.compile("External User MFA|SAML/WS-Fed/OAuth External MFA|ExternalUsersTrustedDevice");
	public static Pattern patternInternalCERTPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal CERT");
	public static Pattern patternInternalMFAPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal MFA|InternalUsersTrustedDevice");
	public static Pattern patternInternalOnlyPolicies = Pattern.compile("Internal_Only_Restriction|Internal_only");

	public static void _process(final JsonNode json) {

		final var policyname = json.get("_id").asText();

		if (!Policy.patternInternalOnlyPolicies.matcher(policyname).find() && !Policy.patternInternalMFAPolicies.matcher(policyname).find()
				&& !Policy.patternExternalMFAPolicies.matcher(policyname).find() && !Policy.patternInternalCERTPolicies.matcher(policyname).find())
			return;
		final var policy = new Policy(policyname);
		if (!json.has("resources")) {
			Policy.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
			return;
		}
		Policy.logger.warn("processing, policy: {} ", json.get("_id").asText());
		json.get("resources").forEach(resource -> {
			policy.getResources().add(EntityID.ParseResourceEntry(resource.asText()));
		});

	}

	public static void process(final JsonNode policies) {
		final var result = policies.get("result");
		result.forEach(p -> {
			Policy._process(p);
		});

	}

	private Set<EntityID> resources = new HashSet<>();

	protected Policy(final String id) {
		super(id, EntityType.POLICY);
	}

	Set<EntityID> getResources() {
		return resources;
	}

	void setResources(final Set<EntityID> resources) {
		this.resources = resources;
	}
}
