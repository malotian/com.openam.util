package com.openam.entity;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

public class CircleOfTrust extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(CircleOfTrust.class);

	private Set<EntityID> idps = new HashSet<>();

	private Set<EntityID> sps = new HashSet<>();

	public CircleOfTrust(final String id) {
		super(id, EntityType.CIRCLE_OF_TRUST);
	}

	public EntityID getIdp() {
		return getIdps().stream().findFirst().get();
	}

	public Set<EntityID> getIdps() {
		return idps;
	}

	public Set<EntityID> getSps() {
		return sps;
	}

	public boolean hasIdp() {
		return getIdps().stream().findFirst().isPresent();
	}

	public void setIdps(final Set<EntityID> idps) {
		this.idps = idps;
	}

	public void setSps(final Set<EntityID> sps) {
		this.sps = sps;
	}
}