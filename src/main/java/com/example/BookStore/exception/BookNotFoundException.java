package com.example.BookStore.exception;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Integer id) {
        super("Book not found with id: " + id);
    }

    public BookNotFoundException(String message) {
        super(message);
    }
}
