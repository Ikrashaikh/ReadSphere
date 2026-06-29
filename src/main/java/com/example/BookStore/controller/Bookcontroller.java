package com.example.BookStore.controller;

import com.example.BookStore.model.BookModel;
import com.example.BookStore.services.BookService;
import com.example.BookStore.services.JsonExportService;
import com.example.BookStore.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Tag(name = "Books", description = "Full CRUD operations for the BookStore catalogue")
@RestController
@RequestMapping("/books")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;
    private final ReportService reportService;
    private final JsonExportService jsonExportService;

    public BookController(BookService bookService, ReportService reportService, JsonExportService jsonExportService) {
        this.bookService = bookService;
        this.reportService = reportService;
        this.jsonExportService = jsonExportService;
    }

    @Operation(summary = "Get all books", description = "Returns the complete list of books loaded from books.csv.")
    @ApiResponses(@ApiResponse(
            responseCode = "200", description = "Successfully retrieved all books",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = BookModel.class)))))
    @GetMapping
    public ResponseEntity<List<BookModel>> getAllBooks(
            @Parameter(description = "Page number to retrieve (starting at 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of books per page", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Field to sort by", example = "price")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDirection) {
        log.debug("GET /books?page={} pageSize={} sortBy={} sortDirection={}", page, pageSize, sortBy, sortDirection);
        List<BookModel> books = bookService.getAllBooks(page, pageSize, sortBy, sortDirection);
        log.info("GET /books – returning {} books", books.size());
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Get a book by ID", description = "Returns a single book by its numeric id. Returns 404 if not found.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BookModel.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookModel> getBookById(
            @Parameter(description = "Numeric ID of the book", required = true, example = "1")
            @PathVariable Integer id) {
        log.debug("GET /books/{}", id);
        BookModel book = bookService.getBookById(id);
        log.info("GET /books/{} – found '{}'", id, book.getBookName());
        return ResponseEntity.ok(book);
    }

    @Operation(summary = "Get books by category", description = "Returns all books in the given category (case-insensitive).")
    @ApiResponses(@ApiResponse(
            responseCode = "200", description = "Books retrieved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = BookModel.class)))))
    @GetMapping("/category/{category}")
    public ResponseEntity<List<BookModel>> getBooksByCategory(
            @Parameter(description = "Category name, e.g. Programming", required = true, example = "Programming")
            @PathVariable String category,
            @Parameter(description = "Page number to retrieve (starting at 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of books per page", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Field to sort by", example = "price")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDirection) {
        log.debug("GET /books/category/{}?page={} pageSize={} sortBy={} sortDirection={}", category, page, pageSize, sortBy, sortDirection);
        List<BookModel> books = bookService.getBooksByCategory(category, page, pageSize, sortBy, sortDirection);
        log.info("GET /books/category/{} – returning {} books", category, books.size());
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Get books by author", description = "Returns all books by the given author (case-insensitive).")
    @ApiResponses(@ApiResponse(
            responseCode = "200", description = "Books retrieved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = BookModel.class)))))
    @GetMapping("/author/{authorName}")
    public ResponseEntity<List<BookModel>> getBooksByAuthor(
            @Parameter(description = "Full name of the author", required = true, example = "Martin Fowler")
            @PathVariable String authorName,
            @Parameter(description = "Page number to retrieve (starting at 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of books per page", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Field to sort by", example = "price")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDirection) {
        log.debug("GET /books/author/{}?page={} pageSize={} sortBy={} sortDirection={}", authorName, page, pageSize, sortBy, sortDirection);
        List<BookModel> books = bookService.getBooksByAuthor(authorName, page, pageSize, sortBy, sortDirection);
        log.info("GET /books/author/{} – returning {} books", authorName, books.size());
        return ResponseEntity.ok(books);
    }

    @Operation(
            summary     = "Add a new book",
            description = "Creates a new book entry. The id field is optional – if omitted or already taken, "
                        + "the server auto-assigns the next available id. Returns 201 with a Location header.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BookModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<BookModel> addBook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Book object to create. The id field is optional.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookModel.class)))
            @RequestBody BookModel book) {
        log.debug("POST /books – adding book: {}", book.getBookName());
        BookModel saved = bookService.addBook(book);
        log.info("POST /books – created book id={}", saved.getId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    @Operation(
            summary     = "Update a book",
            description = "Fully replaces the book with the given id. All fields must be provided. Returns 404 if not found.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BookModel.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookModel> updateBook(
            @Parameter(description = "ID of the book to update", required = true, example = "1")
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated book data. The id field in the body is ignored; the path id is used.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookModel.class)))
            @RequestBody BookModel updatedBook) {
        log.debug("PUT /books/{}", id);
        BookModel book = bookService.updateBook(id, updatedBook);
        log.info("PUT /books/{} – updated '{}'", id, book.getBookName());
        return ResponseEntity.ok(book);
    }

    @Operation(
            summary     = "Delete a book",
            description = "Removes the book with the given id from the catalogue. Returns 204 on success or 404 if not found.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "ID of the book to delete", required = true, example = "1")
            @PathVariable Integer id) {
        log.debug("DELETE /books/{}", id);
        bookService.deleteBook(id);
        log.info("DELETE /books/{} – deleted", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "View inventory report", description = "Returns the latest inventory report as plain text.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report returned successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "500", description = "Report file not found or could not be read")
    })
    @GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getReport() {
        log.debug("GET /books/report");
        try {
            String content = reportService.getReportContent();
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            log.error("Failed to read report.txt", e);
            return ResponseEntity.internalServerError().body("Report file not found. Start the application to generate it.");
        }
    }

    @Operation(summary = "Regenerate report and export", description = "Regenerates the inventory report and JSON export from current in-memory data, then returns the report.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Report regenerated and returned",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)))
    @PostMapping(value = "/report/regenerate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> regenerateReport() {
        log.debug("POST /books/report/regenerate");
        List<BookModel> books = bookService.getAllBooks();
        reportService.generateReport(books);
        jsonExportService.exportToJson(books);
        try {
            String content = reportService.getReportContent();
            log.info("POST /books/report/regenerate – report regenerated for {} books", books.size());
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            log.error("Failed to read regenerated report.txt", e);
            return ResponseEntity.internalServerError().body("Report regenerated but could not be read.");
        }
    }
}
