package com.piggymetrics.test;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;

/**
 * Abstract base class for MongoDB repository tests that provides an in-memory MongoDB
 * instance backed by {@link MongoServer}.
 *
 * The server is bootstrapped once per test JVM and exposed to Spring through
 * {@link DynamicPropertySource}, so extending tests only need to focus on assertions.
 */
@DataMongoTest
public abstract class MongoTestBase {

	private static final MongoServer mongoServer = new MongoServer(new MemoryBackend());
	private static final InetSocketAddress serverAddress = mongoServer.bind();
	private static final String CONNECTION_STRING = String.format("mongodb://%s:%d/test",
			serverAddress.getHostString(), serverAddress.getPort());

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> CONNECTION_STRING);
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(mongoServer::shutdownNow));
	}
}
