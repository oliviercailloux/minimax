package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.Stats;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.regret.Regrets;

public class AnalyzeJsons {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeJsons.class);

  public static void main(String[] args) throws Exception {
    final AnalyzeJsons analyzeJsons = new AnalyzeJsons();
    final Path dir = Path.of("experiments/New");
    final Path out = Path.of("experiments/New/questsThreshold.csv");
    final ImmutableList<String> fileNames =
        ImmutableList.of("m5n10.json", "m5n20.json", "m10n20.json", "m10n30.json", "m15n30.json",
            "m14n9.json", "skate.json", "tshirt.json", "courses.json");

    analyzeJsons.analyzeQuestZeroRegret(fileNames, out, dir);

    // final Path outDir = Path.of("experiments/RealData");
    // final String file = "Css, sushi.soc, k = 100000, ongoing";
    // analyzeJsons.writeFileMMRtoCsv(outDir.resolve(file + ".json"), outDir.resolve(file + ".csv"),
    // 1000);
    // findAndAnalyze();
    // analyzeJsons.analyzePenalty("analyzePenalty.txt");
  }

  public void analyzeQuestZeroRegret(ImmutableList<String> fileNames, Path fileOut, Path dir)
      throws Exception {
    Files.deleteIfExists(fileOut);
    Files.createFile(fileOut);

    final StringWriter stringWriter = new StringWriter();
    final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
    formatter.setMaximumFractionDigits(2);
    final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
    writer.writeHeaders("file", "avgQuestCom", "c σ", "avgQuestVot", "v σ", "runs");

    for (String file : fileNames) {
      LOGGER.info("File: {}", file);
      writer.addValue("file", file);
      final Runs runs = JsonConverter.toRuns(Files.readString(Path.of(dir + "/" + file)));
      LinkedList<Run> necRuns = new LinkedList<>();
      double threshold = ((double) runs.getN()) / ((double) 10);
      System.out.println(threshold);
      for (Run run : runs.getRuns()) {
        final ImmutableList<Question> questions = run.getQuestions();
        final ImmutableList<Integer> times = run.getQuestionTimesMs();
        LinkedList<Question> necQ = new LinkedList<>();
        LinkedList<Integer> necTimes = new LinkedList<>();
        HashMap<Voter, Integer> qstPerVot = new HashMap<>();
        for (Voter v : run.getOracle().getProfile().keySet()) {
          qstPerVot.put(v, 0);
        }
        for (int i = 0; i < run.getK(); i++) {
          if (run.getMinimalMaxRegrets(i).getMinimalMaxRegretValue() > threshold) {
            necQ.add(questions.get(i));
            necTimes.add(times.get(i));
            if (questions.get(i).getType() == QuestionType.VOTER_QUESTION) {
              Voter v = questions.get(i).asQuestionVoter().getVoter();
              qstPerVot.put(v, qstPerVot.get(v) + 1);
            }
          } else {
            LOGGER.info("Tot questions: {} ", i - 1);
            break;
          }
        }
        necRuns.add(Run.of(run.getOracle(), necQ, necTimes));
      }
      LOGGER.info("Stats");
      final ImmutableList<Integer> qC =
          necRuns.stream().map(Run::getNbQCommittee).collect(ImmutableList.toImmutableList());
      final ImmutableList<Integer> qV =
          necRuns.stream().map(Run::getNbQVoters).collect(ImmutableList.toImmutableList());
      Stats statsC = Stats.of(qC);
      writer.addValue("avgQuestCom", formatter.format(statsC.mean()));
      writer.addValue("c σ", formatter.format(statsC.sampleStandardDeviation()));
      Stats statsV = Stats.of(qV);
      writer.addValue("avgQuestVot", formatter.format(statsV.mean()));
      writer.addValue("v σ", formatter.format(statsV.sampleStandardDeviation()));

      writer.addValue("runs", runs.getRuns().size());
      writer.writeValuesToRow();
    }
    Files.writeString(fileOut, stringWriter.toString());

  }

  public void analyzePenalty(String outFile) throws Exception {
    final Path outDir = Path.of("experiments/TableLinearity");
    Files.createDirectories(outDir);
    Files.deleteIfExists(outDir.resolve(outFile));
    final Path file = Files.createFile(outDir.resolve(outFile));
    analyzeQuestions("Limited (×1.1) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0 +1e-6) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.1) MAX, skate.soc, k = 120, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0) MAX, skate.soc, k = 120, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0 +1e-6) MAX, skate.soc, k = 120, nbRuns = 5.json", file);
    analyzeQuestions("Limited (×1.1) MAX, sushi_short.soc, k = 800, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0) MAX, sushi_short.soc, k = 450, nbRuns = 10.json", file);
    analyzeQuestions("Limited (×1.0 +1e-6) MAX, sushi_short.soc, k = 450, nbRuns = 5.json", file);
  }

  private void analyzeQuestions(String fileName, Path fileOut) throws Exception {
    Files.writeString(fileOut, fileName + "\n", StandardOpenOption.APPEND);
    final Path json = Path.of("experiments", "TableLinearity", fileName);
    final Runs runs = JsonConverter.toRuns(Files.readString(json));
    for (Run run : runs.getRuns()) {
      int countCommBef = 0, countVotBef = 0, countCommAft = 0, countVotAft = 0;
      ImmutableList<Question> questions = run.getQuestions();
      for (int i = 0; i < run.getK(); i++) {
        if (run.getMinimalMaxRegrets(i).getMinimalMaxRegretValue() > 1e-6) {
          if (questions.get(i).getType() == QuestionType.COMMITTEE_QUESTION) {
            countCommBef++;
          } else {
            countVotBef++;
          }
        } else {
          if (questions.get(i).getType() == QuestionType.COMMITTEE_QUESTION) {
            countCommAft++;
          } else {
            countVotAft++;
          }
        }
      }
      verify(countCommBef + countCommAft == run.getNbQCommittee());
      verify(countVotBef + countVotAft == run.getNbQVoters());
      Files.writeString(fileOut, String.format("Run: MMR>0 %d qC, %d qV -- MMR=0 %d qC, %d qV.",
          countCommBef, countVotBef, countCommAft, countVotAft) + "\n", StandardOpenOption.APPEND);
      LOGGER.info("Run");
    }
  }

  public void writeFileCsv(Path file, Path fileOut) throws Exception {
    Files.deleteIfExists(fileOut);
    Files.createFile(fileOut);
    final Runs runs = JsonConverter.toRuns(Files.readString(file));
    Files.writeString(fileOut, ToCsv.toCsv(runs, 1));
  }

  public void writeFileMMRtoCsv(Path file, Path fileOut, int step) throws Exception {
    Files.deleteIfExists(fileOut);
    Files.createFile(fileOut);
    LOGGER.info("Reading runs.");
    final Runs runs = JsonConverter.toRuns(Files.readString(file));
    final StringWriter stringWriter = new StringWriter();
    final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
    formatter.setMaximumFractionDigits(2);
    final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
    writer.writeHeaders("k", "MMR avg");

    for (int i = 0; i <= runs.getK(); i += step) {
      LinkedList<Double> mmrs = new LinkedList<>();
      for (Run run : runs.getRuns()) {
        final Regrets regrets = run.getMinimalMaxRegrets(i);
        mmrs.add(regrets.getMinimalMaxRegretValue());
      }
      Stats mmrStat = Stats.of(mmrs);
      double mmr = mmrStat.mean();
      writer.addValue("k", i);
      writer.addValue("MMR avg", formatter.format(mmr));
      writer.writeValuesToRow();
      LOGGER.info("MMR Avg After {}: {}.", i, mmr);
    }
    Files.writeString(fileOut, stringWriter.toString());
  }

  public static void findAndAnalyze() throws Exception {
    final Path inDir = Path.of("experiments/Small/");
    final ImmutableSet<Path> jsonPaths;
    try (Stream<Path> paths = Files.list(inDir)) {
      jsonPaths = paths.filter(p -> p.getFileName().toString().endsWith(".json"))
          .collect(ImmutableSet.toImmutableSet());
    }
    for (Path json : jsonPaths) {
      analyze(json);
    }
  }

  public static void analyze(Path json) throws Exception {
    final Runs runs = JsonConverter.toRuns(Files.readString(json));
    int i = 0;
    for (Run run : runs.getRuns()) {
      final double value = run.getMinimalMaxRegrets().get(0).getMinimalMaxRegretValue();
      final int n = run.getOracle().getN();
      LOGGER.info("i: {}, value: {}, n: {}.", i, value, n);
      // if (value == 3.0) {
      // Files.writeString(Path.of("run.json"), JsonConverter.toJson(run).toString());
      // break;
      // }
      // verify(value == m, String.format("Value: %s, m: %s.", value, m));
      ++i;
    }
  }

  public void analyzeQuestions() throws Exception {
    final int m = 10;
    final int n = 20;
    final int k = 500;
    final int nbRuns = 10;
    final Path json = Path.of("experiments", "TableLinearity",
        String.format("Limited MAX, constrained to [], m = %d, n = %d, k = %d, nbRuns = %d.json", m,
            n, k, nbRuns));
    // final Path json = Path.of("experiments",
    // "Limited MAX, constrained to [], m = 10, n = 20, k = 500, nbRuns = 10.json");
    final Runs runs = JsonConverter.toRuns(Files.readString(json));
    for (Run run : runs.getRuns()) {
      LOGGER.info("Run: {} qC, {} qV, mmr {}.", run.getNbQCommittee(), run.getNbQVoters(),
          run.getMinimalMaxRegrets().get(k).getMinimalMaxRegretValue());
    }
    LOGGER.info("Stats nb qc: {}.",
        Stats.of(runs.getRuns().stream().mapToInt(Run::getNbQCommittee)));
  }

  public void showFinalStats() throws Exception {
    final int m = 6;
    final int n = 6;
    final int k = 30;
    final int nbRuns = 50;
    final Path json = Path.of("experiments",
        String.format("By MMR MAX, m = %d, n = %d, k = %d, nbRuns = %d.json", m, n, k, nbRuns));
    final Runs runs = JsonConverter.toRuns(Files.readString(json));
    LOGGER.info("qst {} , tot {}", runs.getQuestionTimeStats(), runs.getTotalTimeStats());
    // LOGGER.info("Loss after k: {}.", Runner.asStringEstimator(runs.getLossesStats().get(k)));
    // LOGGER.info("MMR after k: {}.",
    // Runner.asStringEstimator(runs.getMinimalMaxRegretStats().get(k)));
  }
}
