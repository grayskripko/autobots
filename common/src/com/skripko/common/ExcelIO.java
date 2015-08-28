package com.skripko.common;

import com.skripko.object.dao.RowObject;
import com.skripko.object.dao.StorageObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.skripko.common.SelenideUtils.print;

public class ExcelIO {
	public static final String DEFAULT_FILE_LOCATION = System.getProperty("user.home") + "/Desktop/out/";
	private String fullPath;
	private FileOutputStream outStream;
	private FileInputStream inStream;
	private XSSFWorkbook book;
	private XSSFSheet sheet;
	private int rowIndex;
	private boolean hasHeader;
	private boolean isInStreamExpired;

	public enum Mode {
		READ, WRITE
	}


	public ExcelIO(String fileName, Mode mode, boolean... hasHeader) {
		this(DEFAULT_FILE_LOCATION, fileName, mode, hasHeader);
	}

	public ExcelIO(String filePath, String fileName, Mode mode, boolean... hasHeader) {
		fullPath = filePath + (fileName.contains(".") ? fileName : fileName + ".xlsx");
		try {
			if (mode == Mode.READ) {
				inStream = new FileInputStream(fullPath);
				book = new XSSFWorkbook(inStream);
				if (hasHeader != null && hasHeader.length > 0) {
					this.hasHeader = hasHeader[0];
				}
			} else {
				outStream = new FileOutputStream(new File(fullPath));
				book = new XSSFWorkbook();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void printRow(boolean printAsString, Object... msg) {
		if (sheet == null) {
			createSheet("Unnamed");
		}
		XSSFRow row = sheet.createRow(rowIndex++);
		int colIndex = 0;
		for (Object obj : msg) {
			if (obj == null) {
				row.createCell(colIndex++).setCellValue("null");
				continue;
			}
			String s = obj.toString();
			if (!printAsString && s.matches("-?\\d+")) {
				row.createCell(colIndex++).setCellValue(Double.parseDouble(s));
			} else {
				row.createCell(colIndex++).setCellValue(s);
			}
		}
	}

	public void createSheet(String sheetName) {
		sheet = book.createSheet(sheetName);
		rowIndex = 0;
	}

	public String[] getSheetNames() {
		int sheetsCount = book.getNumberOfSheets();
		String[] result = new String[sheetsCount];
		for (int i = 0; i < result.length; i++) {
			result[i] = book.getSheetName(i);
		}
		return result;
	}

	public void removeCurrentSheet() {
		book.removeSheetAt(book.getSheetIndex(sheet));
	}

	public void close() {
		try {
			if (outStream != null) {
				book.setSheetOrder(sheet.getSheetName(), 0);
				book.write(outStream);
				outStream.close();
			} else {
				inStream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Iterator<Cell> getHeaderCellIterator(int sheetIndex) {
		if (!hasHeader) {
			throw new IllegalStateException("Table must have header");
		}
		checkInStream();
		sheet = book.getSheetAt(sheetIndex);
		Row row = sheet.rowIterator().next();
		return row.cellIterator();
	}

	/**remember: close inStream!*/
	public List readColumn(int sheetIndex, int columnIndex) {
		List result = new ArrayList<>();

		checkInStream();
		sheet = book.getSheetAt(sheetIndex);

		Iterator<Row> rowIterator = sheet.rowIterator();
		Iterator<Row> checkTypeInterator = sheet.rowIterator();
		if (!checkTypeInterator.hasNext()) {
			throw new IllegalArgumentException("Couldn't find rows");
		}
		Cell cell = checkTypeInterator.next().getCell(columnIndex);
		if (hasHeader) {
			cell = checkTypeInterator.next().getCell(columnIndex);
			rowIterator.next();
		}
		int cellType = cell.getCellType();

		while (rowIterator.hasNext()) {
			cell = rowIterator.next().getCell(columnIndex);
			switch (cellType) {
				case Cell.CELL_TYPE_STRING:
					String valStr = cell.getStringCellValue();
					if (valStr != null && !valStr.isEmpty()) {
						result.add(valStr);
					}
					break;
				case Cell.CELL_TYPE_NUMERIC:
					double valNum = cell.getNumericCellValue();
					if (Math.round(Math.abs(valNum)) != 0) {
						result.add(valNum);
					}
					break;
			}
		}

		if (cellType == Cell.CELL_TYPE_NUMERIC) {
			boolean isAllNumbersLong = true;
			for (Object obj : result) {
				double val = (Double) obj;
				if (val != Math.floor(val) || Double.isInfinite(val)) {
					isAllNumbersLong = false;
					break;
				}
			}
			if (isAllNumbersLong) {
				List<Long> temp = new ArrayList<>(result.size());
				for (int i = 0; i < result.size(); i++) {
					temp.add(i, ((Double) result.get(i)).longValue());
				}
				result = temp;
			}
		}

		isInStreamExpired = true;
		return result;
	}

	private void checkInStream() {
		if (isInStreamExpired) {
			try {
				inStream = new FileInputStream(fullPath);
				book = new XSSFWorkbook(inStream);
			} catch (IOException e) {
				throw new IllegalStateException("What the hell?! Not first call of file or inputStream is broken?");
			}
			isInStreamExpired = false;
		}
	}

	public static List<String> readListFromTxt(String fileName) {
		List<String> list = new ArrayList<>();
		try (Scanner sc = new Scanner(new File(fileName))) {
			while (sc.hasNext()) {
				list.add(sc.next());
			}
			sc.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return list.isEmpty() ? null : list;
	}

	public static void writeListToTxt(String fileName, List list) {
		try (PrintWriter writer = new PrintWriter(DEFAULT_FILE_LOCATION + fileName, "UTF-8")) {
			Object instance = list.get(0);
			if (instance instanceof RowObject || instance instanceof String) {
				list.stream().map(String::valueOf).forEachOrdered(writer::println);
			/*} else if (instance instanceof String) {
				list.stream().forEachOrdered(obj ->*/
			} else {
				List<Field> fields = Arrays.asList(instance.getClass().getFields())
						/*.stream().filter(Field::is).collect(Collectors.toList())*/;
				list.stream().forEachOrdered(obj ->
						writer.println(fields.stream().map(field -> {
							try {
								return String.valueOf(field.get(obj));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						}).collect(Collectors.joining("|"))));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<RowObject> readList(int... sheetIndexArg) { //todo check for bug
		int sheetIndex = sheetIndexArg.length == 0 ? 0 : sheetIndexArg[0];
		sheet = book.getSheetAt(sheetIndex);

		if (RowObject.needBuild()) {
			Iterator<Cell> headerCellIterator;
			headerCellIterator = getHeaderCellIterator(sheetIndex);
			List<String> header = new ArrayList<>();
			while (headerCellIterator.hasNext()) {
				Cell cell = headerCellIterator.next();
				header.add(cell.getStringCellValue());
			}
			RowObject.build(header);
		}

		List<RowObject> result = new LinkedList<>();
		Iterator<Row> rowIterator = sheet.rowIterator(); //todo check if need build of rowObject //
		for (int i = 0; sheet.getRow(++i) != null; ) { //read one line
			XSSFRow curRow = sheet.getRow(i);
			Iterator<Cell> cellIterator = curRow.cellIterator();
			List<String> list = new LinkedList<>();
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				list.add(cell.getStringCellValue());
			}
			result.add(new RowObject(list.toArray(new String[list.size()])));
			rowIterator.next();
		}
		return result.isEmpty() ? null : result;
	}

	public void writeList(List objects) {
		Object instance = objects.get(0);
		if (instance instanceof RowObject) {
			List<String> fieldsStr = RowObject.getLabels();
			printRow(true, fieldsStr.<String>toArray());
			objects.stream().forEachOrdered(object -> printRow(true, ((StorageObject) object).getClearValues().<String>toArray()));
		} else if (instance instanceof String) {
			objects.stream().forEachOrdered(object -> printRow(true, object));
		} else { //it is expected that object is simple class with open non-private fields
			Class aClass = objects.get(0).getClass();
			List<Field> fields = Arrays.asList(aClass.getDeclaredFields());
			printRow(true, fields.stream().map(Field::getName).collect(Collectors.toList()).<String>toArray());
			objects.stream().forEachOrdered(object ->
				printRow(true, fields.stream().map(field -> {
					try {
						return String.valueOf(field.get(object));
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e + Arrays.toString(fields.toArray()));
					}
				}).collect(Collectors.toList()).<String>toArray()) //don't forget about <String>toArray. Else output is [all list elements in one cell]
			);
		}
		close();
	}

	public static void appendList$ClearList(List objects, String fileName, boolean... hasHeader) {
		if (objects == null || objects.isEmpty()) {
			print("WARN: empty list to can not be appended");
			return;
		}
		String fullPath = DEFAULT_FILE_LOCATION + (fileName.contains(".") ? fileName : fileName + ".xlsx");
		if (new File(fullPath).exists()) {
			List alreadySaved = new ExcelIO(fileName, Mode.READ, hasHeader).readList();
			if (alreadySaved == null) {
				new ExcelIO(fileName, Mode.WRITE, hasHeader).writeList(objects);
			} else {
				alreadySaved.addAll(objects);
				new ExcelIO(fileName, Mode.WRITE, hasHeader).writeList(alreadySaved);
			}
		} else {
			new ExcelIO(fileName, Mode.WRITE, hasHeader).writeList(objects);
		}
		objects.removeAll(objects);
	}

	public int defineRowsCount(boolean... needClose) {
		checkInStream();
		int defaultSheetIndex = 0;
		sheet = book.getSheetAt(defaultSheetIndex);

		Iterator<Row> rowIterator = sheet.rowIterator();
		if (hasHeader) {
			rowIterator.next();
		}
		int result = 0;
		while (rowIterator.hasNext()) {
			rowIterator.next();
			result++;
		}

		if (needClose.length != 0 && needClose[0]) {
			close();
		}
		return result;

	}

	public static void test() {
		boolean testRowObject = true;

		class TestClassForMain {
			String name;
			String state;
			TestClassForMain(String name, String state) {
				this.name = name;
				this.state = state;
			}
		}

		ExcelIO excelIO = new ExcelIO("1.xlsx", Mode.WRITE, true);
		List<Object> objects = new LinkedList<>();
		if (testRowObject) {
			RowObject.build(Arrays.asList("schoolName", "state"));
			objects.add(new RowObject("FirstSchoolName", "FirstState"));
			objects.add(new RowObject("SecondSchoolName", "SecondState"));
		} else {
			objects.add(new TestClassForMain("Aa", "Bb"));
			objects.add(new TestClassForMain("Cc", "Dd"));
		}
		excelIO.writeList(objects);
//		writeListToTxt("1.txt", objects);
	}

	public static void clearCsv(String csvNameInDownloadDirectory) {
		final String downloadsPath = "D:/Downloads/";
		List<String> readedLines = readListFromTxt(downloadsPath + csvNameInDownloadDirectory);
		String content = readedLines.stream().collect(Collectors.joining());


	}

	public static void main(String[] args) {
		test();
//		clearCsv("auto.csv");
	}
}