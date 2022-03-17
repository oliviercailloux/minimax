package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Range;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;

public class ConstraintsOnWeightsTest {
  @Test
  void testOneC() throws Exception {
    final ConstraintsOnWeights cow = ConstraintsOnWeights.withRankNumber(3);
    /** (w1 − w2) ≥ 3(w2 − w3) thus w1 + 3 w3 ≥ 4 w2 thus w2 ≤ 1/4. **/
    cow.addConstraint(1, ComparisonOperator.GE, 3d);
    assertThrows(IllegalArgumentException.class, () -> cow.getWeightRange(0));
    assertEquals(Range.closed(1d, 1d), cow.getWeightRange(1));
    assertEquals(Range.closed(0d, 0.25d), cow.getWeightRange(2));
    assertEquals(Range.closed(0d, 0d), cow.getWeightRange(3));
  }

  @Test
  void testMax() throws Exception {
    final ConstraintsOnWeights cow = ConstraintsOnWeights.withRankNumber(6);
    SumTermsBuilder sb = SumTerms.builder();
    sb.add(cow.getTerm(1d, 1));
    sb.add(cow.getTerm(-2d, 3));
    sb.add(cow.getTerm(1d, 5));
    final SumTerms objective = sb.build();
    assertEquals(2d, cow.maximize(objective));

    cow.setConvexityConstraint();
    /**
     * Because of the epsilon margins, we have w3 ≥ 3 epsilon, thus the objective is off by about 6
     * epsilons.
     */
    assertEquals(1d, cow.maximize(objective), 1E-4);
    assertEquals(1d, cow.getLastSolution().getWeightAtRank(1), 1E-4);
    assertEquals(0d, cow.getLastSolution().getWeightAtRank(3), 1E-4);
    assertEquals(0d, cow.getLastSolution().getWeightAtRank(5));
  }

  @Test
  void testMax1() throws Exception {
    final ConstraintsOnWeights cow = ConstraintsOnWeights.withRankNumber(2);
    assertEquals(2d, cow.maximize(SumTerms.of(cow.getTerm(2d, 1), cow.getTerm(3d, 2))));
  }
}
