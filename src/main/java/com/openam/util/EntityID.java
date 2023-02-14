package com.openam.util;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EntityID {

	private static final Logger logger = LoggerFactory.getLogger(EntityID.class);
	private static ObjectMapper mapper = new ObjectMapper();

	public static EntityID ParseProviderEntry(final String provider) {
		final var id = provider.replace("|saml2", "").replace("|wsfed", "").replace("|oauth", "");
		// logger.debug("#{}#", provider);
		if (provider.endsWith("saml2")) {
			return new EntityID(id, EntityType.SAML2);
		}
		if (provider.endsWith("wsfed")) {
			return new EntityID(id, EntityType.WSFED);
		}
		if (provider.endsWith("oauth")) {
			return new EntityID(id, EntityType.OAUTH2);
		}
		return new EntityID(provider, EntityType.UNKNOWN);
	}

	public static EntityID ParseResourceEntry(final String resource) {
		final var id = resource.split("\\|")[0].replace("saml-", "").replace("wsfed-", "").replace("oauth-", "");
		if (resource.startsWith("saml")) {
			return new EntityID(id, EntityType.SAML2);
		}
		if (resource.startsWith("wsfed")) {
			return new EntityID(id, EntityType.WSFED);
		}
		if (resource.startsWith("oauth")) {
			return new EntityID(id, EntityType.OAUTH2);
		}
		return new EntityID(resource, EntityType.UNKNOWN);
	}

	EntityType entityType;

	String id;

	public EntityID(final String id, final EntityType entityType) {
		this.id = id;
		this.entityType = entityType;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		final var that = (EntityID) o;
		return entityType.equals(that.entityType) && id.equals(that.id);
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public String getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entityType, id);
	}

	public String json() {
		try {
			return EntityID.mapper.writeValueAsString(this);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return json();
	}
}
