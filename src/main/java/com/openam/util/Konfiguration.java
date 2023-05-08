package com.openam.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.thymeleaf.util.StringUtils;

@PropertySource("classpath:application.properties")
@ConfigurationProperties("openam")
public class Konfiguration {

	protected String password;

	protected String urlStage;

	protected String urlProd;

	protected String username;

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		if (!StringUtils.isEmptyOrWhitespace(System.getProperty("use.openam.prod.env")) && "true".equalsIgnoreCase(System.getProperty("use.openam.prod.env"))) {
			return urlProd;
		}
		return urlStage;
	}

	public String getUrlProd() {
		return urlProd;
	}

	public String getUrlStage() {
		return urlStage;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setUrlProd(final String urlProd) {
		this.urlProd = urlProd;
	}

	public void setUrlStage(final String urlStage) {
		this.urlStage = urlStage;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

}
