package io.github.oliviercailloux.minimax.regret;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

class RegretComputerTest {

	@Test
	void testPmrValues() {
		final Alternative a = new Alternative(1);
		final Alternative b = new Alternative(2);
		final Voter v1 = new Voter(1);
		final Voter v2 = new Voter(2);
		final PrefKnowledge k = PrefKnowledge.given(ImmutableSet.of(a, b), ImmutableSet.of(v1, v2));
		final RegretComputer regretComputer = new RegretComputer(k);

		final ImmutableMap<Voter, Integer> allSecond = ImmutableMap.of(v1, 2, v2, 2);
		final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1, v2, 1);
		final PairwiseMaxRegret pmrAVsB = PairwiseMaxRegret.given(a, b, allSecond, allFirst,
				PSRWeights.given(ImmutableList.of(1d, 0d)));
		final PairwiseMaxRegret pmrBVsA = PairwiseMaxRegret.given(b, a, allSecond, allFirst,
				PSRWeights.given(ImmutableList.of(1d, 0d)));

		SetMultimap<Alternative, PairwiseMaxRegret> pmrValues = regretComputer.getMinimalMaxRegrets();

		assertTrue(pmrValues.keySet().size() == 2);
		assertTrue(pmrValues.get(a).size() == 1);
		assertTrue(pmrValues.get(b).size() == 1);
		assertTrue(pmrValues.get(a).contains(pmrAVsB));
		assertTrue(pmrValues.get(b).contains(pmrBVsA));
	}

	@Test
	void testEmptyKSizeOne() {
		final Alternative a = new Alternative(1);
		final Voter v1 = new Voter(1);
		final PrefKnowledge k = PrefKnowledge.given(ImmutableSet.of(a), ImmutableSet.of(v1));
		final RegretComputer regretComputer = new RegretComputer(k);
		final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1);
		final PairwiseMaxRegret pmrAVsA = PairwiseMaxRegret.given(a, a, allFirst, allFirst,
				PSRWeights.given(ImmutableList.of(1d)));

		assertEquals(ImmutableSet.of(pmrAVsA), regretComputer.getMinimalMaxRegrets().get(a));
	}

	@Test
	void testEmptyKSizeTwo() {
		final Alternative a = new Alternative(1);
		final Alternative b = new Alternative(2);
		final Voter v1 = new Voter(1);
		final Voter v2 = new Voter(2);
		final PrefKnowledge k = PrefKnowledge.given(ImmutableSet.of(a, b), ImmutableSet.of(v1, v2));
		final RegretComputer regretComputer = new RegretComputer(k);
		final ImmutableMap<Voter, Integer> allSecond = ImmutableMap.of(v1, 2, v2, 2);
		final ImmutableMap<Voter, Integer> allFirst = ImmutableMap.of(v1, 1, v2, 1);
		final PairwiseMaxRegret pmrAVsB = PairwiseMaxRegret.given(a, b, allSecond, allFirst,
				PSRWeights.given(ImmutableList.of(1d, 0d)));
		final PairwiseMaxRegret pmrBVsA = PairwiseMaxRegret.given(b, a, allSecond, allFirst,
				PSRWeights.given(ImmutableList.of(1d, 0d)));

		assertEquals(ImmutableSet.of(pmrAVsB), regretComputer.getMinimalMaxRegrets().get(a));
		assertEquals(ImmutableSet.of(pmrBVsA), regretComputer.getMinimalMaxRegrets().get(b));
	}

	@Test
	void testMMR() throws Exception {
		/** Test with complete knowledge about voters' preferences **/
		Voter v1 = new Voter(1);
		Voter v2 = new Voter(2);
		Voter v3 = new Voter(3);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);
		voters.add(v2);
		voters.add(v3);

		Alternative a = new Alternative(1);
		Alternative b = new Alternative(2);
		Alternative c = new Alternative(3);
		Alternative d = new Alternative(4);
		Set<Alternative> alt = new HashSet<>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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

		SetMultimap<Alternative, PairwiseMaxRegret> mrs = regretComputer.getMinimalMaxRegrets();
		assertEquals(ImmutableSet.of(a), mrs.keySet());
		Set<PairwiseMaxRegret> pmrs = mrs.get(a);

		final ImmutableSet<Alternative> ys = pmrs.stream().map((p) -> p.getY()).collect(ImmutableSet.toImmutableSet());
		assertTrue(ys.contains(a));
		/** TODO check that the whole set, when using an epsilon, is the following. */
//		assertEquals(ImmutableSet.of(a, c, d), ys);
	}

	@Test
	void testRanksXpreferredY() throws Exception {
		/** case 1: x>y put as much alts as possible above x **/
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);
		Alternative a1 = new Alternative(9);
		Alternative b1 = new Alternative(10);
		Alternative c1 = new Alternative(11);
		Alternative d1 = new Alternative(12);
		Alternative u1 = new Alternative(13);

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

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);

		Set<Alternative> alt = new HashSet<>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);
		alt.add(x);
		alt.add(y);
		alt.add(u);
		alt.add(f);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);
		Alternative a1 = new Alternative(9);
		Alternative b1 = new Alternative(10);
		Alternative c1 = new Alternative(11);
		Alternative d1 = new Alternative(12);
		Alternative u1 = new Alternative(13);

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

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);

		Set<Alternative> alt = new HashSet<>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);
		alt.add(x);
		alt.add(y);
		alt.add(u);
		alt.add(f);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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
		 * case 3: assume y>x and go to case 2 (i.e. put as much alts as possible in
		 * between)
		 **/
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);

		Set<Alternative> alt = new HashSet<>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);
		alt.add(x);
		alt.add(y);
		alt.add(u);
		alt.add(f);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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

	@Test
	void TestRanks() throws Exception {
		/** case 2: y>x put as much alts as possible in between **/
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);
		Alternative a1 = new Alternative(9);
		Alternative b1 = new Alternative(10);
		Alternative c1 = new Alternative(11);
		Alternative d1 = new Alternative(12);
		Alternative u1 = new Alternative(13);

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

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

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

		SetMultimap<Alternative, PairwiseMaxRegret> mmr = regretComputer.getMinimalMaxRegrets();
		Alternative xStar = mmr.keySet().iterator().next();
		PairwiseMaxRegret currentSolution = mmr.get(xStar).iterator().next();
		Alternative yBar = currentSolution.getY();

		Map<Voter, Integer> xRanks = regretComputer.getWorstRanksOfX(xStar);
		Map<Voter, Integer> yRanks = regretComputer.getBestRanksOfY(xStar, yBar);
//		for (Voter v : knowledge.getProfile().keySet()) {
//			int[] r = Regret.getWorstRanks(xStar, yBar, knowledge.getProfile().get(v));
//			assertTrue(r[0] == xRanks.get(v));
//			assertTrue(r[1] == yRanks.get(v));
//		}
	}

}
