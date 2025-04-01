package com.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLToTextExtractor {
    // Constants for grouping elements
    private static final double VERTICAL_THRESHOLD = 2.0; // pixels

    public static void main(String[] args) {
        String htmlFilePath = "C:\\Code\\java\\output.html";
        String outputFilePath = "C:\\Code\\java\\output3.html";

        try {
            String htmlContent = new String(Files.readAllBytes(Paths.get(htmlFilePath)));
            String textContent = extractTextWithFormatting(htmlContent);

            // textContent = postProcess(textContent, outputFilePath);

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

        // Find all text elements with position information
        List<PositionedElement> elements = extractPositionedElements(doc);

        // Sort elements by vertical position (top) first, then by horizontal (left)
        elements.sort(Comparator.comparing(PositionedElement::getTop).thenComparing(PositionedElement::getLeft));

        // Group elements into logical lines and render the text
        StringBuilder result = new StringBuilder();
        result.append("<html><head>\n");
        result.append("<style type=\"text/css\">\n");
        result.append("body { font-family: Calibri; color: #000; }\n");
        result.append("</style>\n");
        result.append("</head><body>\n");
        result.append("<pre style=\"font-family: monospace;\">\n");
        result.append(renderText(elements));
        result.append("</body></html>\n");
        return result.toString();
    }

    private static List<PositionedElement> extractPositionedElements(Document doc) {
        List<PositionedElement> elements = new ArrayList<>();

        // Look for paragraphs and divs with absolute positioning
        Elements positioned = doc.select("div[class=p], div[style*=position][style*=top][style*=left]");
        for (Element element : positioned) {
            String text = element.text().trim();
            if (text.isEmpty())
                continue;

            // Extract position information
            Double top = extractNumericValue(element.attr("style"), "top");
            if (top == null && element.hasAttr("id") && element.attr("id").startsWith("p")) {
                // For elements with class="p" and id="p0", "p1", etc.
                top = extractNumericValueFromStyle(element, "top");
            }

            Double left = extractNumericValue(element.attr("style"), "left");
            if (left == null && element.hasAttr("id") && element.attr("id").startsWith("p")) {
                // For elements with class="p" and id="p0", "p1", etc.
                left = extractNumericValueFromStyle(element, "left");
            }

            // Only add elements with valid positioning
            if (top != null && left != null) {
                elements.add(new PositionedElement(text, top, left, element.hasAttr("style") ? element.attr("style") : ""));
            }
        }

        return elements;
    }

    private static Double extractNumericValueFromStyle(Element element, String property) {
        String style = element.attr("style");
        return extractNumericValue(style, property);
    }

    private static Double extractNumericValue(String style, String property) {
        if (style == null || style.isEmpty()) {
            return null;
        }

        // Pattern to extract numeric values with optional units (pt, px, etc.)
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

        // Process each line
        for (List<PositionedElement> line : lines) {
            if (!firstLine) {
                // Check if we need extra vertical spacing
                double currentLineTop = line.get(0).getTop();
                double verticalGap = currentLineTop - lastLineBottom;
                // Add extra line breaks based on vertical gap size
                if (verticalGap > 30.0) {
                    result.append("<br><br><br><br>\n");
                } else if (verticalGap > 20.0) {
                    result.append("<br><br>\n");
                }
            } else {
                firstLine = false;
            }

            // Process each element in the line

            // Check for large gaps between elements in the line
            List<Integer> columnCharacterCounts = new ArrayList<>(); // List to store column character counts
            int currentColumnCharacterCount = 0;

            for (int i = 0; i < line.size(); i++) {
                PositionedElement element = line.get(i);
                currentColumnCharacterCount += element.getText().length();
                if (i < line.size() - 1) {
                    double thisRight = element.getLeft() + estimateElementWidth(element);
                    double nextLeft = line.get(i + 1).getLeft();
                    double gap = nextLeft - thisRight;

                    // Account for spaces between words
                    if (gap > 1 && gap <= 5.0) {
                        currentColumnCharacterCount++; // Add spaces to the character count
                    }

                    // If gap is over 50, treat as a new column
                    if (gap > 50.0) {
                        columnCharacterCounts.add(currentColumnCharacterCount); // Save the character count of the current column
                        currentColumnCharacterCount = 0; // Reset for the next column
                    }
                }
            }

            // Add the last column character count
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

                    // If gap is over 50
                    if (gap > 50.0) {
                        int columnWidth = 50; // Fixed column width of 50 characters
                        int currWidth = columnCharacterCounts.get(c); // total width of the column, approximate number of characters
                        int spaces = columnWidth - currWidth; // Adjust spaces to fit the column width
                        result.append(" ".repeat(Math.max(0, spaces)));
                        c++;
                    } else {
                        // Add spaces based on the gap size
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
        return (int) (gap / 5.0); // Approximate number of spaces for larger gaps

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

            if (Math.abs(current.getTop() - reference.getTop()) <= VERTICAL_THRESHOLD) {
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
        // Extract width from style if available
        Double width = extractNumericValue(element.getStyle(), "width");
        if (width != null) {
            return width;
        }

        // Estimate width based on text length (very approximate)
        return element.getText().length() * 5.0;
    }

    // Helper class to represent positioned elements
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
}