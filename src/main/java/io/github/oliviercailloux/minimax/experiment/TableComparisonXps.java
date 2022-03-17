package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class TableComparisonXps {
  private static final Logger LOGGER = LoggerFactory.getLogger(TableComparisonXps.class);

  public static void main(String[] args) throws Exception {
    final TableComparisonXps table1Xps = new TableComparisonXps();
    int m = 10;
    int n = 20;
    int k = 400;
    int nbRuns = 10;
    table1Xps.runWithOracles(m, n, k, nbRuns);
  }

  public void runWithOracles(int m, int n, int k, int nbRuns) throws IOException {
    @SuppressWarnings("unused")
    final long seed = ThreadLocalRandom.current().nextLong();
    final ImmutableList<StrategyFactory> factoryList =
        ImmutableList.of(StrategyFactory.limited(), StrategyFactory.elitist());

    final Path json = Path.of("experiments/Oracles/",
        String.format("Oracles m = %d, n = %d, %d, geometric.json", m, n, nbRuns));
    final ImmutableList<Oracle> oracles =
        ImmutableList.copyOf(JsonConverter.toOracles(Files.readString(json)));

    for (StrategyFactory factory : factoryList) {
      final Runs runs = runs(factory, oracles, k);
      final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
      final String descr = Runner.asStringEstimator(stats);
      LOGGER.info("Got final estimator: {}.", descr);
    }
  }

  public Runs runs(StrategyFactory factory, ImmutableList<Oracle> oracles, int k)
      throws IOException {
    final int m =
        oracles.stream().map(Oracle::getM).distinct().collect(MoreCollectors.onlyElement());
    final int n =
        oracles.stream().map(Oracle::getN).distinct().collect(MoreCollectors.onlyElement());
    final int nbRuns = oracles.size();

    final Path outDir = Path.of("experiments/TableComparison");
    Files.createDirectories(outDir);
    final String prefixDescription =
        factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", geometric";
    final String prefixTemp = prefixDescription + ", ongoing";
    final Path tmpJson = outDir.resolve(prefixTemp + ".json");
    final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

    final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
    LOGGER.info("Started '{}'.", factory.getDescription());
    for (int i = 0; i < nbRuns; ++i) {
      final Oracle oracle = oracles.get(i);
      LOGGER.info("Before run.");
      final Run run = Runner.run(factory, oracle, k);
      LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
      runsBuilder.add(run);
      final Runs runs = Runs.of(factory, runsBuilder.build());
      LOGGER.info("Runs.");
      // Runner.summarize(runs);
      Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
      LOGGER.info("Written json.");
      Files.writeString(tmpCsv, ToCsv.toCsv(runs, 1));
      LOGGER.info("Written csv.");
    }

    final String prefix = prefixDescription + ", nbRuns = " + nbRuns;
    final Path outJson = outDir.resolve(prefix + ".json");
    final Path outCsv = outDir.resolve(prefix + ".csv");
    Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
    Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);

    return Runs.of(factory, runsBuilder.build());
  }
}
