package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;

class StrategyRandomTest {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(StrategyRandomTest.class);

  @Test
  void testOneAlt() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(1), Generator.getVoters(1));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    assertThrows(IllegalArgumentException.class, () -> s.setKnowledge(k));
  }

  @Test
  void testTwoAltsOneVKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    s.setKnowledge(k);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    assertThrows(IllegalStateException.class, () -> s.nextQuestion());
  }

  @Test
  void testTwoAltsOneV() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    s.setKnowledge(k);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    assertEquals(Question.toVoter(Voter.withId(1), Alternative.withId(1), Alternative.withId(2)),
        s.nextQuestion());
  }

  @Test
  void testTwoAltsTwoVsOneKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    s.setKnowledge(k);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    k.getProfile().get(Voter.withId(1)).asGraph().putEdge(Alternative.withId(1),
        Alternative.withId(2));
    assertEquals(Question.toVoter(Voter.withId(2), Alternative.withId(1), Alternative.withId(2)),
        s.nextQuestion());
  }

  @Test
  void testFourAltsTwoVKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(4), Generator.getVoters(2));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    s.setKnowledge(k);
    final MutableGraph<Alternative> g1 = k.getProfile().get(Voter.withId(1)).asGraph();
    g1.putEdge(Alternative.withId(1), Alternative.withId(2));
    g1.putEdge(Alternative.withId(2), Alternative.withId(3));
    g1.putEdge(Alternative.withId(3), Alternative.withId(4));

    final MutableGraph<Alternative> g2 = k.getProfile().get(Voter.withId(2)).asGraph();
    g2.putEdge(Alternative.withId(1), Alternative.withId(2));
    g2.putEdge(Alternative.withId(2), Alternative.withId(3));
    g2.putEdge(Alternative.withId(3), Alternative.withId(4));
    assertEquals(Question.toCommittee(new Aprational(new Apint(3), new Apint(2)), 2),
        s.nextQuestion());
  }

  @Test
  void testThreeAltsTwoVKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(2));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    s.setKnowledge(k);
    final MutableGraph<Alternative> g1 = k.getProfile().get(Voter.withId(1)).asGraph();
    g1.putEdge(Alternative.withId(1), Alternative.withId(2));
    g1.putEdge(Alternative.withId(2), Alternative.withId(3));
    final MutableGraph<Alternative> g2 = k.getProfile().get(Voter.withId(2)).asGraph();
    g2.putEdge(Alternative.withId(1), Alternative.withId(2));
    g2.putEdge(Alternative.withId(2), Alternative.withId(3));
    assertEquals(Question.toCommittee(new Aprational(new Apint(3), new Apint(2)), 1),
        s.nextQuestion());
  }

  @Test
  void testThreeAltsTwoVAllKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(2));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    s.setKnowledge(k);
    final MutableGraph<Alternative> g1 = k.getProfile().get(Voter.withId(1)).asGraph();
    g1.putEdge(Alternative.withId(1), Alternative.withId(2));
    g1.putEdge(Alternative.withId(2), Alternative.withId(3));
    final MutableGraph<Alternative> g2 = k.getProfile().get(Voter.withId(2)).asGraph();
    g2.putEdge(Alternative.withId(1), Alternative.withId(2));
    g2.putEdge(Alternative.withId(2), Alternative.withId(3));
    k.addConstraint(1, ComparisonOperator.EQ, new Apint(1));
    assertTrue(k.isProfileComplete());
    assertTrue(s.nextQuestion().getType() == QuestionType.COMMITTEE_QUESTION);
    // prof complete and next qst is committee
  }

  @Test
  void testThreeAltsOneVKnown() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(1));
    final StrategyRandom s = StrategyRandom.newInstance(0.5d);
    final Random notRandom = new Random(0);
    s.setRandom(notRandom);
    s.setKnowledge(k);
    final MutableGraph<Alternative> g = k.getProfile().get(Voter.withId(1)).asGraph();
    g.putEdge(Alternative.withId(1), Alternative.withId(2));
    g.putEdge(Alternative.withId(2), Alternative.withId(3));
    assertTrue(k.isProfileComplete());
    assertTrue(s.nextQuestion().getType() == QuestionType.COMMITTEE_QUESTION);
    // prof complete and next qst is committee
  }
}
