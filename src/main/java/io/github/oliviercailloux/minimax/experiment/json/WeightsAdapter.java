package io.github.oliviercailloux.minimax.experiment.json;

import java.util.List;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

public class WeightsAdapter implements JsonbAdapter<PSRWeights, List<Double>> {
  public static final WeightsAdapter INSTANCE = new WeightsAdapter();

  @Override
  public List<Double> adaptToJson(PSRWeights obj) {
    return obj.getWeights();
  }

  @Override
  public PSRWeights adaptFromJson(List<Double> obj) {
    return PSRWeights.given(obj);
  }
}
