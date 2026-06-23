package com.example.BookStore.services;

import com.example.BookStore.model.BookModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a human-readable inventory report from the in-memory book list.
 *
 * <p>Output location: {@code src/main/resources/output/report.txt}
 * (resolved relative to the working directory at runtime).
 *
 * <p>Report sections:
 * <ul>
 *   <li>Summary statistics (count, total stock, total value, average price)</li>
 *   <li>Most / least expensive book</li>
 *   <li>Book count per category</li>
 *   <li>Top 5 most expensive books</li>
 * </ul>
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final String OUTPUT_PATH  = "src/main/resources/output/report.txt";
    private static final String SEPARATOR    = "=".repeat(60);
    private static final String SUB_SEP      = "-".repeat(60);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds the inventory report and writes it to {@value OUTPUT_PATH}.
     *
     * @param books the list of books to analyse
     */
    public void generateReport(List<BookModel> books) {
        if (books == null || books.isEmpty()) {
            log.warn("ReportService: book list is empty – skipping report generation");
            return;
        }

        Path outputPath = Paths.get(OUTPUT_PATH);

        try {
            Files.createDirectories(outputPath.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                writeReport(writer, books);
            }

            log.info("Report generation complete: written to {}", outputPath.toAbsolutePath());

        } catch (IOException e) {
            log.error("ReportService: failed to write report at {}", outputPath.toAbsolutePath(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private – report builder
    // -------------------------------------------------------------------------

    private void writeReport(BufferedWriter w, List<BookModel> books) throws IOException {

        // ── Header ────────────────────────────────────────────────────────────
        writeLine(w, SEPARATOR);
        writeLine(w, "           BOOKSTORE INVENTORY REPORT");
        writeLine(w, "  Generated: " + LocalDateTime.now().format(FORMATTER));
        writeLine(w, SEPARATOR);
        writeLine(w, "");

        // ── 1. Summary statistics ─────────────────────────────────────────────
        writeLine(w, "1. SUMMARY STATISTICS");
        writeLine(w, SUB_SEP);

        long   totalBooks    = books.size();
        long   totalStock    = books.stream().mapToLong(BookModel::getQuantity).sum();
        double totalValue    = books.stream()
                                    .mapToDouble(b -> b.getPrice() * b.getQuantity())
                                    .sum();
        double averagePrice  = books.stream()
                                    .mapToDouble(BookModel::getPrice)
                                    .average()
                                    .orElse(0.0);

        writeLine(w, String.format("  Total number of books    : %d", totalBooks));
        writeLine(w, String.format("  Total stock quantity     : %d units", totalStock));
        writeLine(w, String.format("  Total inventory value    : %.2f", totalValue));
        writeLine(w, String.format("  Average book price       : %.2f", averagePrice));
        writeLine(w, "");

        // ── 2. Most / least expensive ─────────────────────────────────────────
        writeLine(w, "2. PRICE EXTREMES");
        writeLine(w, SUB_SEP);

        books.stream()
             .max(Comparator.comparingDouble(BookModel::getPrice))
             .ifPresent(b -> {
                 try {
                     writeLine(w, String.format("  Most expensive book      : %s (%.2f) by %s",
                             b.getBookName(), b.getPrice(), b.getAuthorName()));
                 } catch (IOException ex) {
                     log.error("Write error", ex);
                 }
             });

        books.stream()
             .min(Comparator.comparingDouble(BookModel::getPrice))
             .ifPresent(b -> {
                 try {
                     writeLine(w, String.format("  Least expensive book     : %s (%.2f) by %s",
                             b.getBookName(), b.getPrice(), b.getAuthorName()));
                 } catch (IOException ex) {
                     log.error("Write error", ex);
                 }
             });

        writeLine(w, "");

        // ── 3. Books per category ─────────────────────────────────────────────
        writeLine(w, "3. BOOKS PER CATEGORY");
        writeLine(w, SUB_SEP);

        Map<String, Long> perCategory = books.stream()
                .collect(Collectors.groupingBy(BookModel::getCategory, Collectors.counting()));

        perCategory.entrySet().stream()
                   .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                   .forEach(e -> {
                       try {
                           writeLine(w, String.format("  %-30s : %d book(s)", e.getKey(), e.getValue()));
                       } catch (IOException ex) {
                           log.error("Write error", ex);
                       }
                   });

        writeLine(w, "");

        // ── 4. Top 5 most expensive ───────────────────────────────────────────
        writeLine(w, "4. TOP 5 MOST EXPENSIVE BOOKS");
        writeLine(w, SUB_SEP);

        List<BookModel> top5 = books.stream()
                .sorted(Comparator.comparingDouble(BookModel::getPrice).reversed())
                .limit(5)
                .toList();

        int rank = 1;
        for (BookModel b : top5) {
            writeLine(w, String.format("  %d. %-40s %.2f  [%s]",
                    rank++, b.getBookName(), b.getPrice(), b.getCategory()));
        }

        writeLine(w, "");
        writeLine(w, SEPARATOR);
        writeLine(w, "  END OF REPORT");
        writeLine(w, SEPARATOR);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }
}
