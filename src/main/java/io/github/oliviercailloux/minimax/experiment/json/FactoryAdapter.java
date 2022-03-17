package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class FactoryAdapter implements JsonbAdapter<StrategyFactory, JsonObject> {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FactoryAdapter.class);

  public static final FactoryAdapter INSTANCE = new FactoryAdapter();

  @Override
  public JsonObject adaptToJson(StrategyFactory obj) {
    return obj.toJson();
  }

  @Override
  public StrategyFactory adaptFromJson(JsonObject obj) {
    return StrategyFactory.fromJson(obj);
  }
}
