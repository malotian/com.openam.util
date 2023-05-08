package com.openam.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class OAuth2Client extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(OAuth2Client.class);

	public OAuth2Client(final String id) {
		super(id, EntityType.OAUTH2);
	}

}
