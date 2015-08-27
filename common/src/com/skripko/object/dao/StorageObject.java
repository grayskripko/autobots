package com.skripko.object.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class StorageObject {
	protected List<Pair> pairs;

	public StorageObject(Pair... wishedValues) {
		this(Arrays.asList(wishedValues));
	}

	public StorageObject(Collection<Pair> wishedValues) {
		pairs = new LinkedList<>(wishedValues);
	}

	public List<Pair> getPairs() {
		return pairs;
	}

	public List<String> getClearValues() {
		return pairs.stream().map(Pair::getValue).collect(Collectors.toList());
	}

	public boolean add(Pair pair) {
		return pairs.add(pair);
	}

	@Override
	public String toString() {
		return pairs.toString();
	}
}