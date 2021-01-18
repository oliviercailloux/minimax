package io.github.oliviercailloux.minimax.experiment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;

import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.j_voting.profiles.ProfileI;
import io.github.oliviercailloux.j_voting.profiles.management.ReadProfile;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class RealDataXps {
	private static final Logger LOGGER = LoggerFactory.getLogger(VariousXps.class);

	public static void main(String[] args) throws Exception {
		final RealDataXps realDataXps = new RealDataXps();
		final boolean onlyJson = true;

	//	realDataXps.runWithRealData("sushi_short.soc", 600, 1, onlyJson);
		realDataXps.runWithRealData("sushi.soc", 100000, 1, onlyJson);
	}

	public void runWithRealData(String file, int k, int nbRuns, boolean onlyJson) throws IOException {
		StrategyFactory factory = StrategyFactory.css();

		String path = "experiments/Oracles/" + file;
		try (InputStream socStream = new FileInputStream(path)) {
			final ProfileI p = new ReadProfile().createProfileFromStream(socStream);
			LinkedList<VoterStrictPreference> profile = new LinkedList<>();
			for (Voter voter : p.getAllVoters()) {
				VoterStrictPreference vpref = VoterStrictPreference.given(voter,
						p.getPreference(voter).toStrictPreference().getAlternatives());
				profile.add(vpref);
			}
			final ImmutableList.Builder<Oracle> oraclesBuilder = ImmutableList.builder();
			for (int i = 0; i < nbRuns; i++) {
				final Oracle o = Oracle.build(profile,
						Generator.genWeightsWithUniformDistribution(p.getNbAlternatives()));
				oraclesBuilder.add(o);
			}
			final ImmutableList<Oracle> oracles = oraclesBuilder.build();
			final String prefixDescription = factory.getDescription() + ", " + file + ", k = " + k;

			if (onlyJson) {
				runsRealJsonOnly(factory, oracles, k, prefixDescription, nbRuns);
			} else {
				runsReal(factory, oracles, k, prefixDescription, nbRuns);
			}

			LOGGER.info("END");
		}

	}

	private Runs runsRealJsonOnly(StrategyFactory factory, ImmutableList<Oracle> oracles, int k,
			String prefixDescription, int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/RealData");
		Files.createDirectories(outDir);
		final String prefixTemp = prefixDescription + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Oracle oracle = oracles.get(i);
			UpdateablePreferenceKnowledge startingKnowledge = UpdateablePreferenceKnowledge
					.given(oracle.getAlternatives(), oracle.getProfile().keySet());

			LOGGER.info("Set weights.");
			addWeights(startingKnowledge, oracle);

			LOGGER.info("Before run.");
			final Run run = Runner.run(factory.get(), oracle, startingKnowledge, k);

			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
			final Runs runs = Runs.of(factory, runsBuilder.build());

			Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
			LOGGER.info("Written json.");
		}

		final String prefix = prefixDescription + ", nbRuns = " + nbRuns;
		final Path outJson = outDir.resolve(prefix + ".json");
		Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);

		return Runs.of(factory, runsBuilder.build());
	}

	private Runs runsReal(StrategyFactory factory, ImmutableList<Oracle> oracles, int k, String prefixDescription,
			int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/RealData");
		Files.createDirectories(outDir);
		final String prefixTemp = prefixDescription + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");
		final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Oracle oracle = oracles.get(i);
			UpdateablePreferenceKnowledge startingKnowledge = UpdateablePreferenceKnowledge
					.given(oracle.getAlternatives(), oracle.getProfile().keySet());

			LOGGER.info("Set weights.");
			addWeights(startingKnowledge, oracle);

			LOGGER.info("Before run.");
			final Run run = Runner.run(factory, oracle, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
			final Runs runs = Runs.of(factory, runsBuilder.build());
			LOGGER.info("Runs.");
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

	/**
	 * Given the scoring vector, it adds the constraint: (w_i − w_{i+1}) = λ
	 * (w_{i+1} − w_{i+2}) to the knowledge.
	 * 
	 */
	private void addWeights(UpdateablePreferenceKnowledge startingKnowledge, Oracle oracle) {
		PSRWeights weights = oracle.getWeights();
		int m = weights.size() - 2;
		for (int i = 1; i <= m; i++) {
			double w_i = weights.getWeightAtRank(i);
			double w_i1 = weights.getWeightAtRank(i + 1);
			double w_i2 = weights.getWeightAtRank(i + 2);
			Aprational a = new Aprational(w_i - w_i1);
			Aprational b = new Aprational(w_i1 - w_i2);
			Aprational lambda = a.divide(b);
			startingKnowledge.addConstraint(i, ComparisonOperator.EQ, lambda);
		}
	}
}
