package io.github.oliviercailloux.minimax.strategies;

import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;

/**
 *
 * <p>
 * An instance of a strategy must return a next question deterministically, given a knowledge and
 * possibly depending on the number of questions it has asked so far (for example, a strategy may
 * systematically start with a fixed set of ten questions, then switch to an adapting behavior).
 * </p>
 *
 */
public interface Strategy {

  /**
   * @param knowledge must have at least two alternatives
   */
  public void setKnowledge(UpdateablePreferenceKnowledge knowledge);

  /**
   * Returns the next question that this strategy thinks is best asking. If the profile is complete
   * the strategy refines the scoring vector.
   *
   * @return a question.
   * @throws IllegalStateException if there are exactly two alternatives and the profile is
   *         complete.
   */
  public Question nextQuestion();
}
