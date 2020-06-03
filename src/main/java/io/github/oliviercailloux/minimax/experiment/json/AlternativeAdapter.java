package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.y2018.j_voting.Alternative;

public class AlternativeAdapter implements JsonbAdapter<Alternative, Integer> {
	@Override
	public Integer adaptToJson(Alternative obj) {
		return obj.getId();
	}

	@Override
	public Alternative adaptFromJson(Integer obj) {
		return new Alternative(obj);
	}
}