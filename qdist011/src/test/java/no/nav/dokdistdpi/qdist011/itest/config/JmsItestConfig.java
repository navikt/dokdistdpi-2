package no.nav.dokdistdpi.qdist011.itest.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

@Configuration
@Profile("itest")
public class JmsItestConfig {

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
	public Queue qdist010UtenforKjernetid(@Value("${dokdistdpi_qdist010_dist_til_dpi_kbq.queuename}") String qdist010UtenforKjernetid) {
		return new ActiveMQQueue(qdist010UtenforKjernetid);
	}

	@Bean
	public Queue backoutQueue() {
		return new ActiveMQQueue("ActiveMQ.DLQ");
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public BrokerService broker() {
		BrokerService service = new BrokerService();
		service.setPersistent(false);
		return service;
	}

	@Bean
	public ConnectionFactory activemqConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");
		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(0);
		activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
		return activeMQConnectionFactory;
	}
}
