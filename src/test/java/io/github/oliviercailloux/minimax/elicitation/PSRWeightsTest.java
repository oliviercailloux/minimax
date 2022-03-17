package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.utils.Generator;

public class PSRWeightsTest {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(PSRWeightsTest.class);

  @Test
  public void testLambda() {
    Aprational a = new Aprational(new Apint(2), new Apint(3));
    QuestionCommittee qc = QuestionCommittee.given(a, 1);
    ComparisonOperator answ;
    PSRWeights weights = Generator.genWeightsWithUnbalancedDistribution(7);
    double left = (weights.getWeightAtRank(1) - weights.getWeightAtRank(2));
    double right = a.doubleValue() * (weights.getWeightAtRank(2) - weights.getWeightAtRank(3));
    if (left > right) {
      answ = ComparisonOperator.GE;
    } else if (left == right) {
      answ = ComparisonOperator.EQ;
    } else {
      answ = ComparisonOperator.LE;
    }
    LOGGER.debug("Weights: {}.", weights);
    LOGGER.debug(a.numerator() + " / " + a.denominator() + " = " + a.doubleValue());
    LOGGER.debug(left + " " + answ + " " + right);
    assertEquals(answ, weights.askQuestion(qc).getOperator());

  }
}
