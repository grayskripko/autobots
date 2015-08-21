package com.skripko.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelIO {
	private static final String DEFAULT_FILE_LOCATION = System.getProperty("user.home") + "/Desktop/";
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

	public void printRow(Object... msg) {
		if (sheet == null) {
			createSheet("Unnamed");
		}

		XSSFRow row = sheet.createRow(rowIndex++);
		int colIndex = 0;
		for (Object obj : msg) {
			String s = obj.toString();
			boolean isNumeric = s.matches("-?\\d+");
			if (isNumeric) {
				row.createCell(colIndex++).setCellValue(Double.parseDouble(s));
			} else {
				row.createCell(colIndex++).setCellValue(s);
			}
		}
	}

	public void printFirstRow(Object... msg) {
		rowIndex = 0;
		printRow(msg);
	}

	public void createSheet(String sheetName) {
		sheet = book.createSheet(sheetName);
		rowIndex = 1;
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

	public List readColumn(int sheetIndex, String columnName) {
		Iterator<Cell> cellIterator = getHeaderCellIterator(sheetIndex);
		int i = 0;
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			if (columnName.equals(cell.getStringCellValue())) {
				return readColumn(sheetIndex, i);
			}
			i++;
		}
		throw new IllegalArgumentException("No such column");
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
		try (PrintWriter writer = new PrintWriter(fileName, "UTF-8")) {
			list.stream().forEach(writer::println);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public List<RowObject> readListFromXlsx(int... sheetIndexArg) {
		int sheetIndex = sheetIndexArg.length == 0 ? 1 : sheetIndexArg[0];
		Iterator<Cell> cellIterator = getHeaderCellIterator(sheetIndex);

		List<String> header = new ArrayList<>();
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			header.add(cell.getStringCellValue());
		}
		RowObject.sculptRowObjectShapeByHeader(header);

		List<RowObject> result = new LinkedList<>();
		for(int i = 1; i < header.size(); i++) {
			cellIterator = sheet.getRow(i).cellIterator();
			RowObject rowObject = new RowObject();
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				rowObject.setNextField(cell.getStringCellValue());
			}
			result.add(rowObject);
		}
		return result.isEmpty() ? null : result;
	}

	public void writeListToXlsx(List objects) {
		Class aClass = objects.get(0).getClass();
		List<Field> fields = Arrays.asList(aClass.getDeclaredFields());

		printFirstRow(fields.stream().map(Field::getName).collect(Collectors.toList())); //todo just remember about list vararg
		objects.stream().forEach(object -> printRow(fields.stream().map(field -> {
			try {
				return String.valueOf(field.get(object));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList())));
		close();
	}

	public static void main(String[] args) {
		ExcelIO excelIO = new ExcelIO("1.xlsx", Mode.WRITE);
		excelIO.createSheet("testThis");
		excelIO.printFirstRow("firstTh", "secondTh");
		excelIO.printRow("firstTd", "secondTd");
		excelIO.close();
		/*ExcelIO excelIO = new ExcelIO("freelanceCompare", ExcelIO.READ_MODE, true);
		StringBuilder result = new StringBuilder();
		List column = excelIO.readColumn(0, 1);
		for (Object obj : column) {
			result.append(String.valueOf(obj)).append(" ");
		}
		System.out.println(result.toString());*/
	}

}