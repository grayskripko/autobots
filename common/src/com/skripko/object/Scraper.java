package com.skripko.object;

import com.skripko.object.dao.RowObject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Scraper {
	private List<RowObject> scrapeResult;
	private String startingUrl;
	private Deque<Action> actions;

	public Scraper(String startingUrl) {
		this.startingUrl = startingUrl;
	}

	public Scraper addAction(Action action) {
		if (actions == null) {
			actions = new ArrayDeque<>();
		}
		actions.add(action);
		return this;
	}

	public void release() {
		if (actions == null) {
			throw new IllegalStateException();
		}

		for (Action action : actions) {

		}
	}
}