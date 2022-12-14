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

	@Value("${openam-url}")
	private String openamUrl;

	@Value("${openam-username}")
	private String openamUsername;

	@Value("${openam-password}")
	private String openamPassword;

	public String getOpenamPassword() {
		return openamPassword;
	}

	public String getOpenamUrl() {
		return openamUrl;
	}

	public String getOpenamUsername() {
		return openamUsername;
	}

}
