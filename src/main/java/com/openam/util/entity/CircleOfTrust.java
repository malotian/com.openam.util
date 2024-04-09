package com.openam.util.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircleOfTrust extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(CircleOfTrust.class);

	private Set<EntityID> idps = new HashSet<>();

	private Set<EntityID> sps = new HashSet<>();

	public CircleOfTrust(final String id) {
		super(id, EntityType.CIRCLE_OF_TRUST);
	}

	public EntityID getIdp() {
		int maxMatches = 0;
		EntityID maxMatchIDP = null;

		Set<String> tokens = new HashSet<>(Arrays.asList(getID().split("_")));
		// Replace specific strings and prefix/suffix single character strings
		tokens = tokens.stream().map(token -> token.length() == 1 ? ":" + token : token.equals("2025") ? "25" : token.equals("2031") ? "31" : token).collect(Collectors.toSet());
		tokens.add("urn");

		String regexPattern = "(" + String.join("|", tokens) + ")";
		Pattern pattern = Pattern.compile(regexPattern);

		for (var idp : idps) {
			Matcher matcher = pattern.matcher(idp.getID());

			int count = 0;
			while (matcher.find()) {
				count++;
			}
			int matches = count;
			if (matches > maxMatches) {
				maxMatches = matches;
				maxMatchIDP = idp;
			}
		}

		if (null == maxMatchIDP) {
			if (!idps.isEmpty()) {
				maxMatchIDP = idps.stream().findFirst().get();
			}
		}

		sps.remove(maxMatchIDP);
		return maxMatchIDP;
	}

	public EntityID setIdp() {
		return getIdps().stream().findFirst().get();
	}

	public Set<EntityID> getIdps() {
		return idps;
	}

	public Set<EntityID> getSps() {
		return sps;
	}

	public boolean hasIdp() {
		return getIdps().stream().findFirst().isPresent();
	}

	public void setIdps(final Set<EntityID> idps) {
		this.idps = idps;
	}

	public void setSps(final Set<EntityID> sps) {
		this.sps = sps;
	}

	public static void main(String[] args) {
		String input = "pwc_pwd_mail_wsfed_s_2025";
		Set<String> tokens = new HashSet<>(Arrays.asList(input.split("_")));
		// Replace specific strings and prefix/suffix single character strings
		tokens = tokens.stream().map(token -> token.length() == 1 ? ":" + token : token.equals("2025") ? "25" : token.equals("2031") ? "31" : token).collect(Collectors.toSet());
		tokens.add("urn");

		String regexPattern = "(" + String.join("|", tokens) + ")";
		Pattern pattern = Pattern.compile(regexPattern);

		System.out.println("Regex Pattern: " + regexPattern);

		var idps = Arrays.asList("https://onlineaccounting-stage.pwc.com/UK_NAV/", "urn:linkia-poc-cloud:poc", "urn:stockbasisep-dev.pwcinternal.com", "urn:us.policyondemand-stg:tax:us",
				"urn:autoprep-dev.pwcinternal.com/", "urn:sharepoint:dmz-mysite-dev", "urn:starsportal-qa.pwc.com:starsencaptureportal-qa:urn", "https://us-planning-stg.pwcinternal.com/",
				"https://global-egrc-dev.pwcinternal.com/archer-dev/default.aspx?/archer-dev/", "https://stgcentralisedscanningreports.pwcinternal.com/", "urn:stockbasisep-tst.pwcinternal.com",
				"https://starsadminportal-dev.pwc.com/Pages/Home.aspx/", "https://nr-returns-stgazure.pwcinternal.com/", "https://C2CIgnite-stg.pwcinternal.com/",
				"urn:starsadminportal-localhost.pwc.com:starsclientportal-dev:urn", "urn:pwc:pwd:mail:wsfed:s_25", "https://archer-stg.pwcinternal.com/archer-Stage/default.aspx?archer-stage/",
				"urn:starsportal-qa.pwc.com:starsclientportal-qa:urn", "urn:starsportal-stg.pwc.com:starsadminportal-stg:urn", "https://einvoicedev.be.ema.pwcinternal.com/",
				"urn:dataload-west-dev.pwc.com:tax", "urn:ldcontractorsecurity-stg.pwcinternal.com:adfs:us", "urn:cars-uat.pwcinternal.com", "urn:cars-dev.pwcinternal.com",
				"https://recursoslan.pwcinternal.com/", "urn:QBAIAnalyzer-dev.pwcinternal.com", "https://nr-returns-devazure.pwcinternal.com/", "urn:cars-stg.pwcinternal.com",
				"urn:guidpwdreset-stg.pwc.com:pwcit:us", "urn:QBAIAnalyzer-tst.pwcinternal.com", "urn:IndependenceWaiverTool", "https://stgcloud-ukiqm.pwc.com/",
				"https://SK.onlineaccounting-stage.pwc.com", "urn:sharepoint:dmz-teamspace-dev", "urn:it:pwcignite:webapp:test", "urn:starsportal-dev.pwc.com:starsencaptureportal-dev:urn",
				"urn:ionic:mobilepwcit:stg", "https://ams.dev.pwc.ch/", "https://ams.test.pwc.ch/", "urn:us.taxsource-stage.pwc.com:tax:us", "https://onlineaccounting-stage.pwc.com/UK_BC/",
				"urn:smartcmsstage:smartcms", "https://solutiotest.pwc.es/", "https://global-egrc-dev.pwcinternal.com/archer-dev/default.aspx", "urn:Instabyte:AdminDev",
				"https://vizdata-stage.pwc.com/", "https://c2cignite-dev.pwcinternal.com/", "urn:VComply:Admin:UAT", "https://us-planning-test.pwcinternal.com/", "urn:CompOff:Admin:UAT",
				"urn:QAAutomationDev:UAT", "https://transparency-us-test.pwcinternal.com/CelgeneUS/", "urn:starsportal-stg.pwc.com:starsencaptureportal-stg:urn", "urn:dataload-west-test.pwc.com:tax",
				"urn:CompOff:UAT", "urn:us-pcs-wsfed-int:stage", "urn:smartcmsint:smartcms", "urn:us.sitecore.taxsource-stage.pwcinternal.com:tax:us", "urn:TPTool:Admin:UAT", "urn:VComply:UAT",
				"urn:guidpwdreset-dev.pwcinternal.com:pwcit:us", "https://iqmstage.pwc.com/", "urn:ldcontractorsecurity-local:adfs:us",
				"https://ar-buetst011.soa.ad.pwcinternal.com/TimeSummary/Broker", "urn:starsportal-dev.pwc.com:starsadminportal-dev:urn", "urn:taxview-uat.pwcinternal.com",
				"urn:calculationservices-stg.pwc.com/", "urn:taxview-dev.pwcinternal.com", "urn:taxview-stg.pwc.com", "urn:aiannotation-uat.pwcinternal.com", "urn:dataload-west-uat.pwc.com:tax",
				"urn:pwc:pwd:mail:wsfed:s:saml2token", "http://adfs-bos.sydneyplus.com/adfs/services/trust", "https://us-planning-dev.pwcinternal.com/", "https://vizdata-dev.pwcinternal.com/",
				"urn:taxview-tst.pwcinternal.com", "urn:beacon-dev.pwcinternal.com", "urn:ldcontractorsecurity-dev.pwcinternal.com:adfs:us", "urn:stockbasisep-stg.pwcinternal.com",
				"https://archer-stg.pwcinternal.com/archer-Stage/default.aspx", "https://einvoiceuat.be.ema.pwcinternal.com/", "http://sts.crmlan-tst.pwc.com/adfs/services/trust",
				"urn:engagementletter-dev.pwcinternal.com:tax", "urn:starsportal-qa.pwc.com:starsadminportal-qa:urn", "urn:dataload-west-stage.pwc.com:tax",
				"https://loginqa.digitalmakerglb.pwcinternal.com/", "https://es-scsm999.es.ema.ad.pwcinternal.com/", "urn:localhost:16998", "urn:QBAIAnalyzer-stg.pwc.com",
				"urn:us.policyondemand-dev:tax:us", "urn:gosysmw-stg.pwcinternal.com", "https://ar-buetst011.soa.ad.pwcinternal.com/TimeSummary/BackOffice",
				"urn:compensationmapper-dev.pwcinternal.com", "urn:taxnoticemanager-dev.pwcinternal.com");

		// idps = Arrays.asList("http://adfs-bos.sydneyplus.com/adfs/services/trust");

		int maxMatches = 0;
		String maxMatchIDP = null;

		for (var idp : idps) {
			Matcher matcher = pattern.matcher(idp);

			int count = 0;
			while (matcher.find()) {
				logger.debug("matcher.group: {}", matcher.group());
				count++;
			}
			int matches = count;
			if (matches > maxMatches) {
				maxMatches = matches;
				maxMatchIDP = idp;
			}

			logger.debug("count:{}, idp: {}", count, idp);
		}

		logger.debug("maxMatchIDP: {}", maxMatchIDP);
	}
}