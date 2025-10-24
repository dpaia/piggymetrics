package com.piggymetrics.statistics.domain;

public enum Currency {

	USD("US Dollar"),
	EUR("Euro"),
	RUB("Russian Ruble");

	private final String name;

	Currency(String name) {
		this.name = name;
	}

	public static Currency getBase() {
		return USD;
	}

	public String getName() {
		return name;
	}
}
