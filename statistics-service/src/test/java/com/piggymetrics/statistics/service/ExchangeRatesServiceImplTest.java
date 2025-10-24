package com.piggymetrics.statistics.service;

import com.google.common.collect.ImmutableMap;
import com.piggymetrics.statistics.client.ExchangeRatesClient;
import com.piggymetrics.statistics.domain.Currency;
import com.piggymetrics.statistics.domain.ExchangeRatesContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ExchangeRatesServiceImplTest.TestConfig.class)
public class ExchangeRatesServiceImplTest {

	@Autowired
	private ExchangeRatesService ratesService;

	@Autowired
	private ExchangeRatesClient client;

	@Autowired
	private CacheManager cacheManager;

	private Cache exchangeRatesCache;

	@BeforeEach
	public void setup() {
		reset(client);
		exchangeRatesCache = cacheManager.getCache("exchangeRates");
		assertNotNull(exchangeRatesCache, "exchangeRates cache should be available");
		exchangeRatesCache.clear();
		assertNull(exchangeRatesCache.get("getCurrentRates"));
	}

	@Test
	public void shouldReturnCurrentRatesWhenContainerIsEmptySoFar() {

		ExchangeRatesContainer container = new ExchangeRatesContainer();
		container.setRates(ImmutableMap.of(
				Currency.EUR.name(), new BigDecimal("0.8"),
				Currency.RUB.name(), new BigDecimal("80")
		));

		when(client.getRates(Currency.getBase())).thenReturn(container);

		Map<Currency, BigDecimal> result = ratesService.getCurrentRates();
		verify(client, times(1)).getRates(Currency.getBase());

		assertEquals(container.getRates().get(Currency.EUR.name()), result.get(Currency.EUR));
		assertEquals(container.getRates().get(Currency.RUB.name()), result.get(Currency.RUB));
		assertEquals(BigDecimal.ONE, result.get(Currency.USD));
	}

	@Test
	public void shouldNotRequestRatesWhenTodaysContainerAlreadyExists() {

		Map<Currency, BigDecimal> cachedValue = ImmutableMap.of(
				Currency.EUR, new BigDecimal("0.8"),
				Currency.RUB, new BigDecimal("80"),
				Currency.USD, BigDecimal.ONE
		);

		exchangeRatesCache.put("getCurrentRates", cachedValue);
		clearInvocations(client);
		doThrow(new AssertionError("ExchangeRatesClient should not be used when cache already holds today's data"))
				.when(client).getRates(any());

		Map<Currency, BigDecimal> cachedRates = ratesService.getCurrentRates();

		verifyNoInteractions(client);
		assertEquals(cachedValue, cachedRates);
	}

	@Test
	public void shouldConvertCurrency() {

		ExchangeRatesContainer container = new ExchangeRatesContainer();
		container.setRates(ImmutableMap.of(
				Currency.EUR.name(), new BigDecimal("0.8"),
				Currency.RUB.name(), new BigDecimal("80")
		));

		when(client.getRates(Currency.getBase())).thenReturn(container);

		final BigDecimal amount = new BigDecimal(100);
		final BigDecimal expectedConvertionResult = new BigDecimal("1.25");

		BigDecimal result = ratesService.convert(Currency.RUB, Currency.USD, amount);

		assertTrue(expectedConvertionResult.compareTo(result) == 0);
	}

	@Test
	public void shouldFailToConvertWhenAmountIsNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			ratesService.convert(Currency.EUR, Currency.RUB, null);
		});
	}

	@Configuration
	@EnableCaching
	static class TestConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("exchangeRates");
		}

		@Bean
		ExchangeRatesClient exchangeRatesClient() {
			return mock(ExchangeRatesClient.class);
		}

		@Bean
		ExchangeRatesService exchangeRatesService(ExchangeRatesClient client) {
			return new ExchangeRatesServiceImpl();
		}
	}
}
