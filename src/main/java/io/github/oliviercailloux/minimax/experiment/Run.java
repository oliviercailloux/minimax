package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.regret.Regrets;

@JsonbPropertyOrder({ "oracle", "questions", "times" })
public class Run {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

	private static ImmutableList<Integer> getQuestionTimesMs(List<Long> startTimes, long endTime) {
		final ImmutableList.Builder<Integer> builder = ImmutableList.builder();

		final PeekingIterator<Long> peekingIterator = Iterators.peekingIterator(startTimes.iterator());
		while (peekingIterator.hasNext()) {
			final long start = peekingIterator.next();
			final long end;
			if (peekingIterator.hasNext()) {
				end = peekingIterator.peek();
			} else {
				end = endTime;
			}
			final long diff = end - start;
			/** Using check argument because this is called from the constructor. */
			checkArgument(diff >= 0l);
			final int diffInt = Math.toIntExact(diff);
			builder.add(diffInt);
		}

		final ImmutableList<Integer> times = builder.build();
		verify(times.stream().mapToInt(Integer::intValue).sum() == Math.toIntExact(endTime - startTimes.get(0)));
		return times;
	}

	@JsonbCreator
	public static Run of(@JsonbProperty("oracle") Oracle oracle, @JsonbProperty("questions") List<Question> questions,
			@JsonbProperty("timesMs") List<Integer> durationsMs) {
		return new Run(oracle, questions, durationsMs);
	}

	public static Run of(Oracle oracle, List<Long> startTimes, List<Question> questions, long endTime) {
		return new Run(oracle, questions, getQuestionTimesMs(startTimes, endTime));
	}

	private final Oracle oracle;
	private final ImmutableList<Question> questions;
	@JsonbTransient
	private final ImmutableList<Integer> durationsMs;
	@JsonbTransient
	private ImmutableList<Regrets> regrets;
	@JsonbTransient
	private Map<Integer, Regrets> regretsMap;
	private ImmutableList<Double> losses;

	private Run(Oracle oracle, List<Question> questions, List<Integer> durationsMs) {
		checkArgument(!questions.isEmpty());
		checkArgument(durationsMs.size() == questions.size());
		checkArgument(questions.size() >= 1);
		this.oracle = checkNotNull(oracle);
		this.questions = ImmutableList.copyOf(questions);
		this.durationsMs = ImmutableList.copyOf(durationsMs);
		this.regrets = null;
		this.regretsMap = new TreeMap<>();
		verify((getNbQVoters() + getNbQCommittee()) == questions.size());
		getQuestionTimesMs();
		getTotalTimeMs();
		losses = null;
	}

	public Oracle getOracle() {
		return oracle;
	}

	@JsonbTransient
	public int getK() {
		return questions.size();
	}

	/**
	 * @return a list of size k.
	 */
	public ImmutableList<Question> getQuestions() {
		return questions;
	}

	/**
	 * @return a list of size k.
	 */
	@JsonbProperty("timesMs")
	public ImmutableList<Integer> getQuestionTimesMs() {
		return durationsMs;
	}

	@JsonbTransient
	public int getNbQVoters() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.VOTER_QUESTION)).count();
	}

	@JsonbTransient
	public int getNbQCommittee() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.COMMITTEE_QUESTION)).count();
	}

	@JsonbTransient
	public double getPropQVoters() {
		return ((double) getNbQVoters()) / (double) (getNbQVoters() + getNbQCommittee());
	}

	@JsonbTransient
	public int getTotalTimeMs() {
		/**
		 * An int can store a time of 20 days, this should be enough for one run.
		 */
		return getQuestionTimesMs().stream().mapToInt(Integer::intValue).sum();
	}

	@JsonbTransient
	public Duration getTotalTime() {
		return Duration.ofMillis(getTotalTimeMs());
	}

	/**
	 * Returns the regrets after having asked i questions.
	 *
	 * @param i ≤ k
	 */
	@JsonbTransient
	public Regrets getMinimalMaxRegrets(int i) {
		checkArgument(i <= questions.size());
		if (!regretsMap.containsKey(i)) {
			final UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge
					.given(oracle.getAlternatives(), oracle.getProfile().keySet());
			final RegretComputer rc = new RegretComputer(knowledge);

			for (Question question : Iterables.limit(questions, i)) {
				knowledge.update(oracle.getPreferenceInformation(question));
			}
			final Regrets computedRegrets = rc.getMinimalMaxRegrets();
			regretsMap.put(i, computedRegrets);
		}
		return regretsMap.get(i);
	}

	/**
	 * @return a list of size k + 1.
	 */
	@JsonbTransient
	public ImmutableList<Regrets> getMinimalMaxRegrets() {
		if (regrets == null) {
			final UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge
					.given(oracle.getAlternatives(), oracle.getProfile().keySet());
			final RegretComputer rc = new RegretComputer(knowledge);

			int questionsAsked = 0;

			final ImmutableList.Builder<Regrets> builder = ImmutableList.builderWithExpectedSize(questions.size() + 1);

			final UnmodifiableIterator<Question> iterator = questions.iterator();
			while (true) {
				final Regrets thoseRegrets;
				if (regretsMap.containsKey(questionsAsked)) {
					thoseRegrets = regretsMap.get(questionsAsked);
				} else {
					thoseRegrets = rc.getMinimalMaxRegrets();
					regretsMap.put(questionsAsked, thoseRegrets);
				}
				builder.add(thoseRegrets);

				if (!iterator.hasNext()) {
					break;
				}
				knowledge.update(oracle.getPreferenceInformation(iterator.next()));
				++questionsAsked;
			}
			verify(questionsAsked == questions.size());

			regrets = builder.build();
		}
		return regrets;
	}

	private double computeLoss(int i) {
		final ImmutableSet<Alternative> chosen = regrets.get(i).asMultimap().keySet();
		final ImmutableList<Double> allLosses = chosen.stream().map(x -> oracle.getBestScore() - oracle.getScore(x))
				.collect(ImmutableList.toImmutableList());
		verify(allLosses.stream().allMatch(l -> l >= 0d));
		return Stats.meanOf(allLosses);
	}

	public double getLoss(int i) {
		return getLosses().get(i);
	}

	/**
	 * The loss is the “effective regret”: defining x as an alternative we
	 * recommend, the loss when choosing x is the score of x minus the score of y,
	 * considering the effective preference data (the oracle), and not merely what
	 * we know about it.
	 *
	 * We can in principle recommend more than one alternative (in case of tied
	 * MMR). The loss is the average of the losses when choosing each of the
	 * recommended alternatives.
	 *
	 * @return a list of size k + 1.
	 */
	@JsonbTransient
	public ImmutableList<Double> getLosses() {
		if (losses != null) {
			return losses;
		}
		getMinimalMaxRegrets();
		losses = IntStream.rangeClosed(0, getK()).mapToObj(this::computeLoss).collect(ImmutableList.toImmutableList());
		return losses;
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof Run)) {
			return false;
		}

		final Run r2 = (Run) o2;
		return oracle.equals(r2.oracle) && questions.equals(r2.questions) && durationsMs.equals(r2.durationsMs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oracle, questions, durationsMs);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("oracle", oracle).add("questions", questions)
				.add("durationsMs", durationsMs).toString();
	}
}
