package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.experiment.json.VoterAdapter;

/**
 * Immutable. A Question to a voter with the form: is alternative <em>a</em> preferred to
 * <em>b</em>? The alternatives <em>a</em> and <em>b</em> are different.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
@JsonbPropertyOrder({"voter", "alternatives"})
public class QuestionVoter implements Comparable<QuestionVoter> {

  @JsonbCreator
  public static QuestionVoter given(@JsonbProperty("voter") Voter voter,
      @JsonbProperty("alternatives") Set<Alternative> alternatives) {
    checkArgument(alternatives.size() == 2);
    final Iterator<Alternative> iterator = alternatives.iterator();
    return new QuestionVoter(voter, iterator.next(), iterator.next());
  }

  public static QuestionVoter given(Voter voter, Alternative a, Alternative b) {
    return new QuestionVoter(voter, a, b);
  }

  public static final Comparator<QuestionVoter> BY_VOTER_THEN_ALTERNATIVES = Comparator
      .comparing(QuestionVoter::getVoter).thenComparing(QuestionVoter::getFirstAlternative)
      .thenComparing(QuestionVoter::getSecondAlternative);

  @JsonbTypeAdapter(VoterAdapter.class)
  private final Voter voter;
  private final Alternative a, b;

  private QuestionVoter(Voter voter, Alternative a, Alternative b) {
    checkArgument(!a.equals(b));
    this.voter = checkNotNull(voter);
    this.a = checkNotNull(a);
    this.b = checkNotNull(b);
  }

  public Voter getVoter() {
    return this.voter;
  }

  public ImmutableSet<Alternative> getAlternatives() {
    return ImmutableSortedSet.orderedBy(Comparator.comparing(Alternative::getId)).add(a, b).build();
  }

  @JsonbTransient
  public Alternative getFirstAlternative() {
    return a;
  }

  @JsonbTransient
  public Alternative getSecondAlternative() {
    return b;
  }

  @JsonbTransient
  public VoterPreferenceInformation getPositiveInformation() {
    return VoterPreferenceInformation.given(voter, a, b);
  }

  @JsonbTransient
  public VoterPreferenceInformation getNegativeInformation() {
    return VoterPreferenceInformation.given(voter, b, a);
  }

  @Override
  public int compareTo(QuestionVoter q2) {
    return BY_VOTER_THEN_ALTERNATIVES.compare(this, q2);
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof QuestionVoter)) {
      return false;
    }
    final QuestionVoter q2 = (QuestionVoter) o2;
    return Objects.equals(voter, q2.voter)
        && Objects.equals(getAlternatives(), q2.getAlternatives());
  }

  @Override
  public int hashCode() {
    return Objects.hash(voter, getAlternatives());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("voter", voter)
        .add("alternatives", getAlternatives()).toString();
  }
}
