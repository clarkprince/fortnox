package com.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public class PDFHelper {
    private static final Logger log = LoggerFactory.getLogger(PDFHelper.class);

    public static PDFProcessResult processPdfFromUrlWithBytes(String pdfUrl) {
        try {
            PDFProcessingResult result = processAndGetResult(pdfUrl);
            return new PDFProcessResult(result.pdfBytes, result.htmlContent);
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    private static class PDFProcessingResult {
        final byte[] pdfBytes;
        final String htmlContent;

        PDFProcessingResult(byte[] pdfBytes, String htmlContent) {
            this.pdfBytes = pdfBytes;
            this.htmlContent = htmlContent;
        }
    }

    private static PDFProcessingResult processAndGetResult(String pdfUrl) throws Exception {
        Logger ttfLogger = LoggerFactory.getLogger("org.mabb.fontverter.opentype.TtfInstructions.TtfInstructionParser");
        if (ttfLogger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) ttfLogger).setLevel(Level.ERROR);
        }
        // Create a temporary file for the downloaded PDF
        File tempPdfFile = File.createTempFile("downloaded_pdf_", ".pdf");
        tempPdfFile.deleteOnExit();

        // Download the PDF from the URL
        log.info("Downloading PDF from: {}", pdfUrl);
        downloadPdf(pdfUrl, tempPdfFile.toPath());
        log.info("PDF downloaded successfully to: {}", tempPdfFile.getAbsolutePath());

        // Read PDF bytes
        byte[] pdfBytes = Files.readAllBytes(tempPdfFile.toPath());

        // Create a temporary file for the output HTML
        File tempHtmlFile = File.createTempFile("output_html_", ".html");
        tempHtmlFile.deleteOnExit();

        // Process the downloaded PDF
        try (PDDocument document = PDDocument.load(tempPdfFile)) {
            PDFDomTree pdfDomTree = new PDFDomTree();

            // Convert PDF to HTML
            try (Writer output = new PrintWriter(tempHtmlFile, "utf-8")) {
                pdfDomTree.writeText(document, output);
            }

            // Post-process the HTML
            String simplifiedHtml = simplifyHtml(tempHtmlFile.getAbsolutePath());
            String formattedHtml = formatHtml(simplifiedHtml);

            return new PDFProcessingResult(pdfBytes, formattedHtml);
        }
    }

    public static class PDFProcessResult {
        private final byte[] pdfBytes;
        private final String htmlContent;

        public PDFProcessResult(byte[] pdfBytes, String htmlContent) {
            this.pdfBytes = pdfBytes;
            this.htmlContent = htmlContent;
        }

        public byte[] getPdfBytes() {
            return pdfBytes;
        }

        public String getHtmlContent() {
            return htmlContent;
        }
    }

    /**
     * Downloads a PDF from the given URL and saves it to the specified file path.
     * Ensures the file is fully written before returning.
     */
    private static void downloadPdf(String pdfUrl, Path outputPath) throws IOException {
        try {
            URI uri = new URI(pdfUrl);
            URL url = uri.toURL();
            try (var inputStream = url.openStream()) {
                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("Error downloading PDF: {}", e.getMessage(), e);
        }
    }

    /**
     * Simplifies the HTML by extracting content from div.page, performing cleanup
     * tasks, and applying replacements for font-family and font-weight.
     */
    private static String simplifyHtml(String htmlPath) throws IOException {
        // Read the HTML file
        File input = new File(htmlPath);
        Document doc = Jsoup.parse(input, "UTF-8");

        // Replace font-family with Calibri and remove font-weight:bold in inline styles
        Elements styledElements = doc.select("[style]");
        for (Element el : styledElements) {
            String styleAttr = el.attr("style");
            styleAttr = styleAttr.replaceAll("font-family\\s*:\\s*Calibri-Bold;?", "").trim();
            styleAttr = styleAttr.replaceAll("font-family\\s*:\\s*[^;]+;?", "").trim();
            styleAttr = styleAttr.replaceAll("font-weight\\s*:\\s*bold;?", "").trim();
            styleAttr = styleAttr.replaceAll("color\\s*:\\s*[^;]+;?", "").trim();
            styleAttr = styleAttr.replaceAll("background-color\\s*:\\s*[^;]+;?", "").trim();
            if (styleAttr.isEmpty()) {
                el.removeAttr("style");
            } else {
                el.attr("style", styleAttr);
            }
        }

        // Remove all table elements
        doc.select("table").remove();

        // Remove all image elements
        doc.select("img").remove();

        // Extract content from div.page
        Elements pageDivs = doc.select("div.page");
        StringBuilder extractedContent = new StringBuilder();
        for (Element pageDiv : pageDivs) {
            extractedContent.append(pageDiv.html().trim());
        }

        // Return the extracted content as a string
        return extractedContent.toString();
    }

    /**
     * Minifies the HTML content by removing unnecessary whitespace and line breaks.
     */
    private static String minifyHtml(String htmlContent) {
        return htmlContent.replaceAll("\\s{2,}", " ") // Replace multiple spaces with a single space
                .replaceAll(">\\s+<", "><") // Remove spaces between tags
                .trim(); // Trim leading and trailing whitespace
    }

    /**
     * Formats the HTML content for email sending by wrapping it in a basic HTML
     * structure and minifying the final output.
     */
    public static String formatHtml(String htmlContent) {
        StringBuilder formattedHtml = new StringBuilder();
        formattedHtml.append("<!DOCTYPE html>\n");
        formattedHtml.append("<html>\n");
        formattedHtml.append("<head>\n");
        formattedHtml.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");

        // Build the style block
        StringBuilder styleBlock = new StringBuilder();
        styleBlock.append("<style type=\"text/css\">\n");
        styleBlock.append(".page { position: relative; border: 1px solid blue; margin: 0.5em; }\n");
        styleBlock.append(".p, .r { position: absolute; }\n");
        styleBlock.append("div { font-family: Calibri; color: #415462; }\n");
        styleBlock.append("@supports(-webkit-text-stroke: 1px black) { .p { text-shadow: none !important; } }\n");
        styleBlock.append("</style>\n");

        formattedHtml.append(styleBlock);

        formattedHtml.append("</head>\n");
        formattedHtml.append("<body>\n");
        formattedHtml.append(htmlContent);
        formattedHtml.append("\n</body>\n");
        formattedHtml.append("</html>");

        // Minify the final HTML
        return minifyHtml(formattedHtml.toString());
    }
}