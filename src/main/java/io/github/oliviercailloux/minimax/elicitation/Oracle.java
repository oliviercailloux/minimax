package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.experiment.json.WeightsAdapter;

/**
 * Immutable. Contains at least one voter (otherwise it would be impossible to provide a list of
 * alternatives).
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
@JsonbPropertyOrder({"profile", "weights"})
public class Oracle {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Oracle.class);

  /**
   * @param profile is a list so that JsonB will keep read ordering. Should contain no duplicate.
   */
  @JsonbCreator
  public static Oracle build(@JsonbProperty("profile") List<VoterStrictPreference> profile,
      @JsonbProperty("weights") PSRWeights weights) {
    LOGGER.debug("Received profiles: {}, weights: {}.", profile, weights);
    final ImmutableSet<VoterStrictPreference> profileSet = ImmutableSet.copyOf(profile);
    checkArgument(profile.size() == profileSet.size());
    return new Oracle(profileSet, weights);
  }

  public static Oracle build(Map<Voter, VoterStrictPreference> profile, PSRWeights weights) {
    checkArgument(
        profile.entrySet().stream().allMatch((e) -> e.getValue().getVoter().equals(e.getKey())));
    return new Oracle(ImmutableSet.copyOf(profile.values()), weights);
  }

  private final ImmutableMap<Voter, VoterStrictPreference> profile;
  @JsonbTypeAdapter(WeightsAdapter.class)
  private final PSRWeights weights;
  @JsonbTransient
  private final ImmutableSortedSet<Alternative> alternatives;

  private Oracle(Set<VoterStrictPreference> profile, PSRWeights weights) {
    checkArgument(profile.size() >= 1);
    this.profile = profile.stream()
        .collect(ImmutableMap.toImmutableMap(VoterStrictPreference::getVoter, p -> p));
    this.weights = checkNotNull(weights);

    final ImmutableSet<Set<Alternative>> allAlternativeSets =
        profile.stream().map(VoterStrictPreference::getAlternatives).map(ImmutableSet::copyOf)
            .collect(ImmutableSet.toImmutableSet());
    checkArgument(allAlternativeSets.size() == 1, allAlternativeSets);
    this.alternatives = ImmutableSortedSet.copyOf(Comparator.comparingInt(Alternative::getId),
        Iterables.getOnlyElement(allAlternativeSets));

    final int nbAlts = alternatives.size();
    checkArgument(weights.size() == nbAlts);
  }

  public PreferenceInformation getPreferenceInformation(Question q) {
    switch (q.getType()) {
      case VOTER_QUESTION: {
        QuestionVoter qv = q.asQuestionVoter();
        Voter v = qv.getVoter();
        checkArgument(profile.containsKey(v));
        VoterStrictPreference vsp = profile.get(v);
        return PreferenceInformation.aboutVoter(vsp.askQuestion(qv));
      }
      case COMMITTEE_QUESTION: {
        QuestionCommittee qc = q.asQuestionCommittee();
        return PreferenceInformation.aboutCommittee(weights.askQuestion(qc));
      }
      default:
        throw new VerifyException();
    }
  }

  public ImmutableMap<Voter, VoterStrictPreference> getProfile() {
    return profile;
  }

  /**
   * Returns the alternatives this profile is about. The size of the weights vector is also the size
   * of the returned set.
   *
   * @return the alternatives all voters’ preferences are about.
   */
  public ImmutableSet<Alternative> getAlternatives() {
    return alternatives;
  }

  public PSRWeights getWeights() {
    return weights;
  }

  @JsonbTransient
  public int getM() {
    return alternatives.size();
  }

  @JsonbTransient
  public int getN() {
    return profile.size();
  }

  public double getScore(Alternative x) {
    final ImmutableCollection<VoterStrictPreference> preferences = profile.values();
    return preferences.stream().mapToDouble(p -> weights.getWeightAtRank(p.getAlternativeRank(x)))
        .sum();
  }

  @JsonbTransient
  public double getBestScore() {
    return alternatives.stream().mapToDouble(this::getScore).max().getAsDouble();
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Oracle)) {
      return false;
    }

    final Oracle or2 = (Oracle) o2;
    return profile.equals(or2.profile) && weights.equals(or2.weights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profile, weights);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("profile", profile).add("weights", weights)
        .toString();
  }
}
