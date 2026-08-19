package com.caioms.java_marketplace.core.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Money {

	@NonNull @Column(precision = 19, scale = 2)
	private BigDecimal amount;

	@NonNull @Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Currency currency;

	public static Money create(String value, Currency currency) {
		return new Money(new BigDecimal(value), currency);
	}
}
