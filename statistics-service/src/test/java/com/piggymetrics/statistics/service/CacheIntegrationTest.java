package com.piggymetrics.statistics.service;

import com.piggymetrics.statistics.client.ExchangeRatesClient;
import com.piggymetrics.statistics.domain.Account;
import com.piggymetrics.statistics.domain.Currency;
import com.piggymetrics.statistics.domain.ExchangeRatesContainer;
import com.piggymetrics.statistics.domain.Saving;
import com.piggymetrics.statistics.domain.timeseries.DataPoint;
import com.piggymetrics.statistics.domain.timeseries.DataPointId;
import com.piggymetrics.statistics.repository.DataPointRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.Rule;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
                "logging.level.org.springframework.cache.interceptor=TRACE"
        }
)
public class CacheIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ExchangeRatesService exchangeRatesService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private DataPointRepository repository;

    @MockBean
    private ExchangeRatesClient exchangeRatesClient;
    
    @Rule
    public OutputCaptureRule output = new OutputCaptureRule();

    private List<DataPoint> testDataPoints;
    private Account testAccount;

    @Before
    public void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName ->
                cacheManager.getCache(cacheName).clear());

        // Setup test data
        testDataPoints = new ArrayList<>();
        DataPoint dataPoint = new DataPoint();
        dataPoint.setId(new DataPointId("testuser", new Date()));
        testDataPoints.add(dataPoint);

        // Setup test account
        testAccount = new Account();
        testAccount.setIncomes(new ArrayList<>());
        testAccount.setExpenses(new ArrayList<>());

        Saving saving = new Saving();
        saving.setAmount(new BigDecimal(1000));
        saving.setCurrency(Currency.USD);
        testAccount.setSaving(saving);

        // Mock exchange rates client
        ExchangeRatesContainer mockContainer = new ExchangeRatesContainer();
        mockContainer.setBase(Currency.USD);
        mockContainer.setDate(java.time.LocalDate.now());
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        rates.put("RUB", new BigDecimal("75.0"));
        mockContainer.setRates(rates);
        when(exchangeRatesClient.getRates(Currency.USD)).thenReturn(mockContainer);
    }

    @Test
    public void testStatisticsCacheHitOnSecondCall() {
        String accountName = "testuser";
        when(repository.findByIdAccount(accountName)).thenReturn(testDataPoints);

        List<DataPoint> result1 = statisticsService.findByAccountName(accountName);

        List<DataPoint> result2 = statisticsService.findByAccountName(accountName);

        assertNotNull("First result should not be null", result1);
        assertNotNull("Second result should not be null", result2);
        assertEquals("Both results should have same size", result1.size(), result2.size());

        verify(repository, times(1)).findByIdAccount(accountName);
    }

    @Test
    public void testCacheEvictionOnSave() {
        String accountName = "testuser";
        when(repository.findByIdAccount(accountName)).thenReturn(testDataPoints);
        when(repository.save(any(DataPoint.class))).thenReturn(new DataPoint());

        List<DataPoint> result1 = statisticsService.findByAccountName(accountName);
        assertNotNull("Initial result should not be null", result1);

        verify(repository, times(1)).findByIdAccount(accountName);

        statisticsService.save(accountName, testAccount);

        List<DataPoint> result2 = statisticsService.findByAccountName(accountName);

        assertNotNull("Result after cache eviction should not be null", result2);

        verify(repository, times(2)).findByIdAccount(accountName);
        verify(repository, times(1)).save(any(DataPoint.class));
    }

    @Test
    public void testCacheManagerIsConfigured() {
        assertNotNull("CacheManager should be configured", cacheManager);
        assertTrue("StatisticsData cache should be available",
                cacheManager.getCacheNames().contains("statisticsData"));
        assertTrue("ExchangeRates cache should be available",
                cacheManager.getCacheNames().contains("exchangeRates"));
    }

    @Test
    public void testCacheWithEmptyResult() {
        String accountName = "emptyaccount";
        when(repository.findByIdAccount(accountName)).thenReturn(Collections.emptyList());

        List<DataPoint> result1 = statisticsService.findByAccountName(accountName);
        List<DataPoint> result2 = statisticsService.findByAccountName(accountName);

        assertNotNull("Result should not be null", result1);
        assertTrue("Result should be empty", result1.isEmpty());
        assertTrue("Cached result should be empty", result2.isEmpty());

        verify(repository, times(1)).findByIdAccount(accountName);
    }

    @Test
    public void testMultipleAccountStatisticsCached() {
        String accountName1 = "testuser1";
        String accountName2 = "testuser2";

        List<DataPoint> dataPoints1 = List.of(new DataPoint());
        List<DataPoint> dataPoints2 = Arrays.asList(new DataPoint(), new DataPoint());

        when(repository.findByIdAccount(accountName1)).thenReturn(dataPoints1);
        when(repository.findByIdAccount(accountName2)).thenReturn(dataPoints2);

        List<DataPoint> result1a = statisticsService.findByAccountName(accountName1);
        List<DataPoint> result2a = statisticsService.findByAccountName(accountName2);
        List<DataPoint> result1b = statisticsService.findByAccountName(accountName1);
        List<DataPoint> result2b = statisticsService.findByAccountName(accountName2);

        assertNotNull("Account1 first call result should not be null", result1a);
        assertNotNull("Account2 first call result should not be null", result2a);
        assertNotNull("Account1 second call result should not be null", result1b);
        assertNotNull("Account2 second call result should not be null", result2b);

        assertEquals("Account1 results should match size", result1a.size(), result1b.size());
        assertEquals("Account2 results should match size", result2a.size(), result2b.size());

        verify(repository, times(1)).findByIdAccount(accountName1);
        verify(repository, times(1)).findByIdAccount(accountName2);
    }

    @Test
    public void testExchangeRatesCacheHitOnSecondCall() {
        Map<Currency, BigDecimal> result1 = exchangeRatesService.getCurrentRates();

        Map<Currency, BigDecimal> result2 = exchangeRatesService.getCurrentRates();

        assertNotNull("First result should not be null", result1);
        assertNotNull("Second result should not be null", result2);
        assertEquals("Both results should be equal", result1, result2);

        verify(exchangeRatesClient, times(1)).getRates(Currency.USD);
    }

    @Test
    public void testExchangeRatesCacheReturnsCorrectValues() {
        Map<Currency, BigDecimal> rates = exchangeRatesService.getCurrentRates();

        assertNotNull("Exchange rates should not be null", rates);
        assertTrue("Should contain EUR rate", rates.containsKey(Currency.EUR));
        assertTrue("Should contain RUB rate", rates.containsKey(Currency.RUB));
        assertTrue("Should contain USD rate", rates.containsKey(Currency.USD));
        assertEquals("USD rate should be 1.0", BigDecimal.ONE, rates.get(Currency.USD));
        assertEquals("EUR rate should match mock", new BigDecimal("0.85"), rates.get(Currency.EUR));
        assertEquals("RUB rate should match mock", new BigDecimal("75.0"), rates.get(Currency.RUB));
    }

    @Test
    public void testCacheHitMissStatistics() {
        String accountName = "stats-test-user";
        when(repository.findByIdAccount(accountName)).thenReturn(testDataPoints);

        cacheManager.getCache("statisticsData").clear();
        
        List<DataPoint> result1 = statisticsService.findByAccountName(accountName);
        assertNotNull("First result should not be null", result1);
        
        List<DataPoint> result2 = statisticsService.findByAccountName(accountName);
        assertNotNull("Second result should not be null", result2);
        
        verify(repository, times(1)).findByIdAccount(accountName);
        
        assertEquals("Both results should have same content", result1.size(), result2.size());
        
        org.springframework.cache.Cache cache = cacheManager.getCache("statisticsData");
        assertNotNull("Cache should exist", cache);
        
        org.springframework.cache.Cache.ValueWrapper cachedValue = cache.get(accountName);
        assertNotNull("Value should be cached", cachedValue);
        
        System.out.println("Cache hit/miss test completed - verified through repository call count");
    }

    @Test
    public void testCacheEvictionStatistics() {
        String accountName = "eviction-test-user";
        when(repository.findByIdAccount(accountName)).thenReturn(testDataPoints);
        when(repository.save(any(DataPoint.class))).thenReturn(new DataPoint());

        cacheManager.getCache("statisticsData").clear();
        
        statisticsService.findByAccountName(accountName);
        
        org.springframework.cache.Cache cache = cacheManager.getCache("statisticsData");
        org.springframework.cache.Cache.ValueWrapper cachedValueBefore = cache.get(accountName);
        assertNotNull("Value should be cached before eviction", cachedValueBefore);
        
        statisticsService.save(accountName, testAccount);
        
        org.springframework.cache.Cache.ValueWrapper cachedValueAfter = cache.get(accountName);
        assertNull("Value should be evicted from cache", cachedValueAfter);
        
        System.out.println("Cache eviction test completed - verified cache value was removed");
    }

    @Test
    public void testExchangeRatesCacheStatistics() {
        cacheManager.getCache("exchangeRates").clear();
        
        Map<Currency, BigDecimal> rates1 = exchangeRatesService.getCurrentRates();  // Cache miss
        Map<Currency, BigDecimal> rates2 = exchangeRatesService.getCurrentRates();  // Cache hit
        Map<Currency, BigDecimal> rates3 = exchangeRatesService.getCurrentRates();  // Cache hit
        
        assertNotNull("First call should return rates", rates1);
        assertNotNull("Second call should return rates", rates2);
        assertNotNull("Third call should return rates", rates3);
        
        verify(exchangeRatesClient, times(1)).getRates(Currency.USD);
        
        org.springframework.cache.Cache cache = cacheManager.getCache("exchangeRates");
        org.springframework.cache.Cache.ValueWrapper cachedValue = cache.get("getCurrentRates");
        assertNotNull("Exchange rates should be cached", cachedValue);
        
        System.out.println("Exchange rates cache test completed - verified through client call count");
    }

    @Test
    public void logsHitAndMiss() {
        String accountName = "A";

        // Ensure clean cache to force a miss on first call
        Objects.requireNonNull(cacheManager.getCache("statisticsData")).clear();

        // Mock repository to return some data for the account
        when(repository.findByIdAccount(accountName)).thenReturn(Arrays.asList(new DataPoint()));

        // First call should be a cache MISS, second a HIT
        statisticsService.findByAccountName(accountName); // MISS
        statisticsService.findByAccountName(accountName); // HIT

        String logs = output.toString();

        assertTrue("Expected cache miss to be logged",
                logs.contains("No cache entry for key 'A' in cache(s) [statisticsData]"));
        assertTrue("Expected cache hit to be logged",
                logs.contains("Cache entry for key 'A' found in cache 'statisticsData'"));

        // Confirm cache actually hit by repository being called only once
        verify(repository, times(1)).findByIdAccount(accountName);
    }
}