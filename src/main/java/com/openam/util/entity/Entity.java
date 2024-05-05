package com.openam.util.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.aspects.Loggable;
import com.openam.util.Util;

public class Entity extends EntityID {

	public static String ASSIGNED_IDP = "IDP";
	public static String AUTH_LEVEL_CERT = "CERT";
	public static String AUTH_LEVEL_MFA = "MFA";
	public static String AUTH_LEVEL_PWD = "PWD";
	public static String AUTH_LEVEL_NA = "N/A";
	public static String AUTH_LEVEL_INTERNAL_NO_FALLBACK_TREE = "INTERNAL_NO_FALLBACK_TREE";
	public static String AUTH_LEVEL_INTERNAL_PWD_TREE = "INTERNAL_NO_PWD_TREE";

	public static String COT = "COT";
	private static HashMap<EntityID, Entity> entities = new HashMap<>();
	public static String EXTERNAL_AUTH = "EXT";
	public static String HOSTED = "HOSTED";

	public static String HOSTED_REMOTE = "HOSTED-REMOTE";
	public static String IDENTITY_PROVIDER = "IDP";

	public static String INTERNAL_AUTH = "INT";

	private static String REMARKS = "REMARKS";

	public static String REMOTE = "REMOTE";
	public static String ACCOUNT_MAPPER = "ACCOUNT-MAPPER";
	public static String ATTRIBUTE_MAPPER = "ATTRIBUTE-MAPPER";

	public static String SERVICE_PROVIDER = "SP";

	public static String SP_IDP = "SP-IDP";
	public static String SIGNING_CERT_ALIAS = "SIGNING-CERT-ALIAS";
	public static String REDIRECT_URLS = "REDIRECT-URLS";
	public static String CLAIMS = "CLAIMS";

	public static String STATUS = "STATUS";
	public static String STATUS_ACTIVE = "ACTIVE";
	public static String STATUS_INACTIVE = "INACTIVE";

	public static Pattern patternPasswordProtectedTransportServiceCertMfa = Pattern.compile("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\|\\d+\\|service=(.*)\\|default");
	public static Pattern patternSaml2DefaultIDPAttributeMapper = Pattern.compile("com.sun.identity.saml2.plugins.DefaultIDPAttributeMapper");
	public static Pattern patternWsfedDefaultIDPAttributeMapper = Pattern.compile("com.sun.identity.wsfederation.plugins.DefaultIDPAttributeMapper");
	public static Pattern patternPwCIdentityIDPAttributeMapper = Pattern.compile("com.pwc.pwcidentity.openam.attributemapper.PwCIdentityIDPAttributeMapper");
	public static Pattern patternPwCIdentityWSFedIDPAttributeMapper = Pattern.compile("com.pwc.pwcidentity.openam.attributemapper.PwCIdentityWSFedIDPAttributeMapper");
	public static Pattern patternSaml2DefaultIDPAccountMapper = Pattern.compile("com.sun.identity.saml2.plugins.DefaultIDPAccountMapper");
	public static Pattern patternWsfedDefaultIDPAccountMapper = Pattern.compile("com.sun.identity.wsfederation.plugins.DefaultIDPAccountMapper");
	public static Pattern patternPwCIdentityMultipleNameIDAccountMapper = Pattern.compile("com.pwc.openam.saml2.mappers.PwCIdentityMultipleNameIDAccountMapper");
	public static Pattern patternPwCIdentityWsfedIDPAccountMapper = Pattern.compile("com.pwc.pwcidentity.openam.accountmapper.PwCIdentityWsfedIDPAccountMapper");

	private static final Logger logger = LoggerFactory.getLogger(Entity.class);
	private static ObjectMapper mapper = new ObjectMapper();

	public static Entity get(final EntityID eid) {
		return Entity.entities.get(eid);
	}

	public static HashMap<EntityID, Entity> getAllEntities() {
		return Entity.entities;
	}

	public static boolean has(final EntityID eid) {
		return Entity.entities.containsKey(eid);
	}

	@Loggable
	public static void initialize() {
		Entity.entities = new HashMap<>();
	}

	public static void main(final String[] args) {
		final var input = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|123|service=pwc-cert|default";
		final var pattern = Pattern.compile("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\|\\d+\\|service=(.*)\\|default");
		final var matcher = pattern.matcher(input);

		if (matcher.find()) {
			final var groupText = matcher.group(1); // This will select the text in group CERT|MFA
			System.out.println(groupText);
		}
	}

	protected HashMap<String, String> attributes = new HashMap<>();

	ArrayList<String> remarks = new ArrayList<>();

	public Entity(final String id, final EntityType type) {
		super(id, type);
		Entity.entities.put(this, this);
	}

	public void addAttribute(final String name, final String value) {
		attributes.put(name, value);

	}

	public void addRemarks(final String value) {
		remarks.add(value);
		attributes.put(Entity.REMARKS, Util.json(remarks));
	}

	public boolean doesJsonArrayAttributeContains(final String attribute, final Set<String> expected) {
		if (!hasAttribute(attribute)) {
			return false;
		}

		try {
			final var jsonArray = Entity.mapper.readValue(getAttribute(attribute), String[].class);
			return Arrays.stream(jsonArray).anyMatch(expected::contains);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	public String getAttribute(final String attribute) {
		return attributes.get(attribute);

	}

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public boolean hasAttribute(final String attribute) {
		return attributes.containsKey(attribute);
	}
}
