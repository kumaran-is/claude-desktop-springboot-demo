package com.example.demo.controller;

import com.example.demo.dto.CreateProductDto;
import com.example.demo.dto.ProductResponse;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProductControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/products";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
    }

    private ProductResponse createProduct(String name, Double price, String category) {
        return webTestClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductDto(name, price, category))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProductTests {

        @Test
        @DisplayName("should create a product and return 201")
        void createProduct_returnsCreated() {
            var dto = new CreateProductDto("Laptop", 1299.99, "Electronics");

            webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ProductResponse.class)
                    .value(product -> {
                        assertThat(product.id()).isNotNull();
                        assertThat(product.name()).isEqualTo("Laptop");
                        assertThat(product.price()).isEqualTo(1299.99);
                        assertThat(product.category()).isEqualTo("Electronics");
                    });
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void createProduct_blankName_returnsBadRequest() {
            var dto = new CreateProductDto("", 10.0, "Category");

            webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(ProblemDetail.class)
                    .value(problem -> {
                        assertThat(problem.getTitle()).isEqualTo("Validation Error");
                        assertThat(problem.getStatus()).isEqualTo(400);
                    });
        }

        @Test
        @DisplayName("should return 400 when price is negative")
        void createProduct_negativePrice_returnsBadRequest() {
            var dto = new CreateProductDto("Item", -5.0, "Category");

            webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("should return 400 when category is missing")
        void createProduct_nullCategory_returnsBadRequest() {
            var dto = new CreateProductDto("Item", 10.0, "");

            webTestClient.post()
                    .uri(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetAllProductsTests {

        @Test
        @DisplayName("should return empty list when no products exist")
        void getAll_empty_returnsEmptyList() {
            webTestClient.get()
                    .uri(BASE_URL)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("should return all products")
        void getAll_withProducts_returnsList() {
            createProduct("Laptop", 1299.99, "Electronics");
            createProduct("Keyboard", 79.99, "Accessories");

            webTestClient.get()
                    .uri(BASE_URL)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(2)
                    .value(products -> {
                        assertThat(products).extracting(ProductResponse::name)
                                .containsExactlyInAnyOrder("Laptop", "Keyboard");
                    });
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductByIdTests {

        @Test
        @DisplayName("should return product when found")
        void getById_exists_returnsProduct() {
            var created = createProduct("Monitor", 499.99, "Electronics");

            webTestClient.get()
                    .uri(BASE_URL + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(product -> {
                        assertThat(product.id()).isEqualTo(created.id());
                        assertThat(product.name()).isEqualTo("Monitor");
                        assertThat(product.price()).isEqualTo(499.99);
                        assertThat(product.category()).isEqualTo("Electronics");
                    });
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void getById_notFound_returns404() {
            webTestClient.get()
                    .uri(BASE_URL + "/{id}", 99999)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .value(problem -> {
                        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
                        assertThat(problem.getDetail()).contains("99999");
                    });
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProductTests {

        @Test
        @DisplayName("should update product and return 200")
        void update_exists_returnsUpdated() {
            var created = createProduct("Laptop", 1299.99, "Electronics");
            var updateDto = new CreateProductDto("Gaming PC", 1899.99, "Electronics");

            webTestClient.put()
                    .uri(BASE_URL + "/{id}", created.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(product -> {
                        assertThat(product.id()).isEqualTo(created.id());
                        assertThat(product.name()).isEqualTo("Gaming PC");
                        assertThat(product.price()).isEqualTo(1899.99);
                        assertThat(product.category()).isEqualTo("Electronics");
                    });
        }

        @Test
        @DisplayName("should return 404 when updating non-existent product")
        void update_notFound_returns404() {
            var dto = new CreateProductDto("Ghost", 0.01, "None");

            webTestClient.put()
                    .uri(BASE_URL + "/{id}", 99999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .value(problem -> assertThat(problem.getDetail()).contains("99999"));
        }

        @Test
        @DisplayName("should return 400 when update body is invalid")
        void update_invalidBody_returnsBadRequest() {
            var created = createProduct("Valid", 10.0, "Cat");
            var invalidDto = new CreateProductDto("", -1.0, "");

            webTestClient.put()
                    .uri(BASE_URL + "/{id}", created.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProductTests {

        @Test
        @DisplayName("should delete product and return 204")
        void delete_exists_returnsNoContent() {
            var created = createProduct("ToDelete", 9.99, "Temp");

            webTestClient.delete()
                    .uri(BASE_URL + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isNoContent();

            // Verify it's actually gone
            webTestClient.get()
                    .uri(BASE_URL + "/{id}", created.id())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent product")
        void delete_notFound_returns404() {
            webTestClient.delete()
                    .uri(BASE_URL + "/{id}", 99999)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/search")
    class SearchByCategoryTests {

        @Test
        @DisplayName("should return products matching category")
        void search_matchingCategory_returnsFiltered() {
            createProduct("Laptop", 1299.99, "Electronics");
            createProduct("Monitor", 499.99, "Electronics");
            createProduct("Keyboard", 79.99, "Accessories");

            webTestClient.get()
                    .uri(BASE_URL + "/search?category=Electronics")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(2)
                    .value(products -> {
                        assertThat(products).extracting(ProductResponse::name)
                                .containsExactlyInAnyOrder("Laptop", "Monitor");
                        assertThat(products).allMatch(p -> p.category().equals("Electronics"));
                    });
        }

        @Test
        @DisplayName("should return empty list when no products match category")
        void search_noMatch_returnsEmptyList() {
            createProduct("Laptop", 1299.99, "Electronics");

            webTestClient.get()
                    .uri(BASE_URL + "/search?category=Furniture")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("should return 400 when category param is missing")
        void search_missingParam_returnsBadRequest() {
            webTestClient.get()
                    .uri(BASE_URL + "/search")
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("Full CRUD Flow")
    class FullCrudFlowTest {

        @Test
        @DisplayName("should complete full create-read-update-delete lifecycle")
        void fullCrudLifecycle() {
            // 1. POST — create two products
            var laptop = createProduct("Laptop", 1299.99, "Electronics");
            var keyboard = createProduct("Keyboard", 79.99, "Accessories");
            assertThat(laptop.id()).isNotNull();
            assertThat(keyboard.id()).isNotNull();

            // 2. GET all — both exist
            webTestClient.get()
                    .uri(BASE_URL)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(2);

            // 3. PUT — rename laptop to Gaming PC
            webTestClient.put()
                    .uri(BASE_URL + "/{id}", laptop.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new CreateProductDto("Gaming PC", 1899.99, "Electronics"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ProductResponse.class)
                    .value(p -> assertThat(p.name()).isEqualTo("Gaming PC"));

            // 4. DELETE — remove keyboard
            webTestClient.delete()
                    .uri(BASE_URL + "/{id}", keyboard.id())
                    .exchange()
                    .expectStatus().isNoContent();

            // 5. GET all — only Gaming PC remains
            webTestClient.get()
                    .uri(BASE_URL)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(ProductResponse.class)
                    .hasSize(1)
                    .value(products -> {
                        assertThat(products.getFirst().name()).isEqualTo("Gaming PC");
                        assertThat(products.getFirst().price()).isEqualTo(1899.99);
                    });
        }
    }
}
