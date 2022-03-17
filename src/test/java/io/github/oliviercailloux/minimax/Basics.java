package io.github.oliviercailloux.minimax;

import org.apfloat.Apint;
import org.apfloat.Aprational;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;
import io.github.oliviercailloux.minimax.strategies.QuestioningConstraint;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class Basics {

  public static final Alternative a1 = Alternative.withId(1);
  public static final Alternative a2 = Alternative.withId(2);
  public static final Alternative a3 = Alternative.withId(3);
  public static final Voter v1 = Voter.withId(1);
  public static final Voter v2 = Voter.withId(2);
  public static final ImmutableList<Alternative> p1 = ImmutableList.of(a1, a2, a3);
  public static final ImmutableList<Alternative> p2 = ImmutableList.of(a3, a2, a1);
  public static final PSRWeights w = PSRWeights.given(ImmutableList.of(1d, 0.4d, 0d));
  public static final ImmutableMap<Voter, VoterStrictPreference> profile = ImmutableMap.of(v1,
      VoterStrictPreference.given(v1, p1), v2, VoterStrictPreference.given(v2, p2));
  public static final Oracle oracle = Oracle.build(profile, w);
  public static final Question q1 = Question.toVoter(v1, a1, a2);
  public static final Question q2 =
      Question.toCommittee(new Aprational(new Apint(1), new Apint(2)), 1);
  public static final Run run =
      Run.of(oracle, ImmutableList.of(10l, 18l), ImmutableList.of(q1, q2), 20l);
  public static final QuestioningConstraint vConstraint =
      QuestioningConstraint.of(QuestionType.VOTER_QUESTION, 1);
  public static final QuestioningConstraint cConstraint =
      QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION, Integer.MAX_VALUE);
  public static final StrategyFactory factory =
      StrategyFactory.limited(100l, ImmutableList.of(vConstraint, cConstraint));
  public static final Runs runs = Runs.of(factory, ImmutableList.of(run, run));
}
