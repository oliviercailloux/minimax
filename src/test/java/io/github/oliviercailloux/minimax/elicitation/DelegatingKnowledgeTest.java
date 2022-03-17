package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.minimax.Basics;

public class DelegatingKnowledgeTest {

  @Test
  void testDelegate() throws Exception {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(3));
    final PreferenceInformation p1 =
        PreferenceInformation.aboutVoter(Basics.v1, Basics.a1, Basics.a2);
    final DelegatingPreferenceKnowledge del = DelegatingPreferenceKnowledge.given(k, p1);
    HashSet<Alternative> h = new HashSet<>();
    h.add(Basics.a1);
    h.add(Basics.a2);
    VoterPartialPreference vp = VoterPartialPreference.about(Basics.v1, h);
    vp.asGraph().putEdge(Basics.a1, Basics.a2);
    VoterPartialPreference empty = VoterPartialPreference.about(Basics.v1, h);

    assertEquals(del.getPartialPreference(Basics.v1), vp);
    assertEquals(k.getPartialPreference(Basics.v1), empty);

    assertEquals(del.getPartialPreference(Basics.v2), k.getPartialPreference(Basics.v2));
  }

  @Test
  void testProfileCompleted() throws Exception {
    final UpdateablePreferenceKnowledge k =
        UpdateablePreferenceKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
    final PreferenceInformation p1 =
        PreferenceInformation.aboutVoter(Basics.v1, Basics.a1, Basics.a2);
    k.update(p1);
    final PreferenceInformation p2 =
        PreferenceInformation.aboutVoter(Basics.v2, Basics.a1, Basics.a2);
    final DelegatingPreferenceKnowledge del = DelegatingPreferenceKnowledge.given(k, p2);

    assertFalse(k.isProfileComplete());
    assertTrue(del.isProfileComplete());

  }
}
