package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import org.apfloat.Apint;
import org.apfloat.Aprational;

public class AprationalAdapter implements JsonbAdapter<Aprational, JsonObject> {
  public static final AprationalAdapter INSTANCE = new AprationalAdapter();

  @Override
  public JsonObject adaptToJson(Aprational obj) {
    return Json.createObjectBuilder().add("numerator", obj.numerator().intValue())
        .add("denominator", obj.denominator().intValue()).build();
  }

  @Override
  public Aprational adaptFromJson(JsonObject obj) {
    return new Aprational(new Apint(obj.getInt("numerator")), new Apint(obj.getInt("denominator")));
  }
}
