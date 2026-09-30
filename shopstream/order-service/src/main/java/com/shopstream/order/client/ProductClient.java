package com.shopstream.order.client;

import com.shopstream.order.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Synchronous REST call to product-service using Spring's RestClient.
 *
 * When to call synchronously vs send an event?
 * - Here we NEED the answer (current prices) before we can create the order: synchronous.
 * - Telling inventory/payment what to do next does not need an immediate answer: async events.
 *
 * Always set timeouts on remote calls. Without them a hung product-service
 * would hang order-service's request threads too.
 */
@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);

    private final RestClient restClient;

    public ProductClient(RestClient.Builder builder, @Value("${app.services.product-url}") String productServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(5_000);
        this.restClient = builder.baseUrl(productServiceUrl).requestFactory(requestFactory).build();
    }

    public List<ProductSnapshot> getProducts(Collection<Long> ids) {
        String idList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<ProductSnapshot> products = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/products/by-ids").queryParam("ids", idList).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductSnapshot>>() {
                    });
            return products == null ? List.of() : products;
        } catch (RestClientException ex) {
            log.error("Could not reach product-service", ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Product catalog is unavailable, please try again");
        }
    }
}
