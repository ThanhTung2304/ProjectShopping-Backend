package com.example.fashionshop.repository.impl;

import com.example.fashionshop.entity.Order;
import com.example.fashionshop.repository.OrderRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Order> filterOrders(
            Order.OrderStatus status,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            where.append("AND o.status = :status ");
            params.put("status", status);
        }

        if (keyword != null && !keyword.isBlank()) {
            where.append("AND (LOWER(o.orderCode) LIKE :keyword " +
                    "OR LOWER(o.shippingName) LIKE :keyword) ");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }

        if (from != null) {
            where.append("AND o.orderedAt >= :from ");
            params.put("from", from);
        }

        if (to != null) {
            where.append("AND o.orderedAt <= :to ");
            params.put("to", to);
        }

        String jpql      = "SELECT o FROM Order o " + where + "ORDER BY o.orderedAt DESC";
        String countJpql = "SELECT COUNT(o) FROM Order o " + where;

        TypedQuery<Order> query      = em.createQuery(jpql, Order.class);
        TypedQuery<Long>  countQuery = em.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            query.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Order> results = query.getResultList();
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}
