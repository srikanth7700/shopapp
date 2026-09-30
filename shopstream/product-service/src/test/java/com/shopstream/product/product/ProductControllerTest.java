package com.shopstream.product.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest starts only the web layer (controllers, JSON, validation, exception handlers).
 * The service is replaced by a mock, so no database or Kafka is needed.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final String VALID_BODY = """
            {"sku":"SS-TEST-001","name":"Test","description":"d","category":"Books","price":19.99,"initialStock":5}
            """;

    @Test
    void listReturnsPagedProducts() throws Exception {
        ProductResponse product = new ProductResponse(1L, "SS-ELEC-001", "Headphones", "d", "Electronics",
                new BigDecimal("199.99"), null, true);
        when(productService.search(null, null, 0, 12)).thenReturn(new PageResponse<>(List.of(product), 0, 12, 1, 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Headphones"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(productService, never()).create(any());
    }

    @Test
    void createValidatesBody() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"bad sku\",\"name\":\"\",\"category\":\"Books\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void adminCanCreate() throws Exception {
        when(productService.create(any())).thenReturn(new ProductResponse(13L, "SS-TEST-001", "Test", "d", "Books",
                new BigDecimal("19.99"), null, true));

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(13));
    }
}
