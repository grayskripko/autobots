package com.skripko.object.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RowObject extends StorageObject {
	private static List<String> labels; // not Map because of it is impossible to put list elements by order

	public RowObject(String... wishedValues) {
		List<Pair> tmpPairs = new ArrayList<>();
		for (int i = 0; i < wishedValues.length; i++) {
			tmpPairs.add(new Pair(labels.get(i), wishedValues[i]));
		}
		fillPairs(tmpPairs);
	}

	public RowObject(List<Pair> wishedValues) {
		fillPairs(wishedValues);
	}

	private void fillPairs(List<Pair> wishedValues) {
		if (wishedValues.size() != labels.size()) {
			throw new IllegalStateException();
		}
		pairs = new ArrayList<>(wishedValues);
	}

	public static void build(List<String> labelsArg) {
		if (labels != null) {
			throw new IllegalStateException();
		}
		labels = new ArrayList<>(labelsArg);
	}

	public static boolean needBuild() {
		return labels == null;
	}

	public static void append(String... labelsArg) {
		if (labels == null) {
			throw new IllegalStateException();
		}
		labels.addAll(Arrays.asList(labelsArg));
	}

	public static List<String> getLabels() {
		return labels;
	}
}