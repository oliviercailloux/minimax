package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.Quantiles;
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

public class AnalyzeQuestions {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeQuestions.class);

	public static void main(String[] args) throws Exception {
		final AnalyzeQuestions analyzeQst = new AnalyzeQuestions();
		final Path dir = Path.of("experiments/xps/questions");
		final Path out = Path.of("experiments/xps/questions/quests.csv");
		final ImmutableList<String> fileNames = ImmutableList.of("m5n20.json", "m10n20.json", "m11n30.json",
				"tshirts.json", "courses.json", "m9n146.json", "m14n9.json", "skate.json", "m15n30.json");
		
		analyzeQst.analyzeQuestionsRegret(fileNames, out, dir);

		//analyzeQst.questionsm10n20("m10n20.json", dir);
	}

	public void questionsm10n20(String file, Path dir) throws Exception {
		final Runs runs = JsonConverter.toRuns(Files.readString(Path.of(dir + "/" + file)));
		StatisticsRuns statistics = new StatisticsRuns();
		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMaximumFractionDigits(2);
		LinkedList<Double> regrets = new LinkedList<>();
		for (int i =0; i<runs.nbRuns();i++) {
			Run run = runs.getRun(i);
			statQuestions(run, 500, 500, statistics);
			double r = run.getMinimalMaxRegrets(500).getMinimalMaxRegretValue();
			regrets.add(r);
			System.out.println("Run " + i + " MMR=" + r + " qC="
					+ statistics.getRunToThreshold().getLast().getQuestionsComm());
		}
		final ImmutableList<Integer> qCommTR = statistics.getRunToThreshold().stream()
				.map(PartialStatisticsRun::getQuestionsComm).collect(ImmutableList.toImmutableList());
		Stats statsComm = Stats.of(qCommTR);
		System.out.println("avgQuestComTR " + formatter.format(statsComm.mean()));
		System.out.println("cTR σ " + formatter.format(statsComm.sampleStandardDeviation()));
		
		Stats statsR = Stats.of(regrets);
		System.out.println("MMR " + formatter.format(statsR.mean()));
		System.out.println("σ " + formatter.format(statsR.sampleStandardDeviation()));

//		final ImmutableList<Integer> qCommZR = statistics.getRunToZero().stream()
//				.map(PartialStatisticsRun::getQuestionsComm).collect(ImmutableList.toImmutableList());
//		statsComm = Stats.of(qCommZR);
//		System.out.println("avgQuestComZR "+ formatter.format(statsComm.mean()));
//		System.out.println("cZR σ "+ formatter.format(statsComm.sampleStandardDeviation()));
	}

	
	public void analyzeQuestionsRegret(ImmutableList<String> fileNames, Path fileOut, Path dir) throws Exception {
		Files.deleteIfExists(fileOut);
		Files.createFile(fileOut);

		final StringWriter stringWriter = new StringWriter();
		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMaximumFractionDigits(2);
		final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
		writer.writeHeaders("file", "m", "n", "avgQuestComTR", "cTR σ", "Q1TR", "MTR", "Q3TR", "avgTotQstTR", "qTR σ",
				"avgQuestComZR", "cZR σ", "Q1ZR", "MZR", "Q3ZR", "avgTotQstZR", "qZR σ", "runs");

		for (String file : fileNames) {
			LOGGER.info("File: {}", file);
			writer.addValue("file", file);
			final Runs runs = JsonConverter.toRuns(Files.readString(Path.of(dir + "/" + file)));
			writer.addValue("m", runs.getM());
			writer.addValue("n", runs.getN());

			double threshold = ((double) runs.getN()) / ((double) 10);

			StatisticsRuns statistics = new StatisticsRuns();

			for (Run run : runs.getRuns()) {
				final int indexQuestionTR = findQuestionIndex(run, threshold, 0, runs.getK());
				final int indexQuestionZR = findQuestionIndex(run, 0.0d, indexQuestionTR, runs.getK());
				statQuestions(run, indexQuestionTR, indexQuestionZR, statistics);
			}

			verify(statistics.getRunToThreshold().size() == statistics.getRunToZero().size());
			verify(statistics.getRunToThreshold().size() == runs.getRuns().size());

			LOGGER.info("Stats");
			final ImmutableList<Integer> qCommTR = statistics.getRunToThreshold().stream()
					.map(PartialStatisticsRun::getQuestionsComm).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ1TR = statistics.getRunToThreshold().stream()
					.map(PartialStatisticsRun::getQuantile1).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ2TR = statistics.getRunToThreshold().stream()
					.map(PartialStatisticsRun::getQuantile2).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ3TR = statistics.getRunToThreshold().stream()
					.map(PartialStatisticsRun::getQuantile3).collect(ImmutableList.toImmutableList());
			final ImmutableList<Integer> qTotTR = statistics.getRunToThreshold().stream()
					.map(PartialStatisticsRun::getTotQuestions).collect(ImmutableList.toImmutableList());

			final ImmutableList<Integer> qCommZR = statistics.getRunToZero().stream()
					.map(PartialStatisticsRun::getQuestionsComm).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ1ZR = statistics.getRunToZero().stream()
					.map(PartialStatisticsRun::getQuantile1).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ2ZR = statistics.getRunToZero().stream()
					.map(PartialStatisticsRun::getQuantile2).collect(ImmutableList.toImmutableList());
			final ImmutableList<Double> qQ3ZR = statistics.getRunToZero().stream()
					.map(PartialStatisticsRun::getQuantile3).collect(ImmutableList.toImmutableList());
			final ImmutableList<Integer> qTotZR = statistics.getRunToZero().stream()
					.map(PartialStatisticsRun::getTotQuestions).collect(ImmutableList.toImmutableList());

			Stats statsComm = Stats.of(qCommTR);
			writer.addValue("avgQuestComTR", formatter.format(statsComm.mean()));
			writer.addValue("cTR σ", formatter.format(statsComm.sampleStandardDeviation()));

			Stats statsQ1 = Stats.of(qQ1TR);
			Stats statsQ2 = Stats.of(qQ2TR);
			Stats statsQ3 = Stats.of(qQ3TR);
			writer.addValue("Q1TR", formatter.format(statsQ1.mean()));
			writer.addValue("MTR", formatter.format(statsQ2.mean()));
			writer.addValue("Q3TR", formatter.format(statsQ3.mean()));

			Stats statsTot = Stats.of(qTotTR);
			writer.addValue("avgTotQstTR", formatter.format(statsTot.mean()));
			writer.addValue("qTR σ", formatter.format(statsTot.sampleStandardDeviation()));

			statsComm = Stats.of(qCommZR);
			writer.addValue("avgQuestComZR", formatter.format(statsComm.mean()));
			writer.addValue("cZR σ", formatter.format(statsComm.sampleStandardDeviation()));

			statsQ1 = Stats.of(qQ1ZR);
			statsQ2 = Stats.of(qQ2ZR);
			statsQ3 = Stats.of(qQ3ZR);
			writer.addValue("Q1ZR", formatter.format(statsQ1.mean()));
			writer.addValue("MZR", formatter.format(statsQ2.mean()));
			writer.addValue("Q3ZR", formatter.format(statsQ3.mean()));

			statsTot = Stats.of(qTotZR);
			writer.addValue("avgTotQstZR", formatter.format(statsTot.mean()));
			writer.addValue("qZR σ", formatter.format(statsTot.sampleStandardDeviation()));

			writer.addValue("runs", runs.getRuns().size());
			writer.writeValuesToRow();
		}
		Files.writeString(fileOut, stringWriter.toString());

	}

	/**
	 * Given a run, it finds qTR and qZR which are, respectively, the question such
	 * that the regret reaches a specific threshold of regret and such that it
	 * reaches zero regret.
	 * 
	 * It should be improved. If it does not reach the threshold/zero maybe we want
	 * it to return something different.
	 */

	private int findQuestionIndex(Run run, double regret, int start, int end) {
		checkArgument(start <= end);
		if (start == end) {
			return start;
		}
		int s = start;
		int f = end;
		int m = (f + s) / 2;
		while ((f - s) > 1) {
			double currRegret = run.getMinimalMaxRegrets(m).getMinimalMaxRegretValue();
			if (currRegret > regret) {
				s = m;
			} else {
				f = m;
			}
			m = (f + s) / 2;
		}
		return (Math.abs(run.getMinimalMaxRegrets(m).getMinimalMaxRegretValue() - regret)) > (Math
				.abs(run.getMinimalMaxRegrets(m + 1).getMinimalMaxRegretValue() - regret)) ? m + 1 : m;
	}

	/**
	 * Given a run, it builds a partial statistic of the questions from 0 to qTR,
	 * and a partial statistic of questions from 0 to qZR.
	 * 
	 * It should be improved. If it does not reach the threshold/zero maybe we want
	 * it to return something different.
	 */
	private void statQuestions(Run run, int toQuestionTR, int toQuestionZR, StatisticsRuns stats) {
		ImmutableList<Question> questions = run.getQuestions();
		HashMap<Voter, Integer> qstPerVoter = new HashMap<>();
		for (Voter v : run.getOracle().getProfile().keySet()) {
			qstPerVoter.put(v, 0);
		}
		int qstComm = 0;

		for (int i = 0; i <= Math.min(toQuestionTR, run.getK() - 1); i++) {
			if (questions.get(i).getType() == QuestionType.VOTER_QUESTION) {
				Voter v = questions.get(i).asQuestionVoter().getVoter();
				qstPerVoter.put(v, qstPerVoter.get(v) + 1);
			} else {
				qstComm++;
			}
		}
		PartialStatisticsRun runToThreshold = new PartialStatisticsRun(qstComm,
				Quantiles.quartiles().index(1).compute(qstPerVoter.values()),
				Quantiles.quartiles().index(2).compute(qstPerVoter.values()),
				Quantiles.quartiles().index(3).compute(qstPerVoter.values()), toQuestionTR);

		for (int i = toQuestionTR + 1; i <= Math.min(toQuestionZR, run.getK() - 1); i++) {
			if (questions.get(i).getType() == QuestionType.VOTER_QUESTION) {
				Voter v = questions.get(i).asQuestionVoter().getVoter();
				qstPerVoter.put(v, qstPerVoter.get(v) + 1);
			} else {
				qstComm++;
			}
		}
		PartialStatisticsRun runToZero = new PartialStatisticsRun(qstComm,
				Quantiles.quartiles().index(1).compute(qstPerVoter.values()),
				Quantiles.quartiles().index(2).compute(qstPerVoter.values()),
				Quantiles.quartiles().index(3).compute(qstPerVoter.values()), toQuestionZR);

		stats.addStatsRun(runToThreshold, runToZero);
	}

	private class StatisticsRuns {
		private LinkedList<PartialStatisticsRun> runToThreshold;
		private LinkedList<PartialStatisticsRun> runToZero;

		public StatisticsRuns() {
			runToThreshold = new LinkedList<>();
			runToZero = new LinkedList<>();
		}

		public void addStatsRun(PartialStatisticsRun statRunTR, PartialStatisticsRun statRunZR) {
			runToThreshold.add(statRunTR);
			runToZero.add(statRunZR);
		}

		public LinkedList<PartialStatisticsRun> getRunToThreshold() {
			return runToThreshold;
		}

		public LinkedList<PartialStatisticsRun> getRunToZero() {
			return runToZero;
		}

	}

	private class PartialStatisticsRun {
		public int questionsComm;
		public double quantile1;

		public int getQuestionsComm() {
			return questionsComm;
		}

		public double getQuantile1() {
			return quantile1;
		}

		public double getQuantile2() {
			return quantile2;
		}

		public double getQuantile3() {
			return quantile3;
		}

		public int getTotQuestions() {
			return totQuestions;
		}

		public double quantile2;
		public double quantile3;
		public int totQuestions;

		public PartialStatisticsRun(int questionsComm, double quantile1, double quantile2, double quantile3,
				int totQuestions) {
			this.questionsComm = questionsComm;
			this.quantile1 = quantile1;
			this.quantile2 = quantile2;
			this.quantile3 = quantile3;
			this.totQuestions = totQuestions;
		}

	}

}
