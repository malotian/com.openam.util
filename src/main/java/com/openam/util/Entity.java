package com.openam.util;

import java.util.HashMap;

public class Entity extends EntityID {

	public static String EXTERNAL_AUTH = "EXTERNAL-AUTH";
	public static String INTERNAL_AUTH = "INTERNAL-AUTH";
	public static String IDENTITY_PROVIDER = "IDP";
	public static String SERVICE_PROVIDER = "SP";
	public static String AUTH_LEVEL_CERT = "CERT";
	public static String AUTH_LEVEL_MFA = "MFA";
	public static String REMARKS = "REMARKS";

	public static String ASSIGNED_IDP = "ASSIGNED-IDP";
	public static String COT = "COT";

	public static String HOSTED = "HOSTED";
	public static String REMOTE = "REMOTE";

	private static HashMap<EntityID, Entity> entities = new HashMap<>();

	public static String HOSTED_REMOTE = "HOSTED-REMOTE";

	public static String SP_IDP = "SP-IDP";

	public static Entity get(final EntityID eid) {
		return Entity.entities.get(eid);
	}

	public static HashMap<EntityID, Entity> getAllEntities() {
		return Entity.entities;
	}

	public static boolean has(final EntityID eid) {
		return Entity.entities.containsKey(eid);
	}

	protected HashMap<String, String> attributes = new HashMap<>();

	public Entity(final String id, final EntityType type) {
		super(id, type);
		Entity.entities.put(this, this);
	}

	public void addAttribute(final String name, final String value) {
		attributes.put(name, value);

	}

	public void addRemarks(final String value) {
		var helper = value;
		if (attributes.containsKey(Entity.REMARKS))
			helper = attributes.get(Entity.REMARKS) + "#" + value;
		attributes.put(Entity.REMARKS, helper);

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
