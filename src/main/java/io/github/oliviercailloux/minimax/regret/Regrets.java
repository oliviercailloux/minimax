package io.github.oliviercailloux.minimax.regret;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.MultimapBuilder.SetMultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.y2018.j_voting.Alternative;

/**
 *
 * <p>
 * Because of imprecision with floating computations, computed PMR values can be
 * distinct when they really should be equal. To illustrate, observe that 2 ×
 * 0.15d = 0.3d but 0.1d + 0.2d ≠ 0.3d (see https://floating-point-gui.de/).
 * Write <i>d_i</i> the difference between <i>w_i</i> and <i>w_{i+1}</i>. We
 * could perhaps have a regret equal to twice <i>d2</i> = 2 × 0.15d and another
 * regret equal to <i>d1</i> + <i>d3</i> = 0.2 + 0.1, which would lead to the
 * problem mentioned: both regrets are really equal, but they will be associated
 * to different values after computation. (I didn’t check whether this example
 * situation is really possible, but it seems likely that similar situations
 * could happen.)
 * </p>
 *
 * <p>
 * Another (more important) source of imprecision is that the weights might not
 * exactly be the real weight values that an adversary could use, as they are
 * typically obtained by mathematical programming, and with a possibly increased
 * error due to constraints to ensure a sufficient gap wrt convexity (see
 * {@link ConstraintsOnWeights}).
 * </p>
 * <p>
 * Example: with x being associated with PMRs having values {12.5, 13, 13.1}, y
 * to {11, 13, 14}, and z to {11, 13}, {@link #getMinimalMaxRegret(double)} with
 * epsilon = 0.2 will return 13.1; and {@link #getMinimalMaxRegrets(double)}
 * will return x associated to the PMRs of value 13 and 13.1 and z associated to
 * the PMR of value 13.
 * </p>
 *
 * @author Olivier Cailloux
 *
 */
public class Regrets {

	public static Regrets given(SetMultimap<Alternative, PairwiseMaxRegret> regrets) {
		return new Regrets(regrets);
	}

	public static Regrets given(Map<Alternative, ImmutableSet<PairwiseMaxRegret>> regrets) {
		return given(regrets.entrySet().stream().collect(
				ImmutableSetMultimap.flatteningToImmutableSetMultimap(Entry::getKey, (e) -> e.getValue().stream())));
	}

	private final ImmutableSetMultimap<Alternative, PairwiseMaxRegret> regrets;
	/**
	 * for each alternative x, the pairwise max regrets concerning x, indexed by the
	 * level of the regret, iterating from lowest to highest regret.
	 */
	private ImmutableMap<Alternative, SetMultimap<Double, PairwiseMaxRegret>> regretsSorted;

	public Regrets(SetMultimap<Alternative, PairwiseMaxRegret> regrets) {
		this.regrets = ImmutableSetMultimap.copyOf(regrets);
		checkArgument(!regrets.isEmpty());
		checkArgument(regrets.entries().stream().allMatch((e) -> e.getValue().getX().equals(e.getKey())));
		regretsSorted = null;
	}

	public ImmutableSet<PairwiseMaxRegret> getAllRegrets() {
		return ImmutableSet.copyOf(regrets.values());
	}

	public ImmutableSetMultimap<Alternative, PairwiseMaxRegret> asMultimap() {
		return regrets;
	}

	private void initSorted() {
		if (regretsSorted != null) {
			return;
		}

		@SuppressWarnings("rawtypes")
		final SetMultimapBuilder<Comparable, Object> builder = MultimapBuilder.treeKeys().linkedHashSetValues();

		regretsSorted = regrets.asMap().entrySet().stream()
				.collect(ImmutableMap.toImmutableMap(Entry::getKey, (e) -> e.getValue().stream().collect(
						Multimaps.toMultimap(PairwiseMaxRegret::getPmrValue, Function.identity(), builder::build))));
	}

	private ImmutableSortedMap<Double, ImmutableSet<PairwiseMaxRegret>> getRegretsSorted(
			ImmutableSetMultimap<Double, PairwiseMaxRegret> e) {
		return Multimaps.asMap(e).entrySet().stream().collect(ImmutableSortedMap.toImmutableSortedMap(
				Comparator.naturalOrder(), Entry::getKey, (e2) -> ImmutableSet.copyOf(e2.getValue())));
	}

	private SortedMap<Double, Set<PairwiseMaxRegret>> getRegretsSorted(Alternative x) {
		initSorted();
		return (SortedMap<Double, Set<PairwiseMaxRegret>>) Multimaps.asMap(regretsSorted.get(x));
	}

	private double getMaxRegret(Alternative x) {
		return getRegretsSorted(x).lastKey();
	}

	/**
	 * @return for each alternative x, the pairwise max regrets concerning x,
	 *         indexed by the level of the regret, iterating from lowest to highest
	 *         regret.
	 *
	 */
	public ImmutableMap<Alternative, SortedMap<Double, Set<PairwiseMaxRegret>>> getRegretsSorted() {
		return regrets.keySet().stream()
				.collect(ImmutableMap.toImmutableMap(Function.identity(), this::getRegretsSorted));
	}

	/**
	 * @return min_x {max {PMR(x, …)}}.
	 */
	public double getMinimalMaxRegret() {
		return getMinimalMaxRegret(0d);
	}

	/**
	 * Define m = min_x {max {PMR(x, …)}}. If epsilon = 0d, returns m.
	 *
	 * @return max_x {max {PMR(x, …) | PMR(x, …) ≤ m + epsilon}}.
	 */
	public double getMinimalMaxRegret(double epsilon) {
		checkArgument(epsilon >= 0d);
		checkArgument(Double.isFinite(epsilon));
		final double m = regrets.keySet().stream().map(this::getMaxRegret).min(Comparator.naturalOrder()).get();
		final double mIncreased = regrets.keySet().stream().map(this::getMaxRegret).filter((v) -> v <= m + epsilon)
				.max(Comparator.naturalOrder()).get();
		verify(regrets.keySet().stream().map(this::getMaxRegret)
				.allMatch((v) -> v <= mIncreased || v >= mIncreased + epsilon),
				"Using an epsilon for considering regret values as equal, but some are separated by more than epsilon but ≤ 2 epsilon. "
						+ regretsSorted.values());
		return mIncreased;
	}

	/**
	 * @return a non-empty map whose key set contains each alternative whose max
	 *         regret is ≤ {@link #getMinimalMaxRegret()}, and for each such
	 *         alternative x, the non-empty set of PMRs(x, …) whose regret value is
	 *         {@link #getMinimalMaxRegret()}].
	 */
	public ImmutableSetMultimap<Alternative, PairwiseMaxRegret> getMinimalMaxRegrets() {
		final ImmutableSetMultimap<Alternative, PairwiseMaxRegret> minimalMaxRegrets = getMinimalMaxRegrets(0d);
		verify(minimalMaxRegrets.values().stream().map(PairwiseMaxRegret::getPmrValue).distinct().count() == 1);
		return minimalMaxRegrets;
	}

	/**
	 * @param epsilon ≥ 0d.
	 * @return a non-empty map whose key set contains each alternative whose max
	 *         regret is ≤ {@link #getMinimalMaxRegret(epsilon)}, and for each such
	 *         alternative x, the non-empty set of PMRs(x, …) whose regret value is
	 *         in [{@link #getMinimalMaxRegret(epsilon)} - epsilon,
	 *         {@link #getMinimalMaxRegret(epsilon)}].
	 */
	public ImmutableSetMultimap<Alternative, PairwiseMaxRegret> getMinimalMaxRegrets(double epsilon) {
		final double value = getMinimalMaxRegret(epsilon);
		return regrets.keySet().stream().filter((x) -> getMaxRegret(x) <= value)
				.collect(ImmutableSetMultimap.flatteningToImmutableSetMultimap(Function.identity(),
						(x) -> getRegretsSorted(x).tailMap(value - epsilon).values().stream().flatMap(Set::stream)));
	}
}