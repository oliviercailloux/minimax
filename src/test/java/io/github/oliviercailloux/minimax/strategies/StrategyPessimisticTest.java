package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;

public class StrategyPessimisticTest {

  @Test
  void testOneAlt() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(1), Generator.getVoters(1));
    final StrategyByMmr s = StrategyByMmr.build();
    assertThrows(IllegalArgumentException.class, () -> s.setKnowledge(k));
  }

  @Test
  void testTwoAltsOneVKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
    final StrategyByMmr s = StrategyByMmr.build();
    s.setKnowledge(k);
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    assertThrows(IllegalStateException.class, () -> s.nextQuestion());
  }

  @Test
  void testTwoAltsOneV() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
    final StrategyByMmr s = StrategyByMmr.build();
    s.setKnowledge(k);
    final Question q1 =
        Question.toVoter(Voter.withId(1), Alternative.withId(1), Alternative.withId(2));
    final Question q2 =
        Question.toVoter(Voter.withId(1), Alternative.withId(2), Alternative.withId(1));
    s.nextQuestion();
    assertEquals(ImmutableSet.of(q1, q2), s.getLastQuestions().keySet());
  }

  @Test
  void testTwoAltsTwoVsOneKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
    final StrategyByMmr s = StrategyByMmr.build();
    s.setKnowledge(k);
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    s.nextQuestion();
    final Question q1 =
        Question.toVoter(Voter.withId(2), Alternative.withId(1), Alternative.withId(2));
    final Question q2 =
        Question.toVoter(Voter.withId(2), Alternative.withId(2), Alternative.withId(1));
    assertEquals(ImmutableSet.of(q1, q2), s.getLastQuestions().keySet());
  }

  @Test
  void testTwoAltsTwoVs() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
    final StrategyByMmr s = StrategyByMmr.build();
    s.setKnowledge(k);
    s.nextQuestion();
    final Question q1 =
        Question.toVoter(Voter.withId(1), Alternative.withId(1), Alternative.withId(2));
    final Question q2 =
        Question.toVoter(Voter.withId(2), Alternative.withId(1), Alternative.withId(2));
    final Set<Question> expected = new HashSet<>();
    expected.add(q1);
    expected.add(q2);
    assertEquals(ImmutableSet.of(q1, q2), s.getLastQuestions().keySet());
    for (Question q : s.getLastQuestions().keySet()) {
      assertEquals(MmrLottery.given(0d, 0d), s.getLastQuestions().get(q));
    }
  }

  @Test
  void testThreeAltsTwoVs() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(2));
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(2),
        Alternative.withId(3));
    k.getProfile().get(Voter.withId(2)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    k.getProfile().get(Voter.withId(1)).setGraphChanged();
    StrategyByMmr s = StrategyByMmr.build();
    s.setKnowledge(k);
    s.nextQuestion();
    final Question q1 =
        Question.toVoter(Voter.withId(2), Alternative.withId(3), Alternative.withId(2));
    final Question q2 =
        Question.toVoter(Voter.withId(2), Alternative.withId(2), Alternative.withId(3));
    final Question q3 =
        Question.toVoter(Voter.withId(2), Alternative.withId(1), Alternative.withId(3));
    final Question q4 =
        Question.toVoter(Voter.withId(2), Alternative.withId(3), Alternative.withId(1));
    final Aprational ap = new Aprational(new Apint(3), new Apint(2));
    final Question q5 = Question.toCommittee(ap, 1);
    assertEquals(ImmutableSet.of(q1, q2, q3, q4, q5), s.getLastQuestions().keySet());
  }
}
