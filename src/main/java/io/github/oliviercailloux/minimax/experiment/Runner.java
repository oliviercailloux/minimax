package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.IntStream;

import org.apfloat.Apcomplex;
import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.elicitation.PreferenceInformation;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.strategies.Strategy;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class Runner {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  public static final NumberFormat FORMATTER = getFormatter();

  private static NumberFormat getFormatter() {
    final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
    formatter.setMinimumFractionDigits(1);
    formatter.setMaximumFractionDigits(1);
    return formatter;
  }

  static Oracle overridingOracle = null;

  /**
   * Creates a new random oracle; returns a single run of asking k questions with the given
   * strategy.
   */
  public static Run run(StrategyFactory strategyFactory, int m, int n, int k) {
    final Oracle oracle =
        Oracle.build(Generator.genProfile(m, n), Generator.genWeightsWithUnbalancedDistribution(m));
    return run(strategyFactory, oracle, k);
  }

  public static Run runWeights(StrategyFactory strategyFactory, Oracle oracle, int k)
      throws IOException {
    final Strategy strategy = strategyFactory.get();

    final UpdateablePreferenceKnowledge knowledge =
        UpdateablePreferenceKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
    PSRWeights p = oracle.getWeights();
    /**
     * We’d need to find l, u so that we can say that λ ∈ [l, u] thus such that l ≤ n/d ≤ u. Thus,
     * if 11/1000 ≤ n = 0.0117334 ≤ 12/1000 and 25/1000 ≤ d = 0.0251189 ≤ 26/1000, we know that
     * 11/26 ≤ n/d ≤ 12/25, and thus a suitable l is 11/26.
     */
    for (int i = 1; i <= oracle.getM() - 2; i++) {
      double n = (p.getWeightAtRank(i) - p.getWeightAtRank(i + 1));
      double d = (p.getWeightAtRank(i + 1) - p.getWeightAtRank(i + 2));
      if (d < 1e-6) {
        knowledge.addConstraint(i, ComparisonOperator.EQ, new Aprational(oracle.getN() - 1));
      } else {
        Aprational lambda = new Aprational(n / d);
        if (lambda.compareTo(new Aprational(oracle.getN() - 1)) == 1
            || lambda.compareTo(Apcomplex.ZERO) < 1) {
          knowledge.addConstraint(i, ComparisonOperator.EQ, new Aprational(oracle.getN() - 1));
        } else {
          knowledge.addConstraint(i, ComparisonOperator.EQ, lambda);
        }
      }
    }
    for (int i = 1; i <= oracle.getM() - 2; i++) {
      LOGGER.info("Range of lambda " + i + ": " + knowledge.getLambdaRange(i).toString());
    }
    strategy.setKnowledge(knowledge);

    final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
    final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();

    try {
      for (int i = 1; i <= k; i++) {
        final long startTime = System.currentTimeMillis();
        final Question q = strategy.nextQuestion();
        final PreferenceInformation a = oracle.getPreferenceInformation(q);
        knowledge.update(a);
        LOGGER.debug("Asked {}.", q);
        qBuilder.add(q);
        tBuilder.add(startTime);
      }
    } catch (Exception e) {
      Files.writeString(Path.of("oracle-crashed.json"), JsonConverter.toJson(oracle).toString());
      throw e;
    }
    final long endTime = System.currentTimeMillis();

    return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
  }

  public static Run run(StrategyFactory strategyFactory, Oracle oracle, int k) {
    final Strategy strategy = strategyFactory.get();

    final UpdateablePreferenceKnowledge knowledge =
        UpdateablePreferenceKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
    strategy.setKnowledge(knowledge);

    final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
    final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();

    for (int i = 1; i <= k; i++) {
      final long startTime = System.currentTimeMillis();
      final Question q = strategy.nextQuestion();
      final PreferenceInformation a = oracle.getPreferenceInformation(q);
      knowledge.update(a);
      LOGGER.debug("Asked {}.", q);
      qBuilder.add(q);
      tBuilder.add(startTime);
    }
    final long endTime = System.currentTimeMillis();

    return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
  }

  public static Run run(Strategy strategy, Oracle oracle,
      UpdateablePreferenceKnowledge startingKnowledge, int k) {
    // final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(),
    // oracle.getProfile().keySet());
    strategy.setKnowledge(startingKnowledge);
    /** Rename for clarity. */
    final UpdateablePreferenceKnowledge knowledge = startingKnowledge;

    final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
    final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();

    for (int i = 1; i <= k; i++) {
      final long startTime = System.currentTimeMillis();
      final Question q = strategy.nextQuestion();
      final PreferenceInformation a = oracle.getPreferenceInformation(q);
      knowledge.update(a);
      LOGGER.debug("Asked {}.", q);
      qBuilder.add(q);
      tBuilder.add(startTime);
    }
    final long endTime = System.currentTimeMillis();

    return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
  }

  public static void show(Run run) {
    for (int i = 0; i < run.getK(); ++i) {
      LOGGER.info("Regret after {} questions: {}.", i,
          run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue());
      LOGGER.info("Question {}: {}.", i, run.getQuestions().get(i));
    }
    LOGGER.info("Regret after {} questions: {}.", run.getK(),
        run.getMinimalMaxRegrets().get(run.getK()).getMinimalMaxRegretValue());
  }

  public static void summarize(Run run) {
    final ImmutableMap<Integer,
        Double> everyFive = IntStream.rangeClosed(0, run.getK()).filter(i -> i % 5 == 0).boxed()
            .collect(ImmutableMap.toImmutableMap(i -> i,
                i -> run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue()));
    LOGGER.info("Regrets: {}.", everyFive);
  }

  public static void summarize(Runs runs) {
    final ImmutableMap<Integer, String> everyFive =
        IntStream.rangeClosed(0, runs.getK()).filter(i -> i % 5 == 0).boxed().collect(ImmutableMap
            .toImmutableMap(i -> i, i -> asString(runs.getMinimalMaxRegretStats().get(i))));
    LOGGER.info("Regrets: {}.", everyFive);
  }

  public static String asString(Stats stats) {
    return "[" + FORMATTER.format(stats.min()) + "; " + FORMATTER.format(stats.mean()) + "; "
        + FORMATTER.format(stats.max()) + "]±"
        + FORMATTER.format(stats.populationStandardDeviation());
  }

  public static String asStringEstimator(Stats stats) {
    final String dev =
        stats.count() >= 2 ? "±" + FORMATTER.format(stats.sampleStandardDeviation()) : "";
    return "[" + FORMATTER.format(stats.min()) + "; " + FORMATTER.format(stats.mean()) + "; "
        + FORMATTER.format(stats.max()) + "]" + dev;
  }
}
