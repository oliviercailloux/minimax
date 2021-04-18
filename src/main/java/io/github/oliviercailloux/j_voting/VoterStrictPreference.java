package io.github.oliviercailloux.j_voting;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.elicitation.VoterPreferenceInformation;
import io.github.oliviercailloux.minimax.experiment.json.VoterAdapter;
import io.github.oliviercailloux.y2018.j_voting.StrictPreference;

/**
 * Immutable.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
@JsonbPropertyOrder({ "voter", "preference" })
public class VoterStrictPreference {

	/**
	 * Returns a voterStrictPreference object.
	 *
	 * @param voter              the person that has these preferences.
	 * @param rankedAlternatives with the preferred alternative as first element
	 *                           (may be empty).
	 * @return a voter strict preference object.
	 */
	@JsonbCreator
	public static VoterStrictPreference given(@JsonbProperty("voter") Voter voter,
			@JsonbProperty("preference") List<Alternative> rankedAlternatives) {
		return new VoterStrictPreference(voter, rankedAlternatives);
	}

	@JsonbTypeAdapter(VoterAdapter.class)
	private final Voter voter;

	@JsonbTransient
	private final StrictPreference preference;

	private VoterStrictPreference(Voter voter, List<Alternative> rankedAlternatives) {
		this.voter = requireNonNull(voter);
		this.preference = new StrictPreference(rankedAlternatives);
	}

	/**
	 * Returns the alternatives this preference is about, the most preferred one
	 * first.
	 *
	 * @return a list of {@link #size()} elements.
	 */
	@JsonbProperty("preference")
	public List<Alternative> getAlternatives() {
		return preference.getAlternatives();
	}

	/**
	 * Returns the number of alternatives, or equivalently, of ranks, this
	 * preference is about.
	 *
	 * @return ≥ 0.
	 */
	public int size() {
		return preference.size();
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof VoterStrictPreference)) {
			return false;
		}
		final VoterStrictPreference v2 = (VoterStrictPreference) o2;
		return v2.voter.equals(voter) && v2.preference.equals(preference);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voter, preference);
	}

	/**
	 * Returns the rank of the given alternative.
	 *
	 * @param alternative not {@code null}.
	 * @return ≥ 1.
	 * @throws IllegalArgumentException if the given alternative is not in this
	 *                                  preference.
	 */
	public int getAlternativeRank(Alternative alternative) {
		return preference.getAlternativeRank(requireNonNull(alternative));
	}

	/**
	 * Returns the alternative at the given rank.
	 *
	 * @param rank ≥ 1.
	 * @return an alternative.
	 */
	public Alternative getAlternativeAtRank(int rank) {
		return preference.getAlternatives().get(rank - 1);
	}

	public Voter getVoter() {
		return voter;
	}

	public StrictPreference asStrictPreference() {
		return preference;
	}

	/**
	 * Given a query a > b
	 *
	 * @return whether a is better or worst than b
	 */
	public VoterPreferenceInformation askQuestion(QuestionVoter qv) {
		final Alternative better;
		final Alternative worst;
		final Alternative a = qv.getFirstAlternative();
		final Alternative b = qv.getSecondAlternative();
		if (preference.getAlternativeRank(a) < preference.getAlternativeRank(b)) {
			better = a;
			worst = b;
		} else {
			worst = a;
			better = b;
		}
		return VoterPreferenceInformation.given(qv.getVoter(), better, worst);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("voter", voter).add("preference", preference).toString();
	}
}
