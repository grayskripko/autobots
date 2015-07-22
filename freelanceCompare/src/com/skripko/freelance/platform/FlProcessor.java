package com.skripko.freelance.platform;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.freelance.Job;

import java.util.Set;

/**
 * Created by Skripko Sergey on 28.06.2015.
 */
public interface FlProcessor {

	Set<SearchQuery> getSearchQueries();

	ElementsCollection getJobElements();

	Job parseElement(String queryName, SelenideElement el);

	boolean clickNextPage();
}
