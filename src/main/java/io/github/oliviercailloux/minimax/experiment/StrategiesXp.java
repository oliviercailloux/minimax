package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class StrategiesXp {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(StrategiesXp.class);

  public static void main(String[] args) throws Exception {
    final int m = 5;
    final int n = 5;
    final int k = 100;
    final int runs = 1;

    final ImmutableList<StrategyFactory> factoryListT3 =
        ImmutableList.of(StrategyFactory.limited());
    runs(factoryListT3, m, n, k, runs);

  }

  /**
   * Repeat (nbRuns times) a run experiment (thus: generate an oracle, ask k questions for each
   * strategy).
   */
  public static void runs(ImmutableList<StrategyFactory> factoryList, int m, int n, int k,
      int nbRuns) throws IOException {
    final Path outDir = Path.of("experiments/Strategy/");
    Files.createDirectories(outDir);
    final ImmutableMap.Builder<StrategyFactory, Path> tmpJsonMapBuilder = ImmutableMap.builder();
    final ImmutableMap.Builder<StrategyFactory, Path> tmpCsvMapBuilder = ImmutableMap.builder();
    final ImmutableMap.Builder<StrategyFactory, ImmutableList.Builder<Run>> tmpRunsBuilders =
        ImmutableMap.builder();
    for (StrategyFactory factory : factoryList) {
      final String prefixTemp =
          "m = " + m + ", n = " + n + ", k = " + k + ", " + factory.getDescription() + ", ongoing";
      tmpJsonMapBuilder.put(factory, outDir.resolve(prefixTemp + ".json"));
      tmpCsvMapBuilder.put(factory, outDir.resolve(prefixTemp + ".csv"));
      tmpRunsBuilders.put(factory, ImmutableList.builder());
    }
    final ImmutableMap<StrategyFactory, Path> tmpJsonMap = tmpJsonMapBuilder.build();
    final ImmutableMap<StrategyFactory, Path> tmpCsvMap = tmpCsvMapBuilder.build();
    final ImmutableMap<StrategyFactory, ImmutableList.Builder<Run>> runsBuilders =
        tmpRunsBuilders.build();

    for (int i = 0; i < nbRuns; ++i) {
      final Oracle oracle = Oracle.build(Generator.genProfile(m, n),
          Generator.genWeightsWithUnbalancedDistribution(m));
      for (StrategyFactory factory : factoryList) {
        // final Run run = Runner.run(factory, oracle, k);
        final Run run = Runner.runWeights(factory, oracle, k);
        LOGGER.info("Strategy {} - Time (run {}): {}.", factory.getDescription(), i,
            run.getTotalTime());
        Files.writeString(Path.of("run " + i + ".json"), JsonConverter.toJson(run).toString());
        runsBuilders.get(factory).add(run);
        final Runs runs = Runs.of(factory, runsBuilders.get(factory).build());
        // Runner.summarize(runs);
        Files.writeString(tmpJsonMap.get(factory), JsonConverter.toJson(runs).toString());
        Files.writeString(tmpCsvMap.get(factory), ToCsv.toCsv(runs, 5));
      }
    }

    for (StrategyFactory factory : factoryList) {
      final String prefix = "m = " + m + ", n = " + n + ", k = " + k + ", nbRuns = " + nbRuns + ", "
          + factory.getDescription();
      final Path outJson = outDir.resolve(prefix + ".json");
      final Path outCsv = outDir.resolve(prefix + ".csv");
      Files.move(tmpJsonMap.get(factory), outJson, StandardCopyOption.REPLACE_EXISTING);
      Files.move(tmpCsvMap.get(factory), outCsv, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
