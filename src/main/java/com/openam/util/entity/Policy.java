package com.openam.util.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Policy extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(Policy.class);
	public static Pattern patternExternalMFAPolicies = Pattern.compile("External User MFA|SAML/WS-Fed/OAuth External MFA|ExternalUsersTrustedDevice", Pattern.CASE_INSENSITIVE);
	public static Pattern patternInternalCERTPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal CERT", Pattern.CASE_INSENSITIVE);
	public static Pattern patternInternalMFAPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal MFA|InternalUsersTrustedDevice", Pattern.CASE_INSENSITIVE);
	public static Pattern patternInternalOnlyPolicies = Pattern.compile("Internal_Only_Restriction|Internal_only", Pattern.CASE_INSENSITIVE);
	public static Pattern patternClientFedPolicies = Pattern.compile("PwCIDClientFed", Pattern.CASE_INSENSITIVE);
	public static Pattern patternInternalNoFallbackTreePolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal_no_fallback Tree", Pattern.CASE_INSENSITIVE);
	public static Pattern patternInternalTreePWDPolicies = Pattern.compile("SAML/WS-Fed/OAuth Internal Tree (PWD)", Pattern.CASE_INSENSITIVE);

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
