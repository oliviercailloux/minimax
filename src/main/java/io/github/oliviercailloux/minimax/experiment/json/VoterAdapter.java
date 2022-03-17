package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.j_voting.Voter;

public class VoterAdapter implements JsonbAdapter<Voter, Integer> {
  public static final VoterAdapter INSTANCE = new VoterAdapter();

  @Override
  public Integer adaptToJson(Voter obj) {
    return obj.getId();
  }

  @Override
  public Voter adaptFromJson(Integer obj) {
    return Voter.withId(obj);
  }
}
