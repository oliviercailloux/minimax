package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.PreferenceInformation;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.utils.Generator;

public class StrategyElitistTests {
  @Test
  void testQuestions() throws Exception {
    final StrategyElitist strategy = StrategyElitist.newInstance();
    final Oracle oracle = Generator.generateOracle(4, 2);
    final UpdateablePreferenceKnowledge knowledge =
        UpdateablePreferenceKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
    strategy.setKnowledge(knowledge);
    final Alternative a1 = Alternative.withId(1);
    final Alternative a2 = Alternative.withId(2);
    final Alternative a3 = Alternative.withId(3);
    final Alternative a4 = Alternative.withId(4);
    final Voter v1 = Voter.withId(1);
    final Voter v2 = Voter.withId(2);
    assertEquals(Question.toVoter(v1, a1, a2), strategy.nextQuestion());
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(1), a1, a2));
    assertEquals(Question.toVoter(v1, a1, a3), strategy.nextQuestion());
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(1), a3, a1));
    assertEquals(Question.toVoter(v1, a3, a4), strategy.nextQuestion());
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(1), a4, a3));
    assertEquals(Question.toVoter(v2, a1, a2), strategy.nextQuestion());
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(2), a1, a2));
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(2), a1, a3));
    knowledge.update(PreferenceInformation.aboutVoter(Voter.withId(2), a1, a4));
    assertTrue(strategy.nextQuestion().getType() == QuestionType.COMMITTEE_QUESTION);
  }
}
