package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;

/**
 * Immutable.
 */
public class StrategyFactory implements Supplier<Strategy> {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyFactory.class);

	public static StrategyFactory fromJson(JsonObject json) {
		final JsonString familyJson = json.getJsonString("family");
		checkArgument(familyJson != null);
		final StrategyType family = StrategyType.valueOf(familyJson.getString());
		switch (family) {
		case PESSIMISTIC:
			return byMmrs(json.getJsonNumber("seed").longValue(), MmrLottery.MAX_COMPARATOR);
		case PESSIMISTIC_HEURISTIC:
			@SuppressWarnings("serial")
			final ArrayList<QuestioningConstraint> type = new ArrayList<>() {// nothing
			};
			return limited(json.getJsonNumber("seed").longValue(), JsonbUtils
					.fromJson(json.getJsonArray("constraints").toString(), type.getClass().getGenericSuperclass()));
		case RANDOM:
		case TWO_PHASES_HEURISTIC:
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static StrategyFactory byMmrs(long seed, Comparator<MmrLottery> comparator) {
		final Random random = new Random(seed);
		final PrintableJsonObject json = JsonbUtils.toJsonObject(
				ImmutableMap.of("family", StrategyType.PESSIMISTIC, "seed", seed, "comparator", comparator));

		return new StrategyFactory(() -> {
			final StrategyByMmr strategy = StrategyByMmr.build(comparator);
			strategy.setRandom(random);
			return strategy;
		}, json, "By MMR " + comparator);
	}

	public static StrategyFactory limited() {
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of());
	}

	public static StrategyFactory limitedCommitteeThenVoters(int nbQuestionsToCommittee) {
		final QuestioningConstraint cConstraint = QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION,
				nbQuestionsToCommittee);
		final QuestioningConstraint vConstraint = QuestioningConstraint.of(QuestionType.VOTER_QUESTION,
				Integer.MAX_VALUE);
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of(cConstraint, vConstraint));
	}

	public static StrategyFactory limitedVotersThenCommittee(int nbQuestionsToVoters) {
		final QuestioningConstraint vConstraint = QuestioningConstraint.of(QuestionType.VOTER_QUESTION,
				nbQuestionsToVoters);
		final QuestioningConstraint cConstraint = QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION,
				Integer.MAX_VALUE);
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of(vConstraint, cConstraint));
	}

	public static StrategyFactory limited(long seed, List<QuestioningConstraint> constraints) {
		final Random random = new Random(seed);

		final PrintableJsonObject json = JsonbUtils.toJsonObject(ImmutableMap.of("family",
				StrategyType.PESSIMISTIC_HEURISTIC, "seed", seed, "constraints", constraints));

		final String prefix = ", constrained to [";
		final String suffix = "]";
		final String constraintsDescription = constraints.stream()
				.map(c -> (c.getNumber() == Integer.MAX_VALUE ? "∞" : c.getNumber())
						+ (c.getKind() == QuestionType.COMMITTEE_QUESTION ? "c" : "v"))
				.collect(Collectors.joining(", ", prefix, suffix));

		return new StrategyFactory(() -> {
			final StrategyByMmr strategy = StrategyByMmr.limited(constraints);
			strategy.setRandom(random);
			return strategy;
		}, json, "Limited" + constraintsDescription);
	}

	public static StrategyFactory random() {
		final long seed = ThreadLocalRandom.current().nextLong();
		final PrintableJsonObject json = JsonbUtils
				.toJsonObject(ImmutableMap.of("family", StrategyType.RANDOM, "seed", seed));

		final Random random = new Random(seed);
		return new StrategyFactory(() -> {
			final StrategyRandom strategy = StrategyRandom.build();
			strategy.setRandom(random);
			return strategy;
		}, json, "Random");
	}

	private final Supplier<Strategy> supplier;
	private final String description;
	private JsonObject json;

	private StrategyFactory(Supplier<Strategy> supplier, JsonObject json, String description) {
		this.supplier = checkNotNull(supplier);
		this.json = checkNotNull(json);
		this.description = checkNotNull(description);
	}

	@Override
	public Strategy get() {
		final Strategy instance = supplier.get();
		checkState(instance != null);
		return instance;
	}

	public JsonObject toJson() {
		return json;
	}

	/**
	 * Returns a short description of this factory (omits the seed).
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
//		TODO return MoreObjects.toStringHelper(this).add("Description", description).add("Seed", seed).toString();
		return MoreObjects.toStringHelper(this).add("Description", description).toString();
	}

}
