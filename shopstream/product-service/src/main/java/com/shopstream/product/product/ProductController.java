package com.shopstream.product.product;

import com.shopstream.product.web.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST design used across ShopStream:
 *   GET    /api/products          list (with ?search=&category=&page=&size=)
 *   GET    /api/products/{id}     one item
 *   POST   /api/products          create  -> 201 Created
 *   PUT    /api/products/{id}     replace -> 200 OK
 *   DELETE /api/products/{id}     remove  -> 204 No Content
 * Nouns in the URL, the HTTP method says what happens.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return productService.search(search, category, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    @GetMapping("/by-ids")
    public List<ProductResponse> byIds(@RequestParam List<Long> ids) {
        return productService.getByIds(ids);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestHeader(value = "X-User-Role", required = false) String role,
                                  @Valid @RequestBody ProductRequest request) {
        Roles.requireAdmin(role);
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@RequestHeader(value = "X-User-Role", required = false) String role,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request) {
        Roles.requireAdmin(role);
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "X-User-Role", required = false) String role,
                       @PathVariable Long id) {
        Roles.requireAdmin(role);
        productService.deactivate(id);
    }
}
