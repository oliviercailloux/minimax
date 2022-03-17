package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Objects;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.experiment.json.AprationalAdapter;

/**
 * Immutable. A Question to the (chair of the) committee, with the form: is (w_r − w_{r+1}) ≥ λ
 * (w_{r+1} − w_{r+2})? With the convention that <em>r</em> equals one for the first rank.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
@JsonbPropertyOrder({"lambda", "rank"})
public class QuestionCommittee implements Comparable<QuestionCommittee> {

  @JsonbCreator
  public static QuestionCommittee given(@JsonbProperty("lambda") Aprational lambda,
      @JsonbProperty("rank") int rank) {
    return new QuestionCommittee(lambda, rank);
  }

  public static final Comparator<QuestionCommittee> BY_RANK_THEN_LAMBDA =
      Comparator.comparing(QuestionCommittee::getRank).thenComparing(QuestionCommittee::getLambda);

  private final Aprational lambda;
  private final int rank;

  private QuestionCommittee(Aprational lambda, int rank) {
    this.lambda = requireNonNull(lambda);
    /**
     * It is unfortunately possible to create an aprational with a null numerator.
     */
    checkArgument(lambda.numerator() != null);
    checkArgument(rank >= 1);
    this.rank = rank;
  }

  @JsonbTypeAdapter(AprationalAdapter.class)
  public Aprational getLambda() {
    return lambda;
  }

  /**
   * Returns <em>r</em>.
   *
   * @return ≥ 1.
   */
  public int getRank() {
    return rank;
  }

  @JsonbTransient
  public CommitteePreferenceInformation getPositiveInformation() {
    return CommitteePreferenceInformation.given(rank, ComparisonOperator.GE, lambda);
  }

  @JsonbTransient
  public CommitteePreferenceInformation getNegativeInformation() {
    return CommitteePreferenceInformation.given(rank, ComparisonOperator.LE, lambda);
  }

  @Override
  public int compareTo(QuestionCommittee q2) {
    return BY_RANK_THEN_LAMBDA.compare(this, q2);
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof QuestionCommittee)) {
      return false;
    }
    final QuestionCommittee q2 = (QuestionCommittee) o2;
    return Objects.equals(lambda, q2.lambda) && Objects.equals(rank, q2.rank);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lambda, rank);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("λ", lambda).add("rank", rank).toString();
  }
}
