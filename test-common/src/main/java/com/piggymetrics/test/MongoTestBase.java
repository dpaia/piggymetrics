package com.piggymetrics.test;

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for MongoDB repository tests using Testcontainers.
 * 
 * This class provides common setup for MongoDB integration tests:
 * - Configures a MongoDB container using Testcontainers
 * - Sets up Spring Boot's @DataMongoTest configuration
 * - Automatically configures MongoDB connection properties
 * 
 * Usage:
 * Simply extend this class in your repository test classes:
 * 
 * <pre>
 * public class YourRepositoryTest extends MongoTestBase {
 *     
 *     @Autowired
 *     private YourRepository repository;
 *     
 *     @Test
 *     public void testSomething() {
 *         // Your test logic here
 *     }
 * }
 * </pre>
 */
@DataMongoTest
@Testcontainers
public abstract class MongoTestBase {

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer();

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
	}
}