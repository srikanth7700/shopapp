package com.shopstream.product.product;

import com.shopstream.common.events.ProductCreatedEvent;
import com.shopstream.product.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private final ProductRequest request = new ProductRequest("SS-TEST-001", "Test Product", "desc", "Books",
            new BigDecimal("19.99"), null, 25);

    @Test
    void createSavesProductAndRaisesProductCreatedEvent() {
        when(productRepository.existsBySku("SS-TEST-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 99L);
            return product;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(99L);
        ArgumentCaptor<ProductCreatedEvent> event = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().productId()).isEqualTo(99L);
        assertThat(event.getValue().initialStock()).isEqualTo(25);
    }

    @Test
    void createRejectsDuplicateSku() {
        when(productRepository.existsBySku("SS-TEST-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(ApiException.class);
        verify(productRepository, never()).save(any());
    }
}
