package io.github.oliviercailloux.minimax.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

public class GeneratorTest {

  @Test
  public void testGenWeightsEquallySpread() {
    int m = 9;
    final PSRWeights weights = Generator.genWeightsEquallySpread(m);
    final ImmutableList<Double> weightsList = weights.getWeights();
    double current = 1d;
    for (double weight : weightsList) {
      assertEquals(current, weight, 1e-10d);
      current -= 1d / 8d;
    }
  }

  @Test
  public void testGenWeightsGeometric() {
    int m = 4;
    final PSRWeights weights = Generator.genWeightsGeometric(m);
    final ImmutableList<Double> weightsList = weights.getWeights();
    final UnmodifiableIterator<Double> iterator = weightsList.iterator();
    /** Expected: d1 = 4/7, d2 = 2/7, d1 = 1/7. */
    assertEquals(1d, iterator.next(), 1e-10d);
    assertEquals(3d / 7d, iterator.next(), 1e-10d);
    assertEquals(1d / 7d, iterator.next(), 1e-10d);
    assertEquals(0d, iterator.next(), 1e-10d);
  }

  @Test
  public void testGenWeights() {
    int m = 9;
    final PSRWeights weights = Generator.genWeightsWithUnbalancedDistribution(m);
    assertEquals(m, weights.getWeights().size());
  }

  @Test
  public void testGenProfile() {
    int m = 3;
    int n = 2;
    final Map<Voter, VoterStrictPreference> rv = Generator.genProfile(m, n);

    final List<Alternative> alt = new LinkedList<>();
    for (int i = 1; i <= m; i++) {
      alt.add(Alternative.withId(i));
    }

    assertEquals(n, rv.size());
    for (VoterStrictPreference vpref : rv.values()) {
      assertEquals(m, vpref.asStrictPreference().getAlternatives().size());
      assertTrue(vpref.asStrictPreference().getAlternatives().containsAll(alt));
    }

  }
}
