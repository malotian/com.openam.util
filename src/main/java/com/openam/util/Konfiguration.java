package com.openam.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:application.properties")
@ConfigurationProperties("app")
public class Konfiguration {

	protected String openamPassword;

	protected String openamStageUrl;

	protected String openamProdUrl;

	protected String openamUsername;

	protected String tableColumns;

	@Autowired
	Kontext kontext;

	public String getOpenamPassword() {
		return openamPassword;
	}

	public String getTableColumns() {
		return tableColumns;
	}

	public String getOpenamUrl() {
		if (kontext.isSetToProd()) {
			return openamProdUrl;
		}
		return openamStageUrl;
	}

	public String getOpenamProdUrl() {
		return openamProdUrl;
	}

	public String getOpenamStageUrl() {
		return openamStageUrl;
	}

	public String getOpenamUsername() {
		return openamUsername;
	}

	public void setOpenamPassword(final String password) {
		this.openamPassword = password;
	}

	public void setTableColumns(final String tableColumns) {
		this.tableColumns = tableColumns;
	}

	public void setOpenamProdUrl(final String urlProd) {
		this.openamProdUrl = urlProd;
	}

	public void setOpenamStageUrl(final String urlStage) {
		this.openamStageUrl = urlStage;
	}

	public void setOpenamUsername(final String username) {
		this.openamUsername = username;
	}

}
