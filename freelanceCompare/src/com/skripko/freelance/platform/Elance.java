package com.skripko.freelance.platform;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.freelance.Job;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import static com.codeborne.selenide.Selenide.$;
//todo save jobs to csv //todo save statistics, but it was unnecessary
/**
 * Created by Skripko Sergey on 29.06.2015.
 */
public class Elance implements FlProcessor {
	public static final FlProcessor instance = new Elance();
	public static final String URL_DOMAIN = "https://www.elance.com/";
	private static final String urlItCategory = URL_DOMAIN + "r/jobs/cat-it-programming/";
	private static Set<SearchQuery> searchQueries = new LinkedHashSet<>();

	static {
		searchQueries.add(new SearchQuery("documentum", urlItCategory + "q-documentum", urlItCategory + "q-dql"));
		searchQueries.add(new SearchQuery("shiny", urlItCategory + "q-shiny"));
		searchQueries.add(new SearchQuery("hadoop", urlItCategory + "q-hadoop", urlItCategory + "q-spark"));
		searchQueries.add(new SearchQuery("selenium", urlItCategory + "q-selenium", urlItCategory + "q-scrapy"));

		searchQueries.add(new SearchQuery("data science", urlItCategory + //category oriented
				"sct-data-science-14176-other-data-science-14178-data-analysis-14174"));
		searchQueries.add(new SearchQuery("mining", urlItCategory + "q-mining", urlItCategory + "q-parse",
				urlItCategory + "q-crawling", urlItCategory + "q-crawler", urlItCategory + "q-scraping", urlItCategory + "q-grabber"));
	}

	private Elance() {
	}

	@Override
	public Set<SearchQuery> getSearchQueries() {
		return searchQueries;
	}

	@Override
	public boolean clickNextPage() {
		SelenideElement elNextButton = $(JobListSelectors.NEXT_PAGE);
		if (!elNextButton.exists()) {
			return false;
		}
		elNextButton.click();
		return true;
	}

	@Override
	public ElementsCollection getJobElements() {
		if (!$(JobListSelectors.ROOT).exists()) {
			return null;
		}
		return $(JobListSelectors.ROOT).$$(JobListSelectors.JOB_CLASS);
	}

	@Override
	public Job parseElement(String queryName, SelenideElement el) {
		String idUrl = el.$(JobSelectors.TITLE).attr("href");

		SelenideElement tempEl = el.$(JobSelectors.STATS)
				.shouldHave(Condition.matchText(".*" + JobSelectors.DURATION_PATTERN + ".*"));
		String[] lifeDurArr = tempEl.getText().split("\\|");
		long lifeDayDuration = 0;
		for (String section : lifeDurArr) {
			if (section.contains(JobSelectors.DURATION_PATTERN)) {
				lifeDayDuration = extractJobDuration(section.replace(JobSelectors.DURATION_PATTERN + ':', "").trim());
				break;
			}
		}

		tempEl = el.$(JobSelectors.STATS + " > " + JobSelectors.PROPOSALS)
				.shouldHave(Condition.matchText(".*" + JobSelectors.PROPOSALS_PATTERN + ".*"));
		int proposalCount = Integer.parseInt(tempEl.$("span").getText());
		return new Job(getClass().getSimpleName(), queryName, idUrl, proposalCount, lifeDayDuration);
	}

	private long extractJobDuration(String strDate) {
		if (strDate == null || strDate.isEmpty()) {
			throw new IllegalArgumentException();
		}

		if (strDate.contains("ago")) {
			return 0;
		}
		try {
			Date date = new SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(strDate);
			return (System.currentTimeMillis() - date.getTime()) / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Date parse exception. Wrong date pattern for:" + strDate);
		}
	}

	static class JobListSelectors {
		public static final String ROOT = "#jobSearchResults";
		public static final String JOB_CLASS = ".jobCard";
		public static final String NEXT_PAGE = "#paginationNext";
	}

	static class JobSelectors {
		public static final String TITLE = ".title";
		public static final String STATS = ".stats";
		public static final String PROPOSALS = ".numproposals-link";
		public static final String DURATION_PATTERN = "Posted";
		public static final String PROPOSALS_PATTERN = "Proposal";
	}
}
