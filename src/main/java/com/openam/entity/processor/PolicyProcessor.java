package com.openam.entity.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.entity.EntityHelper;
import com.openam.entity.EntityID;
import com.openam.entity.Policy;

@Component
public class PolicyProcessor {

	static final Logger logger = LoggerFactory.getLogger(PolicyProcessor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) {

		final var policyname = json.get("_id").asText();

		if (!Policy.patternInternalOnlyPolicies.matcher(policyname).find() && !Policy.patternInternalMFAPolicies.matcher(policyname).find()
				&& !Policy.patternExternalMFAPolicies.matcher(policyname).find() && !Policy.patternInternalCERTPolicies.matcher(policyname).find()) {
			return;
		}
		final var policy = new Policy(policyname);
		if (!json.has("resources")) {
			PolicyProcessor.logger.warn("skipping, resources missing for policy: {} ", json.get("_id").asText());
			return;
		}

		json.get("resources").forEach(resource -> {
			policy.getResources().add(EntityID.ParseResourceEntry(resource.asText()));
		});

	}

	public void process(final JsonNode policies) {
		final var result = policies.get("result");
		result.forEach(p -> {
			_process(p);
		});

	}
}