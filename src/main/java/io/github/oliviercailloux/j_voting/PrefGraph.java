package io.github.oliviercailloux.j_voting;

import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.minimax.utils.ForwardingMutableGraph;

public class PrefGraph extends ForwardingMutableGraph<Alternative>
    implements MutableGraph<Alternative> {

  private VoterPartialPreference v;

  public PrefGraph(MutableGraph<Alternative> delegate) {
    super(delegate);
  }

  public void setCallback(VoterPartialPreference v) {
    this.v = v;
  }

  @Override
  public boolean addNode(Alternative node) {
    final boolean added = super.addNode(node);
    if (added) {
      v.setGraphChanged();
    }
    return added;
  }

  @Override
  public boolean putEdge(Alternative nodeU, Alternative nodeV) {
    final boolean put = super.putEdge(nodeU, nodeV);
    if (put) {
      v.setGraphChanged();
    }
    return put;
  }

  @Override
  public boolean removeEdge(Alternative nodeU, Alternative nodeV) {
    final boolean removed = super.removeEdge(nodeU, nodeV);
    if (removed) {
      v.setGraphChanged();
    }
    return removed;
  }

  @Override
  public boolean removeNode(Alternative node) {
    final boolean removed = super.removeNode(node);
    if (removed) {
      v.setGraphChanged();
    }
    return removed;
  }
}
