package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.Stats;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

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
		//final Path outDir = Path.of("experiments/TableLinearity");
		//analyzeJsons.writeFileMMRtoCsv(outDir.resolve("Css, sushi.soc, k = 100000, ongoing.json"), outDir.resolve("Css, sushi.soc, k = 100000, ongoing.csv"),100);
		//findAndAnalyze();
		analyzeJsons.analyzePenalty("analyzePenalty.txt");
	}

	public void analyzePenalty(String outFile) throws Exception {
		final Path outDir = Path.of("experiments/TableLinearity");
		Files.createDirectories(outDir);
		Files.deleteIfExists(outDir.resolve(outFile));
		final Path file = Files.createFile(outDir.resolve(outFile));
		analyzeQuestions("Limited (×1.1) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0 +1e-6) MAX, m = 14, n = 9, k = 500, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.1) MAX, skate.soc, k = 120, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0) MAX, skate.soc, k = 120, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0 +1e-6) MAX, skate.soc, k = 120, nbRuns = 5.json",file);
		analyzeQuestions("Limited (×1.1) MAX, sushi_short.soc, k = 800, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0) MAX, sushi_short.soc, k = 450, nbRuns = 10.json",file);
		analyzeQuestions("Limited (×1.0 +1e-6) MAX, sushi_short.soc, k = 450, nbRuns = 5.json",file);
	}
	
	public void analyzeQuestions(String fileName, Path fileOut) throws Exception {
		Files.writeString(fileOut, fileName+"\n",StandardOpenOption.APPEND);
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
			Files.writeString(fileOut, String.format("Run: MMR>0 %d qC, %d qV -- MMR=0 %d qC, %d qV.", countCommBef, countVotBef, countCommAft,
					countVotAft)+"\n", StandardOpenOption.APPEND);
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
		
		final Run run = JsonConverter.toRuns(Files.readString(file)).getRun(0);
		ImmutableList<Regrets> regrets = run.getMinimalMaxRegrets();
		
		final StringWriter stringWriter = new StringWriter();
		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMaximumFractionDigits(2);
		final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
		writer.writeHeaders("k", "MMR avg");

		for (int k=0; k< run.getK();k=k+step) {
			LOGGER.info("k {}",k);
			writer.addValue("k", k);
			double mmr = regrets.get(k).getMinimalMaxRegretValue();
			writer.addValue("MMR avg", formatter.format(mmr));
			System.out.println(k);
			writer.writeValuesToRow();
		}
		Files.writeString(fileOut, writer.toString());
	}
	
	public void writeFileCsv(Path file, Path fileOut, int step) throws Exception {
		Files.deleteIfExists(fileOut);
		Files.createFile(fileOut);
		final Runs runs = JsonConverter.toRuns(Files.readString(file));
		final Run run = runs.getRun(0);
		final StringWriter stringWriter = new StringWriter();
		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMaximumFractionDigits(2);
		final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
		writer.writeHeaders("k", "MMR min", "MMR avg", "MMR max", "MMR σ (est.)", "Loss min", "Loss avg", "Loss max",
				"Loss σ (est.)");
		for (int k=0; k< run.getK();k=k+step) {
			LOGGER.info("k {}",k);
			writer.addValue("k", k);
			{
				final Stats stat = runs.getMinimalMaxRegretStats().get(k);
				writer.addValue("MMR min", formatter.format(stat.min()));
				writer.addValue("MMR avg", formatter.format(stat.mean()));
				writer.addValue("MMR max", formatter.format(stat.max()));
				final String dev = stat.count() >= 2 ? formatter.format(stat.sampleStandardDeviation()) : "";
				writer.addValue("MMR σ (est.)", dev);
			}
			{
				final Stats stat = runs.getLossesStats().get(k);
				writer.addValue("Loss min", formatter.format(stat.min()));
				writer.addValue("Loss avg", formatter.format(stat.mean()));
				writer.addValue("Loss max", formatter.format(stat.max()));
				final String dev = stat.count() >= 2 ? formatter.format(stat.sampleStandardDeviation()) : "";
				writer.addValue("Loss σ (est.)", dev);
			}
			writer.writeValuesToRow();
		}
		Files.writeString(fileOut, writer.toString());
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
//			if (value == 3.0) {
//				Files.writeString(Path.of("run.json"), JsonConverter.toJson(run).toString());
//				break;
//			}
//			verify(value == m, String.format("Value: %s, m: %s.", value, m));
			++i;
		}
	}
	
	public void analyzeQuestions() throws Exception {
		final int m = 10;
		final int n = 20;
		final int k = 500;
		final int nbRuns = 10;
		final Path json = Path.of("experiments", "TableLinearity", String
				.format("Limited MAX, constrained to [], m = %d, n = %d, k = %d, nbRuns = %d.json", m, n, k, nbRuns));
//		final Path json = Path.of("experiments",
//				"Limited MAX, constrained to [], m = 10, n = 20, k = 500, nbRuns = 10.json");
		final Runs runs = JsonConverter.toRuns(Files.readString(json));
		for (Run run : runs.getRuns()) {
			LOGGER.info("Run: {} qC, {} qV, mmr {}.", run.getNbQCommittee(), run.getNbQVoters(),
					run.getMinimalMaxRegrets().get(k).getMinimalMaxRegretValue());
		}
		LOGGER.info("Stats nb qc: {}.", Stats.of(runs.getRuns().stream().mapToInt(Run::getNbQCommittee)));
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
//		LOGGER.info("Loss after k: {}.", Runner.asStringEstimator(runs.getLossesStats().get(k)));
//		LOGGER.info("MMR after k: {}.", Runner.asStringEstimator(runs.getMinimalMaxRegretStats().get(k)));
	}
}
