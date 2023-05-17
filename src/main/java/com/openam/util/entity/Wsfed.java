package com.openam.util.entity;

public class Wsfed extends Entity {

	public Wsfed(final String id) {
		super(id, EntityType.WSFED);
	}

	public boolean isIDP() {
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).contains(Entity.IDENTITY_PROVIDER);
	}

	public boolean isNotIDP() {
		return !isIDP();
	}

	public boolean isNotSP() {
		return !isSP();
	}

	public boolean isSP() {
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).contains(Entity.SERVICE_PROVIDER);
	}
}
