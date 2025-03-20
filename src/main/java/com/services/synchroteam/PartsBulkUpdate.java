package com.services.synchroteam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dto.PartPriceUpdateDTO;

@Service
public class PartsBulkUpdate {

    public List<PartPriceUpdateDTO> processFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return processExcelFile(file);
        } else {
            return processTextFile(file);
        }
    }

    private List<PartPriceUpdateDTO> processExcelFile(MultipartFile file) throws Exception {
        List<PartPriceUpdateDTO> prices = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header
                PartPriceUpdateDTO dto = new PartPriceUpdateDTO();
                dto.setReference(convertCellToString(row.getCell(0)));
                dto.setPrice(convertCellToString(row.getCell(1)));
                prices.add(dto);
            }
        }
        return prices;
    }

    private List<PartPriceUpdateDTO> processTextFile(MultipartFile file) throws Exception {
        List<PartPriceUpdateDTO> prices = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }
                String[] values = line.split("[,;]"); // Split by comma or semicolon
                if (values.length >= 2) {
                    PartPriceUpdateDTO dto = new PartPriceUpdateDTO();
                    dto.setReference(values[0].trim());
                    dto.setPrice(values[1].trim());
                    prices.add(dto);
                }
            }
        }
        return prices;
    }

    public String convertCellToString(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
            case STRING:
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.setCellValue(cell.getLocalDateTimeCellValue().toString());
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue % 1 == 0) {
                        cell.setCellValue(String.valueOf((long) numericValue));
                    } else {
                        cell.setCellValue(String.valueOf(numericValue));
                    }
                }
                break;
            case BOOLEAN:
                cell.setCellValue(String.valueOf(cell.getBooleanCellValue()));
                break;
            case FORMULA:
                try {
                    switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            cell.setCellValue(cell.getLocalDateTimeCellValue().toString());
                        } else {
                            double numericValue = cell.getNumericCellValue();
                            if (numericValue % 1 == 0) {
                                cell.setCellValue(String.valueOf((long) numericValue));
                            } else {
                                cell.setCellValue(String.valueOf(numericValue));
                            }
                        }
                        break;
                    case STRING:
                        cell.setCellValue(cell.getStringCellValue());
                        break;
                    case BOOLEAN:
                        cell.setCellValue(String.valueOf(cell.getBooleanCellValue()));
                        break;
                    case ERROR:
                        cell.setCellValue(FormulaError.forInt(cell.getErrorCellValue()).getString());
                        break;
                    default:
                        cell.setCellValue("");
                    }
                } catch (IllegalStateException e) {
                    cell.setCellValue("");
                }
                break;
            case ERROR:
                cell.setCellValue(FormulaError.forInt(cell.getErrorCellValue()).getString());
                break;
            case BLANK:
                cell.setCellValue("");
                break;
            default:
                cell.setCellValue("");
            }

            return cell.getStringCellValue();
        }
        return null;
    }
}
