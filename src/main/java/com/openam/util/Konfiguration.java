package com.openam.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:application.properties")
@ConfigurationProperties("openam")
@RefreshScope
public class Konfiguration {

	protected String password;

	protected String urlStage;

	protected String urlProd;

	protected String username;

	protected String tableColumns;

	@Autowired
	Kontext kontext;

	public String getPassword() {
		return password;
	}

	public String getTableColumns() {
		return tableColumns;
	}

	public String getUrl() {
		if (kontext.isSetToProd())
			return urlProd;
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

	public void setTableColumns(final String tableColumns) {
		this.tableColumns = tableColumns;
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
