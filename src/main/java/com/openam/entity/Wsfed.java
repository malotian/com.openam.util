package com.openam.entity;

public class Wsfed extends Entity {

	public Wsfed(final String id) {
		super(id, EntityType.WSFED);
	}

	public boolean isIDP() {
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).equals(Entity.IDENTITY_PROVIDER);
	}

	public boolean isNotIDP() {
		return !isIDP();
	}

	public boolean isNotSP() {
		return !isSP();
	}

	public boolean isSP() {
		return hasAttribute(Entity.SP_IDP) && getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER);
	}
}