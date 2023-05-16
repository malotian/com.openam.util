package com.openam.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Saml2 extends Entity {

	static final Logger logger = LoggerFactory.getLogger(Saml2.class);

	public Saml2(final String id) {
		super(id, EntityType.SAML2);
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
