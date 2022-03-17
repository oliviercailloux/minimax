package io.github.oliviercailloux.minimax.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.graph.Graph;
import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;

class SetOperationsTest {

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

    Graph<Alternative> vpref = knowledge.getProfile().get(v1).asTransitiveGraph();
    HashSet<Alternative> U = new HashSet<>();
    for (Alternative al : vpref.nodes()) {
      if (vpref.inDegree(al) == 0 && vpref.outDegree(al) == 0) {
        U.add(al);
      }
    }

    HashSet<Alternative> DC = new HashSet<>(vpref.nodes());
    DC.remove(x);
    DC.remove(y);
    DC.removeAll(vpref.predecessors(x));
    DC.removeAll(vpref.successors(y));
    DC.removeAll(U);
    HashSet<Alternative> B = new HashSet<>(vpref.successors(x));
    B.retainAll(vpref.predecessors(y));
    DC.removeAll(B);

    Set<Alternative> trueU = new HashSet<>();
    trueU.add(u);
    trueU.add(u1);
    Set<Alternative> trueB = new HashSet<>();
    trueB.add(b);
    trueB.add(b1);
    Set<Alternative> trueDC = new HashSet<>();
    trueDC.add(d);
    trueDC.add(d1);
    trueDC.add(c);
    trueDC.add(c1);

    assertTrue(DC.containsAll(trueDC));
    assertTrue(B.containsAll(B));
    assertTrue(U.containsAll(U));
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

  }
}
