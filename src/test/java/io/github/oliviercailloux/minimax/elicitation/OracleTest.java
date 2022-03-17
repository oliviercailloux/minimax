package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apfloat.Apint;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

class OracleTest {

  @Test
  void test() {
    final Voter v1 = Voter.withId(1);
    final Voter v2 = Voter.withId(2);
    final Alternative a1 = Alternative.withId(1);
    final Alternative a2 = Alternative.withId(2);
    final ImmutableList<Alternative> a1List = ImmutableList.of(a1);
    final ImmutableList<Alternative> a2List = ImmutableList.of(a2);
    final VoterStrictPreference v1PrefA1 = VoterStrictPreference.given(v1, a1List);
    final VoterStrictPreference v2PrefA1 = VoterStrictPreference.given(v2, a1List);
    final VoterStrictPreference v2PrefA2 = VoterStrictPreference.given(v2, a2List);

    final Oracle oracle1 =
        Oracle.build(ImmutableMap.of(v1, v1PrefA1), PSRWeights.given(ImmutableList.of(1d)));
    assertEquals(ImmutableSet.of(a1), oracle1.getAlternatives());

    final Oracle oracle2 = Oracle.build(ImmutableMap.of(v1, v1PrefA1, v2, v2PrefA1),
        PSRWeights.given(ImmutableList.of(1d)));
    assertEquals(ImmutableSet.of(a1), oracle2.getAlternatives());

    assertThrows(IllegalArgumentException.class,
        () -> Oracle.build(ImmutableMap.of(v1, v1PrefA1, v2, v2PrefA2),
            PSRWeights.given(ImmutableList.of(1d))));
  }

  @Test
  void testJustConvex() throws Exception {
    final Voter v1 = Voter.withId(1);
    final Alternative a1 = Alternative.withId(1);
    final Alternative a2 = Alternative.withId(2);
    final Alternative a3 = Alternative.withId(3);
    final ImmutableList<Alternative> pref1 = ImmutableList.of(a1, a2, a3);
    final Oracle oracle = Oracle.build(ImmutableMap.of(v1, VoterStrictPreference.given(v1, pref1)),
        PSRWeights.given(ImmutableList.of(1d, 0.5d, 0d)));
    assertEquals(ComparisonOperator.EQ,
        oracle.getPreferenceInformation(Question.toCommittee(new Apint(1), 1))
            .asCommitteeInformation().getOperator());
  }
}
