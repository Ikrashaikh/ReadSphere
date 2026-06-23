package com.example.BookStore.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


// Represents a book entity loaded from books.csv.
@Schema(description = "Represents a book in the BookStore catalogue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookModel {

    @Schema(description = "Unique identifier of the book", example = "1")
    private Integer id;

    @Schema(description = "Title of the book", example = "Clean Code")
    private String bookName;

    @Schema(description = "Full name of the author", example = "Robert C. Martin")
    private String authorName;

    @Schema(description = "Genre or category the book belongs to", example = "Programming")
    private String category;

    @Schema(description = "Publisher of the book", example = "Prentice Hall")
    private String publisher;

    @Schema(description = "Price of the book in local currency", example = "599.0")
    private Double price;

    @Schema(description = "Number of copies available in stock", example = "20")
    private Integer quantity;

    @Schema(description = "Year the book was published", example = "2008")
    private Integer publishedYear;

    @Schema(description = "International Standard Book Number", example = "9780132350884")
    private String isbn;

    @Schema(description = "Language the book is written in", example = "English")
    private String language;
}
