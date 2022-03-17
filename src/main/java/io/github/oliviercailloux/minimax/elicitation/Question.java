package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Objects;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.VerifyException;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;

/**
 * Immutable.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class Question implements Comparable<Question> {

  public static Question toVoter(Voter voter, Alternative a, Alternative b) {
    return new Question(QuestionVoter.given(voter, a, b));
  }

  public static Question toVoter(QuestionVoter question) {
    return new Question(question);
  }

  public static Question toCommittee(Aprational lambda, int rank) {
    return new Question(QuestionCommittee.given(lambda, rank));
  }

  public static Question toCommittee(QuestionCommittee question) {
    return new Question(question);
  }

  public static Comparator<Question> TO_VOTER_FIRST = Comparator.naturalOrder();
  private static final Comparator<Question> BY_TYPE = Comparator.comparing(Question::getType);
  private static final Comparator<Question> BY_QUESTION_COMMITTEE =
      Comparator.comparing(Question::asQuestionCommittee);
  private static final Comparator<Question> BY_QUESTION_VOTER =
      Comparator.comparing(Question::asQuestionVoter);
  private final QuestionVoter qv;
  private final QuestionCommittee qc;

  private Question(QuestionVoter qv) {
    this.qv = checkNotNull(qv);
    this.qc = null;
  }

  private Question(QuestionCommittee qc) {
    this.qc = checkNotNull(qc);
    this.qv = null;
  }

  public QuestionVoter asQuestionVoter() {
    checkState(qc == null);
    verify(qv != null);
    return qv;
  }

  public QuestionCommittee asQuestionCommittee() {
    checkState(qv == null);
    verify(qc != null);
    return qc;
  }

  public QuestionType getType() {
    if (qc != null) {
      return QuestionType.COMMITTEE_QUESTION;
    }
    if (qv != null) {
      return QuestionType.VOTER_QUESTION;
    }
    throw new VerifyException();
  }

  public PreferenceInformation getPositiveInformation() {
    final PreferenceInformation information;
    switch (getType()) {
      case COMMITTEE_QUESTION:
        information = PreferenceInformation.aboutCommittee(qc.getPositiveInformation());
        break;
      case VOTER_QUESTION:
        information = PreferenceInformation.aboutVoter(qv.getPositiveInformation());
        break;
      default:
        throw new VerifyError();
    }
    return information;
  }

  public PreferenceInformation getNegativeInformation() {
    final PreferenceInformation information;
    switch (getType()) {
      case COMMITTEE_QUESTION:
        information = PreferenceInformation.aboutCommittee(qc.getNegativeInformation());
        break;
      case VOTER_QUESTION:
        information = PreferenceInformation.aboutVoter(qv.getNegativeInformation());
        break;
      default:
        throw new VerifyError();
    }
    return information;
  }

  @Override
  public int compareTo(Question q2) {
    final int compareTypes = BY_TYPE.compare(this, q2);
    if (compareTypes != 0) {
      return compareTypes;
    }

    switch (getType()) {
      case VOTER_QUESTION:
        return BY_QUESTION_VOTER.compare(this, q2);
      case COMMITTEE_QUESTION:
        return BY_QUESTION_COMMITTEE.compare(this, q2);
      default:
        throw new VerifyException();
    }
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Question)) {
      return false;
    }
    final Question q2 = (Question) o2;
    return Objects.equals(qv, q2.qv) && Objects.equals(qc, q2.qc);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qv, qc);
  }

  @Override
  public String toString() {
    final ToStringHelper helper = MoreObjects.toStringHelper(this);
    switch (getType()) {
      case COMMITTEE_QUESTION:
        helper.addValue(qc);
        break;
      case VOTER_QUESTION:
        helper.addValue(qv);
        break;
      default:
        throw new VerifyError();
    }
    return helper.toString();
  }
}
