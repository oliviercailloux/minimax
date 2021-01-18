package io.github.oliviercailloux.minimax.experiment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.j_voting.profiles.ProfileI;
import io.github.oliviercailloux.j_voting.profiles.management.ReadProfile;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class TableLinearityXps {
	private static final Logger LOGGER = LoggerFactory.getLogger(TableLinearityXps.class);

	public static void main(String[] args) throws Exception {
		final TableLinearityXps tableLinXps = new TableLinearityXps();
//		tableLinXps.runWithOracles(m, n, k, nbRuns);
//		tableLinXps.runWithOracles(14, 9, 300, 10);
//		tableLinXps.runWithOracles(6, 6, 90, 1);
//		tableLinXps.runWithFile("sushi_short.soc", 450, 5);
//		tableLinXps.runWithFile("skate.soc", 120, 5);
		tableLinXps.runWithFile("tshirts.soc", 1000, 10);
	}

	public void runWithOracles(int m, int n, int k, int nbRuns) throws IOException {
		StrategyFactory factory = StrategyFactory.limited();
		
		final Path json = Path.of("experiments/Oracles/",
				String.format("Oracles m = %d, n = %d, %d.json", m, n, nbRuns));
		final ImmutableList<Oracle> oracles = ImmutableList.copyOf(JsonConverter.toOracles(Files.readString(json)));
		final String prefixDescription = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k;

		final Runs runs = runs(factory, oracles, k, prefixDescription, nbRuns);
		final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
		final String descr = Runner.asStringEstimator(stats);
		LOGGER.info("Got final estimator: {}.", descr);
	}

	public void runWithFile(String file, int k, int nbRuns) throws IOException {
		StrategyFactory factory = StrategyFactory.limited();

//		StrategyFactory factory = StrategyFactory.css();
		
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
			for(int i=0; i<nbRuns;i++) {
				final Oracle o = Oracle.build(profile, Generator.genWeightsWithUniformDistribution(p.getNbAlternatives()));
				oraclesBuilder.add(o);
			}
			final ImmutableList<Oracle> oracles = oraclesBuilder.build();
			final String prefixDescription = factory.getDescription() + ", " + file + ", k = " + k ;

			final Runs runs = runs(factory, oracles, k, prefixDescription, nbRuns);
			final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
			final String descr = Runner.asStringEstimator(stats);
			LOGGER.info("Got final estimator: {}.", descr);
		}

	}
	
	public Runs runs(StrategyFactory factory, ImmutableList<Oracle> oracles, int k, String prefixDescription,
			int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/TableLinearity");
		Files.createDirectories(outDir);
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
