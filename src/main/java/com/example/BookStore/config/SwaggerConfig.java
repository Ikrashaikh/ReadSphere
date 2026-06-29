package com.example.BookStore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI bookStoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookStore API")
                        .description("REST API for managing and querying the BookStore catalogue")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BookStore Team")
                                .email("support@bookstore.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
