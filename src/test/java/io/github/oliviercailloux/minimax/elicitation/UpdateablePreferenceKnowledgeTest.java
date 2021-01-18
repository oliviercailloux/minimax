package io.github.oliviercailloux.minimax.elicitation;

import static io.github.oliviercailloux.minimax.Basics.a1;
import static io.github.oliviercailloux.minimax.Basics.a2;
import static io.github.oliviercailloux.minimax.Basics.a3;
import static io.github.oliviercailloux.minimax.Basics.v1;
import static io.github.oliviercailloux.minimax.Basics.v2;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.jlp.elements.Term;

public class UpdateablePreferenceKnowledgeTest {

	@Test
	void settingWeightsTest () {
		final Alternative a4 = Alternative.withId(4);
		final Alternative a5 = Alternative.withId(5);
		final Voter v3 = Voter.withId(3);
		final Voter v4 = Voter.withId(4);
		final ImmutableList<Alternative> p = ImmutableList.of(a1, a2, a3, a4, a5);
		final PSRWeights weights = PSRWeights.given(ImmutableList.of(1d, 0.5d, 0.2d, 0.05d, 0d));
		final ImmutableMap<Voter, VoterStrictPreference> profile = ImmutableMap.of(v1,
				VoterStrictPreference.given(v1, p), v2, VoterStrictPreference.given(v2, p), v3,
				VoterStrictPreference.given(v3, p), v4, VoterStrictPreference.given(v4, p));
		final Oracle oracle = Oracle.build(profile, weights);
		
		UpdateablePreferenceKnowledge startingKnowledge = UpdateablePreferenceKnowledge.given(ImmutableSet.copyOf(p),
				ImmutableSet.of(v1, v2, v3, v4));
		
		addWeights(startingKnowledge, oracle);
		
		final SumTermsBuilder builder = SumTerms.builder();
		for (int r = 1; r <= 5; ++r) {
			final Term term = startingKnowledge.getConstraintsOnWeights().getTerm(5, r);
			builder.add(term);
		}
		final SumTerms sum = builder.build();
		
		startingKnowledge.getConstraintsOnWeights().maximize(sum);
		
		final PSRWeights w = startingKnowledge.getConstraintsOnWeights().getLastSolution();
		for (int r = 1; r <= 5; ++r) {
			assertEquals(weights.getWeightAtRank(r),w.getWeightAtRank(r),1e-5);
		}
		
	}

	void addWeights(UpdateablePreferenceKnowledge startingKnowledge, Oracle oracle) {
		PSRWeights weights = oracle.getWeights();
		int m = weights.size() - 2;
		for (int i = 1; i <= m; i++) {
			double w_i = weights.getWeightAtRank(i);
			double w_i1 = weights.getWeightAtRank(i + 1);
			double w_i2 = weights.getWeightAtRank(i + 2);
			Aprational a = new Aprational(w_i - w_i1);
			Aprational b = new Aprational(w_i1 - w_i2);
			Aprational lambda = a.divide(b);
			startingKnowledge.addConstraint(i, ComparisonOperator.EQ, lambda);
		}
	}

}
