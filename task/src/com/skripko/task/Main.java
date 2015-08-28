package com.skripko.task;

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
	public static final boolean isBrowserConfigured = configureBrowser(BrowserType.PHANTOMJS);
	static boolean isBackward = false;
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
				new FieldSelector("SITO WEB", "div.common-company-website a.button-inline").setNeedFastProcess().setOption(GET_HREF));

		RowObject.build(Item.getLabels());
		RowObject.append("URL");
	}

	public static void main(String[] args) throws Exception {
//		applyProxy$ClosePrevBrowser(getFastProxies(getProxyInfoList(), 1).get(0));
//		print(SelenideUtils.saveImage("http://www.infoedile.it/images/g.elettrica.jpg", "pic.jpg"));
		buildRowAndItem();
		while (true) {
			try {
				process(isBackward);
				break;
			} catch (Throwable th) {
				print(">> Main fails: ");
				th.printStackTrace();
				Thread.sleep(20000);
			}
		}
	}

	public static void process(boolean... backwardArg) {
		int defaultPageSize = 15;
		int categoriesCount = 14;
		int savingIntervalInPages = 10;
		boolean backward = backwardArg.length != 0 && backwardArg[0];

		File ouputDir = new File(ExcelIO.DEFAULT_FILE_LOCATION);
		List<String> readyFiles = Arrays.asList(ouputDir.listFiles()).stream()
				.filter(File::isFile).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
		open("http://aziende.lavorincasa.it/");
		List<SelenideElement> categories = new ArrayList<>($$("h2 a").shouldHaveSize(categoriesCount));
		if (backward) {
			if (!readyFiles.isEmpty()) {
				Collections.reverse(readyFiles);
			}
			Collections.reverse(categories);
		}

		int j = 0;
		for (; j < categoriesCount; j++) {
			SelenideElement category = categories.get(j);
			String probReadyFile = category.getText().trim() + ".xlsx";
			if (readyFiles.isEmpty() || !readyFiles.contains(probReadyFile)) {
				break;
			}
		}
		int pageNumberToContinue = 1;
		if (j > 0) {
			int rowsCount = new ExcelIO(readyFiles.get(j - 1), READ, true).defineRowsCount(true);
			pageNumberToContinue = rowsCount <= defaultPageSize ? 1 : 1 + (rowsCount - 1) / defaultPageSize;
			List<Pair> joinedList = new ArrayList<>(Item.getNullItemWithMessage("Script continues", "<<").getPairs());
			joinedList.add(new Pair("URL", "<<"));
			rowObjects.add(new RowObject(joinedList));
			for (; j > 1; j--) {
				categories.remove(0);
			}
		}

		List<String> categoryLinks = categories.stream().map(cat -> cat.getAttribute("href")).collect(Collectors.toList());
		List<String> categoryNames = categories.stream().map(SelenideElement::getText).collect(Collectors.toList());
		for (int i = 0; i < categoryLinks.size(); i++) {
			String categoryName = categoryNames.get(i).trim();
			open(categoryLinks.get(i) + pageNumberToContinue); //todo reminder: every open is happens-before for SelenideElement

			SelenideElement curPageButton = (SelenideElement) retryUntilAttached(() -> //todo extract method
					$("#list-navigation-container > a[href='#']"));
			int lastPageNumber = Integer.valueOf(AlgoUtils.getFirstRegexMatch(curPageButton.getText(), "\\d+\\s*$"));

			for (int pageNumber = pageNumberToContinue; pageNumber <= lastPageNumber; pageNumber++) {
				print("Category: %s, page: %s, collected: %s", categoryName, pageNumber, rowObjects.size());
				String itemLinksCssPath = "div.common-company-city a";
				if (pageNumber == lastPageNumber) {
					humanWait(10000);
				} else {
					retryUntilAttached(() -> $$(itemLinksCssPath).get(defaultPageSize - 1));
				}
				List<String> wrapperItemLinkEls = new ArrayList<>($$(itemLinksCssPath).stream()
						.map(el -> el.getAttribute("href")).collect(Collectors.toList())) ;
				for(String wrapperItemLink : wrapperItemLinkEls) { //process page
					open(wrapperItemLink);
					Item item = Item.collect(wrapperItemLink);
					List<Pair> joinedList = new ArrayList<>(item.getPairs());
					joinedList.add(new Pair("URL", wrapperItemLink));
					rowObjects.add(new RowObject(joinedList));
				}

				open(categoryLinks.get(i) + (pageNumber + 1));
				if (pageNumber % savingIntervalInPages == 0) {
					ExcelIO.appendList$ClearList(rowObjects, categoryName, true);
				}
			}
			ExcelIO.appendList$ClearList(rowObjects, categoryName, true);
			pageNumberToContinue = 1;
		}
	}
}