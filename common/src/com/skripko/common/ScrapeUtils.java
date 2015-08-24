package com.skripko.common;

import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

public class ScrapeUtils {

	public static List<String> getUsaCities100KList() {
		String wikipediaUrl = "https://en.wikipedia.org/wiki/List_of_United_States_cities_by_population";
		open(wikipediaUrl);
		return Arrays.asList($$("table.wikitable:nth-of-type(4) td:nth-of-type(2) > a").getTexts());
	}
}