package io.github.oliviercailloux.j_voting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

class VoterPartialPreferenceTest {

  @Test
  void test() {
    final VoterPartialPreference p = VoterPartialPreference.about(Voter.withId(1),
        ImmutableSet.of(Alternative.withId(1), Alternative.withId(2), Alternative.withId(3)));
    testIt(p);
  }

  private void testIt(final VoterPartialPreference p) {
    assertEquals(0, p.asGraph().edges().size());
    assertEquals(0, p.asTransitiveGraph().edges().size());
    p.asGraph().putEdge(Alternative.withId(1), Alternative.withId(2));
    assertEquals(1, p.asGraph().edges().size());
    assertEquals(1, p.asTransitiveGraph().edges().size());
    p.asGraph().putEdge(Alternative.withId(2), Alternative.withId(3));
    assertEquals(2, p.asGraph().edges().size());
    assertEquals(3, p.asTransitiveGraph().edges().size());
  }

  @Test
  void testCopy() {
    final VoterPartialPreference p = VoterPartialPreference.about(Voter.withId(1),
        ImmutableSet.of(Alternative.withId(1), Alternative.withId(2), Alternative.withId(3)));
    final VoterPartialPreference p2 = VoterPartialPreference.copyOf(p);
    testIt(p2);
  }

  @Test
  void testAddRemove() {
    final Alternative a2 = Alternative.withId(2);
    final Alternative a3 = Alternative.withId(3);
    final Alternative a1 = Alternative.withId(1);
    final VoterPartialPreference p =
        VoterPartialPreference.about(Voter.withId(1), ImmutableSet.of(a1, a2, a3));
    p.asGraph().putEdge(a1, a2);
    p.asGraph().putEdge(a2, a3);
    assertEquals(2, p.asGraph().edges().size());
    assertEquals(3, p.asTransitiveGraph().edges().size());
    p.asGraph().removeEdge(a2, a3);
    assertEquals(1, p.asGraph().edges().size());
    assertEquals(1, p.asTransitiveGraph().edges().size());
  }
}
