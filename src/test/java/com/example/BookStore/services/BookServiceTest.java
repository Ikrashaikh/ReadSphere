package com.example.BookStore.services;

import com.example.BookStore.exception.BookNotFoundException;
import com.example.BookStore.model.BookModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BookServiceTest {

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(mock(JsonExportService.class), mock(ReportService.class));
    }

    @Test
    void addBookAssignsNextIdWhenMissingOrDuplicate() {
        BookModel first = bookService.addBook(book(10, "Clean Code", "Robert C. Martin", "Programming"));
        BookModel second = bookService.addBook(book(null, "Effective Java", "Joshua Bloch", "Programming"));
        BookModel third = bookService.addBook(book(10, "Refactoring", "Martin Fowler", "Software Design"));

        assertEquals(10, first.getId());
        assertEquals(11, second.getId());
        assertEquals(12, third.getId());
        assertEquals(3, bookService.getAllBooks().size());
    }

    @Test
    void getBookByIdReturnsMatchAndThrowsWhenMissing() {
        bookService.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        assertEquals("Clean Code", bookService.getBookById(1).getBookName());
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(99));
    }

    @Test
    void filtersBooksByCategoryAndAuthorIgnoringCase() {
        bookService.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));
        bookService.addBook(book(2, "Refactoring", "Martin Fowler", "Software Design"));
        bookService.addBook(book(3, "Effective Java", "Joshua Bloch", "Programming"));

        assertEquals(2, bookService.getBooksByCategory("programming").size());
        assertEquals(1, bookService.getBooksByAuthor("martin fowler").size());
        assertEquals("Refactoring", bookService.getBooksByAuthor("MARTIN FOWLER").get(0).getBookName());
    }

    @Test
    void getAllBooksSupportsPaginationAndSorting() {
        BookModel first = book(1, "Clean Code", "Robert C. Martin", "Programming");
        first.setPrice(100.0);
        BookModel second = book(2, "Refactoring", "Martin Fowler", "Software Design");
        second.setPrice(80.0);
        BookModel third = book(3, "Effective Java", "Joshua Bloch", "Programming");
        third.setPrice(120.0);

        bookService.addBook(first);
        bookService.addBook(second);
        bookService.addBook(third);

        List<BookModel> page = bookService.getAllBooks(0, 2, "price", "desc");

        assertEquals(2, page.size());
        assertEquals("Effective Java", page.get(0).getBookName());
        assertEquals("Clean Code", page.get(1).getBookName());
    }

    @Test
    void updateBookReplacesFieldsAndDeleteBookRemovesEntry() {
        bookService.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        BookModel updated = book(99, "Clean Architecture", "Robert C. Martin", "Architecture");
        updated.setPublisher("Prentice Hall");
        updated.setPrice(799.0);
        updated.setQuantity(30);
        updated.setPublishedYear(2017);
        updated.setIsbn("9780134494166");
        updated.setLanguage("English");

        BookModel result = bookService.updateBook(1, updated);

        assertEquals("Clean Architecture", result.getBookName());
        assertEquals("Architecture", result.getCategory());
        assertEquals(799.0, result.getPrice());

        bookService.deleteBook(1);
        assertTrue(bookService.getAllBooks().isEmpty());
    }

    @Test
    void returnedBookListIsUnmodifiableSnapshot() {
        bookService.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        List<BookModel> books = bookService.getAllBooks();

        assertThrows(UnsupportedOperationException.class, () -> books.add(book(2, "New Book", "Author", "Category")));
    }

    private static BookModel book(Integer id, String bookName, String authorName, String category) {
        return new BookModel(id, bookName, authorName, category, "Publisher", 100.0, 5, 2024, "9780000000000", "English");
    }
}
