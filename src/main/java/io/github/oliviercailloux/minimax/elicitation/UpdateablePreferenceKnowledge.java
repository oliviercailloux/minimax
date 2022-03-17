package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

public class UpdateablePreferenceKnowledge implements PreferenceKnowledge {

  public static UpdateablePreferenceKnowledge given(Set<Alternative> alternatives,
      Set<Voter> voters) {
    return new UpdateablePreferenceKnowledge(alternatives, voters);
  }

  private ImmutableSet<Alternative> alternatives;
  private ImmutableMap<Voter, VoterPartialPreference> partialProfile;
  private ConstraintsOnWeights cow;
  private Map<Integer, Range<Aprational>> lambdaRanges;

  private UpdateablePreferenceKnowledge(Set<Alternative> alternatives, Set<Voter> voters) {
    this.alternatives = ImmutableSet.copyOf(alternatives);

    final int m = alternatives.size();
    final int n = voters.size();

    checkArgument(m >= 1);
    checkArgument(n >= 1);

    cow = ConstraintsOnWeights.withRankNumber(m);
    cow.setConvexityConstraint();

    final ImmutableMap.Builder<Voter, VoterPartialPreference> builder = ImmutableMap.builder();
    for (Voter voter : voters) {
      builder.put(voter, VoterPartialPreference.about(voter, alternatives));
    }
    partialProfile = builder.build();

    if (m == 1) {
      lambdaRanges = null;
    } else {
      lambdaRanges = new LinkedHashMap<>(m - 2);
      for (int rank = 1; rank <= m - 2; ++rank) {
        assert m - 2 >= 1;
        /**
         * When d_i/d_{i+1} > n−1, the rule is plurality-i-PD, meaning that when two alternatives
         * share the same number of times they reach rank j, for all j < i, then what counts is the
         * number of times the alternative reaches the rank i, and if ex-æquo, they are resolved
         * using the further weight constraints for lower ranks. Thus, it is unnecessary to
         * distinguish weights greater than n−1.
         *
         * To be more precise, it is useful to distinguish such great differences when giving those
         * weights a cardinal (utilitaristic) interpretation. But this interval seems like a
         * reasonable approximation.
         */
        lambdaRanges.put(rank, Range.closed(new Apint(1), new Apint(n)));
      }
    }
  }

  /**
   * @return a non empty set.
   */
  @Override
  public ImmutableSet<Alternative> getAlternatives() {
    return alternatives;
  }

  /**
   * @return a non-empty set.
   */
  @Override
  public ImmutableSet<Voter> getVoters() {
    return partialProfile.keySet();
  }

  /**
   * Adds the constraint: (w_i − w_{i+1}) OP λ (w_{i+1} − w_{i+2}).
   *
   * @param rank 1 ≤ rank ≤ m-2.
   * @param op the operator.
   * @param lambda a finite value.
   */
  public void addConstraint(int rank, ComparisonOperator op, Aprational lambda) {
    checkArgument(rank >= 1);
    checkArgument(rank <= alternatives.size() - 2);

    /** The constraint is that D_i/D_{i+1} OP lambda. */
    final Range<Aprational> providedRange;
    switch (op) {
      case EQ:
        providedRange = Range.closed(lambda, lambda);
        break;
      case GE:
        providedRange = Range.atLeast(lambda);
        break;
      case LE:
        providedRange = Range.atMost(lambda);
        break;
      default:
        throw new VerifyException();
    }
    final Range<Aprational> existingRange = lambdaRanges.get(rank);
    checkArgument(existingRange.isConnected(providedRange),
        "The provided constraint makes the program infeasible.");
    final Range<Aprational> restr = existingRange.intersection(providedRange);
    checkArgument(!restr.isEmpty(), "The provided constraint makes the program (just) infeasible.");

    cow.addConstraint(rank, op, lambda.doubleValue());
    lambdaRanges.put(rank, restr);
  }

  @Override
  public ImmutableMap<Voter, VoterPartialPreference> getProfile() {
    return partialProfile;
  }

  @Override
  public VoterPartialPreference getPartialPreference(Voter voter) {
    return partialProfile.get(voter);
  }

  @Override
  public ConstraintsOnWeights getConstraintsOnWeights() {
    return cow;
  }

  @Override
  public Range<Aprational> getLambdaRange(int rank) {
    checkArgument(rank >= 1);
    checkArgument(rank <= alternatives.size() - 2);
    return lambdaRanges.get(rank);
  }

  /**
   * Check the number of edges in the transitive graphs associated to each voter preference.
   */
  @Override
  public boolean isProfileComplete() {
    for (Voter voter : partialProfile.keySet()) {
      final Graph<Alternative> graph = getPartialPreference(voter).asTransitiveGraph();
      if (graph.edges().size() != alternatives.size() * (alternatives.size() - 1) / 2) {
        return false;
      }
    }
    return true;
  }

  public void update(PreferenceInformation information) {
    switch (information.getType()) {
      case VOTER_QUESTION:
        final VoterPreferenceInformation v = information.asVoterInformation();
        final Alternative better = v.getBetterAlternative();
        final Alternative worst = v.getWorstAlternative();
        final VoterPartialPreference voterPartialPreference = getProfile().get(v.getVoter());
        voterPartialPreference.asGraph().putEdge(better, worst);
        voterPartialPreference.setGraphChanged();
        break;
      case COMMITTEE_QUESTION:
        final CommitteePreferenceInformation c = information.asCommitteeInformation();
        final int rank = c.getRank();
        final ComparisonOperator op = c.getOperator();
        final Aprational lambda = c.getLambda();
        addConstraint(rank, op, lambda);
        break;
      default:
        throw new VerifyException();
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("Partial profile", partialProfile)
        .add("Lambda ranges", lambdaRanges).add("Constraints on weights", cow).toString();
  }
}
