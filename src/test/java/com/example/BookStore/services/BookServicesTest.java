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

class BookServicesTest {

    private BookServices bookServices;

    @BeforeEach
    void setUp() {
        bookServices = new BookServices(mock(JsonExportService.class), mock(ReportService.class));
    }

    @Test
    void addBookAssignsNextIdWhenMissingOrDuplicate() {
        BookModel first = bookServices.addBook(book(10, "Clean Code", "Robert C. Martin", "Programming"));
        BookModel second = bookServices.addBook(book(null, "Effective Java", "Joshua Bloch", "Programming"));
        BookModel third = bookServices.addBook(book(10, "Refactoring", "Martin Fowler", "Software Design"));

        assertEquals(10, first.getId());
        assertEquals(11, second.getId());
        assertEquals(12, third.getId());
        assertEquals(3, bookServices.getAllBooks().size());
    }

    @Test
    void getBookByIdReturnsMatchAndThrowsWhenMissing() {
        bookServices.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        assertEquals("Clean Code", bookServices.getBookById(1).getBookName());
        assertThrows(BookNotFoundException.class, () -> bookServices.getBookById(99));
    }

    @Test
    void filtersBooksByCategoryAndAuthorIgnoringCase() {
        bookServices.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));
        bookServices.addBook(book(2, "Refactoring", "Martin Fowler", "Software Design"));
        bookServices.addBook(book(3, "Effective Java", "Joshua Bloch", "Programming"));

        assertEquals(2, bookServices.getBooksByCategory("programming").size());
        assertEquals(1, bookServices.getBooksByAuthor("martin fowler").size());
        assertEquals("Refactoring", bookServices.getBooksByAuthor("MARTIN FOWLER").get(0).getBookName());
    }

    @Test
    void updateBookReplacesFieldsAndDeleteBookRemovesEntry() {
        bookServices.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        BookModel updated = book(99, "Clean Architecture", "Robert C. Martin", "Architecture");
        updated.setPublisher("Prentice Hall");
        updated.setPrice(799.0);
        updated.setQuantity(30);
        updated.setPublishedYear(2017);
        updated.setIsbn("9780134494166");
        updated.setLanguage("English");

        BookModel result = bookServices.updateBook(1, updated);

        assertEquals("Clean Architecture", result.getBookName());
        assertEquals("Architecture", result.getCategory());
        assertEquals(799.0, result.getPrice());

        bookServices.deleteBook(1);
        assertTrue(bookServices.getAllBooks().isEmpty());
    }

    @Test
    void returnedBookListIsUnmodifiableSnapshot() {
        bookServices.addBook(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        List<BookModel> books = bookServices.getAllBooks();

        assertThrows(UnsupportedOperationException.class, () -> books.add(book(2, "New Book", "Author", "Category")));
    }

    private static BookModel book(Integer id, String bookName, String authorName, String category) {
        return new BookModel(id, bookName, authorName, category, "Publisher", 100.0, 5, 2024, "9780000000000", "English");
    }
}