package com.openam.util;

import java.util.HashMap;

public class Entity extends EntityID {

	public static String EXTERNAL_AUTH_LEVEL = "EXTERNAL_AUTH_LEVEL";
	public static String INTERNAL_AUTH_LEVEL = "INTERNAL_AUTH_LEVEL";
	public static String IDENTITY_PROVIDER = "IDP";
	public static String SERVICE_PROVIDER = "SP";
	public static String AUTH_LEVEL_CERT = "CERT";
	public static String AUTH_LEVEL_MFA = "MFA";
	
	public static String ASSIGNED_IDENTITY_PROVIDER = "ASSIGNED_IDENTITY_PROVIDER";
	public static String ASSIGNED_COT = "ASSIGNED_COT";


	public static String HOSTED = "HOSTED";
	public static String REMOTE = "REMOTE";

	private static HashMap<EntityID, Entity> entities = new HashMap<>();

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

	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public Entity(final String id, final EntityType type) {
		super(id, type);
		Entity.entities.put(this, this);
	}

	public void addAttribute(final String name) {
		attributes.put(name, "true");

	}

	public void addAttribute(final String name, final String value) {
		attributes.put(name, value);

	}
	
	public String getAttribute(final String attribute) {
		return attributes.get(attribute);

	}

	public boolean hasAttribute(final String attribute) {
		return attributes.containsKey(attribute);

	}
}
