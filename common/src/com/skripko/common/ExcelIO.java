package com.skripko.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelIO {
	private static final String defaultFileLocation = System.getProperty("user.home") + "/Desktop/";
	private String fullPath;
	private FileOutputStream outStream;
	private FileInputStream inStream;
	private boolean hasHeader;
	private XSSFWorkbook book;
	private XSSFSheet sheet;
	private int rowIndex;
	private boolean isInStreamExpired;

	public static int READ_MODE = 0;
	public static int WRITE_MODE = 1;


	public ExcelIO(String fileName, int mode, boolean... hasHeader) throws IOException {
		this(defaultFileLocation, fileName, mode, hasHeader);
	}

	public ExcelIO(String filePath, String fileName, int mode, boolean... hasHeader) throws IOException {
		this.fullPath = filePath + (fileName.contains(".") ? fileName : fileName + ".xlsx");
		if (mode == 0) {
			inStream = new FileInputStream(fullPath);
			book = new XSSFWorkbook(inStream);
			if (hasHeader != null && hasHeader.length > 0) {
				this.hasHeader = hasHeader[0];
			}
		} else {
			outStream = new FileOutputStream(new File(fullPath));
			book = new XSSFWorkbook();
		}
	}

	public void printRow(Object... msg) throws IllegalAccessException {
		if (sheet == null) {
			createSheet("Unnamed");
		}

		XSSFRow row = sheet.createRow(rowIndex++);
		int colIndex = 0;
		for (Object obj : msg) {
			String s = obj.toString();
			boolean isNumeric = s.matches("-?\\d+");
			if (!isNumeric) {
				row.createCell(colIndex++).setCellValue(s);
			} else {
				row.createCell(colIndex++).setCellValue(Double.parseDouble(s));
			}
		}
	}

	public void printFirstRow(Object... msg) throws IllegalAccessException {
		rowIndex = 0;
		printRow(msg);
	}

	public void createSheet(String sheetName) throws IllegalAccessException {
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

	public void close() throws IllegalAccessException, IOException {
		if (outStream != null) {
			book.setSheetOrder(sheet.getSheetName(), 0);
			book.write(outStream);
			outStream.close();
		} else {
			inStream.close();
		}
	}

	public List readColumn(int sheetIndex, String columnName) {
		if (sheetIndex < 0 || columnName == null || columnName.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (!hasHeader) {
			throw new IllegalStateException("Table must have header");
		}
		checkInStream();
		sheet = book.getSheetAt(sheetIndex);
		Iterator<Row> rowIterator = sheet.rowIterator();
		if (!rowIterator.hasNext()) {
			throw new RuntimeException("Couldn't find rows");
		}
		Row row = sheet.rowIterator().next();
		Iterator<Cell> cellIterator = row.cellIterator();
		if (!cellIterator.hasNext()) {
			throw new RuntimeException("Couldn't find cells");
		}

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

	/**remember: close inStream!*/
	public List readColumn(int sheetIndex, int columnIndex) {
		checkInStream();
		List result = new ArrayList<>();
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
					if (valNum != 0) {
						result.add(valNum);
					}
					break;
			}
		}

		if (cellType == Cell.CELL_TYPE_NUMERIC) {
			boolean isAllNumbersLong = true;
			for (Object obj : result) {
				double val = (Double) obj;
				if ((val != Math.floor(val)) || Double.isInfinite(val)) {
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

	public static void main(String[] args) throws IOException, IllegalAccessException {
		ExcelIO excelIO = new ExcelIO("1.xlsx", ExcelIO.WRITE_MODE);
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