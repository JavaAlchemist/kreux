package at.diwh.kreux.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
public class Application {
    
    public static int BRUTTOARBEITSZEITZELLE = 41;
    public static Map<String, String> wochentagsmerker = new HashMap<>();
    
    public static String HOMEDIR = System.getProperty("user.home");
	public static String FILETRENNER = File.separator; // nicht (!) pathSeparator, denn der pathSeparator ist für Classpath-Abschnitte, also klassisch der Doppelpunkt
    
    public static void main(String[] args) throws IOException {
        String xlsxFilename = HOMEDIR + FILETRENNER + "Downloads" + FILETRENNER + "XtraReport.xlsx";
        
        executeTransformation(xlsxFilename, true);
  
    }
    
    public static void executeTransformation(String xlsxFilename, Boolean split) throws IOException {
    	String xlsxCatsFilename = xlsxFilename+".CATS.xlsx";
        DataFormatter dataFormatter = new DataFormatter();
        Workbook w = loadExcel(xlsxFilename);
        Sheet s = w.getSheet("Sheet");
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
        Row outDataR1 = outS.createRow(1); // Data
        Row outDataR2 = null;
        if (split) {
        	outDataR2 = outS.createRow(2);
        }
        int i=0;
        
        for (Row element : datensaetze) {
            System.out.println("Am " + fetchWochentagFromRow(element) 
            + " (" + fetchTagesdatumFromRow(element) 
            + ") : " + fetchAzFromRow(element)
            + " CATS: " + fetchCatsAzFromRow(element));
            String cellElementHeader = fetchWochentagFromRow(element) + " " + fetchTagesdatumFromRow(element);
            String cellElementData = fetchCatsAzFromRow(element);
            Cell headerC = outHeaderR.createCell(i);
            Cell dataC1 = outDataR1.createCell(i);
            Cell dataC2 = null;
            if (split) {
            	dataC2 = outDataR2.createCell(i);
            }
            headerC.setCellValue(cellElementHeader + "("+cellElementData+")");
            dataC1.setCellValue(cellElementData);
            if (split) {
            	String[] splittedTimes = splitCellValue(cellElementData);
            	dataC1.setCellValue(splittedTimes[0]);
            	dataC2.setCellValue(splittedTimes[1]);
            }
            i++;
        }
        FileOutputStream outputStream = new FileOutputStream(xlsxCatsFilename);
        outW.write(outputStream);
        outW.close();
        outputStream.close();
        System.out.println("Geschrieben: " + xlsxCatsFilename);
    }
    
    public static String[] splitCellValue(String cv) {
    	System.out.println("\t Übergebener String: " + cv);
    	String[] ergebnis = new String[2];
        String h = cv.substring(0, cv.indexOf(","));
        String m = cv.substring(cv.indexOf(",")+1, cv.length());
        System.out.println("h= " + h + " m= " + m);
        
        int minutenInTeilung15Min = (Integer.valueOf(m).intValue() / 25);
        System.out.println("\tminutenInTeilung15Min: " + minutenInTeilung15Min);
        
        int stundenInTeilung15Min = (Integer.valueOf(h).intValue() * 4);
        System.out.println("\tstundenInTeilung15Min " + stundenInTeilung15Min);
        
        int gesamtInTeilung15 = stundenInTeilung15Min+minutenInTeilung15Min;
        System.out.println("\tgesamtInTeilung15: " + gesamtInTeilung15);
        
        int ganzeStunden = gesamtInTeilung15/4;
        System.out.println("\tGanze Stunden " + ganzeStunden);
        
        int restNachGanzeStunden = gesamtInTeilung15 % 4;
        System.out.println("\tRest in 15er Teilung: " + restNachGanzeStunden);
        
        double h1 = ganzeStunden/2.0;
        double h2 = ganzeStunden/2.0;;
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
            if (c01 != null && c01.getStringCellValue() != null && !c01.getStringCellValue().isEmpty()
                    && c00!=null && c00.getStringCellValue() != null && !c00.getStringCellValue().isEmpty()) {
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
                if (c.getStringCellValue() != null 
                        && c.getStringCellValue().trim().startsWith("Brutto") && c.getStringCellValue().trim().endsWith("Tag")) {
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
        while(rowIt.hasNext()) {
            Row r = rowIt.next();
            // die relevante Zeile hat auf Zelle 43 einen Wert für Tagespause und auf Zelle 21 einen Gevierteltstrich ("-")
//            Cell c21 = r.getCell(21+korrektur);
//            Cell c43 = r.getCell(43+korrektur);
//            if ((c21 != null && c43 != null) // Zur Sicherheit 
//                && // Wenn folgende Felder belegt sind, ist es eine Datenzeile
//                ("-".equals(c21.getStringCellValue()) && !c43.getStringCellValue().isEmpty())) {
//                    ergebnis.add(r);
//            }
            Cell c = r.getCell(BRUTTOARBEITSZEITZELLE);
            if (c!=null && c.getStringCellValue().contains(".")) {
                ergebnis.add(r);
            }
        }
        return ergebnis;
    }
    
    public static void printSheet(Sheet s) {
        Iterator<Row> rowiterator = s.rowIterator();
        while(rowiterator.hasNext()) {
            Row r = rowiterator.next();
                printLine(r);
        }
        System.out.println();
    }
    
    public static void printLine(Row r) {
        Iterator<Cell> celliterator = r.cellIterator();
        while(celliterator.hasNext()) {
            Cell c = celliterator.next();
            try {
                System.out.print(c.getStringCellValue() + " ");
            }catch (Exception e) {
                System.out.print("#");
            }
        }
        System.out.println();
    }
    
    public static Float fetchAzFromRow(Row r) {
        Float ergebnis = null;
        // Arbeitszeit ist auf Index 41 (wenn die Row bereits als Datenrow verifiziert ist)
        ergebnis = Float.valueOf(r.getCell(BRUTTOARBEITSZEITZELLE).getStringCellValue());
        return ergebnis;
    }
    
    public static String fetchCatsAzFromRow(Row r) {
        String ergebnis = null;
        // Arbeitszeit ist auf Index 41 (wenn die Row bereits als Datenrow verifiziert ist)
        String tmp = r.getCell(BRUTTOARBEITSZEITZELLE).getStringCellValue();
        String h = tmp.substring(0, tmp.indexOf("."));
        String m = tmp.substring(tmp.indexOf(".")+1, tmp.length());
        int minutenIn25 = (Integer.valueOf(m).intValue() / 15)*25;
        ergebnis = h+ ","+ minutenIn25; 
        return ergebnis;
    }
    
    public static String fetchWochentagFromRow(Row r) {
        String ergebnis = null;
        // Arbeitszeit ist auf Index 0 (wenn die Row bereits als Datenrow verifiziert ist)
        ergebnis = r.getCell(0).getStringCellValue();
        if (ergebnis == null || ergebnis.trim().isEmpty()) {
            ergebnis = wochentagsmerker.get(r.getCell(1).getStringCellValue());
        }
        return ergebnis;
    }
    
    public static String fetchTagesdatumFromRow(Row r) {
        String ergebnis = null;
        // Arbeitszeit ist auf Index 1 (wenn die Row bereits als Datenrow verifiziert ist)
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
}