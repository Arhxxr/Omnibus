package com.Omnibus.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MoneyTest {

    @Test
    void shouldCreateMoneyWithCorrectScale() {
        Money money = Money.of("100.50", "USD");
        assertEquals(new BigDecimal("100.5000"), money.getAmount());
        assertEquals("USD", money.getCurrency());
    }

    @Test
    void shouldAddMoneySameCurrency() {
        Money a = Money.of("100.0000", "USD");
        Money b = Money.of("50.5000", "USD");
        Money result = a.add(b);
        assertEquals(Money.of("150.5000", "USD"), result);
    }

    @Test
    void shouldSubtractMoney() {
        Money a = Money.of("100.0000", "USD");
        Money b = Money.of("30.2500", "USD");
        Money result = a.subtract(b);
        assertEquals(Money.of("69.7500", "USD"), result);
    }

    @Test
    void shouldThrowOnCurrencyMismatchAdd() {
        Money usd = Money.of("100", "USD");
        Money eur = Money.of("50", "EUR");
        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
    }

    @Test
    void shouldThrowOnCurrencyMismatchSubtract() {
        Money usd = Money.of("100", "USD");
        Money eur = Money.of("50", "EUR");
        assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
    }

    @Test
    void shouldDetectPositiveNegativeZero() {
        assertTrue(Money.of("100", "USD").isPositive());
        assertFalse(Money.of("100", "USD").isNegative());
        assertFalse(Money.of("100", "USD").isZero());

        assertTrue(Money.of("-50", "USD").isNegative());
        assertTrue(Money.zero("USD").isZero());
    }

    @Test
    void shouldCompareCorrectly() {
        Money a = Money.of("100", "USD");
        Money b = Money.of("50", "USD");
        assertTrue(a.isGreaterThanOrEqual(b));
        assertTrue(a.isGreaterThanOrEqual(a));
        assertFalse(b.isGreaterThanOrEqual(a));
        assertTrue(b.isLessThan(a));
    }

    @Test
    void shouldNegate() {
        Money positive = Money.of("100", "USD");
        Money negated = positive.negate();
        assertEquals(Money.of("-100", "USD"), negated);
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("100", "US"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("100", "USDD"));
    }

    @Test
    void shouldRejectNullInputs() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null, "USD"));
        assertThrows(NullPointerException.class, () -> Money.of("100", null));
    }

    @Test
    void equalityShouldIgnoreTrailingZeros() {
        assertEquals(Money.of("100", "USD"), Money.of("100.0000", "USD"));
    }
}
