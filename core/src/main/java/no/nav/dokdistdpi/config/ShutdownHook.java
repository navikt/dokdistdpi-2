package no.nav.dokdistdpi.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.jms.ConnectionFactory;

/**
 * Rydder opp ressurser som Spring ikke gjør selv.
 */
@Slf4j
@Component
public class ShutdownHook {

	private final ConnectionFactory connectionFactory;

	public ShutdownHook(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@PreDestroy
	public void destroy() {
		log.info("Graceful shutdown - Lukker koblinger til ConnectionFactory pool");
		((PooledConnectionFactory) connectionFactory).clear();
	}
}
