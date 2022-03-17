package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class ReproduceXp {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ReproduceXp.class);

  public static void main(String[] args) throws Exception {
    // reproduce();
    readAndAct();
  }

  public static void reproduce() throws Exception {
    final Path json =
        Path.of("experiments/Limited, constrained to [], m = 10, n = 20, k = 10, nbRuns = 2.json");
    final Runs runs = JsonConverter.toRuns(Files.readString(json));
    final StrategyFactory factory = runs.getFactory();
    for (Run run : runs.getRuns()) {
      final Oracle oracle = run.getOracle();
      final int k = run.getK();
      LOGGER.info("Re-running with {}, {}.", oracle, k);
      final Run runAgain = Runner.run(factory, oracle, k);
      LOGGER.info("Questions: {}.", runAgain.getQuestions());
      verify(run.getQuestions().equals(runAgain.getQuestions()),
          String.format("Mismatch with %s, %d.", oracle, k));
    }
  }

  public static void generate() throws Exception {
    final int m = 4;
    final int n = 4;
    final int k = 300;

    final long seed = ThreadLocalRandom.current().nextLong();
    final StrategyFactory factory = StrategyFactory.css(seed);

    final Oracle oracle =
        Oracle.build(Generator.genProfile(m, n), Generator.genWeightsWithUnbalancedDistribution(m));
    Files.writeString(Path.of("oracle.json"), JsonConverter.toJson(oracle).toString());
    Files.writeString(Path.of("factory.json"), JsonConverter.toJson(factory).toString());
    Runner.run(factory, oracle, k);
  }

  public static void readAndAct() throws Exception {
    final int k = 300;

    final Oracle oracle = JsonConverter.toOracle(Files.readString(Path.of("oracle.json")));
    final StrategyFactory factory =
        JsonConverter.toFactory(Files.readString(Path.of("factory.json")));
    Runner.run(factory, oracle, k);
  }
}
