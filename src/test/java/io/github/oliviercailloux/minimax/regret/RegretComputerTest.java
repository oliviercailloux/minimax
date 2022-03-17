package io.github.oliviercailloux.minimax.regret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;

class RegretComputerTest {

  @Test
  void testPmrValues() {
    final Alternative a = Alternative.withId(1);
    final Alternative b = Alternative.withId(2);
    final Voter v1 = Voter.withId(1);
    final Voter v2 = Voter.withId(2);
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(ImmutableSet.of(a, b), ImmutableSet.of(v1, v2));
    final RegretComputer regretComputer = new RegretComputer(k);

    final ImmutableMap<Voter, Integer> allSecond = ImmutableMap.of(v1, 2, v2, 2);
    final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1, v2, 1);
    final PairwiseMaxRegret pmrAVsB = PairwiseMaxRegret.given(a, b, allSecond, allFirst,
        PSRWeights.given(ImmutableList.of(1d, 0d)));
    final PairwiseMaxRegret pmrBVsA = PairwiseMaxRegret.given(b, a, allSecond, allFirst,
        PSRWeights.given(ImmutableList.of(1d, 0d)));

    SetMultimap<Alternative, PairwiseMaxRegret> pmrValues =
        regretComputer.getMinimalMaxRegrets().asMultimap();

    assertTrue(pmrValues.keySet().size() == 2);
    assertTrue(pmrValues.get(a).size() == 1);
    assertTrue(pmrValues.get(b).size() == 1);
    assertTrue(pmrValues.get(a).contains(pmrAVsB));
    assertTrue(pmrValues.get(b).contains(pmrBVsA));
  }

  @Test
  void testEmptyKSizeOne() {
    final Alternative a = Alternative.withId(1);
    final Voter v1 = Voter.withId(1);
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(ImmutableSet.of(a), ImmutableSet.of(v1));
    final RegretComputer regretComputer = new RegretComputer(k);
    final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1);
    final PairwiseMaxRegret pmrAVsA =
        PairwiseMaxRegret.given(a, a, allFirst, allFirst, PSRWeights.given(ImmutableList.of(1d)));

    assertEquals(ImmutableSet.of(pmrAVsA),
        regretComputer.getMinimalMaxRegrets().asMultimap().get(a));
  }

  @Test
  void testEmptyKSizeTwo() {
    final Alternative a = Alternative.withId(1);
    final Alternative b = Alternative.withId(2);
    final Voter v1 = Voter.withId(1);
    final Voter v2 = Voter.withId(2);
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(ImmutableSet.of(a, b), ImmutableSet.of(v1, v2));
    final RegretComputer regretComputer = new RegretComputer(k);
    final ImmutableMap<Voter, Integer> allSecond = ImmutableMap.of(v1, 2, v2, 2);
    final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1, v2, 1);
    final PairwiseMaxRegret pmrAVsB = PairwiseMaxRegret.given(a, b, allSecond, allFirst,
        PSRWeights.given(ImmutableList.of(1d, 0d)));
    final PairwiseMaxRegret pmrBVsA = PairwiseMaxRegret.given(b, a, allSecond, allFirst,
        PSRWeights.given(ImmutableList.of(1d, 0d)));

    assertEquals(ImmutableSet.of(pmrAVsB),
        regretComputer.getMinimalMaxRegrets().asMultimap().get(a));
    assertEquals(ImmutableSet.of(pmrBVsA),
        regretComputer.getMinimalMaxRegrets().asMultimap().get(b));
  }

  @Test
  void testMMR() throws Exception {
    /** Test with complete knowledge about voters' preferences **/
    Voter v1 = Voter.withId(1);
    Voter v2 = Voter.withId(2);
    Voter v3 = Voter.withId(3);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);
    voters.add(v2);
    voters.add(v3);

    Alternative a = Alternative.withId(1);
    Alternative b = Alternative.withId(2);
    Alternative c = Alternative.withId(3);
    Alternative d = Alternative.withId(4);
    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref1 = knowledge.getProfile().get(v1).asGraph();
    pref1.putEdge(a, b);
    pref1.putEdge(b, c);
    pref1.putEdge(c, d);

    MutableGraph<Alternative> pref2 = knowledge.getProfile().get(v2).asGraph();
    pref2.putEdge(d, a);
    pref2.putEdge(a, b);
    pref2.putEdge(b, c);

    MutableGraph<Alternative> pref3 = knowledge.getProfile().get(v3).asGraph();
    pref3.putEdge(c, a);
    pref3.putEdge(a, b);
    pref3.putEdge(b, d);

    final RegretComputer regretComputer = new RegretComputer(knowledge);

    SetMultimap<Alternative, PairwiseMaxRegret> mrs =
        regretComputer.getMinimalMaxRegrets().asMultimap();
    assertEquals(ImmutableSet.of(a), mrs.keySet());
    Set<PairwiseMaxRegret> pmrs = mrs.get(a);

    final ImmutableSet<Alternative> ys =
        pmrs.stream().map((p) -> p.getY()).collect(ImmutableSet.toImmutableSet());
    assertTrue(ys.contains(a));
    assertEquals(ImmutableSet.of(a, c, d),
        regretComputer.getAllPairwiseMaxRegrets().getMinimalMaxRegrets(1E-4).get(a).stream()
            .map((p) -> p.getY()).collect(ImmutableSet.toImmutableSet()));
  }

  @Test
  void testRanksXpreferredY() throws Exception {
    /** case 1: x>y put as much alts as possible above x **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);
    Alternative a1 = Alternative.withId(9);
    Alternative b1 = Alternative.withId(10);
    Alternative c1 = Alternative.withId(11);
    Alternative d1 = Alternative.withId(12);
    Alternative u1 = Alternative.withId(13);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);
    alt.add(a1);
    alt.add(b1);
    alt.add(c1);
    alt.add(d1);
    alt.add(u1);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.putEdge(a, x);
    pref.putEdge(x, b);
    pref.putEdge(x, d);
    pref.putEdge(b, y);
    pref.putEdge(c, y);
    pref.putEdge(y, f);
    pref.addNode(u);
    pref.putEdge(a1, a);
    pref.putEdge(c1, c);
    pref.putEdge(x, d1);
    pref.putEdge(b1, y);
    pref.putEdge(b, b1);
    pref.putEdge(a1, u1);

    final RegretComputer regretComputer = new RegretComputer(knowledge);
    assertEquals(7, regretComputer.getWorstRankOfX(x, knowledge.getPartialPreference(v1)));
    assertEquals(10, regretComputer.getBestRankOfY(x, y, knowledge.getPartialPreference(v1)));
    assertEquals(7, regretComputer.getBestRankOfY(x, x, knowledge.getPartialPreference(v1)));
  }

  @Test
  void testRanksXprefY() throws Exception {
    /** case 1: x>y put as much alts as possible above x **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.putEdge(a, x);
    pref.putEdge(x, b);
    pref.putEdge(x, d);
    pref.putEdge(b, y);
    pref.putEdge(c, y);
    pref.putEdge(y, f);
    pref.addNode(u);

    final RegretComputer regretComputer = new RegretComputer(knowledge);
    assertEquals(4, regretComputer.getWorstRankOfX(x, knowledge.getPartialPreference(v1)));
    assertEquals(6, regretComputer.getBestRankOfY(x, y, knowledge.getPartialPreference(v1)));
    assertEquals(4, regretComputer.getBestRankOfY(x, x, knowledge.getPartialPreference(v1)));
  }

  @Test
  void TestRanksYPreferredX() throws Exception {
    /** case 2: y>x put as much alts as possible in between **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);
    Alternative a1 = Alternative.withId(9);
    Alternative b1 = Alternative.withId(10);
    Alternative c1 = Alternative.withId(11);
    Alternative d1 = Alternative.withId(12);
    Alternative u1 = Alternative.withId(13);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);
    alt.add(a1);
    alt.add(b1);
    alt.add(c1);
    alt.add(d1);
    alt.add(u1);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.putEdge(a, y);
    pref.putEdge(y, b);
    pref.putEdge(y, d);
    pref.putEdge(b, x);
    pref.putEdge(c, x);
    pref.putEdge(x, f);
    pref.addNode(u);
    pref.putEdge(a1, a);
    pref.putEdge(c1, c);
    pref.putEdge(x, d1);
    pref.putEdge(b1, x);
    pref.putEdge(b, b1);
    pref.putEdge(a1, u1);

    final RegretComputer regretComputer = new RegretComputer(knowledge);
    assertEquals(11, regretComputer.getWorstRankOfX(x, knowledge.getPartialPreference(v1)));
    assertEquals(3, regretComputer.getBestRankOfY(x, y, knowledge.getPartialPreference(v1)));
    assertEquals(11, regretComputer.getBestRankOfY(x, x, knowledge.getPartialPreference(v1)));
  }

  @Test
  void TestRanksYPrefX() throws Exception {
    /** case 2: y>x put as much alts as possible in between **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.putEdge(a, y);
    pref.putEdge(y, b);
    pref.putEdge(y, d);
    pref.putEdge(b, x);
    pref.putEdge(c, x);
    pref.putEdge(x, f);
    pref.addNode(u);

    final RegretComputer regretComputer = new RegretComputer(knowledge);
    assertEquals(7, regretComputer.getWorstRankOfX(x, knowledge.getPartialPreference(v1)));
    assertEquals(2, regretComputer.getBestRankOfY(x, y, knowledge.getPartialPreference(v1)));
    assertEquals(7, regretComputer.getBestRankOfY(x, x, knowledge.getPartialPreference(v1)));
  }

  @Test
  void testRanksZeroKnowledge() throws Exception {
    /**
     * case 3: assume y>x and go to case 2 (i.e. put as much alts as possible in between)
     **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.addNode(x);
    pref.addNode(y);
    pref.addNode(a);
    pref.addNode(c);
    pref.addNode(u);
    pref.addNode(d);
    pref.addNode(f);
    pref.addNode(b);

    final RegretComputer regretComputer = new RegretComputer(knowledge);
    assertEquals(8, regretComputer.getWorstRankOfX(x, knowledge.getPartialPreference(v1)));
    assertEquals(1, regretComputer.getBestRankOfY(x, y, knowledge.getPartialPreference(v1)));
    assertEquals(8, regretComputer.getBestRankOfY(x, x, knowledge.getPartialPreference(v1)));
  }

  /**
   * TODO complete or delete this test.
   */
  @SuppressWarnings("unused")
  // @Test
  void TestRanks() throws Exception {
    /** case 2: y>x put as much alts as possible in between **/
    Voter v1 = Voter.withId(1);
    Set<Voter> voters = new HashSet<>();
    voters.add(v1);

    Alternative x = Alternative.withId(1);
    Alternative y = Alternative.withId(2);

    Alternative a = Alternative.withId(3);
    Alternative u = Alternative.withId(4);
    Alternative c = Alternative.withId(5);
    Alternative b = Alternative.withId(6);
    Alternative d = Alternative.withId(7);
    Alternative f = Alternative.withId(8);
    Alternative a1 = Alternative.withId(9);
    Alternative b1 = Alternative.withId(10);
    Alternative c1 = Alternative.withId(11);
    Alternative d1 = Alternative.withId(12);
    Alternative u1 = Alternative.withId(13);

    Set<Alternative> alt = new HashSet<>();
    alt.add(a);
    alt.add(b);
    alt.add(c);
    alt.add(d);
    alt.add(x);
    alt.add(y);
    alt.add(u);
    alt.add(f);
    alt.add(a1);
    alt.add(b1);
    alt.add(c1);
    alt.add(d1);
    alt.add(u1);

    UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge.given(alt, voters);

    MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

    pref.putEdge(a, y);
    pref.putEdge(y, b);
    pref.putEdge(y, d);
    pref.putEdge(b, x);
    pref.putEdge(c, x);
    pref.putEdge(x, f);
    pref.addNode(u);
    pref.putEdge(a1, a);
    pref.putEdge(c1, c);
    pref.putEdge(x, d1);
    pref.putEdge(b1, x);
    pref.putEdge(b, b1);
    pref.putEdge(a1, u1);

    final RegretComputer regretComputer = new RegretComputer(knowledge);

    SetMultimap<Alternative, PairwiseMaxRegret> mmr =
        regretComputer.getMinimalMaxRegrets().asMultimap();
    Alternative xStar = mmr.keySet().iterator().next();
    PairwiseMaxRegret currentSolution = mmr.get(xStar).iterator().next();
    Alternative yBar = currentSolution.getY();

    Map<Voter, Integer> xRanks = regretComputer.getWorstRanksOfX(xStar);
    Map<Voter, Integer> yRanks = regretComputer.getBestRanksOfY(xStar, yBar);
    // for (Voter v : knowledge.getProfile().keySet()) {
    // int[] r = Regret.getWorstRanks(xStar, yBar, knowledge.getProfile().get(v));
    // assertTrue(r[0] == xRanks.get(v));
    // assertTrue(r[1] == yRanks.get(v));
    // }
  }
}
