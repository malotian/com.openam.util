package com.openam.util;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class CircleOfTrust extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(CircleOfTrust.class);

	public static void _process(final JsonNode json) throws ParserConfigurationException, SAXException, IOException {
		final var id = json.get("_id").asText();
		if (!json.has("trustedProviders")) {
			CircleOfTrust.logger.warn("skipping, trustedProviders missing : {} ", json.get("_id").asText());
			return;
		}

		final var typesEncountered = new HashSet<>();

		final var cot = new CircleOfTrust(id);
		final var trustedProviders = new HashSet<EntityID>();
		json.get("trustedProviders").forEach(provider -> {
			final var eid = EntityID.ParseProviderEntry(provider.asText());
			typesEncountered.add(eid.getEntityType());
			if (!Entity.has(eid)) {
				CircleOfTrust.logger.warn("invalid entry: {} in cot: {}", eid, id);
				return;
			}
			trustedProviders.add(eid);
		});

		if (typesEncountered.size() > 1)
			CircleOfTrust.logger.warn("warning, mixed entitities: {} in cot: {}", Util.json(typesEncountered), cot.getID());

		final var sps = new HashSet<>(trustedProviders);
		// collect elements that are only idps
		var idps = CircleOfTrust.filterIdp(trustedProviders, true);
		sps.removeAll(idps); // remove all only idps

		if (idps.size() == 0) {
			CircleOfTrust.logger.warn("no idps(strict=true) in cot: {}", cot.getID());
			idps = CircleOfTrust.filterIdp(trustedProviders, false);
		}

		cot.setIdps(idps);
		cot.setSps(sps);

		// we will remove first one only from lits of sps
		if (idps.isEmpty())
			CircleOfTrust.logger.warn("skipping, no idps(strict=false) in cot: {}", cot.getID());
		else {
			if (idps.size() > 1) {
				final var helper = idps.stream().filter(idp -> idp.getID().contains("pwc")).collect(Collectors.toSet());
				if (!helper.isEmpty())
					idps = helper;

			}

			sps.remove(idps.stream().findFirst().get());
		}

		if (idps.size() > 1)
			CircleOfTrust.logger.warn("warning, multiple idps: {} in cot: {}", Util.json(idps), cot.getID());

		cot.setSps(sps);
		cot.setIdps(idps);
		cot.getSps().forEach(sp -> Entity.get(sp).addAttribute(Entity.COT, cot.getID()));

		if (cot.hasIdp()) {
			cot.addAttribute(Entity.ASSIGNED_IDP, cot.getIdp().getID());
			final var idp = Entity.get(cot.getIdp());
			cot.getSps().forEach(spID -> {
				final var sp = Entity.get(spID);
				sp.addAttribute(Entity.ASSIGNED_IDP, idp.getID());
				if (idp.hasAttribute(Entity.INTERNAL_AUTH) && (!sp.hasAttribute(Entity.INTERNAL_AUTH) || "PWD".equals(sp.getAttribute(Entity.INTERNAL_AUTH)))) {
					sp.addAttribute(Entity.INTERNAL_AUTH, idp.getAttribute(Entity.INTERNAL_AUTH));
					final var remarks = MessageFormat.format("INTERNAL_AUTH:{0}, IDP: {1}", sp.getAttribute(Entity.INTERNAL_AUTH), idp.getID());
					sp.addRemarks(remarks);
				}
				if (idp.hasAttribute(Entity.EXTERNAL_AUTH) && (!sp.hasAttribute(Entity.EXTERNAL_AUTH) || "PWD".equals(sp.getAttribute(Entity.EXTERNAL_AUTH)))) {
					sp.addAttribute(Entity.EXTERNAL_AUTH, idp.getAttribute(Entity.EXTERNAL_AUTH));
					final var remarks = MessageFormat.format("EXTERNAL_AUTH:{0}, IDP: {1}", sp.getAttribute(Entity.EXTERNAL_AUTH), idp.getID());
					sp.addRemarks(remarks);
				}
			});
		}
	}

	private static Set<EntityID> filterIdp(final Set<EntityID> trustedProviders, final boolean strict) {

		return trustedProviders.stream().filter(eid -> {
			final var entity = Entity.get(eid);

			if (entity.getClass() == Saml2.class) {
				final var se = (Saml2) entity;
				if (se.isIDP())
					return strict ? se.isNotSP() : true;
			} else if (entity.getClass() == Wsfed.class) {
				final var we = (Wsfed) entity;
				if (we.isIDP())
					return strict ? we.isNotSP() : true;
			}
			return false;

		}).collect(Collectors.toSet());
	}

	public static void process(final JsonNode circelOfTruts) {
		final var result = circelOfTruts.get("result");
		result.forEach(cot -> {
			try {
				CircleOfTrust._process(cot);
			} catch (final ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
		});

	}

	private Set<EntityID> idps = new HashSet<>();

	private Set<EntityID> sps = new HashSet<>();

	protected CircleOfTrust(final String id) {
		super(id, EntityType.CIRCLE_OF_TRUST);
	}

	EntityID getIdp() {
		return getIdps().stream().findFirst().get();
	}

	public Set<EntityID> getIdps() {
		return idps;
	}

	public Set<EntityID> getSps() {
		return sps;
	}

	boolean hasIdp() {
		return getIdps().stream().findFirst().isPresent();
	}

	public void setIdps(final Set<EntityID> idps) {
		this.idps = idps;
	}

	public void setSps(final Set<EntityID> sps) {
		this.sps = sps;
	}
}