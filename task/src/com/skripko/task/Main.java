package com.skripko.task;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.common.AlgoUtils;
import com.skripko.common.ExcelIO;
import com.skripko.object.FieldSelector;
import com.skripko.object.dao.Item;
import com.skripko.object.dao.Pair;
import com.skripko.object.dao.RowObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.*;
import static com.skripko.common.ExcelIO.Mode.READ;
import static com.skripko.common.SelenideUtils.*;
import static com.skripko.object.FieldSelector.Option.GET_HREF;

//todo plus rotating pool of proxies -> +2 threads
//todo add stat for time estimation. Go through all categories if it is possible to know pages count. Then multiply on average of 5 pages processing
//todo stable proxy open with ip rotation and addition. I'm not sure about multiprocess addition of new proxies, but I can start second jvm with shared stable txt
//todo make Object-Modules: Paginator(Type.Scroll), List, Item, ScreenshotForSpeed ... Or maybe it is more appropriate to use functional approach
//todo smart email scraper. Google -> right filter first 3 results -> Find about, contacts, persons links and buttons

public class Main {
	public static final boolean isBrowserConfigured = configureBrowser(BrowserType.CHROME);
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
		while (true) {
			try {
				process();
				break;
			} catch (Throwable th) {
				print(">> Main fails: ");
				th.printStackTrace();
				Thread.sleep(30000);
			}
		}
	}

	public static void process(boolean... backwardArg) {
		int defaultPageSize = 15;
		int categoriesCount = 14;
		boolean backward = backwardArg.length != 0 && backwardArg[0];

		File ouputDir = new File(ExcelIO.DEFAULT_FILE_LOCATION);
		List<String> readyFiles = Arrays.asList(ouputDir.listFiles()).stream()
				.filter(File::isFile).map(File::getName).collect(Collectors.toList());
		open("http://aziende.lavorincasa.it/");
		ElementsCollection categories = $$("h2 a").shouldHaveSize(categoriesCount);
		if (backward) {
			if (!readyFiles.isEmpty()) {
				Collections.reverse(readyFiles);
			}
			Collections.reverse(categories);
		}

		int j = 0;
		for ( ; j < categoriesCount; j++) {
			SelenideElement category = categories.get(j);
			String probReadyFile = category.getText().trim() + ".xlsx";
			if (readyFiles.isEmpty() || !readyFiles.contains(probReadyFile)) {
				break;
			}
		}
		String pageNumberToContinue = "";
		if (j > 0) {
			int rowsCount = new ExcelIO(readyFiles.get(j - 1), READ, true).defineRowsCount(true);
			pageNumberToContinue = rowsCount == 0 ? "" : String.valueOf((rowsCount - 1) / defaultPageSize);
			rowObjects.add(new RowObject(Item.getNullItemWithMessage("Script continues", "<<").getPairs()));

			for ( ; j > 1; j--) {
				categories.remove(0);
			}
		}

		for (SelenideElement category : categories) {
			String categoryName = category.getText().trim();
			open(category.getAttribute("href") + (pageNumberToContinue.isEmpty() ? "" : pageNumberToContinue + "/"));

			SelenideElement firstPageButton = (SelenideElement) retryUntilAttached(() -> //todo extract method
					$("#list-navigation-container > a:first-child"));
			int lastPageNumber = Integer.valueOf(AlgoUtils.getFirstRegexMatch(firstPageButton.getText(), "\\d+\\s*$"));
			for (int pageNumber = pageNumberToContinue.isEmpty() ? 1 : Integer.parseInt(pageNumberToContinue);
																		pageNumber <= lastPageNumber; pageNumber++) {
				print("Category: %s, page: %s, collected: %s", categoryName, pageNumber, rowObjects.size());
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

				if (pageNumber % 20 == 0) {
					ExcelIO.appendList$ClearList(rowObjects, categoryName, true);
				}
				SelenideElement nextPage = $("#list-navigation-container > a[href='#'] + a");
				if (!nextPage.exists()) {
					ExcelIO.appendList$ClearList(rowObjects, categoryName, true);
					break;
				}
				nextPage.click();
			}
			pageNumberToContinue = "";
		}
	}
}