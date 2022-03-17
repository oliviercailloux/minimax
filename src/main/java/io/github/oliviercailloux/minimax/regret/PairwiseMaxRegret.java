package io.github.oliviercailloux.minimax.regret;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

/**
 * Immutable.
 *
 * @author Olivier Cailloux
 *
 */
public class PairwiseMaxRegret {
  public static double getScore(Map<Voter, Integer> ranks, PSRWeights weights) {
    return ranks.values().stream().mapToDouble(weights::getWeightAtRank).sum();
  }

  public static double getScore(Alternative alternative, VoterStrictPreference v,
      PSRWeights weights) {
    final int rank = v.getAlternativeRank(alternative);
    return weights.getWeightAtRank(rank);
  }

  public static double getScore(Alternative alternative, Map<Voter, VoterStrictPreference> profile,
      PSRWeights weights) {
    return profile.values().stream().mapToDouble((v) -> getScore(alternative, v, weights)).sum();
  }

  public static PairwiseMaxRegret given(Alternative x, Alternative y, Map<Voter, Integer> ranksOfX,
      Map<Voter, Integer> ranksOfY, PSRWeights weights) {
    return new PairwiseMaxRegret(x, y, ranksOfX, ranksOfY, weights,
        getScore(ranksOfY, weights) - getScore(ranksOfX, weights));
  }

  public static PairwiseMaxRegret given(Alternative x, Alternative y, Map<Voter, Integer> ranksOfX,
      Map<Voter, Integer> ranksOfY, PSRWeights weights, double pmrValue) {
    return new PairwiseMaxRegret(x, y, ranksOfX, ranksOfY, weights, pmrValue);
  }

  public static final Comparator<PairwiseMaxRegret> BY_VALUE =
      Comparator.comparingDouble(PairwiseMaxRegret::getPmrValue);
  public static final Comparator<PairwiseMaxRegret> BY_ALTERNATIVES =
      Comparator.comparing(PairwiseMaxRegret::getX).thenComparing(PairwiseMaxRegret::getY);
  /**
   * Useful when the pmr has been optained by computing, for example, (3 − 2) w1 + (4 − 1) w2, and
   * this differs slightly from (3 w1 + 4 w2) − (2 w1 + 1 w2).
   */
  private static final double IMPRECISION_TOLERATED = 1e-8;
  private Alternative x;

  private Alternative y;

  private ImmutableMap<Voter, Integer> ranksOfX;
  private ImmutableMap<Voter, Integer> ranksOfY;
  private PSRWeights weights;
  private double pmrValue;

  private PairwiseMaxRegret(Alternative x, Alternative y, Map<Voter, Integer> ranksOfX,
      Map<Voter, Integer> ranksOfY, PSRWeights weights, double pmrValue) {
    this.x = requireNonNull(x);
    this.y = requireNonNull(y);
    this.ranksOfX = ImmutableMap.copyOf(requireNonNull(ranksOfX));
    this.ranksOfY = ImmutableMap.copyOf(requireNonNull(ranksOfY));
    this.weights = requireNonNull(weights);
    this.pmrValue = pmrValue;
    checkArgument(Math.abs(pmrValue
        - (getScore(ranksOfY, weights) - getScore(ranksOfX, weights))) < IMPRECISION_TOLERATED);
    if (x.equals(y)) {
      checkArgument(ranksOfX.equals(ranksOfY),
          this.ranksOfX.toString() + ", " + this.ranksOfY.toString());
      checkArgument(pmrValue == 0d);
    }
  }

  public Alternative getX() {
    return x;
  }

  public Alternative getY() {
    return y;
  }

  public ImmutableMap<Voter, Integer> getRanksOfX() {
    return ranksOfX;
  }

  public ImmutableMap<Voter, Integer> getRanksOfY() {
    return ranksOfY;
  }

  public PSRWeights getWeights() {
    return weights;
  }

  public double getPmrValue() {
    return pmrValue;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof PairwiseMaxRegret)) {
      return false;
    }
    final PairwiseMaxRegret p2 = (PairwiseMaxRegret) o2;
    return x.equals(p2.x) && y.equals(p2.y) && ranksOfX.equals(p2.ranksOfX)
        && ranksOfY.equals(p2.ranksOfY) && weights.equals(p2.weights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, ranksOfX, ranksOfY, weights);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("x", x).add("y", y).add("ranks x", ranksOfX)
        .add("ranks y", ranksOfY).add("weights", weights).add("value", pmrValue).toString();
  }
}
