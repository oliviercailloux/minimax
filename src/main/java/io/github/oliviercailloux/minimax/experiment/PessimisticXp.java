package io.github.oliviercailloux.minimax.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.strategies.MmrOperator;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class PessimisticXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(PessimisticXp.class);

	public static void main(String[] args) {
		final StrategyFactory pessimisticFactory = StrategyFactory.aggregatingMmrs(MmrOperator.MAX);
		final int m = 5;
		final int n = 5;
		final int k = 30;
		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started.");
		for (int i = 0; i < 5; ++i) {
			final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
		}
		final Runs runs = Runs.of(runsBuilder.build());
		Runner.summarize(runs);
	}
}
