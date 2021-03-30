package at.diwh.kreux.main;

import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import at.diwh.kreux.tools.SystemOutToSwing;

public class Application {

	public static JTextField inFileName = new JTextField(200);
	public static JTextField sapWartungCode = new JTextField(200);
	public static JTextField sapEntwicklungCode = new JTextField(200);

	public static JTextArea ausgabeTextFeld = new JTextArea();
	public static JCheckBox randomizeZeitenCheckBox = new JCheckBox();

	public static JCheckBox wartungCheckBox = new JCheckBox();
	public static JCheckBox entwicklungCheckBox = new JCheckBox();

	public static File inputFile = null;

	public static int BRUTTOARBEITSZEITZELLE = 41;
	public static Map<String, String> wochentagsmerker = new HashMap<>();

	public static int bemerkungIndex;
	public static int[] urlaubsIndexe = new int[60];
	public static boolean urlaubGefunden = false;

	public static String HOMEDIR = System.getProperty("user.home");
	public static String FILETRENNER = File.separator; // nicht (!) pathSeparator, denn der pathSeparator ist für
														// Classpath-Abschnitte, also klassisch der Doppelpunkt
	public static String xlsxFilename = null; // HOMEDIR + FILETRENNER + "Downloads" + FILETRENNER + "XtraReport.xlsx";

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		String lookAndFeelClassName = "";
		// lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
		lookAndFeelClassName = UIManager.getCrossPlatformLookAndFeelClassName();
		// lookAndFeelClassName = "javax.swing.plaf.metal.MetalLookAndFeel"; // cross
		// platform Metal
		// lookAndFeelClassName = "javax.swing.plaf.metal.MetalLookAndFeel"; // cross
		// platform Metal
		// lookAndFeelClassName = "com.sun.java.swing.plaf.motif.MotifLookAndFeel"; //
		// MOTIF on any platform
		// lookAndFeelClassName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		// // WDos, will not work on other platforms

		UIManager.setLookAndFeel(lookAndFeelClassName);

		JFrame frame = new JFrame();

		frame.setTitle("KREUX");

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // Beende Programm bei Close des Fensters
		frame.setSize(1000, 500);
		frame.setLocationRelativeTo(null); // Mitte des Bildschirms

		JPanel meinPanel = new JPanel();
		frame.add(meinPanel);
		meinPanel.setLayout(null);

		// Input Filename
		JLabel inFileNameLabel = new JLabel("Input File:");
		inFileNameLabel.setBounds(10, 35, 100, 25);
		meinPanel.add(inFileNameLabel);

		inFileName.setBounds(110, 35, 750, 100);
		inFileName.setText("Datei hier herein ziehen.");
		inFileName.setEditable(false);
		inFileName.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			@Override
			@SuppressWarnings("unchecked")
			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>) evt.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					for (File file : droppedFiles) {
						inputFile = file;
						inFileName.setText(inputFile.getPath());
						xlsxFilename = inputFile.getPath();
						say("File " + xlsxFilename + " wurde als Quelle gewählt.");
						Boolean splitTimes = Boolean.FALSE;
						if (wartungCheckBox.isSelected() && entwicklungCheckBox.isSelected()) {
							splitTimes = Boolean.TRUE;
						}
						if (!wartungCheckBox.isSelected() && !entwicklungCheckBox.isSelected()) {
							say("Sie MÜSSEN zumindest einen der SAP-Bereiche auswählen! KEIN FILE wurde geschrieben, weil ich nicht weiß, was Sie wollen!");
							return;
						}
						if (Boolean.valueOf(randomizeZeitenCheckBox.isSelected())
								&& !(wartungCheckBox.isSelected() && entwicklungCheckBox.isSelected())) {
							say("Sie wollen, dass ich auf nur einer Zeile die Zeiten verzufallen?! KEIN FILE wurde geschrieben, weil ich nicht weiß, was Sie wollen!");
							return;
						}
						String outReturn = executeTransformation(xlsxFilename, splitTimes,
								Boolean.valueOf(randomizeZeitenCheckBox.isSelected()));
						say("File " + outReturn + " wurde geschrieben.");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		meinPanel.add(inFileName);

		JLabel sapWartungLabel = new JLabel("SAP Wartung: ");
		sapWartungLabel.setBounds(10, 140, 100, 25);
		meinPanel.add(sapWartungLabel);

		sapWartungCode.setBounds(110, 140, 100, 25);
		sapWartungCode.setText("A4050013");
		meinPanel.add(sapWartungCode);
		wartungCheckBox.setBounds(220, 140, 25, 25);
		meinPanel.add(wartungCheckBox);

		JLabel sapEntwicklungLabel = new JLabel("SAP Entwicklung: ");
		sapEntwicklungLabel.setBounds(10, 165, 100, 25);
		meinPanel.add(sapEntwicklungLabel);

		sapEntwicklungCode.setBounds(110, 165, 100, 25);
		sapEntwicklungCode.setText("A4050011");
		meinPanel.add(sapEntwicklungCode);
		entwicklungCheckBox.setBounds(220, 165, 25, 25);
		entwicklungCheckBox.setSelected(true);
		meinPanel.add(entwicklungCheckBox);

		// Check Box f Random
		JLabel randomizeZeitenLabel = new JLabel("Verzufalle die Teilung");
		randomizeZeitenLabel.setBounds(10, 200, 120, 25);
		meinPanel.add(randomizeZeitenLabel);
		randomizeZeitenCheckBox.setBounds(130, 200, 25, 25);
		meinPanel.add(randomizeZeitenCheckBox);

		// Ausgabetextfeld
		ausgabeTextFeld.setLineWrap(true);
		ausgabeTextFeld.setWrapStyleWord(true);
		Font fontFuerText = new Font("Courier", Font.BOLD, 12);
		ausgabeTextFeld.setFont(fontFuerText);
		ausgabeTextFeld.setBounds(10, 240, 900, 200);
		meinPanel.add(ausgabeTextFeld);

		frame.setVisible(true);

		// redirect StdOut to TextArea
		PrintStream printStream = null;
		try {
			printStream = new PrintStream(new SystemOutToSwing(ausgabeTextFeld));
			System.setOut(printStream);
			System.setErr(printStream);
		} catch (IOException e) {
			System.out.println("Warnung: Umleitung von System.out zu Swing hat nicht funktioniert."
					+ "\nIst kein Problem an sich, sollte aber bekannt sein. \nGrund: " + e.getMessage());
		}

	}

	/**
	 * Wie ein System.out.writeln (mit autom. newline). Nur halt in den
	 * Standard-Ausgabe-Bereich am Schirm.
	 * 
	 * @param wordsToSay : Worte, nichts als Worte
	 */
	static void say(String wordsToSay) {
		ausgabeTextFeld.append(wordsToSay + "\n");
	}

	@SuppressWarnings("resource")
	public static String executeTransformation(String xlsxFN, Boolean split, Boolean randomTimes) throws IOException {
		String xlsxCatsFilename = xlsxFN + ".CATS.xlsx";
		Workbook w = loadExcel(xlsxFN);
		Sheet s = (w != null ? w.getSheet("Sheet") : new XSSFWorkbook().createSheet());
		BRUTTOARBEITSZEITZELLE = scanneSheetAufbau(s);
		fillWochentagsmerker(s);

//        printSheet(s);
//        
		List<Row> datensaetze = fetchAllLinePerDay(s);
//        for (Row element : datensaetze) {
//            printLine(element);
//        }

		Workbook outW = new XSSFWorkbook();
		Sheet outS = outW.createSheet();
		Row outHeaderR = outS.createRow(0); // Header
		Cell firstHeaderCell = outHeaderR.createCell(0);
		firstHeaderCell.setCellValue("SAP Code");
		Cell emptySecondHeaderCell = outHeaderR.createCell(1);
		emptySecondHeaderCell.setCellValue("");
		Cell emptyThirdHeaderCell = outHeaderR.createCell(2);
		emptyThirdHeaderCell.setCellValue("");

		Row outDataR1 = outS.createRow(1); // Data
		Cell firstDataR1Cell = outDataR1.createCell(0);
		if (wartungCheckBox.isSelected()) {
			firstDataR1Cell.setCellValue(sapWartungCode.getText());
		} else {
			firstDataR1Cell.setCellValue(sapEntwicklungCode.getText());
		}
		Cell emptySecondDataR1Cell = outDataR1.createCell(1);
		emptySecondDataR1Cell.setCellValue("");
		Cell emptyThirdDataR1Cell = outDataR1.createCell(2);
		emptyThirdDataR1Cell.setCellValue("");

		Row outDataR2 = null;
		if (split.booleanValue()) {
			outDataR2 = outS.createRow(2);
			Cell firstDataR2Cell = outDataR2.createCell(0);
			firstDataR2Cell.setCellValue(sapEntwicklungCode.getText());
			Cell emptySecondDataR2Cell = outDataR2.createCell(1);
			emptySecondDataR2Cell.setCellValue("");
			Cell emptyThirdDataR2Cell = outDataR2.createCell(2);
			emptyThirdDataR2Cell.setCellValue("");
		}
		Row urlaubsData = null;
		if (urlaubGefunden) {
			if (outDataR2 == null) {
				urlaubsData = outS.createRow(2);
			} else {
				urlaubsData = outS.createRow(3);
			}
			Cell firstUCell = urlaubsData.createCell(0);
			firstUCell.setCellValue("A2099999");
			Cell emptySecondUCell = urlaubsData.createCell(1);
			emptySecondUCell.setCellValue("");
			Cell emptyThirdUCell = urlaubsData.createCell(2);
			emptyThirdUCell.setCellValue("");
		}
		int i = 3;
		int index = 0;

		for (Row element : datensaetze) {
			System.out.println("Am " + fetchWochentagFromRow(element) + " (" + fetchTagesdatumFromRow(element) + ") : "
					+ fetchAzFromRow(element) + " CATS: " + fetchCatsAzFromRow(element));
			String cellElementHeader = fetchWochentagFromRow(element) + " " + fetchTagesdatumFromRow(element);
			String cellElementData = fetchCatsAzFromRow(element);
			Cell headerC = outHeaderR.createCell(i);
			Cell dataC1 = outDataR1.createCell(i);
			Cell dataC2 = null;
			if (split.booleanValue()) {
				dataC2 = outDataR2.createCell(i);
			}
			Cell dataU = null;
			if (urlaubGefunden) {
				dataU = urlaubsData.createCell(i);
			}
			headerC.setCellValue(cellElementHeader + "(" + cellElementData + ")");
			if (urlaubGefunden && urlaubsIndexe[index] > 0) {
				dataU.setCellValue(cellElementData);
				System.out.println("Urlaubszeile eingetragen. (" + cellElementData + ")");
			} else {
				dataC1.setCellValue(cellElementData);
				if (split.booleanValue()) {
					String[] splittedTimes = splitCellValue(cellElementData, randomTimes);
					dataC1.setCellValue(splittedTimes[0]);
					dataC2.setCellValue(splittedTimes[1]);
				}
			}
			i++;
			index++;
		}
		Workbook writeW = makeWeekends(outHeaderR, outDataR1, outDataR2, urlaubsData);

		FileOutputStream outputStream = new FileOutputStream(xlsxCatsFilename);
		writeW.write(outputStream);
		outputStream.close();
		outW.close();
		writeW.close();
		System.out.println("Geschrieben: " + xlsxCatsFilename);
		return xlsxCatsFilename;
	}

	public static String[] splitCellValue(String cv, Boolean randomizeIT) {
		System.out.println("\t Übergebener String: " + cv);
		String[] ergebnis = new String[2];
		String h = cv.substring(0, cv.indexOf(","));
		String m = cv.substring(cv.indexOf(",") + 1, cv.length());
		System.out.println("h= " + h + " m= " + m);

		int minutenInTeilung15Min = (Integer.valueOf(m).intValue() / 25);
		System.out.println("\tminutenInTeilung15Min: " + minutenInTeilung15Min);

		int stundenInTeilung15Min = (Integer.valueOf(h).intValue() * 4);
		System.out.println("\tstundenInTeilung15Min " + stundenInTeilung15Min);

		int gesamtInTeilung15 = stundenInTeilung15Min + minutenInTeilung15Min;
		System.out.println("\tgesamtInTeilung15: " + gesamtInTeilung15);

		int ganzeStunden = gesamtInTeilung15 / 4;
		System.out.println("\tGanze Stunden " + ganzeStunden);

		int restNachGanzeStunden = gesamtInTeilung15 % 4;
		System.out.println("\tRest in 15er Teilung: " + restNachGanzeStunden);

		double h1 = ganzeStunden / 2.0;
		double h2 = ganzeStunden / 2.0;
		System.out.println("Geteilte ganze Stunden: " + h1 + " und " + h2);

		if (restNachGanzeStunden == 1) {
			h1 = h1 + 0.25;
		} else if (restNachGanzeStunden == 2) {
			h1 = h1 + 0.25;
			h2 = h2 + 0.25;
		} else if (restNachGanzeStunden == 3) {
			h1 = h1 + 0.50;
			h2 = h2 + 0.25;
		}

		if (randomizeIT.booleanValue()) {
			int rounds = liefereZufallszahl(2, 20);
			for (int i = 0; i < rounds; i++) {
				if (h1 - 0.25 > 0) {
					h1 = h1 - 0.25;
					h2 = h2 + 0.25;
				}
			}
			rounds = liefereZufallszahl(2, 20);
			for (int i = 0; i < rounds; i++) {
				if (h2 - 0.25 > 0) {
					h2 = h2 - 0.25;
					h1 = h1 + 0.25;
				}
			}
			System.out.println("Stunden nach Verzufallung: " + h1 + " und " + h2);
		}

		ergebnis[0] = String.valueOf(h1).replace('.', ',');
		ergebnis[1] = String.valueOf(h2).replace('.', ',');

		return ergebnis;
	}

	public static void fillWochentagsmerker(Sheet s) {
		Iterator<Row> iterR = s.rowIterator();
		while (iterR.hasNext()) {
			Row r = iterR.next();
			Cell c00 = r.getCell(0);
			Cell c01 = r.getCell(1);
			if (c01 != null && c01.getStringCellValue() != null && !c01.getStringCellValue().isEmpty() && c00 != null
					&& c00.getStringCellValue() != null && !c00.getStringCellValue().isEmpty()) {
				wochentagsmerker.put(c01.getStringCellValue(), c00.getStringCellValue());
				System.out.println("Setze " + c01.getStringCellValue() + " -> " + c00.getStringCellValue());
			}
		}
	}

	public static int scanneSheetAufbau(Sheet s) {

		Iterator<Row> iterR = s.rowIterator();
		while (iterR.hasNext()) {
			Row r = iterR.next();
//            System.out.print("Durchsuche: ");
//            printLine(r);
			Iterator<Cell> iterC = r.cellIterator();
			while (iterC.hasNext()) {
				Cell c = iterC.next();
//                System.out.println("\t Prüfe ["+c.getStringCellValue()+"] ob es Brutto ist");
				if (c.getStringCellValue() != null && c.getStringCellValue().trim().startsWith("Bemerkung")) {
					System.out.println("Bemerkung-Spalte gefunden: " + c.getColumnIndex());
					bemerkungIndex = c.getColumnIndex(); // global gesetzt (schlechter Stil, aber Urlaubserkennung wurde
															// angeflanscht)
				}
				if (c.getStringCellValue() != null && c.getStringCellValue().trim().startsWith("Brutto")
						&& c.getStringCellValue().trim().endsWith("Tag")) {
					System.out.println("Brutto Spalte gefunden: " + c.getColumnIndex());
					return c.getColumnIndex();
				}
			}
		}
		System.out.println("Tödlicher Fehler: Kein erfolgreicher Scan nach Spalte Brutto");
		return 0;
	}

	public static List<Row> fetchAllLinePerDay(Sheet s) {
		List<Row> ergebnis = new ArrayList<>();
		Iterator<Row> rowIt = s.rowIterator();
		int index = 0;
		while (rowIt.hasNext()) {
			Row r = rowIt.next();
			// die relevante Zeile hat auf Zelle 43 einen Wert für Tagespause und auf Zelle
			// 21 einen Gevierteltstrich ("-")
//            Cell c21 = r.getCell(21+korrektur);
//            Cell c43 = r.getCell(43+korrektur);
//            if ((c21 != null && c43 != null) // Zur Sicherheit 
//                && // Wenn folgende Felder belegt sind, ist es eine Datenzeile
//                ("-".equals(c21.getStringCellValue()) && !c43.getStringCellValue().isEmpty())) {
//                    ergebnis.add(r);
//            }
			Cell c = r.getCell(BRUTTOARBEITSZEITZELLE);
			if (c != null && c.getStringCellValue().contains(".")) {
				ergebnis.add(r);
				Cell uc = r.getCell(bemerkungIndex);
				if (uc != null && uc.getStringCellValue().startsWith("101")
						&& uc.getStringCellValue().endsWith("Urlaub")) {
					urlaubGefunden = true;
					urlaubsIndexe[index] = index;
					System.out.println("Habe Zeile mit Urlaub gefunden: " + index);
				}
				index++;
			}
		}
		return ergebnis;
	}

	public static void printSheet(Sheet s) {
		Iterator<Row> rowiterator = s.rowIterator();
		while (rowiterator.hasNext()) {
			Row r = rowiterator.next();
			printLine(r);
		}
		System.out.println();
	}

	public static void printLine(Row r) {
		Iterator<Cell> celliterator = r.cellIterator();
		while (celliterator.hasNext()) {
			Cell c = celliterator.next();
			try {
				System.out.print(c.getStringCellValue() + " ");
			} catch (Exception e) {
				System.out.print("#");
			}
		}
		System.out.println();
	}

	public static Float fetchAzFromRow(Row r) {
		Float ergebnis = null;
		// Arbeitszeit ist auf Index 41 (wenn die Row bereits als Datenrow verifiziert
		// ist)
		ergebnis = Float.valueOf(r.getCell(BRUTTOARBEITSZEITZELLE).getStringCellValue());
		return ergebnis;
	}

	public static String fetchCatsAzFromRow(Row r) {
		String ergebnis = null;
		// Arbeitszeit ist auf Index 41 (wenn die Row bereits als Datenrow verifiziert
		// ist)
		String tmp = r.getCell(BRUTTOARBEITSZEITZELLE).getStringCellValue();
		String h = tmp.substring(0, tmp.indexOf("."));
		String m = tmp.substring(tmp.indexOf(".") + 1, tmp.length());
		int minutenIn25 = (Integer.valueOf(m).intValue() / 15) * 25;
		ergebnis = h + "," + minutenIn25;
		return ergebnis;
	}

	public static String fetchWochentagFromRow(Row r) {
		String ergebnis = null;
		// Arbeitszeit ist auf Index 0 (wenn die Row bereits als Datenrow verifiziert
		// ist)
		ergebnis = r.getCell(0).getStringCellValue();
		if (ergebnis == null || ergebnis.trim().isEmpty()) {
			ergebnis = wochentagsmerker.get(r.getCell(1).getStringCellValue());
		}
		return ergebnis;
	}

	public static String fetchTagesdatumFromRow(Row r) {
		String ergebnis = null;
		// Arbeitszeit ist auf Index 1 (wenn die Row bereits als Datenrow verifiziert
		// ist)
		ergebnis = r.getCell(1).getStringCellValue();
		if (ergebnis == null || ergebnis.trim().isEmpty()) {
			ergebnis = wochentagsmerker.get(r.getCell(1).getStringCellValue());
		}
		return ergebnis;
	}

	public static Workbook loadExcel(String filename) {
		Workbook result = null;
		if (Files.exists(new File(filename).toPath())) {
			try {
				result = WorkbookFactory.create(new File(filename));
			} catch (EncryptedDocumentException | IOException e) {
				e.printStackTrace();
			}

		}

		return result;
	}

	public static Workbook makeWeekends(Row r1, Row r2, Row r3, Row r4) {
		Workbook resultW = new XSSFWorkbook();
		Sheet resultS = resultW.createSheet();
		int rowNeuIndex = 0;
		Row resultHeaderR = resultS.createRow(rowNeuIndex); // Header
		rowNeuIndex++;
		Row resultDataR1 = resultS.createRow(rowNeuIndex); // Data
		rowNeuIndex++;
		Row resultDataR2 = null;
		Row resultDataR3 = null;
		if (r3 != null) {
			resultDataR2 = resultS.createRow(rowNeuIndex);
			rowNeuIndex++;
		}
		if (r4 != null) {
			resultDataR3 = resultS.createRow(rowNeuIndex);
			rowNeuIndex++;
		}
		int maxIndex = r1.getPhysicalNumberOfCells();
		int zielIndex = 0;

		for (int idx = 0; idx < maxIndex; idx++) {
			Cell r1Cell = r1.getCell(idx);
			Cell r2Cell = r2.getCell(idx);
			Cell r3Cell = (r3 != null ? r3.getCell(idx) : null);
			Cell r4Cell = (r4 != null ? r4.getCell(idx) : null);

			String scanStringR1 = r1Cell.getStringCellValue();
			String scanStringR2 = r2Cell.getStringCellValue();
			String scanStringR3 = (r3Cell != null ? r3Cell.getStringCellValue() : "");
			String scanStringR4 = (r4Cell != null ? r4Cell.getStringCellValue() : "");

			Cell resultCellR1 = resultHeaderR.createCell(zielIndex);
			Cell resultCellR2 = resultDataR1.createCell(zielIndex);
			Cell resultCellR3 = (resultDataR2 != null ? resultDataR2.createCell(zielIndex) : null);
			Cell resultCellR4 = (resultDataR3 != null ? resultDataR3.createCell(zielIndex) : null);

			if (scanStringR1.startsWith("Mo")) {
				System.out.println("Montag gefunden auf Index " + idx);
				int zusatztage = 2;
				if (scanStringR1.startsWith("Mo 01")) {
					zusatztage = 0;
				} else if (scanStringR1.startsWith("Mo 02")) {
					zusatztage = 1;
				}
				for (int k = 0; k < zusatztage; k++) {
					zielIndex++;
					resultCellR1 = resultHeaderR.createCell(zielIndex);
					resultCellR2 = resultDataR1.createCell(zielIndex);
					resultCellR3 = (resultDataR2 != null ? resultDataR2.createCell(zielIndex) : null);
					resultCellR4 = (resultDataR3 != null ? resultDataR3.createCell(zielIndex) : null);
				}
			}
			resultCellR1.setCellValue(scanStringR1);
			resultCellR2.setCellValue(scanStringR2);
			if (resultCellR3 != null) {
				resultCellR3.setCellValue(scanStringR3);
			}
			if (resultCellR4 != null) {
				resultCellR4.setCellValue(scanStringR4);
			}

			zielIndex++;
		}
		System.out.println("All shift done.");

		return resultW;
	}

	/**
	 * Diese Methode liefert eine (ganze) int-Zufallszahl von <u>von</u> bis
	 * <u>bis</u> und zwar wirklich von-bis, also explizit <b>inklusive</b> der
	 * beiden Grenzwerte. Ein Aufruf mit <i>liefereZufallszahl(1,6)</i> entspricht
	 * also einem Wurf mit einem üblichen, sechsseitigen Würfel. <br>
	 * <b>Grund:</b><br>
	 * Mich hat immer genervt, nachdenken zu müssen ob diese "von bis"-Angaben die
	 * Grenzen enthalten. Zudem drücken sich viele Menschen richtig bescheiden aus
	 * und meinen mit einem Satz wie "Eine Zahl <i>zwischen</i> 1 und 6" nicht etwa,
	 * 2, 3, 4 oder 5 sondern auch 1 und 6, obwohl sie <i>zwischen</i> gesagt
	 * haben.<br>
	 *
	 * @param von (Untere Grenzen, inklusive) - int
	 * @param bis (Obere Grenze, inklusive) - int
	 * @return int: Zufallszahl
	 */
	public static int liefereZufallszahl(int von, int bis) {
		if (von > bis) { // tausche, ganz elegant mit XOR: a XOR b XOR a = a XOR a XOR b = 0000 XOR b = b
			von ^= bis; // 1010 XOR 1100 = 0110 (von ist nun der xor-"Schlüssel"
			bis ^= von; // 1100 XOR 0110 = 1010 (bis ist nun von)
			von ^= bis; // 0110 XOR 1010 = 1100 (von ist nun bis)
		}
		return von + (int) (Math.random() * ((bis - von) + 1));
	}
}