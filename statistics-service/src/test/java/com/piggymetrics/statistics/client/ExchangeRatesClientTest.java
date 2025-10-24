package com.piggymetrics.statistics.client;

import com.piggymetrics.statistics.domain.Currency;
import com.piggymetrics.statistics.domain.ExchangeRatesContainer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExchangeRatesClientTest {

	private final ExchangeRatesClient client = new StubExchangeRatesClient();

	@Test
	public void shouldRetrieveExchangeRates() {

		ExchangeRatesContainer container = client.getRates(Currency.getBase());

		assertEquals(LocalDate.now(), container.getDate());
		assertEquals(Currency.getBase(), container.getBase());

		assertNotNull(container.getRates());
		assertNotNull(container.getRates().get(Currency.USD.name()));
		assertNotNull(container.getRates().get(Currency.EUR.name()));
		assertNotNull(container.getRates().get(Currency.RUB.name()));
	}

	@Test
	public void shouldRetrieveExchangeRatesForSpecifiedCurrency() {

		Currency requestedCurrency = Currency.EUR;
		ExchangeRatesContainer container = client.getRates(Currency.getBase());

		assertEquals(LocalDate.now(), container.getDate());
		assertEquals(Currency.getBase(), container.getBase());

		assertNotNull(container.getRates());
		assertNotNull(container.getRates().get(requestedCurrency.name()));
	}

	private static class StubExchangeRatesClient implements ExchangeRatesClient {

		@Override
		public ExchangeRatesContainer getRates(Currency base) {
			ExchangeRatesContainer container = new ExchangeRatesContainer();
			container.setDate(LocalDate.now());
			container.setBase(Currency.getBase());

			Map<String, BigDecimal> rates = new HashMap<>();
			rates.put(Currency.USD.name(), BigDecimal.ONE);
			rates.put(Currency.EUR.name(), new BigDecimal("0.9"));
			rates.put(Currency.RUB.name(), new BigDecimal("90.0"));
			container.setRates(rates);

			return container;
		}
	}
}
