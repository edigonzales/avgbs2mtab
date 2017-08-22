package ch.so.agi.avgbs2mtab.writeexcel;

import ch.so.agi.avgbs2mtab.mutdat.DPRContainer;
import ch.so.agi.avgbs2mtab.mutdat.ParcelContainer;
import ch.so.agi.avgbs2mtab.util.Avgbs2MtabException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class ExcelData implements WriteExcel {


    public XSSFWorkbook fillValuesIntoParcelTable (String filePath, XSSFWorkbook workbook){

        ParcelContainer parcelContainer = new ParcelContainer();

        Integer area = null;

        List<Integer> orderedListOfOldParcelNumbers = parcelContainer.getOldParcelNumbers();
        List<Integer> orderedListOfNewParcelNumbers = parcelContainer.getNewParcelNumbers();

        workbook = writeOldParcelsInTemplate(orderedListOfOldParcelNumbers, filePath, workbook);
        workbook = writeNewParcelsInTemplate(orderedListOfNewParcelNumbers, filePath, workbook);

        for (int oldParcel : orderedListOfOldParcelNumbers) {
            for (int newParcel : orderedListOfNewParcelNumbers) {
                if (oldParcel != newParcel) {
                    area = parcelContainer.getAddedArea(newParcel, oldParcel);
                } else {
                    area = parcelContainer.getRestAreaOfParcel(oldParcel);
                }
                workbook = writeInflowAndOutflows(oldParcel, newParcel, area, filePath, workbook);
            }
        }

        for (int oldParcel : orderedListOfOldParcelNumbers) {
            int roundingDifference = parcelContainer.getRoundingDifference(oldParcel);
            workbook = writeRoundingDifference(oldParcel, roundingDifference, filePath, workbook);
        }

        for (int newParcel : orderedListOfNewParcelNumbers){
            int newArea = parcelContainer.getNewArea(newParcel);
            workbook = writeNewArea(newParcel, newArea, filePath, workbook);
        }


        HashMap<Integer, Integer> oldAreaHashMap = getAllOldAreas(orderedListOfNewParcelNumbers,
                orderedListOfOldParcelNumbers, parcelContainer);

        for (int oldParcel : orderedListOfOldParcelNumbers) {
            Integer oldArea = oldAreaHashMap.get(oldParcel);
            Integer roundingDifference = parcelContainer.getRoundingDifference(oldParcel);
            workbook = writeOldArea(oldParcel, oldArea, roundingDifference, filePath, workbook);
        }

        workbook = writeAreaSum(oldAreaHashMap, getAllNewAreas(orderedListOfNewParcelNumbers, parcelContainer),
                calculateRoundingDifference(orderedListOfOldParcelNumbers, parcelContainer), filePath, workbook);

        return workbook;
    }

    public XSSFWorkbook fillValuesIntoDPRTable (String filePath, XSSFWorkbook workbook) {

        DPRContainer dprContainer = new DPRContainer();
        ParcelContainer parcelContainer = new ParcelContainer();

        List<Integer> orderedListOfParcelNumbers = dprContainer.getParcelsAffectedByDPRs();
        List<Integer> orderedListOfDPRs = dprContainer.getNewDPRs();

        Integer numberOfNewParcelsInParcelTable = parcelContainer.getNumberOfNewParcels();

        workbook = writeParcelsAffectedByDPRsInTemplate(orderedListOfParcelNumbers, numberOfNewParcelsInParcelTable,
                filePath, workbook);
        workbook = writeDPRsInTemplate( orderedListOfDPRs, numberOfNewParcelsInParcelTable, filePath, workbook);

        for (int parcel : orderedListOfParcelNumbers) {
            for (int dpr : orderedListOfDPRs) {
                Integer area = dprContainer.getAddedAreaDPR(parcel, dpr);
                workbook = writeDPRInflowAndOutflows(parcel, dpr, area, numberOfNewParcelsInParcelTable, filePath,
                        workbook);
            }
        }

        for (int dpr : orderedListOfDPRs) {
            Integer roundingDifference = dprContainer.getRoundingDifferenceDPR(dpr);
            workbook = writeDPRRoundingDifference(dpr, roundingDifference, numberOfNewParcelsInParcelTable, filePath,
                    workbook);
        }

        for (int dpr : orderedListOfDPRs) {
            Integer newArea = dprContainer.getNewAreaDPR(dpr);
            workbook = writeNewDPRArea(dpr, newArea, numberOfNewParcelsInParcelTable, filePath, workbook);
        }



        return workbook;
    }

    @Override
    public XSSFWorkbook writeOldParcelsInTemplate(List<Integer> orderedListOfOldParcelNumbers, String filePath, XSSFWorkbook workbook) {

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Row row =xlsxSheet.getRow(2);

            Integer column = 1;

            for (Integer parcelNumber : orderedListOfOldParcelNumbers){
                Cell cell =row.getCell(column);
                cell.setCellValue(parcelNumber);
                column++;
            }
            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeNewParcelsInTemplate(List<Integer> orderedListOfNewParcelNumbers, String filePath, XSSFWorkbook workbook) {
        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            int i = 1;
            for (Integer parcelNumber : orderedListOfNewParcelNumbers){
                Row row = xlsxSheet.getRow(2+2*i);
                Cell cell = row.getCell(0);
                cell.setCellValue(parcelNumber);
                i++;

            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeInflowAndOutflows(int oldParcelNumber, int newParcelNumber, int area, String filePath, XSSFWorkbook workbook) {

        Integer indexOldParcelNumber = null;
        Integer indexNewParcelNumber = null;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Row row = xlsxSheet.getRow(2);
            for (Cell cell : row){
                if (cell.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell.getNumericCellValue() == oldParcelNumber){
                        indexOldParcelNumber = cell.getColumnIndex();
                        break;
                    }
                }
            }

            Iterator<Row> rowIterator = xlsxSheet.iterator();
            rowIterator.next();
            while(rowIterator.hasNext()){
                Row row1 = rowIterator.next();
                Cell cell1 = row1.getCell(0);
                if (cell1.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell1.getNumericCellValue() == newParcelNumber){
                        indexNewParcelNumber = cell1.getRowIndex();
                        break;
                    }
                }
            }

            if (indexNewParcelNumber==null || indexOldParcelNumber==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "Either the old parcel "
                        + oldParcelNumber + " or the new parcel " + newParcelNumber + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(indexNewParcelNumber);
                Cell cellFlows = rowFlows.getCell(indexOldParcelNumber);
                cellFlows.setCellValue(area);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeNewArea(int newParcelNumber, int area, String filePath, XSSFWorkbook workbook) {

        Integer rowNewParcelNumber = null;
        Integer columnNewParcelNumber = null;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Iterator<Row> rowIterator = xlsxSheet.iterator();
            rowIterator.next();
            while(rowIterator.hasNext()){
                Row row1 = rowIterator.next();
                Cell cell1 = row1.getCell(0);
                if (cell1.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell1.getNumericCellValue() == newParcelNumber){
                        rowNewParcelNumber = cell1.getRowIndex();
                        try {
                            columnNewParcelNumber = (int) row1.getLastCellNum()-1;
                        } catch (Exception e){
                            throw new Avgbs2MtabException("Could not found last row");
                        }
                        break;
                    }
                }
            }

            if (rowNewParcelNumber==null || columnNewParcelNumber==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "The new parcel "
                        + newParcelNumber + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(rowNewParcelNumber);
                Cell cellFlows = rowFlows.getCell(columnNewParcelNumber);
                cellFlows.setCellValue(area);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeRoundingDifference(int oldParcelNumber,int roundingDifference, String filePath, XSSFWorkbook workbook) {

        Integer columnOldParcelNumber = null;
        Integer rowOldParcelNumber = null;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Row row = xlsxSheet.getRow(2);
            for (Cell cell : row){
                if (cell.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell.getNumericCellValue() == oldParcelNumber){
                        columnOldParcelNumber = cell.getColumnIndex();
                        break;
                    }
                }
            }

            try {
                rowOldParcelNumber = xlsxSheet.getLastRowNum() - 1;
            } catch (Exception e){
                throw new Avgbs2MtabException("Could not find last row in excel");
            }

            if (columnOldParcelNumber==null || rowOldParcelNumber==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "The old parcel "
                        + oldParcelNumber + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(rowOldParcelNumber);
                Cell cellFlows = rowFlows.getCell(columnOldParcelNumber);
                cellFlows.setCellValue(roundingDifference);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }



    @Override
    public XSSFWorkbook writeOldArea(int oldParcelNumber,
                                     int oldArea,
                                     int roundingDifference,
                                     String filePath,
                                     XSSFWorkbook workbook){
        Integer sum;
        sum = oldArea + roundingDifference;


        Integer columnOldParcelNumber = null;
        Integer rowOldParcelArea = null;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

           Row row = xlsxSheet.getRow(2);
            for (Cell cell : row){
                if (cell.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell.getNumericCellValue() == oldParcelNumber){
                        columnOldParcelNumber = cell.getColumnIndex();
                        break;
                    }
                }
            }

            rowOldParcelArea = xlsxSheet.getLastRowNum();



            if (columnOldParcelNumber==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "The old parcel "
                        + oldParcelNumber + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(rowOldParcelArea);
                Cell cellFlows = rowFlows.getCell(columnOldParcelNumber);
                cellFlows.setCellValue(sum);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }


    @Override
    public XSSFWorkbook writeAreaSum(HashMap<Integer, Integer> oldAreas,
                                 List<Integer> newAreas,
                                 int roundingDifference,
                                 String filePath,
                                 XSSFWorkbook workbook){

        Integer sumOldAreas = 0;
        Integer sumNewAreas = 0;

        Iterator hashmapIterator = oldAreas.entrySet().iterator();
        while(hashmapIterator.hasNext()) {
            Map.Entry pair = (Map.Entry)hashmapIterator.next();
            sumOldAreas += (int) pair.getValue();
        }
        for (int area : newAreas){
            sumNewAreas += area;
        }

        if (sumOldAreas != sumNewAreas + roundingDifference){
            throw new Avgbs2MtabException("The sum of the old areas does not equal with the sum of the new areas.");
        }

        Integer numberOfOldParcels = oldAreas.size();
        Integer numberOfNewParcels = newAreas.size();

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");


            Row row = xlsxSheet.getRow(5 + 2 * numberOfNewParcels);
            Cell cell = row.getCell(1 + numberOfOldParcels);
            cell.setCellValue(sumOldAreas);

            workbook.write(ExcelFile);
            ExcelFile.close();

        } catch (IOException e){
            throw new RuntimeException(e);
        }

        return workbook;
    }

    private List<Integer> getAllNewAreas (List<Integer> orderedListOfNewParcelNumbers, ParcelContainer parcelContainer) {

        List<Integer> newAreaList = new ArrayList<>();

        for (int newParcel : orderedListOfNewParcelNumbers) {
            newAreaList.add(parcelContainer.getNewArea(newParcel));
        }

        return newAreaList;
    }

    private HashMap<Integer, Integer> getAllOldAreas(List<Integer> orderedListOfNewParcelNumbers,
                                         List<Integer> orderedListOfOldParcelNumbers, ParcelContainer parcelContainer) {

        HashMap<Integer, Integer> oldAreaHashMap = new HashMap<>();
        Integer oldArea = null;
        Integer area = null;

        for (int oldParcel : orderedListOfOldParcelNumbers) {
            for (int newParcel : orderedListOfNewParcelNumbers) {
                if (oldParcel != newParcel) {
                    area = parcelContainer.getAddedArea(newParcel, oldParcel);
                } else {
                    area = parcelContainer.getRestAreaOfParcel(oldParcel);
                }
                oldArea =+ area;
            }
            oldAreaHashMap.put(oldParcel, oldArea);
        }

        return oldAreaHashMap;

    }

    private Integer calculateRoundingDifference(List<Integer> orderedListOfOldParcelNumbers,
                                                ParcelContainer parcelContainer) {

        Integer roundingDifference = null;

        for (int oldParcel : orderedListOfOldParcelNumbers) {
            roundingDifference += parcelContainer.getRoundingDifference(oldParcel);
        }

        return roundingDifference;

    }


    @Override
    public XSSFWorkbook writeParcelsAffectedByDPRsInTemplate(List<Integer> orderedListOfParcelNumbersAffectedByDPRs,
                                                             int newParcelNumber,
                                                             String filePath,
                                                             XSSFWorkbook workbook) {

        int indexOfParcelRow = newParcelNumber*2 + 10;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Row row =xlsxSheet.getRow(indexOfParcelRow);

            Integer column = 1;

            for (Integer parcelNumber : orderedListOfParcelNumbersAffectedByDPRs){
                Cell cell =row.getCell(column);
                cell.setCellValue(parcelNumber);
                column++;
            }
            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeDPRsInTemplate(List<Integer> orderedListOfDPRs,
                                            int newParcelNumber,
                                            String filePath,
                                            XSSFWorkbook workbook) {

        int indexOfDPRRow = newParcelNumber*2 + 12;

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");


            Integer rowIndex = indexOfDPRRow;

            for (Integer dpr : orderedListOfDPRs){
                XSSFRow row = xlsxSheet.getRow(rowIndex);
                XSSFCell cell =row.getCell(0);
                cell.setCellValue("(" + dpr + ")");
                rowIndex++;
                rowIndex++;
            }
            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeDPRInflowAndOutflows(int parcelNumberAffectedByDPR,
                                                  int dpr,
                                                  int area,
                                                  int newParcelNumber,
                                                  String filePath,
                                                  XSSFWorkbook workbook) {

        Integer indexParcel= null;
        Integer indexDPR = null;

        int indexOfParcelRow = newParcelNumber*2 + 10;
        int lastRow = workbook.getSheet("Mutationstabelle").getLastRowNum();


        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            Row row = xlsxSheet.getRow(indexOfParcelRow);
            for (Cell cell : row){
                if (cell.getCellTypeEnum() == CellType.NUMERIC){
                    if (cell.getNumericCellValue() == parcelNumberAffectedByDPR){
                        indexParcel = cell.getColumnIndex();
                        break;
                    }
                }
            }

            for (int i = indexOfParcelRow + 2; i <= lastRow; i++){
                Row row1 = xlsxSheet.getRow(i);
                Cell cell1 = row1.getCell(0);
                if (cell1.getCellTypeEnum() == CellType.STRING){
                    String dprString = cell1.getStringCellValue();
                    int dprStringLength = dprString.length();
                    int dprNumber = Integer.parseInt(dprString.substring(1, (dprStringLength-1)));
                    if (dprNumber == dpr){
                        indexDPR = cell1.getRowIndex();
                        break;
                    }
                }
            }

            if (indexParcel==null || indexDPR==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "Either the parcel "
                        + parcelNumberAffectedByDPR + " or the DPR " + dpr + " could not be found in the excel.");
            } else {
                XSSFRow rowFlows = xlsxSheet.getRow(indexDPR);
                XSSFCell cellFlows = rowFlows.getCell(indexParcel);
                cellFlows.setCellValue(area);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeNewDPRArea(int dpr,
                                        int area,
                                        int newParcelNumber,
                                        String filePath,
                                        XSSFWorkbook workbook) {

        Integer rowDPRNumber = null;
        Integer columnNewArea = null;

        int indexOfParcelRow = newParcelNumber*2 + 10;
        int lastRow = workbook.getSheet("Mutationstabelle").getLastRowNum();

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            for (int i = indexOfParcelRow + 2; i <= lastRow; i++){
                Row row1 = xlsxSheet.getRow(i);
                Cell cell1 = row1.getCell(0);
                if (cell1.getCellTypeEnum() == CellType.STRING){
                    String dprString = cell1.getStringCellValue();
                    int dprStringLength = dprString.length();
                    int dprNumber = Integer.parseInt(dprString.substring(1, (dprStringLength-1)));
                    if (dprNumber == dpr){
                        rowDPRNumber = cell1.getRowIndex();
                        columnNewArea = (int) row1.getLastCellNum()-1;
                        break;
                    }
                }
            }


            if (rowDPRNumber==null || columnNewArea==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "The DPR "
                        + dpr + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(rowDPRNumber);
                Cell cellFlows = rowFlows.getCell(columnNewArea);
                if (area > 0) {
                    cellFlows.setCellValue(area);
                } else {
                    cellFlows.setCellValue("gelöscht");
                }
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return workbook;
    }

    @Override
    public XSSFWorkbook writeDPRRoundingDifference(int dpr,
                                                   int roundingDifference,
                                                   int newParcelNumber,
                                                   String filePath,
                                                   XSSFWorkbook workbook) {

        Integer rowDPRNumber = null;
        Integer columnRoundingDifference = null;

        int indexOfParcelRow = newParcelNumber*2 + 10;
        int lastRow = workbook.getSheet("Mutationstabelle").getLastRowNum();

        try {
            OutputStream ExcelFile = new FileOutputStream(filePath);
            XSSFSheet xlsxSheet = workbook.getSheet("Mutationstabelle");

            for (int i = indexOfParcelRow + 2; i <= lastRow; i++){
                Row row1 = xlsxSheet.getRow(i);
                Cell cell1 = row1.getCell(0);
                if (cell1.getCellTypeEnum() == CellType.STRING){
                    String dprString = cell1.getStringCellValue();
                    int dprStringLength = dprString.length();
                    int dprNumber = Integer.parseInt(dprString.substring(1, (dprStringLength-1)));
                    if (dprNumber == dpr){
                        rowDPRNumber = cell1.getRowIndex();
                        columnRoundingDifference = (int) row1.getLastCellNum()-2;
                        break;
                    }
                }
            }


            if (rowDPRNumber==null || columnRoundingDifference==null){
                throw new Avgbs2MtabException(Avgbs2MtabException.TYPE_MISSING_PARCEL_IN_EXCEL, "The DPR "
                        + newParcelNumber + " could not be found in the excel.");
            } else {
                Row rowFlows = xlsxSheet.getRow(rowDPRNumber);
                Cell cellFlows = rowFlows.getCell(columnRoundingDifference);
                cellFlows.setCellValue(roundingDifference);
            }

            workbook.write(ExcelFile);
            ExcelFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return workbook;
    }
}