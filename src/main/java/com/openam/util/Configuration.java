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

	@Value("${openam-stage-url}")
	private String openamStageUrl;

	@Value("${openam-prod-url}")
	private String openamProdUrl;

	@Value("${openam-username}")
	private String openamUsername;

	private String environment;

	public String getOpenamPassword() {
		return openamPassword;
	}

	public String getOpenamUrl() {
		if ("stage".equals(environment)) {
			return openamStageUrl;
		}
		if ("prod".equals(environment)) {
			return openamProdUrl;
		} else {
			return null;
		}
	}

	public String getOpenamUsername() {
		return openamUsername;
	}

	public void useEnvironment(final String env) {
		environment = env;
	}

}
