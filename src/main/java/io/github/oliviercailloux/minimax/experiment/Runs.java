package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.FactoryAdapter;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

@JsonbPropertyOrder({"factory", "runs"})
public class Runs {

  @JsonbCreator
  public static Runs of(@JsonbProperty("factory") StrategyFactory factory,
      @JsonbProperty("runs") List<Run> runs) {
    return new Runs(factory, runs);
  }

  @JsonbTypeAdapter(FactoryAdapter.class)
  private final StrategyFactory factory;
  private final ImmutableList<Run> runs;
  @JsonbTransient
  private final int k;

  private Runs(StrategyFactory factory, List<Run> runs) {
    this.factory = checkNotNull(factory);
    checkArgument(!runs.isEmpty());
    this.runs = ImmutableList.copyOf(runs);
    k = runs.stream().map(Run::getK).distinct().collect(MoreCollectors.onlyElement());
    final ImmutableSet<Integer> ms =
        runs.stream().map(Run::getOracle).map(Oracle::getM).collect(ImmutableSet.toImmutableSet());
    final ImmutableSet<Integer> ns =
        runs.stream().map(Run::getOracle).map(Oracle::getN).collect(ImmutableSet.toImmutableSet());
    checkArgument(ms.size() == 1, "All runs should have the same number of alternatives.");
    checkArgument(ns.size() == 1, "All runs should have the same number of voters.");
  }

  public StrategyFactory getFactory() {
    return factory;
  }

  /**
   * @return a list of size k + 1.
   */
  @JsonbTransient
  public ImmutableList<Double> getAverageMinimalMaxRegrets() {
    final ImmutableList<Stats> stats = getMinimalMaxRegretStats();
    return stats.stream().map(Stats::mean).collect(ImmutableList.toImmutableList());
  }

  /**
   * @return a list of size k + 1.
   */
  @JsonbTransient
  public ImmutableList<Stats> getMinimalMaxRegretStats() {
    final ImmutableList.Builder<Stats> statsBuilder = ImmutableList.builder();
    for (int i = 0; i < k + 1; ++i) {
      final int finali = i;
      final ImmutableList<Double> allIthRegrets = runs.stream().map((r) -> r.getMinimalMaxRegrets())
          .map(l -> l.get(finali).getMinimalMaxRegretValue())
          .collect(ImmutableList.toImmutableList());
      statsBuilder.add(Stats.of(allIthRegrets));
    }
    return statsBuilder.build();
  }

  /**
   * @return a list of size k + 1.
   */
  @JsonbTransient
  public ImmutableList<Stats> getLossesStats() {
    final ImmutableList.Builder<Stats> statsBuilder = ImmutableList.builder();
    for (int i = 0; i < k + 1; ++i) {
      final int finali = i;
      final Stats iThStats = runs.stream().map(r -> r.getLoss(finali)).collect(Stats.toStats());
      statsBuilder.add(iThStats);
    }
    return statsBuilder.build();
  }

  @JsonbTransient
  public Stats getQuestionTimeStats() {
    final ImmutableList<Integer> allTimes = runs.stream()
        .flatMap((r) -> r.getQuestionTimesMs().stream()).collect(ImmutableList.toImmutableList());
    return Stats.of(allTimes);
  }

  @JsonbTransient
  public Stats getTotalTimeStats() {
    final IntStream totalTimesStream = runs.stream().mapToInt(Run::getTotalTimeMs);
    return Stats.of(totalTimesStream);
  }

  @JsonbTransient
  public int nbRuns() {
    return runs.size();
  }

  public ImmutableList<Run> getRuns() {
    return runs;
  }

  @JsonbTransient
  public Run getRun(int i) {
    checkArgument(i < runs.size());
    return runs.get(i);
  }

  public int getK() {
    return k;
  }

  @JsonbTransient
  public int getM() {
    return runs.stream().map(Run::getOracle).map(Oracle::getM).distinct()
        .collect(MoreCollectors.onlyElement());
  }

  @JsonbTransient
  public int getN() {
    return runs.stream().map(Run::getOracle).map(Oracle::getN).distinct()
        .collect(MoreCollectors.onlyElement());
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Runs)) {
      return false;
    }

    final Runs r2 = (Runs) o2;
    return runs.equals(r2.runs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runs);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("runs", runs).toString();
  }
}
