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

	private static final Set<String> SET_OF_2025_CERT_ALIAS = Set.of("PwCIdentitySigning_Stg_exp_2035", "pwcidentitysigning_stg", "PwCIdentitySigning_Prod_exp_2035", "pwcidentitysigning_prd");

	private static final Set<String> SET_OF_2031_CERT_ALIAS = Set.of("pwcidentitysigning_stg_exp_2031", "pwcidentitysigning_prd_exp_2031");
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

	private Stream<Entity> getStreamOfInternalAppEntititesSpecficCertAlias(final Set<String> certAliasSet) {
		return entityHelper.getStreamOfAppEntititesOnly()
				.filter(v -> (EntityType.SAML2.equals(v.getEntityType()) || EntityType.WSFED.equals(v.getEntityType())) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER)
						&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, certAliasSet) && v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)));
	}

	private Stream<Entity> getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(final EntityType entityType, final Set<String> certAliasSet) {
		return entityHelper.getStreamOfAppEntititesOnly().filter(v -> entityType.equals(v.getEntityType()) && v.getAttribute(Entity.SP_IDP).equals(Entity.SERVICE_PROVIDER)
				&& v.doesJsonArrayAttributeContains(Entity.SIGNING_CERT_ALIAS, certAliasSet) && v.hasAttribute(Entity.EXTERNAL_AUTH) && "N/A".equals(v.getAttribute(Entity.EXTERNAL_AUTH)));
	}

	private Stream<Policy> getStreamOfPolicies(final Pattern pattern, final boolean active) {
		return entityHelper.getPolicies().stream().filter(p -> pattern.matcher(p.getID()).find() && p.hasAttribute(Entity.ACTIVE) && active == Boolean.valueOf(p.getAttribute(Entity.ACTIVE)));
	}

	public HashMap<String, Long> stats() {
		final var result = new HashMap<String, Long>();

		final Set<String> setOf2025CertAlias = Set.of("PwCIdentitySigning_Stg_exp_2035", "pwcidentitysigning_stg", "PwCIdentitySigning_Prod_exp_2035", "pwcidentitysigning_prd");
		final Set<String> setOf2031CertAlias = Set.of("pwcidentitysigning_stg_exp_2031", "pwcidentitysigning_prd_exp_2031");

		result.put("COUNT_OAUTH", getStreamOfAppEntititesWithSepcficAttributes(EntityType.OAUTH2, null).count());

		result.put("COUNT_SAML",
				getStreamOfAppEntititesWithSepcficAttributes(EntityType.SAML2, List.of(ImmutablePair.of(Entity.SP_IDP, Entity.SERVICE_PROVIDER), ImmutablePair.of(Entity.HOSTED_REMOTE, Entity.REMOTE)))
						.count());

		result.put("COUNT_WSFED",
				getStreamOfAppEntititesWithSepcficAttributes(EntityType.WSFED, List.of(ImmutablePair.of(Entity.SP_IDP, Entity.SERVICE_PROVIDER), ImmutablePair.of(Entity.HOSTED_REMOTE, Entity.REMOTE)))
						.count());

		result.put("COUNT_2025", getStreamOfAppEntititesWithSpecficCertAlias(setOf2025CertAlias).count());
		result.put("COUNT_2025_INTERNAL_ONLY", getStreamOfInternalAppEntititesSpecficCertAlias(setOf2025CertAlias).count());
		result.put("COUNT_SAML2_2025", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.SAML2, setOf2025CertAlias).count());
		result.put("COUNT_SAML2_2025_INTERNAL_ONLY", getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.SAML2, setOf2025CertAlias).count());
		result.put("COUNT_WSFED_2025", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.WSFED, setOf2025CertAlias).count());
		result.put("COUNT_WSFED_2025_INTERNAL_ONLY", getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.WSFED, setOf2025CertAlias).count());
		result.put("COUNT_2031", getStreamOfAppEntititesWithSpecficCertAlias(setOf2031CertAlias).count());
		result.put("COUNT_2031_INTERNAL_ONLY", getStreamOfInternalAppEntititesSpecficCertAlias(setOf2031CertAlias).count());
		result.put("COUNT_SAML2_2031", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.SAML2, setOf2031CertAlias).count());
		result.put("COUNT_SAML2_2031_INTERNAL_ONLY", getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.SAML2, setOf2031CertAlias).count());
		result.put("COUNT_WSFED_2031", getStreamOfAppEntititesWithSpecificEntityTypeAndCertAlias(EntityType.WSFED, setOf2031CertAlias).count());
		result.put("COUNT_WSFED_2031_INTERNAL_ONLY", getStreamOfInternalAppEntititesWithSpecficEntityTypeAndCertAlias(EntityType.WSFED, setOf2031CertAlias).count());
		result.put("COUNT_CLIENTFED_ACTIVE", getStreamOfPolicies(Policy.patternClientFedPolicies, true).count());
		result.put("COUNT_CLIENTFED_NOT_ACTIVE", getStreamOfPolicies(Policy.patternClientFedPolicies, false).count());
		return result;
	}
}