package com.example.BookStore.services;

import com.example.BookStore.model.BookModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exports the in-memory book list to a pretty-printed JSON file.
 *
 * <p>Output location: {@code src/main/resources/output/books.json}
 * (resolved relative to the working directory at runtime).
 */
@Service
public class JsonExportService {

    private static final Logger log = LoggerFactory.getLogger(JsonExportService.class);

    /** Output path relative to the project working directory. */
    private static final String OUTPUT_PATH = "src/main/resources/output/books.json";

    private final ObjectMapper objectMapper;

    public JsonExportService() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);   // pretty-print
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Serialises {@code books} to {@value OUTPUT_PATH} as indented JSON.
     *
     * <p>The output directory is created automatically if it does not exist.
     *
     * @param books the list of books to export
     */
    public void exportToJson(List<BookModel> books) {
        if (books == null || books.isEmpty()) {
            log.warn("JsonExportService: book list is empty – skipping JSON export");
            return;
        }

        Path outputPath = Paths.get(OUTPUT_PATH);

        try {
            // Create parent directories if they do not exist
            Files.createDirectories(outputPath.getParent());

            objectMapper.writeValue(outputPath.toFile(), books);

            log.info("JSON export complete: {} books written to {}", books.size(),
                    outputPath.toAbsolutePath());

        } catch (IOException e) {
            log.error("JsonExportService: failed to write JSON file at {}", outputPath.toAbsolutePath(), e);
        }
    }
}
