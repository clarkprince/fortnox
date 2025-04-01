package com.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HTMLFomatter {
    private static final double VERTICAL_THRESHOLD = 10.0;

    public static void main(String[] args) {
        String htmlFilePath = "C:\\Code\\java\\output.html";
        String outputFilePath = "C:\\Code\\java\\output3.html";

        try {
            String htmlContent = new String(Files.readAllBytes(Paths.get(htmlFilePath)));
            String textContent = extractTextWithFormatting(htmlContent);

            try (FileWriter writer = new FileWriter(outputFilePath)) {
                writer.write(textContent);
            }

            System.out.println("Text extraction completed successfully. Output saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    public static String extractTextWithFormatting(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        StringBuilder result = new StringBuilder();

        result.append("<html><head>\n");
        result.append("<style type=\"text/css\">\n");
        result.append("body { font-family: Calibri; color: #000; font-size:13px }\n");
        result.append("table { border-collapse: collapse; margin: 60px 0; width: 100%; }\n");
        result.append("td { padding: 6px; font-size:14px; border: 1px solid #dcdcdc }\n");
        result.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
        result.append("</style>\n");
        result.append("</head><body>\n");

        // Find table structures and content
        Elements tableBorders = doc.select("div.r[style*=border]");
        Elements contentElements = doc.select("div.p");

        // Extract all positioned elements first
        List<PositionedElement> allElements = extractPositionedElements(doc.select("div[class=p], div[style*=position]:not(.r)"));

        // Process tables and get their positions
        List<TableSection> tableSections = new ArrayList<>();
        if (!tableBorders.isEmpty()) {
            List<List<TableCell>> tables = buildTablesFromStructure(tableBorders, contentElements);
            for (List<TableCell> table : tables) {
                if (!table.isEmpty()) {
                    double tableTop = table.stream().mapToDouble(TableCell::getTop).min().getAsDouble();
                    double tableBottom = table.stream().mapToDouble(TableCell::getTop).max().getAsDouble() + 20.0;
                    String tableContent = buildTableFromCells(table, contentElements);
                    tableSections.add(new TableSection(tableTop, tableBottom, tableContent));
                }
            }
        }

        // Group text elements into sections
        List<TextSection> textSections = groupTextIntoSections(allElements, tableSections);

        // Combine all sections in order
        List<Section> allSections = new ArrayList<>();
        allSections.addAll(tableSections);
        allSections.addAll(textSections);
        allSections.sort(Comparator.comparing(Section::getTop));

        // Build final output
        for (Section section : allSections) {
            result.append(section.getContent());
            result.append("\n");
        }

        result.append("</body></html>");
        return result.toString();
    }

    private static List<TextSection> groupTextIntoSections(List<PositionedElement> elements, List<TableSection> tables) {
        if (elements.isEmpty())
            return new ArrayList<>();

        List<TextSection> sections = new ArrayList<>();
        List<PositionedElement> currentSection = new ArrayList<>();
        double lastY = elements.get(0).getTop();

        for (PositionedElement element : elements) {
            // Check if element is within any table
            boolean inTable = false;
            for (TableSection table : tables) {
                if (element.getTop() >= table.getTop() - 5 && element.getTop() <= table.getBottom() + 5) {
                    inTable = true;
                    break;
                }
            }

            if (!inTable) {
                if (Math.abs(element.getTop() - lastY) > 30.0 && !currentSection.isEmpty()) {
                    // Start new section
                    sections.add(new TextSection(currentSection.stream().mapToDouble(PositionedElement::getTop).min().getAsDouble(),
                            renderText(currentSection)));
                    currentSection.clear();
                }
                currentSection.add(element);
                lastY = element.getTop();
            }
        }

        if (!currentSection.isEmpty()) {
            sections.add(
                    new TextSection(currentSection.stream().mapToDouble(PositionedElement::getTop).min().getAsDouble(), renderText(currentSection)));
        }

        return sections;
    }

    private interface Section {
        double getTop();

        String getContent();
    }

    private static class TableSection implements Section {
        private final double top;
        private final double bottom;
        private final String content;

        public TableSection(double top, double bottom, String content) {
            this.top = top;
            this.bottom = bottom;
            this.content = content;
        }

        @Override
        public double getTop() {
            return top;
        }

        public double getBottom() {
            return bottom;
        }

        @Override
        public String getContent() {
            return content;
        }
    }

    private static class TextSection implements Section {
        private final double top;
        private final String content;

        public TextSection(double top, String content) {
            this.top = top;
            this.content = content;
        }

        @Override
        public double getTop() {
            return top;
        }

        @Override
        public String getContent() {
            return content;
        }
    }

    private static List<List<TableCell>> buildTablesFromStructure(Elements borders, Elements content) {
        List<TableCell> cells = findTableCells(borders, content);

        // Group cells into tables by checking vertical gaps
        List<List<TableCell>> tables = new ArrayList<>();
        if (!cells.isEmpty()) {
            cells.sort(Comparator.comparing(TableCell::getTop).thenComparing(TableCell::getLeft));
            List<TableCell> currentTable = new ArrayList<>();
            currentTable.add(cells.get(0));

            for (int i = 1; i < cells.size(); i++) {
                TableCell current = cells.get(i);
                TableCell previous = cells.get(i - 1);

                // If vertical gap is too large, start a new table
                if (current.getTop() - previous.getTop() > 25.0) { // 25 pixels threshold
                    if (!currentTable.isEmpty()) {
                        tables.add(new ArrayList<>(currentTable));
                        currentTable.clear();
                    }
                }
                currentTable.add(current);
            }

            if (!currentTable.isEmpty()) {
                tables.add(new ArrayList<>(currentTable));
            }
        }

        return tables;
    }

    private static List<TableCell> findTableCells(Elements borders, Elements content) {
        Map<String, Element> bottomBorders = new HashMap<>();
        Map<String, Element> topBorders = new HashMap<>();
        Map<String, Element> leftBorders = new HashMap<>();
        Map<String, Element> rightBorders = new HashMap<>();
        Set<String> processedCells = new HashSet<>();
        List<TableCell> cells = new ArrayList<>();

        // Find the maximum left position of content
        double maxContentLeft = content.stream().map(element -> extractNumericValue(element.attr("style"), "left")).filter(left -> left != null)
                .max(Double::compare).orElse(Double.MAX_VALUE);

        // Filter out borders whose left is greater than the max content left
        borders.removeIf(border -> {
            Double left = extractNumericValue(border.attr("style"), "left");
            return left != null && left > maxContentLeft;
        });

        // Collect all borders by type
        for (Element border : borders) {
            String style = border.attr("style");
            Double top = extractNumericValue(style, "top");
            Double left = extractNumericValue(style, "left");

            if (top == null || left == null)
                continue;

            String posKey = String.format("%.0f-%.0f", top, left);
            if (style.contains("border-bottom"))
                bottomBorders.put(posKey, border);
            if (style.contains("border-top"))
                topBorders.put(posKey, border);
            if (style.contains("border-left"))
                leftBorders.put(posKey, border);
            if (style.contains("border-right"))
                rightBorders.put(posKey, border);
        }

        // Check all positions for potential cells
        Set<String> allPositions = new HashSet<>();
        allPositions.addAll(bottomBorders.keySet());
        allPositions.addAll(topBorders.keySet());
        allPositions.addAll(leftBorders.keySet());
        allPositions.addAll(rightBorders.keySet());

        for (String posKey : allPositions) {
            if (processedCells.contains(posKey))
                continue;

            String[] pos = posKey.split("-");
            double top = Double.parseDouble(pos[0]);
            double left = Double.parseDouble(pos[1]);

            // If we find any border at this position, it's part of a cell
            if (bottomBorders.containsKey(posKey) || topBorders.containsKey(posKey) || leftBorders.containsKey(posKey)
                    || rightBorders.containsKey(posKey)) {
                cells.add(new TableCell(top, left, null));
                processedCells.add(posKey);
            }
        }

        return cells;
    }

    private static String buildTableFromCells(List<TableCell> cells, Elements content) {
        if (cells.isEmpty())
            return "";

        // Sort cells by position
        cells.sort(Comparator.comparing(TableCell::getTop).thenComparing(TableCell::getLeft));

        // Group cells into rows with precise vertical alignment
        Map<Double, List<TableCell>> rowGroups = new TreeMap<>();
        double currentRowTop = cells.get(0).getTop();
        List<TableCell> currentRow = new ArrayList<>();

        for (TableCell cell : cells) {
            if (Math.abs(cell.getTop() - currentRowTop) <= VERTICAL_THRESHOLD) {
                currentRow.add(cell);
            } else {
                if (!currentRow.isEmpty()) {
                    rowGroups.put(currentRowTop, new ArrayList<>(currentRow));
                }
                currentRow.clear();
                currentRow.add(cell);
                currentRowTop = cell.getTop();
            }
        }
        if (!currentRow.isEmpty()) {
            rowGroups.put(currentRowTop, currentRow);
        }

        // Process rows to remove empty ones and isolated rows
        Map<Double, List<TableCell>> filteredRows = new TreeMap<>();
        List<Double> rowTops = new ArrayList<>(rowGroups.keySet());

        for (int i = 0; i < rowTops.size(); i++) {
            Double top = rowTops.get(i);
            List<TableCell> row = rowGroups.get(top);

            // Check if row has any content
            boolean hasContent = false;
            for (TableCell cell : row) {
                String cellContent = findContentForCell(cell, content);
                if (cellContent != null && !cellContent.trim().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }

            // Keep row if it has content and either:
            // 1. It's not the only row
            // 2. It has a neighbor row within 30 points
            if (hasContent) {
                boolean keepRow = rowTops.size() > 1;
                if (!keepRow && i > 0) {
                    keepRow = Math.abs(top - rowTops.get(i - 1)) <= 30.0;
                }
                if (!keepRow && i < rowTops.size() - 1) {
                    keepRow = Math.abs(rowTops.get(i + 1) - top) <= 30.0;
                }

                if (keepRow) {
                    filteredRows.put(top, row);
                }
            }
        }

        // Build table HTML
        StringBuilder table = new StringBuilder("<table>\n");
        for (List<TableCell> row : filteredRows.values()) {
            if (row.isEmpty())
                continue;

            row.sort(Comparator.comparing(TableCell::getLeft));
            table.append("<tr>\n");

            // Remove cells that have a small gap to the left
            for (int i = 0; i < row.size(); i++) {
                TableCell cell = row.get(i);
                if (i > 0) {
                    double thisLeft = cell.getLeft();
                    double prevRight = row.get(i - 1).getLeft() + cell.getWidth();
                    double gap = thisLeft - prevRight;
                    if (gap < 2.0) {
                        row.remove(i--);
                    }
                }
            }

            // Add cells with content
            for (TableCell cell : row) {
                String cellContent = findContentForCell(cell, content);
                if (cellContent != null && !cellContent.trim().isEmpty()) {
                    table.append("<td>").append(cellContent).append("</td>\n");
                } else {
                    table.append("<td>&nbsp;</td>\n");
                }
            }

            table.append("</tr>\n");
        }
        table.append("</table>\n");
        return table.toString();
    }

    private static String findContentForCell(TableCell cell, Elements content) {
        if (cell == null)
            return "";

        List<PositionedElement> cellElements = new ArrayList<>();

        // Collect all elements that belong in this cell's vertical position
        for (Element element : content) {
            Double elementTop = extractNumericValue(element.attr("style"), "top");
            Double elementLeft = extractNumericValue(element.attr("style"), "left");

            if (elementTop == null || elementLeft == null)
                continue;

            // Only check vertical alignment initially
            if (Math.abs(elementTop - cell.getTop()) < VERTICAL_THRESHOLD) {
                cellElements.add(new PositionedElement(element.text(), elementTop, elementLeft, element.attr("style")));
            }
        }

        if (cellElements.isEmpty())
            return "";

        // Sort elements by horizontal position
        cellElements.sort(Comparator.comparing(PositionedElement::getLeft));
        for (int i = 0; i < cellElements.size(); i++) {
            PositionedElement element = cellElements.get(i);
            if (element.getLeft() < cell.getLeft()) {
                cellElements.remove(i--);
            }
        }

        // Group text by columns similar to renderText
        List<StringBuilder> columnTexts = new ArrayList<>();
        StringBuilder currentColumn = new StringBuilder();

        for (int i = 0; i < cellElements.size(); i++) {
            PositionedElement element = cellElements.get(i);

            // If this is not the first element, check the gap
            if (i > 0) {
                double thisLeft = element.getLeft();
                double prevRight = cellElements.get(i - 1).getLeft() + estimateElementWidth(cellElements.get(i - 1));
                double gap = thisLeft - prevRight;

                if (currentColumn.length() > 0 && gap >= 1.0) {
                    currentColumn.append(" ");
                }

                // If gap is large enough, not in the same column
                if (gap > 50.0) {
                    break;
                }
            }

            currentColumn.append(element.getText());
        }

        if (currentColumn.length() > 0) {
            columnTexts.add(currentColumn);
        }

        // Return the text from the appropriate column (usually first)
        return columnTexts.isEmpty() ? "" : columnTexts.get(0).toString();
    }

    private static List<PositionedElement> extractPositionedElements(Elements elements) {
        List<PositionedElement> positionedElements = new ArrayList<>();

        for (Element element : elements) {
            String text = element.text().trim();
            if (text.isEmpty())
                continue;

            Double top = extractNumericValue(element.attr("style"), "top");
            Double left = extractNumericValue(element.attr("style"), "left");

            if (top != null && left != null) {
                positionedElements.add(new PositionedElement(text, top, left, element.attr("style")));
            }
        }

        return positionedElements;
    }

    private static Double extractNumericValue(String style, String property) {
        if (style == null || style.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(property + "\\s*:\\s*([\\d.]+)(?:pt|px)?");
        Matcher matcher = pattern.matcher(style);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private static String renderText(List<PositionedElement> elements) {
        if (elements.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        List<List<PositionedElement>> lines = groupElementsIntoLines(elements);

        double lastLineBottom = 0;
        boolean firstLine = true;

        for (List<PositionedElement> line : lines) {
            if (!firstLine) {
                double currentLineTop = line.get(0).getTop();
                double verticalGap = currentLineTop - lastLineBottom;
                if (verticalGap > 30.0) {
                    result.append("<br><br><br><br>\n");
                } else if (verticalGap > 20.0) {
                    result.append("<br><br>\n");
                }
            } else {
                firstLine = false;
            }

            List<Integer> columnCharacterCounts = new ArrayList<>();
            int currentColumnCharacterCount = 0;

            for (int i = 0; i < line.size(); i++) {
                PositionedElement element = line.get(i);
                currentColumnCharacterCount += element.getText().length();
                if (i < line.size() - 1) {
                    double thisRight = element.getLeft() + estimateElementWidth(element);
                    double nextLeft = line.get(i + 1).getLeft();
                    double gap = nextLeft - thisRight;

                    if (gap > 1 && gap <= 5.0) {
                        currentColumnCharacterCount++;
                    }

                    if (gap > 50.0) {
                        columnCharacterCounts.add(currentColumnCharacterCount);
                        currentColumnCharacterCount = 0;
                    }
                }
            }

            if (currentColumnCharacterCount > 0) {
                columnCharacterCounts.add(currentColumnCharacterCount);
            }

            result.append("<div>");
            int c = 0;
            for (PositionedElement element : line) {

                result.append(element.getText());
                if (line.indexOf(element) < line.size() - 1) {
                    double thisRight = element.getLeft() + estimateElementWidth(element);
                    double nextLeft = line.get(line.indexOf(element) + 1).getLeft();
                    double gap = nextLeft - thisRight;

                    if (gap > 50.0) {
                        int columnWidth = 50;
                        int currWidth = columnCharacterCounts.get(c);
                        int spaces = columnWidth - currWidth;
                        result.append(" ".repeat(Math.max(0, spaces)));
                        c++;
                    } else {
                        int spaces = calculateSpaces(gap);
                        result.append(" ".repeat(Math.max(0, spaces)));
                    }
                }
            }

            lastLineBottom = line.get(0).getTop() + 10.0;
        }
        result.append("</div>\n");

        return result.toString();
    }

    private static int calculateSpaces(double gap) {
        if (gap <= 1.0)
            return 0;
        if (gap <= 5.0)
            return 1;
        if (gap <= 20.0)
            return 2;
        if (gap <= 50.0)
            return 3;
        return (int) (gap / 5.0);
    }

    private static List<List<PositionedElement>> groupElementsIntoLines(List<PositionedElement> elements) {
        List<List<PositionedElement>> lines = new ArrayList<>();
        if (elements.isEmpty()) {
            return lines;
        }

        List<PositionedElement> currentLine = new ArrayList<>();
        currentLine.add(elements.get(0));

        // Group elements by vertical position (top)
        for (int i = 1; i < elements.size(); i++) {
            PositionedElement current = elements.get(i);
            PositionedElement reference = currentLine.get(0); // Use first element in line as reference

            if (Math.abs(current.getTop() - reference.getTop()) <= 2.0) { // 2 pixels tolerance
                // Same line
                currentLine.add(current);
            } else {
                // Sort the current line by left position
                currentLine.sort(Comparator.comparing(PositionedElement::getLeft));
                lines.add(new ArrayList<>(currentLine));
                currentLine.clear();
                currentLine.add(current);
            }
        }

        // Add the last line
        if (!currentLine.isEmpty()) {
            currentLine.sort(Comparator.comparing(PositionedElement::getLeft));
            lines.add(currentLine);
        }

        return lines;
    }

    private static double estimateElementWidth(PositionedElement element) {
        Double width = extractNumericValue(element.getStyle(), "width");
        if (width != null) {
            return width;
        }

        return element.getText().length() * 5.0;
    }

    private static class PositionedElement {
        private final String text;
        private final double top;
        private final double left;
        private final String style;

        public PositionedElement(String text, double top, double left, String style) {
            this.text = text;
            this.top = top;
            this.left = left;
            this.style = style;
        }

        public String getText() {
            return text;
        }

        public double getTop() {
            return top;
        }

        public double getLeft() {
            return left;
        }

        public String getStyle() {
            return style;
        }
    }

    private static class TableCell {
        private final double top;
        private final double left;
        private final double width;

        public TableCell(double top, double left, Double width) {
            this.top = top;
            this.left = left;
            this.width = width != null ? width : 0;
        }

        public double getTop() {
            return top;
        }

        public double getLeft() {
            return left;
        }

        public double getWidth() {
            return width;
        }
    }
}