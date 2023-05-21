package com.openam.util.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcabi.aspects.Loggable;
import com.openam.util.Util;

public class Entity extends EntityID {

	public static String ASSIGNED_IDP = "IDP";
	public static String AUTH_LEVEL_CERT = "CERT";
	public static String AUTH_LEVEL_MFA = "MFA";
	public static String COT = "COT";
	private static HashMap<EntityID, Entity> entities = new HashMap<>();
	public static String EXTERNAL_AUTH = "EXT";
	public static String HOSTED = "HOSTED";

	public static String HOSTED_REMOTE = "HOSTED-REMOTE";
	public static String IDENTITY_PROVIDER = "IDP";

	public static String INTERNAL_AUTH = "INT";
	public static String REMARKS = "REMARKS";

	public static String REMOTE = "REMOTE";
	public static String ACCOUNT_MAPPER = "ACCOUNT-MAPPER";
	public static String ATTRIBUTE_MAPPER = "ATTRIBUTE-MAPPER";

	public static String SERVICE_PROVIDER = "SP";

	public static String SP_IDP = "SP-IDP";
	public static String REDIRECT_URLS = "REDIRECT-URLS";
	public static String CLAIMS = "CLAIMS";

	public static String ACTIVE = "ACTIVE";

	public static Pattern patternPasswordProtectedTransportServiceCertMfa = Pattern.compile("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\|\\d+\\|service=(CERT|MFA)\\|default");
	public static Pattern patternSaml2DefaultIDPAttributeMapper = Pattern.compile("com.sun.identity.saml2.plugins.DefaultIDPAttributeMapper");
	public static Pattern patternWsfedDefaultIDPAttributeMapper = Pattern.compile("com.sun.identity.wsfederation.plugins.DefaultIDPAttributeMapper");
	public static Pattern patternPwCIdentityIDPAttributeMapper = Pattern.compile("com.pwc.pwcidentity.openam.attributemapper.PwCIdentityIDPAttributeMapper");
	public static Pattern patternPwCIdentityWSFedIDPAttributeMapper = Pattern.compile("com.pwc.pwcidentity.openam.attributemapper.PwCIdentityWSFedIDPAttributeMapper");
	public static Pattern patternSaml2DefaultIDPAccountMapper = Pattern.compile("com.sun.identity.saml2.plugins.DefaultIDPAccountMapper");
	public static Pattern patternWsfedDefaultIDPAccountMapper = Pattern.compile("com.sun.identity.wsfederation.plugins.DefaultIDPAccountMapper");
	public static Pattern patternPwCIdentityMultipleNameIDAccountMapper = Pattern.compile("com.pwc.openam.saml2.mappers.PwCIdentityMultipleNameIDAccountMapper");
	public static Pattern patternPwCIdentityWsfedIDPAccountMapper = Pattern.compile("com.pwc.pwcidentity.openam.accountmapper.PwCIdentityWsfedIDPAccountMapper");

	private static final Logger logger = LoggerFactory.getLogger(Entity.class);

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
