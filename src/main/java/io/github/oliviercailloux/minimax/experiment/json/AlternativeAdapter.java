package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.j_voting.Alternative;

public class AlternativeAdapter implements JsonbAdapter<Alternative, Integer> {
  public static final AlternativeAdapter INSTANCE = new AlternativeAdapter();

  @Override
  public Integer adaptToJson(Alternative obj) {
    return obj.getId();
  }

  @Override
  public Alternative adaptFromJson(Integer obj) {
    return Alternative.withId(obj);
  }
}
