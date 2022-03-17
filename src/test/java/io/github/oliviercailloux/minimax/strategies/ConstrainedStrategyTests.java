package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;

class ConstrainedStrategyTests {

  @Test
  void test() {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(1));
    final StrategyFactory factory = StrategyFactory.limitedCommitteeThenVoters(10);
    final Strategy twoPhase = factory.get();
    assertEquals("Limited (×1.1) MAX, constrained to [10c, ∞v]", factory.getDescription());
    twoPhase.setKnowledge(k);
    for (int i = 0; i < 10; ++i) {
      assertEquals(QuestionType.COMMITTEE_QUESTION, twoPhase.nextQuestion().getType(), "" + i);
    }
    for (int i = 0; i < 15; ++i) {
      assertEquals(QuestionType.VOTER_QUESTION, twoPhase.nextQuestion().getType());
    }
  }
}
