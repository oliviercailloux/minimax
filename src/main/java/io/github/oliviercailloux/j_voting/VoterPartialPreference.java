package io.github.oliviercailloux.j_voting;

import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;

public class VoterPartialPreference {

  public static VoterPartialPreference about(Voter voter, Set<Alternative> alternatives) {
    final MutableGraph<Alternative> graph = GraphBuilder.directed().build();
    for (Alternative alternative : alternatives) {
      graph.addNode(alternative);
    }
    final PrefGraph watcher = new PrefGraph(graph);
    final VoterPartialPreference v = new VoterPartialPreference(voter, watcher);
    watcher.setCallback(v);
    return v;
  }

  public static VoterPartialPreference copyOf(VoterPartialPreference vpp) {
    final MutableGraph<Alternative> graph = Graphs.copyOf(vpp.asGraph());
    final PrefGraph watcher = new PrefGraph(graph);
    final VoterPartialPreference v = new VoterPartialPreference(vpp.getVoter(), watcher);
    watcher.setCallback(v);
    return v;
  }

  private final Voter voter;
  private final PrefGraph pref;
  private ImmutableGraph<Alternative> transitiveEquivalent;

  public VoterPartialPreference(Voter voter, PrefGraph pref) {
    this.voter = voter;
    this.pref = pref;
    transitiveEquivalent = null;
  }

  public MutableGraph<Alternative> asGraph() {
    return this.pref;
  }

  /**
   * Returns the “is strictly preferred to” relation, transitively closed, thus, not including
   * reflective edges.
   *
   * @return a transitively closed, irreflexive graph.
   */
  public ImmutableGraph<Alternative> asTransitiveGraph() {
    if (transitiveEquivalent == null) {
      final MutableGraph<Alternative> trans = Graphs.copyOf(Graphs.transitiveClosure(pref));
      for (Alternative a : trans.nodes()) {
        trans.removeEdge(a, a);
      }
      transitiveEquivalent = ImmutableGraph.copyOf(trans);
    }
    return transitiveEquivalent;
  }

  public Voter getVoter() {
    return voter;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof VoterPartialPreference)) {
      return false;
    }
    VoterPartialPreference p2 = (VoterPartialPreference) o2;
    return voter.equals(p2.voter) && pref.equals(p2.pref);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.voter, this.pref);
  }

  @Override
  public String toString() {
    return "Voter: " + voter + " Pref: " + pref.edges();
  }

  public void setGraphChanged() {
    transitiveEquivalent = null;
  }
}
