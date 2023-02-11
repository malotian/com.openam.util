package com.openam.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configuration {

	static Configuration singleton;

	public static Configuration getInstance() {
		return Configuration.singleton;
	}

	public static void setInstance(final Configuration instance) {
		Configuration.singleton = instance;
	}

	@Value("${openam-password}")
	private String openamPassword;

	@Value("${openam-url}")
	private String openamUrl;

	@Value("${openam-username}")
	private String openamUsername;

	public String getOpenamPassword() {
		return this.openamPassword;
	}

	public String getOpenamUrl() {
		return this.openamUrl;
	}

	public String getOpenamUsername() {
		return this.openamUsername;
	}

}
