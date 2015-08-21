package com.skripko.common;

import java.util.*;

public class RowObject {
	private static List<String> fields = null; // not Map because of it is impossible to put list elements by order
	private Map<String, String> values = null;
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

	public void setNextField(String value) {
		if (fieldPointer == -1) {
			throw new IllegalStateException();
		} else if (fieldPointer == 0) {
			values = new LinkedHashMap<>();
		}
		values.put(fields.get(fieldPointer), value);
		fieldPointer = fieldPointer == fields.size() ? -1 : fieldPointer + 1;
	}

	public static void sculptRowObjectShapeByHeader(List<String> wishedFields) {
		fields = new LinkedList<>();
		wishedFields.stream().forEachOrdered(fields::add);
	}

	public static List<String> getFields() {
		return fields;
	}

	/*
	public static void sculptRowObjectShapeByHeader(List wishedFields) {
		List<String> wishedFieldsStr = new LinkedList<>();
		Object instance = wishedFields.get(0);
		if (instance instanceof Field) {
			wishedFields.stream().forEachOrdered(field -> {
				Field detectedField = (Field) field;
				wishedFieldsStr.add(detectedField.getName());
			});
		} else if(instance instanceof String) {
			wishedFieldsStr = wishedFields;
		} else {
			throw new IllegalArgumentException();
		}
	}*/
}