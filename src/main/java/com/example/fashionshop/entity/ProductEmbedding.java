package com.example.fashionshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_embeddings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Lob
    @Column(name = "embedding_vector", columnDefinition = "TEXT")
    private String embeddingVector;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;
}
