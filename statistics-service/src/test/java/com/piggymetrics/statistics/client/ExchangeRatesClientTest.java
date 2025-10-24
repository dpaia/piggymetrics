package com.piggymetrics.statistics.client;

import com.piggymetrics.statistics.domain.Currency;
import com.piggymetrics.statistics.domain.ExchangeRatesContainer;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExchangeRatesClientTest {

	private final ExchangeRatesClient client = new ExchangeRatesClientFallback();

	@Test
	public void shouldRetrieveExchangeRates() {

		ExchangeRatesContainer container = client.getRates(Currency.getBase());

		assertEquals(container.getDate(), LocalDate.now());
		assertEquals(Currency.getBase(), container.getBase());
		assertNotNull(container.getRates());
		assertTrue(container.getRates().isEmpty());
	}

	@Test
	public void shouldRetrieveExchangeRatesForSpecifiedCurrency() {

		Currency requestedCurrency = Currency.EUR;
		ExchangeRatesContainer container = client.getRates(requestedCurrency);

		assertEquals(container.getDate(), LocalDate.now());
		assertEquals(Currency.getBase(), container.getBase());
		assertNotNull(container.getRates());
		assertTrue(container.getRates().isEmpty());
	}
}
