package com.skripko.common;

import java.util.*;

public class RowObject {
	private static List<String> fields = null; // not Map because of it is impossible to put list elements by order
	private Map<String, String> values = null; // Map is more safe. I'm afraid that another implementation may produce hard bugs with shift inside key-value pairs
	private int fieldPointer;

	public RowObject() {
	}

	public RowObject(Collection<String> wishedValues) {
		if (fields == null || fields.isEmpty()) {
			throw new IllegalStateException();
		}
		values = new LinkedHashMap<>();
		Deque<String> wishedValuesDeq = new ArrayDeque<>(wishedValues);
		fields.stream().forEachOrdered(field -> values.put(field, wishedValuesDeq.pollFirst()));
		fieldPointer = -1;
	}

	public RowObject(String... wishedValues) {
		this(Arrays.asList(wishedValues));
	}

	public void setNextField(String value) {
		if (fieldPointer == -1) {
			throw new IllegalStateException();
		} else if (fieldPointer == 0) {
			values = new LinkedHashMap<>();
		}
		values.put(fields.get(fieldPointer), value);
		fieldPointer = fieldPointer == fields.size() ? -1 : fieldPointer + 1;
	}

	public static void sculptShapeByHeader(List<String> wishedFields) {
		fields = new LinkedList<>();
		wishedFields.stream().forEachOrdered(fields::add);
	}

	public List<String> getFields() {
		return fields;
	}

	public Map<String, String> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return values.toString();
	}
}