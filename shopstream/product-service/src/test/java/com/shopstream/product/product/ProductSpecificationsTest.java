package com.shopstream.product.product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSpecificationsTest {

    @Test
    void isActiveChecksTrueActiveProperty() {
        Root<Product> root = mock(Root.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Boolean> active = mock(Path.class);
        Predicate expected = mock(Predicate.class);
        when(root.<Boolean>get("active")).thenReturn(active);
        when(criteriaBuilder.isTrue(active)).thenReturn(expected);

        Predicate result = ProductSpecifications.isActive().toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void nameOrDescriptionSearchesLowercaseNameAndDescription() {
        Root<Product> root = mock(Root.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<String> name = mock(Path.class);
        Path<String> description = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        Expression<String> lowerDescription = mock(Expression.class);
        Predicate nameMatch = mock(Predicate.class);
        Predicate descriptionMatch = mock(Predicate.class);
        Predicate expected = mock(Predicate.class);
        when(root.<String>get("name")).thenReturn(name);
        when(root.<String>get("description")).thenReturn(description);
        when(criteriaBuilder.lower(name)).thenReturn(lowerName);
        when(criteriaBuilder.lower(description)).thenReturn(lowerDescription);
        when(criteriaBuilder.like(lowerName, "%lamp%")).thenReturn(nameMatch);
        when(criteriaBuilder.like(lowerDescription, "%lamp%")).thenReturn(descriptionMatch);
        when(criteriaBuilder.or(nameMatch, descriptionMatch)).thenReturn(expected);

        Predicate result = ProductSpecifications.nameOrDescriptionContains("LaMp")
                .toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(expected);
        verify(criteriaBuilder).like(lowerName, "%lamp%");
        verify(criteriaBuilder).like(lowerDescription, "%lamp%");
        verify(criteriaBuilder).or(nameMatch, descriptionMatch);
    }

    @Test
    void hasCategoryMatchesExactCategory() {
        Root<Product> root = mock(Root.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> category = mock(Path.class);
        Predicate expected = mock(Predicate.class);
        when(root.get("category")).thenReturn(category);
        when(criteriaBuilder.equal(category, "Home & Kitchen")).thenReturn(expected);

        Predicate result = ProductSpecifications.hasCategory("Home & Kitchen")
                .toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(expected);
        verify(criteriaBuilder).equal(category, "Home & Kitchen");
    }
}
