package com.skripko.object;

public class FieldSelector {
	private String name;
	private String cssPath;
	private String regex;
	private boolean needFastProcess;
	private Option option;

	public enum Option {
		GET_HREF,
	}

	public FieldSelector(String name, String cssPath, String... regex) {
		this.name = name;
		this.cssPath = cssPath;
		if (regex.length != 0) {
			this.regex = regex[0];
		}
	}

	public FieldSelector setOption(Option optionArg) {
		option = optionArg;
		return this;
	}

	public FieldSelector setNeedFastProcess() {
		needFastProcess = true;
		return this;
	}

	public String getName() {
		return name;
	}

	public String getCssPath() {
		return cssPath;
	}

	public String getRegex() {
		return regex;
	}

	public Option getOption() {
		return option;
	}

	public boolean hasOption() {
		return option != null;
	}

	public boolean hasRegex() {
		return regex != null;
	}

	public boolean isNeedFastProcess() {
		return needFastProcess;
	}
}