package com.openam.util.entity;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.openam.util.OpenAM;
import com.openam.util.Util;

@Component
public class EntityHelper {

	public Set<CircleOfTrust> getCircleOfTrusts() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof CircleOfTrust).map(e -> (CircleOfTrust) e).collect(Collectors.toSet());
	}

	public Set<CircleOfTrust> getCircleOfTrusts2025() {
		return getCircleOfTrusts().stream().filter(e -> OpenAM.patternCirceOfTrust2025.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Entity> getCircleOfTrusts2031() {
		return getCircleOfTrusts().stream().filter(e -> OpenAM.patternCirceOfTrust2031.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getExternalMFAPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternExternalMFAPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getExternalMFAPoliciesApplicable(final EntityID id) {
		return getExternalMFAPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalCERTPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalCERTPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalCERTPoliciesApplicable(final EntityID id) {
		return getInternalCERTPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalMFAPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalMFAPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalMFAPoliciesApplicable(final EntityID id) {
		return getInternalMFAPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalNoFallbackTreePolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalNoFallbackTreePolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalNoFallbackTreePoliciesApplicable(final EntityID id) {
		return getInternalNoFallbackTreePolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalOnlyPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalOnlyPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalOnlyPoliciesApplicable(final EntityID id) {
		return getInternalOnlyPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalTreePWDPolicies() {
		return getPolicies().stream().filter(e -> Policy.patternInternalTreePWDPolicies.matcher(e.getID()).find()).collect(Collectors.toSet());
	}

	public Set<Policy> getInternalTreePWDPoliciesApplicable(final EntityID id) {
		return getInternalTreePWDPolicies().stream().filter(p -> p.getResources().contains(id)).collect(Collectors.toSet());
	}

	public Set<OAuth2Client> getOAuth2Clients() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof OAuth2Client).map(e -> (OAuth2Client) e).collect(Collectors.toSet());
	}

	public Set<Policy> getPolicies() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Policy).map(e -> (Policy) e).collect(Collectors.toSet());
	}

	public Set<EntityID> getResourcesForExternalMFAPolices() {
		final var reources = new HashSet<EntityID>();
		getExternalMFAPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalCERTPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalCERTPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalMFAPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalMFAPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesForInternalOnlyPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalOnlyPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesInternalNoFallbackTreePolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalNoFallbackTreePolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<EntityID> getResourcesInternalTreePWDPolicies() {
		final var reources = new HashSet<EntityID>();
		getInternalTreePWDPolicies().stream().forEach(p -> reources.addAll(p.getResources()));
		return reources;
	}

	public Set<Saml2> getSaml2Entities() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Saml2).map(e -> (Saml2) e).collect(Collectors.toSet());
	}

	public Set<Saml2> getSaml2IdPEntities() {
		return getSaml2Entities().stream().filter(e -> e.hasAttribute(Entity.IDENTITY_PROVIDER)).collect(Collectors.toSet());
	}

	public Set<Saml2> getSaml2SpEntities() {
		return getSaml2Entities().stream().filter(e -> e.hasAttribute(Entity.SERVICE_PROVIDER)).collect(Collectors.toSet());
	}

	public Stream<Entity> getStreamOfAppEntititesOnly() {
		return Entity.getAllEntities().values().stream()
				.filter(v -> ((EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType()) || EntityType.OAUTH2.equals(v.getEntityType()))
						&& (!v.hasAttribute(Entity.HOSTED_REMOTE) || v.getAttribute(Entity.HOSTED_REMOTE).equals(Entity.REMOTE))
						&& (!v.hasAttribute(Entity.SP_IDP) || v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER))));
	}

	public Set<Wsfed> getWsfedEntities() {
		return Entity.getAllEntities().values().stream().filter(e -> e instanceof Wsfed).map(e -> (Wsfed) e).collect(Collectors.toSet());
	}

	public Set<Wsfed> getWsfedIdPEntities() {
		return getWsfedEntities().stream().filter(e -> e.hasAttribute(Entity.IDENTITY_PROVIDER)).collect(Collectors.toSet());
	}

	public Set<Wsfed> getWsfedSpEntities() {
		return getWsfedEntities().stream().filter(e -> e.hasAttribute(Entity.SERVICE_PROVIDER)).collect(Collectors.toSet());
	}

	public void updateAuthAsPerPolicies(final Entity entity) {

		final var lookup = new EntityID(entity.getID(), entity.getEntityType());

		if (getResourcesForInternalMFAPolicies().contains(lookup)) {
			final var policies = getInternalMFAPoliciesApplicable(lookup);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_MFA);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (getResourcesForInternalCERTPolicies().contains(lookup)) {
			final var policies = getInternalCERTPoliciesApplicable(lookup);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_CERT);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (getResourcesInternalNoFallbackTreePolicies().contains(lookup)) {
			final var policies = getInternalNoFallbackTreePoliciesApplicable(lookup);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_INTERNAL_NO_FALLBACK_TREE);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (getResourcesInternalTreePWDPolicies().contains(lookup)) {
			final var policies = getInternalTreePWDPoliciesApplicable(lookup);
			entity.addAttribute(Entity.INTERNAL_AUTH, Entity.AUTH_LEVEL_INTERNAL_PWD_TREE);
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.INTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else {
			entity.addAttribute(Entity.INTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("INTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.INTERNAL_AUTH));
			entity.addRemarks(remarks);
		}

		if (getResourcesForExternalMFAPolices().contains(lookup)) {
			final var policies = getExternalMFAPoliciesApplicable(lookup);
			entity.addAttribute(Entity.EXTERNAL_AUTH, "MFA");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.EXTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else if (getResourcesForInternalOnlyPolicies().contains(lookup)) {
			final var policies = getInternalOnlyPoliciesApplicable(lookup);
			entity.addAttribute(Entity.EXTERNAL_AUTH, "N/A");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: {1}", entity.getAttribute(Entity.EXTERNAL_AUTH), Util.json(policies.stream().map(Policy::getID).toArray()));
			entity.addRemarks(remarks);
		} else {
			entity.addAttribute(Entity.EXTERNAL_AUTH, "PWD");
			final var remarks = MessageFormat.format("EXTERNAL_AUTH: {0}, Policies: None", entity.getAttribute(Entity.EXTERNAL_AUTH));
			entity.addRemarks(remarks);
		}
	}

}
