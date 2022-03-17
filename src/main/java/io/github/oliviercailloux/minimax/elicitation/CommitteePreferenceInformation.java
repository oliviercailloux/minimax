package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

/**
 * <p>
 * Immutable.
 * </p>
 * <p>
 * Represents the constraint: (w_i − w_{i+1}) OP λ (w_{i+1} − w_{i+2}).
 * </p>
 */
public class CommitteePreferenceInformation {

  public static CommitteePreferenceInformation given(int rank, ComparisonOperator op,
      Aprational lambda) {
    return new CommitteePreferenceInformation(rank, op, lambda);
  }

  private final int rank;
  private ComparisonOperator op;
  private final Aprational lambda;

  private CommitteePreferenceInformation(int rank, ComparisonOperator op, Aprational lambda) {
    checkArgument(rank >= 1);
    this.rank = rank;
    this.op = checkNotNull(op);
    this.lambda = requireNonNull(lambda);
  }

  /**
   * Returns <em>r</em>.
   *
   * @return ≥ 1.
   */
  public int getRank() {
    return rank;
  }

  public ComparisonOperator getOperator() {
    return op;
  }

  public Aprational getLambda() {
    return lambda;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof CommitteePreferenceInformation)) {
      return false;
    }
    final CommitteePreferenceInformation i2 = (CommitteePreferenceInformation) o2;
    return Objects.equals(rank, i2.rank) && Objects.equals(op, i2.op)
        && Objects.equals(lambda, i2.lambda);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rank, op, lambda);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("rank", rank).add("op", op).add("λ", lambda)
        .toString();
  }
}
