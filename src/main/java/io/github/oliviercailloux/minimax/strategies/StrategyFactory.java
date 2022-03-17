package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;

/**
 * Immutable.
 */
public class StrategyFactory implements Supplier<Strategy> {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(StrategyFactory.class);

  public static StrategyFactory fromJson(JsonObject json) {
    final JsonString familyJson = json.getJsonString("family");
    checkArgument(familyJson != null);

    @SuppressWarnings("serial")
    final ArrayList<QuestioningConstraint> typeQc = new ArrayList<>() {// nothing
    };

    final StrategyType family = StrategyType.valueOf(familyJson.getString());
    switch (family) {
      case PESSIMISTIC:
        return byMmrs(json.getJsonNumber("seed").longValue(), MmrLottery.MAX_COMPARATOR);
      case PESSIMISTIC_HEURISTIC:
        return limited(json.getJsonNumber("seed").longValue(), JsonbUtils.fromJson(
            json.getJsonArray("constraints").toString(), typeQc.getClass().getGenericSuperclass()));
      case LIMITED:
        final String comparatorDescription = json.getString("comparator");
        return limited(json.getJsonNumber("seed").longValue(),
            MmrLottery.comparatorFromDescription(comparatorDescription),
            JsonbUtils.fromJson(json.getJsonArray("constraints").toString(),
                typeQc.getClass().getGenericSuperclass()),
            json.getJsonNumber("penalty").doubleValue());
      case ELITIST:
        return elitist();
      case CSS:
        return css(json.getJsonNumber("seed").longValue());
      case RANDOM:
        return random(json.getJsonNumber("seed").longValue(),
            json.getJsonNumber("probabilityCommittee").doubleValue(),
            json.getBoolean("toVoters", false));
      case TWO_PHASES_HEURISTIC:
      default:
        throw new UnsupportedOperationException("" + family);
    }
  }

  public static StrategyFactory byMmrs(long seed, Comparator<MmrLottery> comparator) {
    final Random random = new Random(seed);
    final PrintableJsonObject json = JsonbUtils.toJsonObject(ImmutableMap.of("family",
        StrategyType.PESSIMISTIC, "seed", seed, "comparator", comparator));

    return new StrategyFactory(() -> {
      final StrategyByMmr strategy = StrategyByMmr.build(comparator);
      strategy.setRandom(random);
      return strategy;
    }, json, "By MMR " + comparator);
  }

  public static StrategyFactory css() {
    final long seed = ThreadLocalRandom.current().nextLong();
    return css(seed);
  }

  public static StrategyFactory css(long seed) {
    final Random random = new Random(seed);

    final PrintableJsonObject json =
        JsonbUtils.toJsonObject(ImmutableMap.of("family", StrategyType.CSS, "seed", seed));

    return new StrategyFactory(() -> {
      final StrategyCss strategy = StrategyCss.newInstance();
      strategy.setRandom(random);
      return strategy;
    }, json, "Css");
  }

  public static StrategyFactory limited() {
    final long seed = ThreadLocalRandom.current().nextLong();
    return limited(seed, ImmutableList.of());
  }

  public static StrategyFactory limitedCommitteeThenVoters(int nbQuestionsToCommittee) {
    final long seed = ThreadLocalRandom.current().nextLong();
    final QuestioningConstraint vConstraint =
        QuestioningConstraint.of(QuestionType.VOTER_QUESTION, Integer.MAX_VALUE);

    if (nbQuestionsToCommittee == 0) {
      return limited(seed, ImmutableList.of(vConstraint));
    }

    final QuestioningConstraint cConstraint =
        QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION, nbQuestionsToCommittee);
    return limited(seed, ImmutableList.of(cConstraint, vConstraint));
  }

  public static StrategyFactory limitedVotersThenCommittee(int nbQuestionsToVoters) {
    final QuestioningConstraint vConstraint =
        QuestioningConstraint.of(QuestionType.VOTER_QUESTION, nbQuestionsToVoters);
    final QuestioningConstraint cConstraint =
        QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION, Integer.MAX_VALUE);
    final long seed = ThreadLocalRandom.current().nextLong();
    return limited(seed, ImmutableList.of(vConstraint, cConstraint));
  }

  public static StrategyFactory limited(long seed, List<QuestioningConstraint> constraints) {
    return limited(seed, MmrLottery.MAX_COMPARATOR, constraints, 1.1d);
  }

  public static StrategyFactory limited(long seed, ComparatorWithDescription<MmrLottery> comparator,
      List<QuestioningConstraint> constraints, double penalty) {
    LOGGER.info("Using seed {}.", seed);
    final Random random = new Random(seed);

    final String comparatorDescription = comparator.toString();
    final PrintableJsonObject json =
        JsonbUtils.toJsonObject(ImmutableMap.of("family", StrategyType.LIMITED, "seed", seed,
            "comparator", comparatorDescription, "constraints", constraints, "penalty", penalty));

    final String prefix = constraints.isEmpty() ? "" : ", constrained to [";
    final String suffix = constraints.isEmpty() ? "" : "]";
    final String constraintsDescription = constraints.stream()
        .map(c -> (c.getNumber() == Integer.MAX_VALUE ? "∞" : c.getNumber())
            + (c.getKind() == QuestionType.COMMITTEE_QUESTION ? "c" : "v"))
        .collect(Collectors.joining(", ", prefix, suffix));

    return new StrategyFactory(() -> {
      final StrategyByMmr strategy = StrategyByMmr.build(comparator, true, constraints, penalty);
      strategy.setRandom(random);
      return strategy;
    }, json, String.format("Limited (×%s) %s%s", penalty, comparator.toString(),
        constraintsDescription));
  }

  public static StrategyFactory elitist() {
    final PrintableJsonObject json =
        JsonbUtils.toJsonObject(ImmutableMap.of("family", StrategyType.ELITIST));

    return new StrategyFactory(() -> {
      final StrategyElitist strategy = StrategyElitist.newInstance();
      return strategy;
    }, json, "Elitist");
  }

  public static StrategyFactory random(double probabilityCommittee) {
    final long seed = ThreadLocalRandom.current().nextLong();
    return random(seed, probabilityCommittee, false);
  }

  public static StrategyFactory randomToVoters(double probabilityCommittee) {
    final long seed = ThreadLocalRandom.current().nextLong();
    return random(seed, probabilityCommittee, true);
  }

  private static StrategyFactory random(long seed, double probabilityCommittee, boolean toVoters) {
    final PrintableJsonObject json =
        JsonbUtils.toJsonObject(ImmutableMap.of("family", StrategyType.RANDOM, "seed", seed,
            "probabilityCommittee", probabilityCommittee, "toVoters", toVoters));

    final Random random = new Random(seed);
    return new StrategyFactory(() -> {
      final StrategyRandom strategy = toVoters ? StrategyRandom.onlyVoters(probabilityCommittee)
          : StrategyRandom.newInstance(probabilityCommittee);
      strategy.setRandom(random);
      return strategy;
    }, json, "Random");
  }

  private final Supplier<Strategy> supplier;
  private final String description;
  private JsonObject json;

  private StrategyFactory(Supplier<Strategy> supplier, JsonObject json, String description) {
    this.supplier = checkNotNull(supplier);
    this.json = checkNotNull(json);
    this.description = checkNotNull(description);
  }

  @Override
  public Strategy get() {
    final Strategy instance = supplier.get();
    checkState(instance != null);
    return instance;
  }

  public JsonObject toJson() {
    return json;
  }

  /**
   * Returns a short description of this factory (omits the seed).
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    // TODO return MoreObjects.toStringHelper(this).add("Description", description).add("Seed",
    // seed).toString();
    return MoreObjects.toStringHelper(this).add("Description", description).toString();
  }
}
