package com.openam.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

public class Policy extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(Policy.class);
	public static Pattern patternExternalMFAPolicies = Pattern.compile("External User MFA|SAML/WS-Fed/OAuth External MFA|ExternalUsersTrustedDevice");
	public static Pattern patternInternalCERTPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal CERT");
	public static Pattern patternInternalMFAPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal MFA|InternalUsersTrustedDevice");
	public static Pattern patternInternalOnlyPolicies = Pattern.compile("Internal_Only_Restriction|Internal_only");

	private Set<EntityID> resources = new HashSet<>();

	public Policy(final String id) {
		super(id, EntityType.POLICY);
	}

	public Set<EntityID> getResources() {
		return resources;
	}

	public void setResources(final Set<EntityID> resources) {
		this.resources = resources;
	}

}
