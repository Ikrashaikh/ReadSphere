package com.example.BookStore.controller;

import com.example.BookStore.exception.GlobalExceptionHandler;
import com.example.BookStore.exception.BookNotFoundException;
import com.example.BookStore.model.BookModel;
import com.example.BookStore.services.BookServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//Trial commit Message

class BookcontrollerTest {

    private MockMvc mockMvc;

    private BookServices bookServices;

        @BeforeEach
        void setUp() {
                bookServices = mock(BookServices.class);
                mockMvc = MockMvcBuilders.standaloneSetup(new Bookcontroller(bookServices))
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

    @Test
    void getAllBooksReturnsBooks() throws Exception {
        when(bookServices.getAllBooks()).thenReturn(List.of(
                book(1, "Clean Code", "Robert C. Martin", "Programming"),
                book(2, "Refactoring", "Martin Fowler", "Software Design")
        ));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].bookName").value("Clean Code"));
    }

    @Test
    void getBookByIdReturns404WhenBookIsMissing() throws Exception {
        when(bookServices.getBookById(99)).thenThrow(new BookNotFoundException(99));

        mockMvc.perform(get("/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book not found with id: 99"));
    }

    @Test
    void getBookByIdReturnsBook() throws Exception {
        when(bookServices.getBookById(1)).thenReturn(book(1, "Clean Code", "Robert C. Martin", "Programming"));

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Clean Code"));
    }

    @Test
    void postBookCreatesResourceWithLocationHeader() throws Exception {
        when(bookServices.addBook(any(BookModel.class)))
                .thenReturn(book(21, "Domain Driven Design", "Eric Evans", "Architecture"));

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookName": "Domain Driven Design",
                                  "authorName": "Eric Evans",
                                  "category": "Architecture",
                                  "publisher": "Addison-Wesley",
                                  "price": 1100,
                                  "quantity": 5,
                                  "publishedYear": 2003,
                                  "isbn": "9780321125217",
                                  "language": "English"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/books/21"))
                .andExpect(jsonPath("$.id").value(21));
    }

    @Test
    void putBookUpdatesBook() throws Exception {
        when(bookServices.updateBook(eq(1), any(BookModel.class)))
                .thenReturn(book(1, "Clean Architecture", "Robert C. Martin", "Architecture"));

        mockMvc.perform(put("/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookName": "Clean Architecture",
                                  "authorName": "Robert C. Martin",
                                  "category": "Architecture",
                                  "publisher": "Prentice Hall",
                                  "price": 799,
                                  "quantity": 30,
                                  "publishedYear": 2017,
                                  "isbn": "9780134494166",
                                  "language": "English"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Clean Architecture"));
    }

    @Test
    void deleteBookReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/books/1"))
                .andExpect(status().isNoContent());

        verify(bookServices).deleteBook(1);
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void serviceRuntimeExceptionMapsToGenericErrorResponse() throws Exception {
        when(bookServices.getBooksByCategory("Programming"))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/books/category/Programming"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    private static BookModel book(Integer id, String bookName, String authorName, String category) {
        return new BookModel(id, bookName, authorName, category, "Publisher", 100.0, 5, 2024, "9780000000000", "English");
    }
}