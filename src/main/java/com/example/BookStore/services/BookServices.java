package com.example.BookStore.services;

import com.example.BookStore.exception.BookNotFoundException;
import com.example.BookStore.model.BookModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Service responsible for loading books from the CSV file at startup
 * and providing full CRUD operations over the in-memory book list.
 *
 * <p>The CSV (actually TSV) file is expected at:
 * {@code src/main/resources/books.csv}
 *
 * <p>Expected header (tab-separated):
 * {@code id  bookName  authorName  category  publisher  price  quantity  publishedYear  isbn  language}
 */
@Service
public class BookServices {

    private static final Logger log = LoggerFactory.getLogger(BookServices.class);

    /** Tab character used as the column delimiter in books.csv */
    private static final String DELIMITER = "\t";

    private final List<BookModel> books = new ArrayList<>();

    private final JsonExportService jsonExportService;
    private final ReportService     reportService;

    public BookServices(JsonExportService jsonExportService, ReportService reportService) {
        this.jsonExportService = jsonExportService;
        this.reportService     = reportService;
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Loads books from {@code books.csv} once the bean is fully initialised.
     * Any row that cannot be parsed is skipped with a warning.
     */
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
                if (line.isBlank()) continue;
                parseRow(line, lineNumber).ifPresent(books::add);
            }

            log.info("Successfully loaded {} books", books.size());

            // Export to JSON and generate report after successful CSV load
            jsonExportService.exportToJson(List.copyOf(books));
            reportService.generateReport(List.copyOf(books));

        } catch (IOException e) {
            log.error("Failed to read books.csv from classpath", e);
        }
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** Returns an unmodifiable snapshot of all books. */
    public List<BookModel> getAllBooks() {
        return List.copyOf(books);
    }

    /**
     * Returns a book by its id.
     *
     * @throws BookNotFoundException if no book with the given id exists
     */
    public BookModel getBookById(Integer id) {
        return books.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    /** Returns all books whose category matches (case-insensitive). */
    public List<BookModel> getBooksByCategory(String category) {
        return books.stream()
                .filter(b -> b.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    /** Returns all books whose author name matches (case-insensitive). */
    public List<BookModel> getBooksByAuthor(String authorName) {
        return books.stream()
                .filter(b -> b.getAuthorName().equalsIgnoreCase(authorName))
                .toList();
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Adds a new book to the in-memory store.
     *
     * <p>If {@code book.getId()} is {@code null} or already taken,
     * a new id is generated automatically (max existing id + 1).
     *
     * @param book the book to add (id may be omitted)
     * @return the saved book with its assigned id
     */
    public BookModel addBook(BookModel book) {
        // Auto-assign an id if missing or already in use
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

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    /**
     * Fully replaces the book identified by {@code id} with the provided data.
     *
     * @param id          the id of the book to update
     * @param updatedBook the new book data (id field in body is ignored; path id is used)
     * @return the updated book
     * @throws BookNotFoundException if no book with the given id exists
     */
    public BookModel updateBook(Integer id, BookModel updatedBook) {
        BookModel existing = getBookById(id); // throws 404 if absent

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

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Removes the book with the given id from the in-memory store.
     *
     * @param id the id of the book to delete
     * @throws BookNotFoundException if no book with the given id exists
     */
    public void deleteBook(Integer id) {
        BookModel book = getBookById(id); // throws 404 if absent
        books.remove(book);
        log.info("Deleted book id={} title='{}'", id, book.getBookName());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean idExists(Integer id) {
        return books.stream().anyMatch(b -> b.getId().equals(id));
    }

    /**
     * Parses a single TSV row into a {@link BookModel}.
     * Returns empty if the row has the wrong column count or a bad numeric value.
     */
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
