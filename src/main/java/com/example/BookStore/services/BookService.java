package com.example.BookStore.services;

import com.example.BookStore.exception.BookNotFoundException;
import com.example.BookStore.model.BookModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final String DELIMITER = "\t";

    private final List<BookModel> books = new ArrayList<>();
    private final JsonExportService jsonExportService;
    private final ReportService reportService;

    public BookService(JsonExportService jsonExportService, ReportService reportService) {
        this.jsonExportService = jsonExportService;
        this.reportService = reportService;
    }

    @PostConstruct
    public void loadBooks() {
        log.info("Loading books from classpath:books.csv");
        ClassPathResource resource = new ClassPathResource("books.csv");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("books.csv is empty – no books loaded");
                return;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isBlank()) {
                    continue;
                }
                parseRow(line, lineNumber).ifPresent(books::add);
            }

            log.info("Successfully loaded {} books", books.size());

            jsonExportService.exportToJson(List.copyOf(books));
            reportService.generateReport(List.copyOf(books));

        } catch (IOException e) {
            log.error("Failed to read books.csv from classpath", e);
        }
    }

    public List<BookModel> getAllBooks() {
        return getAllBooks(0, Integer.MAX_VALUE, "id", "asc");
    }

    @Cacheable(value = "books")
    public List<BookModel> getAllBooks(int page, int pageSize, String sortBy, String sortDirection) {
        return applyPaginationAndSorting(books, page, pageSize, sortBy, sortDirection);
    }

    @Cacheable(value = "bookById", key = "#id")
    public BookModel getBookById(Integer id) {
        return books.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public List<BookModel> getBooksByCategory(String category) {
        return getBooksByCategory(category, 0, Integer.MAX_VALUE, "id", "asc");
    }

    @Cacheable(value = "booksByCategory")
    public List<BookModel> getBooksByCategory(String category, int page, int pageSize, String sortBy, String sortDirection) {
        List<BookModel> filtered = books.stream()
                .filter(b -> b.getCategory().equalsIgnoreCase(category))
                .toList();
        return applyPaginationAndSorting(filtered, page, pageSize, sortBy, sortDirection);
    }

    public List<BookModel> getBooksByAuthor(String authorName) {
        return getBooksByAuthor(authorName, 0, Integer.MAX_VALUE, "id", "asc");
    }

    @Cacheable(value = "booksByAuthor")
    public List<BookModel> getBooksByAuthor(String authorName, int page, int pageSize, String sortBy, String sortDirection) {
        List<BookModel> filtered = books.stream()
                .filter(b -> b.getAuthorName().equalsIgnoreCase(authorName))
                .toList();
        return applyPaginationAndSorting(filtered, page, pageSize, sortBy, sortDirection);
    }

    @CacheEvict(value = {"books", "bookById", "booksByCategory", "booksByAuthor"}, allEntries = true)
    public BookModel addBook(BookModel book) {
        // Auto-assign a unique ID if it is missing or already taken
        if (book.getId() == null || idExists(book.getId())) {
            int nextId = books.stream()
                    .map(BookModel::getId)
                    .max(Comparator.naturalOrder())
                    .orElse(0) + 1;
            book.setId(nextId);
            log.debug("Auto-assigned id {} to new book", nextId);
        }

        books.add(book);
        log.info("Added book id={} title='{}'", book.getId(), book.getBookName());
        return book;
    }

    @CacheEvict(value = {"books", "bookById", "booksByCategory", "booksByAuthor"}, allEntries = true)
    public BookModel updateBook(Integer id, BookModel updatedBook) {
        BookModel existing = getBookById(id);

        existing.setBookName(updatedBook.getBookName());
        existing.setAuthorName(updatedBook.getAuthorName());
        existing.setCategory(updatedBook.getCategory());
        existing.setPublisher(updatedBook.getPublisher());
        existing.setPrice(updatedBook.getPrice());
        existing.setQuantity(updatedBook.getQuantity());
        existing.setPublishedYear(updatedBook.getPublishedYear());
        existing.setIsbn(updatedBook.getIsbn());
        existing.setLanguage(updatedBook.getLanguage());

        log.info("Updated book id={} title='{}'", id, existing.getBookName());
        return existing;
    }

    @CacheEvict(value = {"books", "bookById", "booksByCategory", "booksByAuthor"}, allEntries = true)
    public void deleteBook(Integer id) {
        BookModel book = getBookById(id);
        books.remove(book);
        log.info("Deleted book id={} title='{}'", id, book.getBookName());
    }

    private boolean idExists(Integer id) {
        return books.stream().anyMatch(b -> b.getId().equals(id));
    }

    private List<BookModel> applyPaginationAndSorting(List<BookModel> source, int page, int pageSize, String sortBy, String sortDirection) {
        List<BookModel> sorted = new ArrayList<>(source);
        sorted.sort(buildComparator(sortBy, sortDirection));

        int effectivePage = Math.max(page, 0);
        int effectivePageSize = Math.max(pageSize, 1);
        int fromIndex = effectivePage * effectivePageSize;

        if (fromIndex >= sorted.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + effectivePageSize, sorted.size());
        return List.copyOf(sorted.subList(fromIndex, toIndex));
    }

    private Comparator<BookModel> buildComparator(String sortBy, String sortDirection) {
        Comparator<BookModel> comparator = switch (sortBy == null ? "id" : sortBy.trim().toLowerCase()) {
            case "bookname" -> Comparator.comparing(BookModel::getBookName, String.CASE_INSENSITIVE_ORDER);
            case "authorname" -> Comparator.comparing(BookModel::getAuthorName, String.CASE_INSENSITIVE_ORDER);
            case "category" -> Comparator.comparing(BookModel::getCategory, String.CASE_INSENSITIVE_ORDER);
            case "publisher" -> Comparator.comparing(BookModel::getPublisher, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparingDouble(BookModel::getPrice);
            case "quantity" -> Comparator.comparingInt(BookModel::getQuantity);
            case "publishedyear" -> Comparator.comparingInt(BookModel::getPublishedYear);
            case "isbn" -> Comparator.comparing(BookModel::getIsbn, String.CASE_INSENSITIVE_ORDER);
            case "language" -> Comparator.comparing(BookModel::getLanguage, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingInt(BookModel::getId);
        };

        return "desc".equalsIgnoreCase(sortDirection) ? comparator.reversed() : comparator;
    }

    private Optional<BookModel> parseRow(String line, int lineNumber) {
        String[] cols = line.split(DELIMITER, -1);

        if (cols.length != 10) {
            log.warn("Line {}: expected 10 columns but found {} – skipping: [{}]",
                    lineNumber, cols.length, line);
            return Optional.empty();
        }

        try {
            return Optional.of(new BookModel(
                    Integer.parseInt(cols[0].trim()),
                    cols[1].trim(),
                    cols[2].trim(),
                    cols[3].trim(),
                    cols[4].trim(),
                    Double.parseDouble(cols[5].trim()),
                    Integer.parseInt(cols[6].trim()),
                    Integer.parseInt(cols[7].trim()),
                    cols[8].trim(),
                    cols[9].trim()
            ));
        } catch (NumberFormatException e) {
            log.warn("Line {}: failed to parse numeric field – skipping: {}", lineNumber, e.getMessage());
            return Optional.empty();
        }
    }
}
