package io.github.oliviercailloux.minimax.experiment.json;

import static io.github.oliviercailloux.minimax.Basics.cConstraint;
import static io.github.oliviercailloux.minimax.Basics.oracle;
import static io.github.oliviercailloux.minimax.Basics.p1;
import static io.github.oliviercailloux.minimax.Basics.profile;
import static io.github.oliviercailloux.minimax.Basics.run;
import static io.github.oliviercailloux.minimax.Basics.runs;
import static io.github.oliviercailloux.minimax.Basics.v1;
import static io.github.oliviercailloux.minimax.Basics.vConstraint;
import static io.github.oliviercailloux.minimax.Basics.w;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.json.PrintableJsonValue;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;
import io.github.oliviercailloux.minimax.strategies.MmrLottery;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class JsonConverterTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverterTests.class);

  @Test
  void testVoter() throws Exception {
    final PrintableJsonObject json = JsonConverter.toJson(v1);
    final String expected = Files.readString(Path.of(getClass().getResource("Voter.json").toURI()));
    assertEquals(expected, json.toString());
  }

  /**
   * TODO ask Jsonb whether I can annotate VSP so that I don’t need to go through custom adapter for
   * the whole type.
   */
  @Test
  void testPreference() throws Exception {
    final PrintableJsonObject json = JsonConverter.toJson(VoterStrictPreference.given(v1, p1));
    final String expected =
        Files.readString(Path.of(getClass().getResource("Strict preference.json").toURI()));
    assertEquals(expected, json.toString());
  }

  @Test
  void testToPreference() throws Exception {
    final String source =
        Files.readString(Path.of(getClass().getResource("Strict preference.json").toURI()));
    final VoterStrictPreference actual = JsonConverter.toVoterStrictPreference(source);
    assertEquals(VoterStrictPreference.given(v1, p1), actual);
  }

  @Test
  void testProfile() throws Exception {
    final PrintableJsonValue json = JsonConverter.profileToJson(profile);
    final String expected =
        Files.readString(Path.of(getClass().getResource("Profile.json").toURI()));
    assertEquals(expected, json.toString());
  }

  @Test
  void testWeights() throws Exception {
    final PrintableJsonValue json = JsonConverter.toJson(w);
    final String expected =
        Files.readString(Path.of(getClass().getResource("Weights.json").toURI()));
    assertEquals(expected, json.toString());
  }

  @Test
  void testOracle() throws Exception {
    final PrintableJsonObject json = JsonConverter.toJson(oracle);
    final String expected =
        Files.readString(Path.of(getClass().getResource("Oracle.json").toURI()));
    assertEquals(expected, json.toString());
  }

  @Test
  void testToOracle() throws Exception {
    final String source = Files.readString(Path.of(getClass().getResource("Oracle.json").toURI()));
    final Oracle actual = JsonConverter.toOracle(source);
    final Oracle expected = oracle;
    assertEquals(expected, actual);
  }

  @Test
  void testRun() throws Exception {
    final PrintableJsonObject json = JsonConverter.toJson(run);
    final String expected = Files.readString(Path.of(getClass().getResource("Run.json").toURI()));
    assertEquals(expected, json.toString());
  }

  @Test
  void testRuns() throws Exception {
    final PrintableJsonObject json =
        JsonConverter.toJson(Runs.of(StrategyFactory.limited(100l, MmrLottery.MAX_COMPARATOR,
            ImmutableList.of(vConstraint, cConstraint), 1d), ImmutableList.of(run, run)));
    final String expected = Files.readString(Path.of(getClass().getResource("Runs.json").toURI()));
    assertEquals(expected, json.toString());
  }

  /**
   * TODO throw back the runtime exception unchanged in JsonbUtils.
   */
  @Test
  void testToRuns() throws Exception {
    final String source = Files.readString(Path.of(getClass().getResource("Runs.json").toURI()));
    final Runs actual = JsonConverter.toRuns(source);
    final Runs expected = runs;
    final Run r1 = expected.getRuns().get(0);
    final Run r1Actual = actual.getRuns().get(0);
    assertEquals(r1.getOracle(), r1Actual.getOracle());
    assertEquals(r1.getQuestions(), r1Actual.getQuestions());
    assertEquals(expected, actual);
  }

  /**
   * Just a timing experiment involving a somewhat bigger file.
   */
  @Test
  void testBig() throws Exception {
    final String source = Files.readString(Path.of(getClass()
        .getResource("Random to voters, m = 6, n = 6, k = 30, nbRuns = 50.json").toURI()));
    LOGGER.info("Reading.");
    final Runs input = JsonConverter.toRuns(source);
    LOGGER.info("Read.");
    LOGGER.info("Writing.");
    final PrintableJsonObject json = JsonConverter.toJson(input);
    LOGGER.info("Written.");
    assertEquals(source, json.toString());
  }
}
