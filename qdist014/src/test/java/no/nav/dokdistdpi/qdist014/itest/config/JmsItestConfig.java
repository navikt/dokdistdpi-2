package no.nav.dokdistdpi.qdist014.itest.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.jms.Queue;

@Configuration
@Profile("itest")
public class JmsItestConfig {

	public static final String QDIST014_BQ = "qdist014Bq";

	@Bean
	public Queue qdist014(@Value("${dokdistdpi_qdist014_kvittering_fra_dpi.queuename}") String qdist014QueueName) {
		return new ActiveMQQueue(qdist014QueueName);
	}

	@Bean
	public Queue qdist014FunksjonellFeil(@Value("${dokdistdpi_qdist014_funk_feil.queuename}") String qdist014FunksjonellFeil) {
		return new ActiveMQQueue(qdist014FunksjonellFeil);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) {
		return new ActiveMQQueue(qdist009QueueName);
	}

	@Bean
	public Queue qdist011(@Value("${dokdistdpi_qdist011_dist_til_dpi.queuename}") String qdist011QueueName) {
		return new ActiveMQQueue(qdist011QueueName);
	}

	@Bean
	public Queue qdist011FunksjonellFeil(@Value("${dokdistdpi_qdist011_funk_feil.queuename}") String qdist011FunksjonellFeil) {
		return new ActiveMQQueue(qdist011FunksjonellFeil);
	}

	@Bean
	public Queue backoutQueue() {
		return new ActiveMQQueue(QDIST014_BQ);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public EmbeddedActiveMQ activeMQServer() {
		EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
		embeddedActiveMQ.setConfigResourcePath("artemis-server.xml");
		return embeddedActiveMQ;
	}

	@Bean
	public ConnectionFactory activemqConnectionFactory(EmbeddedActiveMQ embeddedActiveMQ) {
		org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://0");
		JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
		pooledFactory.setConnectionFactory(activeMQConnectionFactory);
		pooledFactory.setMaxConnections(1);
		return pooledFactory;
	}
}
