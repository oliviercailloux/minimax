package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Range;

import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.Basics;

class PreferenceKnowledgeTest {

  @Test
  void testLambdaRange() throws Exception {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(5), Generator.getVoters(10));
    final Range<Aprational> startRange = k.getLambdaRange(1);
    assertEquals(1d, startRange.lowerEndpoint().doubleValue());
    final Aprational startUpper = startRange.upperEndpoint();
    final Apint ap1 = new Apint(1);
    final Apint ap2 = new Apint(2);
    final Apint ap3 = new Apint(3);
    k.addConstraint(1, ComparisonOperator.GE, ap2);
    assertEquals(Range.closed(ap2, startUpper), k.getLambdaRange(1));
    k.addConstraint(1, ComparisonOperator.LE, ap3);
    assertEquals(Range.closed(ap2, ap3), k.getLambdaRange(1));
    k.addConstraint(1, ComparisonOperator.LE, ap2);
    assertEquals(Range.closed(ap2, ap2), k.getLambdaRange(1));
    k.addConstraint(1, ComparisonOperator.LE, ap3);
    assertEquals(Range.closed(ap2, ap2), k.getLambdaRange(1));
    assertThrows(IllegalArgumentException.class,
        () -> k.addConstraint(1, ComparisonOperator.LE, ap1));
  }
}
