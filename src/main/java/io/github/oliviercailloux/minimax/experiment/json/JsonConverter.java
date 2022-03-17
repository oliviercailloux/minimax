package io.github.oliviercailloux.minimax.experiment.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.json.PrintableJsonObjectFactory;
import io.github.oliviercailloux.json.PrintableJsonValue;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class JsonConverter {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);
  private static Jsonb preferenceBuilder = null;

  public static PrintableJsonObject toJson(Voter voter) {
    return JsonbUtils.toJsonObject(voter);
  }

  public static PrintableJsonObject toJson(VoterStrictPreference preference) {
    final String asStr;
    if (preferenceBuilder == null) {
      preferenceBuilder = JsonbBuilder.create(new JsonbConfig()
          .withAdapters(VoterAdapter.INSTANCE, AlternativeAdapter.INSTANCE).withFormatting(true));
    }
    try {
      asStr = preferenceBuilder.toJson(preference);
      assert asStr.startsWith("\n");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return PrintableJsonObjectFactory.wrapPrettyPrintedString(asStr.substring(1));
  }

  public static VoterStrictPreference toVoterStrictPreference(String json) {
    return JsonbUtils.fromJson(json, VoterStrictPreference.class, VoterAdapter.INSTANCE,
        AlternativeAdapter.INSTANCE);
  }

  /**
   * TODO call getDelegate() in JsonObjectGeneralWrapper constructor to fail early in case of non
   * convertible.
   */
  public static PrintableJsonValue profileToJson(Map<Voter, VoterStrictPreference> profile) {
    return JsonbUtils.toJsonValue(profile.values(), PreferenceAdapter.INSTANCE);
  }

  public static PrintableJsonValue toJson(PSRWeights weights) {
    return JsonbUtils.toJsonValue(weights.getWeights());
  }

  public static PrintableJsonObject toJson(Oracle oracle) {
    return JsonbUtils.toJsonObject(oracle, ProfileAdapter.INSTANCE, PreferenceAdapter.INSTANCE);
  }

  public static PrintableJsonObject toJson(List<Oracle> oracles) {
    return JsonbUtils.toJsonObject(oracles, ProfileAdapter.INSTANCE, PreferenceAdapter.INSTANCE);
  }

  public static Oracle toOracle(String json) {
    return JsonbUtils.fromJson(json, Oracle.class, ProfileAdapter.INSTANCE,
        PreferenceAdapter.INSTANCE, WeightsAdapter.INSTANCE);
  }

  public static List<Oracle> toOracles(String json) {
    @SuppressWarnings("all")
    final Type superclass = new ArrayList<Oracle>() {}.getClass().getGenericSuperclass();
    return JsonbUtils.fromJson(json, superclass, ProfileAdapter.INSTANCE,
        PreferenceAdapter.INSTANCE, WeightsAdapter.INSTANCE);
  }

  public static PrintableJsonObject toJson(Run run) {
    return JsonbUtils.toJsonObject(run, ProfileAdapter.INSTANCE, PreferenceAdapter.INSTANCE,
        QuestionAdapter.INSTANCE);
  }

  public static Run toRun(String json) {
    return JsonbUtils.fromJson(json, Run.class, ProfileAdapter.INSTANCE, PreferenceAdapter.INSTANCE,
        QuestionAdapter.INSTANCE, WeightsAdapter.INSTANCE);
  }

  public static PrintableJsonObject toJson(Runs run) {
    return JsonbUtils.toJsonObject(run, ProfileAdapter.INSTANCE, PreferenceAdapter.INSTANCE,
        QuestionAdapter.INSTANCE);
  }

  public static Runs toRuns(String json) {
    return JsonbUtils.fromJson(json, Runs.class, ProfileAdapter.INSTANCE,
        PreferenceAdapter.INSTANCE, QuestionAdapter.INSTANCE, WeightsAdapter.INSTANCE,
        FactoryAdapter.INSTANCE);
  }

  public static PrintableJsonObject toJson(StrategyFactory factory) {
    return JsonbUtils.toJsonObject(factory, FactoryAdapter.INSTANCE);
  }

  public static StrategyFactory toFactory(String json) {
    return JsonbUtils.fromJson(json, StrategyFactory.class, FactoryAdapter.INSTANCE);
  }
}
