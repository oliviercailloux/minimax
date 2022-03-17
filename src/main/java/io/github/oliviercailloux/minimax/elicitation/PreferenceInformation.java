package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Objects;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

/**
 *
 * <p>
 * An information about our knowledge of the preferences of the voters or of the committee.
 * Typically obtained by asking a question.
 * </p>
 * <p>
 * Immutable.
 * </p>
 */
public class PreferenceInformation {

  private final VoterPreferenceInformation v;
  private final CommitteePreferenceInformation c;

  private PreferenceInformation(VoterPreferenceInformation v) {
    this.v = checkNotNull(v);
    this.c = null;
  }

  private PreferenceInformation(CommitteePreferenceInformation information) {
    this.c = checkNotNull(information);
    this.v = null;
  }

  public static PreferenceInformation aboutVoter(Voter voter, Alternative best, Alternative worst) {
    return new PreferenceInformation(VoterPreferenceInformation.given(voter, best, worst));
  }

  public static PreferenceInformation aboutVoter(VoterPreferenceInformation information) {
    return new PreferenceInformation(information);
  }

  public static PreferenceInformation aboutCommittee(int rank, ComparisonOperator op,
      Aprational lambda) {
    return new PreferenceInformation(CommitteePreferenceInformation.given(rank, op, lambda));
  }

  public static PreferenceInformation aboutCommittee(CommitteePreferenceInformation information) {
    return new PreferenceInformation(information);
  }

  public VoterPreferenceInformation asVoterInformation() {
    checkState(c == null);
    verify(v != null);
    return v;
  }

  public CommitteePreferenceInformation asCommitteeInformation() {
    checkState(v == null);
    verify(c != null);
    return c;
  }

  public QuestionType getType() {
    if (v == null) {
      return QuestionType.COMMITTEE_QUESTION;
    }
    return QuestionType.VOTER_QUESTION;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof PreferenceInformation)) {
      return false;
    }
    final PreferenceInformation i2 = (PreferenceInformation) o2;
    return Objects.equals(v, i2.v) && Objects.equals(c, i2.c);
  }

  @Override
  public int hashCode() {
    return Objects.hash(v, c);
  }

  @Override
  public String toString() {
    final ToStringHelper helper = MoreObjects.toStringHelper(this);
    switch (getType()) {
      case COMMITTEE_QUESTION:
        helper.addValue(c);
        break;
      case VOTER_QUESTION:
        helper.addValue(v);
        break;
      default:
        throw new AssertionError();
    }
    return helper.toString();
  }
}
