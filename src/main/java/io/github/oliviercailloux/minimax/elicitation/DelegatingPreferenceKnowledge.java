package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;

public class DelegatingPreferenceKnowledge implements PreferenceKnowledge {

  final private UpdateablePreferenceKnowledge prefKnowledge;
  final private PreferenceInformation newInformation;
  private ImmutableMap<Voter, VoterPartialPreference> newProfile;

  public static DelegatingPreferenceKnowledge given(UpdateablePreferenceKnowledge prefKnowledge,
      PreferenceInformation newInfo) {
    checkNotNull(prefKnowledge);
    checkNotNull(newInfo);
    return new DelegatingPreferenceKnowledge(prefKnowledge, newInfo);
  }

  private DelegatingPreferenceKnowledge(UpdateablePreferenceKnowledge knowledge,
      PreferenceInformation newInfo) {
    prefKnowledge = knowledge;
    newInformation = newInfo;
    newProfile = null;
  }

  @Override
  public ImmutableSet<Alternative> getAlternatives() {
    return prefKnowledge.getAlternatives();
  }

  @Override
  public ImmutableSet<Voter> getVoters() {
    return prefKnowledge.getVoters();
  }

  @Override
  public ImmutableMap<Voter, VoterPartialPreference> getProfile() {
    if (newProfile == null) {
      if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
        return prefKnowledge.getProfile();

      final ImmutableMap.Builder<Voter, VoterPartialPreference> builder = ImmutableMap.builder();
      for (Voter voter : prefKnowledge.getVoters()) {
        builder.put(voter, getPartialPreference(voter));
      }
      newProfile = builder.build();
    }
    return newProfile;
  }

  @Override
  public VoterPartialPreference getPartialPreference(Voter voter) {
    if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
      return prefKnowledge.getPartialPreference(voter);

    VoterPreferenceInformation newVotPref = newInformation.asVoterInformation();
    if (!voter.equals(newVotPref.getVoter()))
      return prefKnowledge.getPartialPreference(voter);

    VoterPartialPreference partialPref =
        VoterPartialPreference.copyOf(prefKnowledge.getPartialPreference(voter));
    partialPref.asGraph().putEdge(newVotPref.getBetterAlternative(),
        newVotPref.getWorstAlternative());
    return partialPref;
  }

  @Override
  public ConstraintsOnWeights getConstraintsOnWeights() {
    return prefKnowledge.getConstraintsOnWeights();
  }

  @Override
  public Range<Aprational> getLambdaRange(int rank) {
    return prefKnowledge.getLambdaRange(rank);
  }

  @Override
  public boolean isProfileComplete() {
    if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
      return prefKnowledge.isProfileComplete();

    int m = prefKnowledge.getAlternatives().size();

    for (Voter voter : prefKnowledge.getProfile().keySet()) {
      final Graph<Alternative> graph = getPartialPreference(voter).asTransitiveGraph();
      if (graph.edges().size() != m * (m - 1) / 2) {
        return false;
      }
    }
    return true;
  }
}
