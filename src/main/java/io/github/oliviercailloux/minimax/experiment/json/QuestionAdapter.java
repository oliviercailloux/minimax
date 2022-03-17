package io.github.oliviercailloux.minimax.experiment.json;

import static com.google.common.base.Preconditions.checkArgument;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;

import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.json.PrintableJsonObjectFactory;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;

public class QuestionAdapter implements JsonbAdapter<Question, JsonObject> {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAdapter.class);

  public static final QuestionAdapter INSTANCE = new QuestionAdapter();

  private static Jsonb builderWithAlternativeAdapter = null;

  private static Jsonb builderWithAprationalAdapter = null;

  private QuestionAdapter() {}

  @Override
  public JsonObject adaptToJson(Question obj) {

    final JsonObjectBuilder builder = Json.createObjectBuilder();
    final String asStr;
    switch (obj.getType()) {
      case COMMITTEE_QUESTION:
        if (builderWithAprationalAdapter == null) {
          builderWithAprationalAdapter = JsonbBuilder.create(
              new JsonbConfig().withAdapters(AprationalAdapter.INSTANCE).withFormatting(true));
        }
        try {
          asStr = builderWithAprationalAdapter.toJson(obj.asQuestionCommittee());
          assert asStr.startsWith("\n");
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
        final PrintableJsonObject wrapped =
            PrintableJsonObjectFactory.wrapPrettyPrintedString(asStr.substring(1));
        builder.add("toCommittee", wrapped);
        break;
      case VOTER_QUESTION:
        if (builderWithAlternativeAdapter == null) {
          builderWithAlternativeAdapter = JsonbBuilder.create(
              new JsonbConfig().withAdapters(AlternativeAdapter.INSTANCE).withFormatting(true));
        }
        try {
          asStr = builderWithAlternativeAdapter.toJson(obj.asQuestionVoter());
          assert asStr.startsWith("\n");
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
        builder.add("toVoter",
            PrintableJsonObjectFactory.wrapPrettyPrintedString(asStr.substring(1)));
        break;
      default:
        throw new VerifyException();
    }
    return builder.build();
  }

  @Override
  public Question adaptFromJson(JsonObject obj) {
    final boolean com = obj.containsKey("toCommittee");
    final boolean vot = obj.containsKey("toVoter");
    checkArgument(com == !vot);
    if (com) {
      final QuestionCommittee q = JsonbUtils.fromJson(obj.getJsonObject("toCommittee").toString(),
          QuestionCommittee.class, AprationalAdapter.INSTANCE);
      // final JsonObject lambdaObject = obj.getJsonObject("lambda");
      // final Aprational lambda = new AprationalAdapter().adaptFromJson(lambdaObject);
      // final int rank = obj.getInt("rank");
      // return Question.toCommittee(lambda, rank);
      return Question.toCommittee(q);
    }
    final JsonObject toVoter = obj.getJsonObject("toVoter");
    final QuestionVoter q = JsonbUtils.fromJson(toVoter.toString(), QuestionVoter.class,
        AlternativeAdapter.INSTANCE, VoterAdapter.INSTANCE);
    return Question.toVoter(q);
    // final int voterId = obj.getInt("voter");
    // final JsonArray alternativesArray = obj.getJsonArray("alternatives");
    // checkArgument(alternativesArray.size() == 2);
    // final ImmutableSet<Alternative> alternatives = alternativesArray.stream().map(v ->
    // ((JsonNumber) v).intValue())
    // .map(Alternative::new).collect(ImmutableSet.toImmutableSet());
    // checkArgument(alternatives.size() == 2);
    // return Question.toVoter(new Voter(voterId), alternatives);
  }
}
