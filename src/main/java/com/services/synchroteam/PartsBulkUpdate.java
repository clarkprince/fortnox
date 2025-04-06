package com.services.synchroteam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dto.PartDTO;
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

    private String normalizeHeaderName(String header) {
        if (header == null)
            return null;
        return header.toLowerCase().trim().replaceAll("[^a-z0-9]", ""); // Remove all special characters and spaces
    }

    public List<PartDTO> processPartsFile(MultipartFile file) throws Exception {
        List<PartDTO> parts = new ArrayList<>();
        Workbook workbook = null;

        try {
            workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            // Get header row and create column mapping
            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                return parts;

            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String headerValue = convertCellToString(headerRow.getCell(i));
                if (headerValue != null) {
                    String normalizedHeader = normalizeHeaderName(headerValue);
                    if (normalizedHeader != null && !normalizedHeader.isEmpty()) {
                        columnMap.put(normalizedHeader, i);
                    }
                }
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                PartDTO part = new PartDTO();

                // Map each field based on normalized header names
                mapStringField(row, columnMap, "reference", part::setReference);
                mapStringField(row, columnMap, "name", part::setName);
                mapStringField(row, columnMap, "description", part::setDescription);
                mapStringField(row, columnMap, "price", part::setPrice);
                mapIntegerField(row, columnMap, "minquantity", part::setMinQuantity);
                mapBooleanField(row, columnMap, "istracked", part::setIsTracked);
                mapBooleanField(row, columnMap, "isserializable", part::setIsSerializable);
                mapStringField(row, columnMap, "status", part::setStatus);
                mapStringField(row, columnMap, "type", part::setType);

                // Map category and tax with normalized headers
                if (columnMap.containsKey("categoryid")) {
                    String categoryId = convertCellToString(row.getCell(columnMap.get("categoryid")));
                    if (categoryId != null && !categoryId.isEmpty()) {
                        PartDTO.Category category = new PartDTO.Category();
                        category.setId(Integer.parseInt(categoryId));
                        part.setCategory(category);
                    }
                }

                if (columnMap.containsKey("taxid")) {
                    String taxId = convertCellToString(row.getCell(columnMap.get("taxid")));
                    if (taxId != null && !taxId.isEmpty()) {
                        PartDTO.Tax tax = new PartDTO.Tax();
                        tax.setId(Integer.parseInt(taxId));
                        part.setTax(tax);
                    }
                }

                if (part.getReference() != null && !part.getReference().isEmpty()) {
                    parts.add(part);
                }
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }

        return parts;
    }

    private void mapStringField(Row row, Map<String, Integer> columnMap, String columnName, Consumer<String> setter) {
        if (columnMap.containsKey(columnName)) {
            String value = convertCellToString(row.getCell(columnMap.get(columnName)));
            if (value != null) {
                setter.accept(value);
            }
        }
    }

    private void mapIntegerField(Row row, Map<String, Integer> columnMap, String columnName, Consumer<Integer> setter) {
        if (columnMap.containsKey(columnName)) {
            String value = convertCellToString(row.getCell(columnMap.get(columnName)));
            if (value != null && !value.isEmpty()) {
                try {
                    setter.accept(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // Skip invalid number
                }
            }
        }
    }

    private void mapBooleanField(Row row, Map<String, Integer> columnMap, String columnName, Consumer<Boolean> setter) {
        if (columnMap.containsKey(columnName)) {
            String value = convertCellToString(row.getCell(columnMap.get(columnName)));
            if (value != null) {
                setter.accept(Boolean.parseBoolean(value));
            }
        }
    }
}
