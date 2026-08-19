package com.caioms.java_marketplace.modules.catalog.application.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class Seller {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NonNull @Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@NonNull @Column(name = "store_name", nullable = false)
	private String storeName;
}
