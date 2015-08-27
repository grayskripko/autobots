package com.skripko.task;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.common.AlgoUtils;
import com.skripko.common.ExcelIO;
import com.skripko.object.FieldSelector;
import com.skripko.object.dao.Item;
import com.skripko.object.dao.Pair;
import com.skripko.object.dao.RowObject;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.*;
import static com.skripko.common.ExcelIO.Mode.WRITE;
import static com.skripko.common.SelenideUtils.*;
import static com.skripko.object.FieldSelector.Option.GET_HREF;

//todo plus rotating pool of proxies -> +2 threads
//todo add stat for time estimation. Go through all categories if it is possible to know pages count. Then multiply on average of 5 pages processing
//todo stable proxy open with ip rotation and addition. I'm not sure about multiprocess addition of new proxies, but I can start second jvm with shared stable txt
//todo make Object-Modules: Paginator(Type.Scroll), List, Item, ScreenshotForSpeed ... Or maybe it is more appropriate to use functional approach
//todo smart email scraper. Google -> right filter first 3 results -> Find about, contacts, persons links and buttons

public class Main {
//		public static final boolean isBrowserConfigured = configureBrowser(BrowserType.CHROME);
	private static List<RowObject> rowObjects = new ArrayList<>();

	public static void buildRowAndItem() {
		Item.build(".common-company",
				new FieldSelector("SETTORE", "div.common-company-category strong:nth-of-type(1)"),
				new FieldSelector("CATEGORIE", "div.common-company-category strong:nth-of-type(2)"),
				new FieldSelector("DESCRIZIONE", "div.common-company-description strong"),
				new FieldSelector("INDIRIZZO", "div.common-company-indirizzo strong"),
				new FieldSelector("PROVINCIA", "div.common-company-city strong", ".+(?= - )"),
				new FieldSelector("CAP", "div.common-company-city strong", "\\d{5}\\s*$"),
				new FieldSelector("TEL", "div.common-company-recapiti strong.-font-num:nth-of-type(1)"),
				new FieldSelector("FAX", "strong.-font-num:nth-of-type(2)").setNeedFastProcess(),
				new FieldSelector("SITO_WEB", "div.common-company-website a.button-inline").setOption(GET_HREF));

		RowObject.build(Item.getLabels());
		RowObject.append("URL");
	}

	public static void main(String[] args) throws Exception {
//		applyProxy$ClosePrevBrowser(getFastProxies(getProxyInfoList(), 1).get(0));
//		print(SelenideUtils.saveImage("http://www.infoedile.it/images/g.elettrica.jpg", "pic.jpg"));
		buildRowAndItem();


		rowObjects = new ArrayList<>();
		rowObjects.add(new RowObject("set", "cat", "desc", "ind", "prov", "cap", "tel", "fax", "sito", "url"));
//		new ExcelIO("ItalianCompTotal.xlsx", WRITE, true).writeList(rowObjects);
		ExcelIO.appendList$ClearList(rowObjects, "ItalianCompTotal.xlsx", true);
		rowObjects = new ArrayList<>();
		rowObjects.add(new RowObject("set2", "cat2", "desc2", "ind2", "prov2", "cap2", "tel2", "fax2", "sito2", "url2"));
		ExcelIO.appendList$ClearList(rowObjects, "ItalianCompTotal.xlsx", true);
		if(true)return;


		process();

//		PrivateSchool.process();
	}

	public static void process() {
		open("http://aziende.lavorincasa.it/");
		ElementsCollection categories = $$("h2 a").shouldHaveSize(14);
		int defaultPageSize = 15;
		for (SelenideElement category : categories) {
			String categoryName = category.getText().trim();
			open(category.getAttribute("href"));

			SelenideElement firstPageButton = (SelenideElement) retryUntilAttached(() -> //todo extract method
					$("#list-navigation-container > a:first-child"));
			int lastPageNumber = Integer.valueOf(AlgoUtils.getFirstRegexMatch(firstPageButton.getText(), "\\d+\\s*$"));
			for (int pageNumber = 1; pageNumber <= lastPageNumber; pageNumber++) {
				print("Category: %s, page: %s, collected: %s", categoryName, pageNumber, rowObjects.size());
				/*ElementsCollection itemLinkOnPage = null;
				for (int i = 0; i < attempts; i++) {
					try { //todo make method wrapper. TakeElementWithAttempts(css, condition, attempts)
						//todo think about: strict shouldHave size vs soft version where we need to distinguish error from not full page
						itemLinkOnPage = $$("div.common-company-city a").shouldHaveSize(pageSize);
						print(itemLinkOnPage.get(pageSize - 1).shouldHave(Condition.attribute("href")));
						break;
					} catch (Error e) {
						print("Error attempt [%s] to take wrapped items on page. Message: %s", i + 1, e.getMessage());
					}
				}*/
				int pageSize = defaultPageSize;
				if (pageNumber == lastPageNumber) {
					humanWait(10000);
					pageSize = $$("div.common-company-city a").size();
				}
				for (int i = 0; i < pageSize; i++) { //process page
					final int elIndex = i;
					SelenideElement wrappedElement = (SelenideElement) retryUntilAttached(() ->
							$$("div.common-company-city a").get(elIndex));
					String companyCardUrl = wrappedElement.getAttribute("href").trim();

					openNewTabAndMap(wrappedElement, () -> {
						Item item = Item.collect(companyCardUrl);
						List<Pair> joinedList = new ArrayList<>(item.getPairs());
						joinedList.add(new Pair("URL", companyCardUrl));
						rowObjects.add(new RowObject(joinedList));
						return null;
					});

				}

				ExcelIO.appendList$ClearList(rowObjects, "ItalianCompTotal.xlsx", true);
				SelenideElement nextPage = $("#list-navigation-container > a[href='#'] + a");
				if (!nextPage.exists()) {
					break;
				}
				nextPage.click();
			}
			new ExcelIO("Italian" + categoryName + ".xlsx", WRITE, true).writeList(rowObjects);
			print("Saved category: %s. Memory has been flushed", categoryName);
			rowObjects = new ArrayList<>();
		}
		new ExcelIO("ItalianCompTotal.xlsx", WRITE, true).writeList(rowObjects);
	}
}