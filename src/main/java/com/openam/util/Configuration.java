package com.openam.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configuration {

	static Configuration singleton;

	public static Configuration getInstance() {
		return Configuration.singleton;
	}

	public static void setInstance(Configuration instance) {
		Configuration.singleton = instance;
	}

	public String getOpenamUrl() {
		return openamUrl;
	}

	public String getOpenamUsername() {
		return openamUsername;
	}

	public String getOpenamPassword() {
		return openamPassword;
	}

	@Value("${openam-url}")
	private String openamUrl;

	@Value("${openam-username}")
	private String openamUsername;

	@Value("${openam-password}")
	private String openamPassword;

}
