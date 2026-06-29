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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final String OUTPUT_PATH = "src/main/resources/output/report.txt";
    private static final String SEPARATOR = "=".repeat(60);
    private static final String SUB_SEP = "-".repeat(60);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    public String getReportContent() throws IOException {
        return Files.readString(Paths.get(OUTPUT_PATH), StandardCharsets.UTF_8);
    }

    private void writeReport(BufferedWriter w, List<BookModel> books) throws IOException {
        // Header
        writeLine(w, SEPARATOR);
        writeLine(w, "           BOOKSTORE INVENTORY REPORT");
        writeLine(w, "  Generated: " + LocalDateTime.now().format(FORMATTER));
        writeLine(w, SEPARATOR);
        writeLine(w, "");

        // 1. Summary statistics
        writeLine(w, "1. SUMMARY STATISTICS");
        writeLine(w, SUB_SEP);

        long totalBooks = books.size();
        long totalStock = books.stream().mapToLong(BookModel::getQuantity).sum();
        double totalValue = books.stream()
                .mapToDouble(b -> b.getPrice() * b.getQuantity())
                .sum();
        double averagePrice = books.stream()
                .mapToDouble(BookModel::getPrice)
                .average()
                .orElse(0.0);

        writeLine(w, String.format("  Total number of books    : %d", totalBooks));
        writeLine(w, String.format("  Total stock quantity     : %d units", totalStock));
        writeLine(w, String.format("  Total inventory value    : %.2f", totalValue));
        writeLine(w, String.format("  Average book price       : %.2f", averagePrice));
        writeLine(w, "");

        // 2. Price extremes
        writeLine(w, "2. PRICE EXTREMES");
        writeLine(w, SUB_SEP);

        Optional<BookModel> mostExpensive = books.stream()
                .max(Comparator.comparingDouble(BookModel::getPrice));
        if (mostExpensive.isPresent()) {
            BookModel b = mostExpensive.get();
            writeLine(w, String.format("  Most expensive book      : %s (%.2f) by %s",
                    b.getBookName(), b.getPrice(), b.getAuthorName()));
        }

        Optional<BookModel> leastExpensive = books.stream()
                .min(Comparator.comparingDouble(BookModel::getPrice));
        if (leastExpensive.isPresent()) {
            BookModel b = leastExpensive.get();
            writeLine(w, String.format("  Least expensive book     : %s (%.2f) by %s",
                    b.getBookName(), b.getPrice(), b.getAuthorName()));
        }
        writeLine(w, "");

        // 3. Books per category
        writeLine(w, "3. BOOKS PER CATEGORY");
        writeLine(w, SUB_SEP);

        Map<String, Long> perCategory = books.stream()
                .collect(Collectors.groupingBy(BookModel::getCategory, Collectors.counting()));

        List<Map.Entry<String, Long>> sortedCategories = perCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        for (Map.Entry<String, Long> entry : sortedCategories) {
            writeLine(w, String.format("  %-30s : %d book(s)", entry.getKey(), entry.getValue()));
        }
        writeLine(w, "");

        // 4. Top 5 most expensive books
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

    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }
}
