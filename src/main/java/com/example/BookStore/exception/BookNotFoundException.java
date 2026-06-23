package com.example.BookStore.exception;

/**
 * Thrown when a requested book cannot be found in the in-memory store.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Integer id) {
        super("Book not found with id: " + id);
    }

    public BookNotFoundException(String message) {
        super(message);
    }
}
