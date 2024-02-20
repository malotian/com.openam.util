package com.openam.util.entity.processor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.openam.util.Util;
import com.openam.util.entity.CircleOfTrust;
import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.EntityID;
import com.openam.util.entity.Saml2;
import com.openam.util.entity.Wsfed;

@Component
public class CircleOfTrustProcessor {

	static final Logger logger = LoggerFactory.getLogger(CircleOfTrustProcessor.class);

	@Autowired
	protected EntityHelper helper;

	public void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		if (!json.has("trustedProviders")) {
			CircleOfTrustProcessor.logger.warn("skipping, trustedProviders missing : {} ", json.get("_id").asText());
			return;
		}

		// CircleOfTrust.logger.info("cot: {}, count: {}", id,
		// StreamSupport.stream(json.get("trustedProviders").spliterator(),
		// false).count());

		final var typesEncountered = new HashSet<>();

		final var cot = new CircleOfTrust(id);
		final var trustedProviders = new HashSet<EntityID>();
		json.get("trustedProviders").forEach(provider -> {
			final var eid = EntityID.ParseProviderEntry(provider.asText());
			typesEncountered.add(eid.getEntityType());
			if (!Entity.has(eid)) {
				CircleOfTrustProcessor.logger.warn("invalid(doesn't exist) entry: {} in cot: {}", eid, id);
				return;
			}
			trustedProviders.add(eid);
		});

		if (typesEncountered.size() > 1) {
			CircleOfTrustProcessor.logger.warn("warning, mixed entitities: {} in cot: {}", Util.json(typesEncountered), cot.getID());
		}

		final var sps = new HashSet<>(trustedProviders);
		// collect elements that are only idps
		var idps = filterIdp(trustedProviders, true);
		sps.removeAll(idps); // remove all only idps

		if (idps.size() == 0) {
			CircleOfTrustProcessor.logger.warn("no idps(strict=true) in cot: {}", cot.getID());
			idps = filterIdp(trustedProviders, false);
		}

		cot.setIdps(idps);
		cot.setSps(sps);

		// we will remove first one only from lits of sps
		if (idps.isEmpty()) {
			CircleOfTrustProcessor.logger.warn("skipping, no idps(strict=false) in cot: {}", cot.getID());
		} else {
			if (idps.size() > 1) {
				int maxMatches = 0;
				EntityID idp = null;
				for (var e : idps) {
					int matches = countMatches(e.getID(), "pwc") + countMatches(e.getID(), ":") + countMatches(e.getID(), "urn");
					if (matches > maxMatches) {
						maxMatches = matches;
						idp = e;
					}
				}
				if (null != idp)
					sps.remove(idp);
			}

		}

		if (idps.size() > 1) {
			CircleOfTrustProcessor.logger.warn("warning, multiple idps: {} in cot: {}", Util.json(idps), cot.getID());
			final var remarks = MessageFormat.format("IDP(s): {0}, By default first will be used", Util.json(idps.stream().map(EntityID::getID).toArray()));
			cot.addRemarks(remarks);
		}

		cot.setSps(sps);
		cot.setIdps(idps);
		cot.getSps().forEach(sp -> Entity.get(sp).addAttribute(Entity.COT, cot.getID()));

		if (!cot.hasIdp()) {
			cot.addRemarks("IDP(s): None");
			return;
		}

		cot.addAttribute(Entity.ASSIGNED_IDP, cot.getIdp().getID());
		cot.addRemarks(MessageFormat.format("ASSIGNED-IDP: {0}", cot.getIdp().getID()));

		final var idp = Entity.get(cot.getIdp());
		idp.addRemarks(MessageFormat.format("COT: {0}", id));

		cot.addAttribute(Entity.INTERNAL_AUTH, idp.getAttribute(Entity.INTERNAL_AUTH));
		cot.addRemarks(MessageFormat.format("INTERNAL_AUTH: {0}, IDP: {1}", idp.getAttribute(Entity.INTERNAL_AUTH), idp.getID()));
		cot.addRemarks(MessageFormat.format("EXTERNAL_AUTH: {0}, IDP: {1}", idp.getAttribute(Entity.EXTERNAL_AUTH), idp.getID()));

		cot.getSps().forEach(spID -> {
			final var sp = Entity.get(spID);
			sp.addAttribute(Entity.ASSIGNED_IDP, idp.getID());
			if (idp.hasAttribute(Entity.CERT_ALIAS)) {
				sp.addAttribute(Entity.CERT_ALIAS, idp.getAttribute(Entity.CERT_ALIAS));
			}
			if (idp.hasAttribute(Entity.INTERNAL_AUTH) && (!sp.hasAttribute(Entity.INTERNAL_AUTH) || "PWD".equals(sp.getAttribute(Entity.INTERNAL_AUTH)))) {
				sp.addAttribute(Entity.INTERNAL_AUTH, idp.getAttribute(Entity.INTERNAL_AUTH));
				sp.addRemarks(MessageFormat.format("INTERNAL_AUTH: {0}, IDP: {1}", sp.getAttribute(Entity.INTERNAL_AUTH), idp.getID()));
			}
			if (idp.hasAttribute(Entity.EXTERNAL_AUTH) && (!sp.hasAttribute(Entity.EXTERNAL_AUTH) || "PWD".equals(sp.getAttribute(Entity.EXTERNAL_AUTH)))) {
				sp.addAttribute(Entity.EXTERNAL_AUTH, idp.getAttribute(Entity.EXTERNAL_AUTH));
				sp.addRemarks(MessageFormat.format("EXTERNAL_AUTH: {0}, IDP: {1}", sp.getAttribute(Entity.EXTERNAL_AUTH), idp.getID()));
			}
			if (idp.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(idp.getAttribute(Entity.EXTERNAL_AUTH))) {
				sp.addAttribute(Entity.EXTERNAL_AUTH, idp.getAttribute(Entity.EXTERNAL_AUTH));
				sp.addRemarks(MessageFormat.format("EXTERNAL_AUTH: {0}, IDP: {1}", sp.getAttribute(Entity.EXTERNAL_AUTH), idp.getID()));
			}

		});

	}

	public static String findMaxMatchString(List<String> strings) {
		int maxMatches = 0;
		String maxMatchString = null;

		for (String str : strings) {
			int matches = countMatches(str, "pwc") + countMatches(str, ":") + countMatches(str, "urn");
			if (matches > maxMatches) {
				maxMatches = matches;
				maxMatchString = str;
			}
		}

		return maxMatchString;
	}

	public static int countMatches(String str, String substring) {
		int count = 0;
		int idx = 0;

		while ((idx = str.indexOf(substring, idx)) != -1) {
			count++;
			idx += substring.length();
		}

		return count;
	}

	private Set<EntityID> filterIdp(final Set<EntityID> trustedProviders, final boolean strict) {

		return trustedProviders.stream().filter(eid -> {
			final var entity = Entity.get(eid);

			if (entity.getClass() == Saml2.class) {
				final var se = (Saml2) entity;
				if (se.isIDP()) {
					return strict ? se.isNotSP() : true;
				}
			} else if (entity.getClass() == Wsfed.class) {
				final var we = (Wsfed) entity;
				if (we.isIDP()) {
					return strict ? we.isNotSP() : true;
				}
			}
			return false;

		}).collect(Collectors.toSet());
	}

	public void process(final JsonNode circelOfTruts) {
		final var result = circelOfTruts.get("result");
		result.forEach(cot -> {
			try {
				_process(cot);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}
}