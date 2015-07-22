package com.skripko.freelance.platform;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Skripko Sergey on 29.06.2015.
 */
public class SearchQuery {
	private String name;
	private List<String> queryOptions = new LinkedList<>();

	public SearchQuery(String name) {
		this.name = name.toLowerCase();
	}

	public SearchQuery(String name, String... queryOptions) {
		this.name = name.toLowerCase();
		this.queryOptions = Arrays.asList(queryOptions);
	}

	public String getName() {
		return name;
	}

	public List<String> getQueryUrls() {
		return queryOptions;
	}

	public void addQuery(String query) {
		queryOptions.add(query);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SearchQuery searchQuery = (SearchQuery) o;

		return name.equals(searchQuery.name);

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
