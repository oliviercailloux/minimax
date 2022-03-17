package io.github.oliviercailloux.minimax.elicitation;

import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;

public interface PreferenceKnowledge {

  /**
   * @return a non empty set.
   */
  public ImmutableSet<Alternative> getAlternatives();

  /**
   * @return a non-empty set.
   */
  public ImmutableSet<Voter> getVoters();

  /**
   * Adds the constraint: (w_i − w_{i+1}) OP λ (w_{i+1} − w_{i+2}).
   *
   * @param rank 1 ≤ rank ≤ m-2.
   * @param op the operator.
   * @param lambda a finite value.
   */

  public ImmutableMap<Voter, VoterPartialPreference> getProfile();

  public VoterPartialPreference getPartialPreference(Voter voter);

  public ConstraintsOnWeights getConstraintsOnWeights();

  public Range<Aprational> getLambdaRange(int rank);

  public boolean isProfileComplete();
}
