package com.openam.util.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openam.util.entity.Entity;
import com.openam.util.entity.EntityHelper;
import com.openam.util.entity.EntityType;
import com.openam.util.entity.Policy;

@Component
public class StatsCalculator {

	@Autowired
	EntityHelper entityHelper;

	private boolean checkAttributes(final Entity entity, final List<ImmutablePair<String, String>> keyValuePairs) {
		if (keyValuePairs == null || keyValuePairs.isEmpty()) {
			return true;
		}
		for (final ImmutablePair<String, String> pair : keyValuePairs) {
			final var key = pair.getLeft();
			final var value = pair.getRight();
			if (!value.equals(entity.getAttribute(key))) {
				return false;
			}
		}
		return true;
	}

	private Stream<Entity> getStreamOfAppEntititesWithSepcficAttributes(final EntityType entityType, final List<ImmutablePair<String, String>> attributePairs) {
		return entityHelper.getStreamOfAppEntititesOnly().filter(v -> entityType.equals(v.getEntityType()) && checkAttributes(v, attributePairs));
	}

	private Stream<Entity> getStreamOfAppEntititesWithSpecficCertAlias(final Set<String> certAliasSet) {
		return entityHelper.getStreamOfAppEntititesOnly().filter(v -> (EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType()))
				&& v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER) && v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, certAliasSet));
	}

	private Stream<Entity> getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(final EntityType entityType, final Set<String> certAliasSet) {
		return entityHelper.getStreamOfAppEntititesOnly().filter(v -> entityType.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER)
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, certAliasSet));
	}

	private Stream<Policy> getStreamOfPolicies(final Pattern pattern, final boolean active) {
		return entityHelper.getPolicies().stream().filter(p -> pattern.matcher(p.getID()).find() && p.hasAttribute(Entity.STATUS)
				&& (active ? p.getAttribute(Entity.STATUS).equals(Entity.STATUS_ACTIVE) : p.getAttribute(Entity.STATUS).equals(Entity.STATUS_INACTIVE)));
	}

	public HashMap<String, Long> stats() {
		final var result = new HashMap<String, Long>();
		final var setOf2035CertAlias = Set.of("PwCIdentitySigning_Stg_exp_2035", "PwCIdentitySigning_Prod_exp_2035");
		final var setOf2025CertAlias = Set.of("pwcidentitysigning_stg", "pwcidentitysigning_prd");
		final var setOf2031CertAlias = Set.of("pwcidentitysigning_stg_exp_2031", "pwcidentitysigning_prd_exp_2031");
		final var attribuesRemoteServiceProvider = List.of(ImmutablePair.of(Entity.SP_IDP, Entity.SERVICE_PROVIDER), ImmutablePair.of(Entity.HOSTED_REMOTE, Entity.REMOTE));

		// TOTAL
		result.put("COUNT_OAUTH", getStreamOfAppEntititesWithSepcficAttributes(EntityType.OAUTH2, null).count());
		result.put("COUNT_SAML", getStreamOfAppEntititesWithSepcficAttributes(EntityType.SAML2, attribuesRemoteServiceProvider).count());
		result.put("COUNT_WSFED", getStreamOfAppEntititesWithSepcficAttributes(EntityType.WSFED, attribuesRemoteServiceProvider).count());

		// 2025
		result.put("COUNT_2025", getStreamOfAppEntititesWithSpecficCertAlias(setOf2025CertAlias).count());
		// result.put("COUNT_2025_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesSpecficCertAlias(setOf2025CertAlias).count());
		result.put("COUNT_SAML2_2025", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.SAML2, setOf2025CertAlias).count());
		// result.put("COUNT_SAML2_2025_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.SAML2,
		// setOf2025CertAlias).count());
		result.put("COUNT_WSFED_2025", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.WSFED, setOf2025CertAlias).count());
		// result.put("COUNT_WSFED_2025_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.WSFED,
		// setOf2025CertAlias).count());

		// 2031
		result.put("COUNT_2031", getStreamOfAppEntititesWithSpecficCertAlias(setOf2031CertAlias).count());
		// result.put("COUNT_2031_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesSpecficCertAlias(setOf2031CertAlias).count());
		result.put("COUNT_SAML2_2031", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.SAML2, setOf2031CertAlias).count());
		// result.put("COUNT_SAML2_2031_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.SAML2,
		// setOf2031CertAlias).count());
		result.put("COUNT_WSFED_2031", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.WSFED, setOf2031CertAlias).count());
		// result.put("COUNT_WSFED_2031_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.WSFED,
		// setOf2031CertAlias).count());

		// 2035
		result.put("COUNT_2035", getStreamOfAppEntititesWithSpecficCertAlias(setOf2035CertAlias).filter(v -> !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());
		// result.put("COUNT_2035_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesSpecficCertAlias(setOf2035CertAlias).filter(v
		// -> !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());
		result.put("COUNT_SAML2_2035",
				getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.SAML2, setOf2035CertAlias).filter(v -> !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());
		// result.put("COUNT_SAML2_2035_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.SAML2,
		// setOf2035CertAlias).filter(v ->
		// !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());
		result.put("COUNT_WSFED_2035",
				getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.WSFED, setOf2035CertAlias).filter(v -> !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());
		// result.put("COUNT_WSFED_2035_INTERNAL_ONLY",
		// getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.WSFED,
		// setOf2035CertAlias).filter(v ->
		// !v.getAttribute(Entity.SIGNING_CERT_ALIAS).contains(",")).count());

		// CLIENTFED
		result.put("COUNT_CLIENTFED_ACTIVE", getStreamOfPolicies(Policy.patternClientFedPolicies, true).count());
		result.put("COUNT_CLIENTFED_NOT_ACTIVE", getStreamOfPolicies(Policy.patternClientFedPolicies, false).count());
		return result;
	}
}
