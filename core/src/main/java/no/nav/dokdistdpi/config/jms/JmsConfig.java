package no.nav.dokdistdpi.config.jms;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_CHARACTER_SET;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_ENCODING;
import static com.ibm.msg.client.jms.JmsConstants.USERID;
import static com.ibm.msg.client.wmq.common.CommonConstants.WMQ_CM_CLIENT;

@Configuration
@Profile({"nais", "local"})
public class JmsConfig {
	private static final int UTF_8_WITH_PUA = 1208;

	@Bean
	public Queue qdist014(@Value("${dokdistdpi_qdist014_kvittering_fra_dpi.queuename}") String qdist014QueueName) throws JMSException {
		return new MQQueue(qdist014QueueName);
	}

	@Bean
	public Queue qdist014FunksjonellFeil(@Value("${dokdistdpi_qdist014_funk_feil.queuename}") String qdist014FunksjonellFeil) throws JMSException {
		return new MQQueue(qdist014FunksjonellFeil);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) throws JMSException {
		return new MQQueue(qdist009QueueName);
	}

	@Bean
	public Queue qdist011(@Value("${dokdistdpi_qdist011_dist_til_dpi.queuename}") String qdist011QueueName) throws JMSException {
		return new MQQueue(qdist011QueueName);
	}

	@Bean
	public Queue qdist011FunksjonellFeil(@Value("${dokdistdpi_qdist011_funk_feil.queuename}") String qdist011FunksjonellFeil) throws JMSException {
		return new MQQueue(qdist011FunksjonellFeil);
	}

	@Bean
	public ConnectionFactory connectionFactory(final MqGatewayProperties mqGatewayProperties,
											   final ServiceuserProperties serviceuserProperties) throws JMSException {
		return createConnectionFactory(mqGatewayProperties, serviceuserProperties);
	}

	private PooledConnectionFactory createConnectionFactory(final MqGatewayProperties mqGatewayProperties,
															final ServiceuserProperties serviceuserProperties) throws JMSException {
		MQConnectionFactory mqConnectionFactory = new MQConnectionFactory();
		mqConnectionFactory.setHostName(mqGatewayProperties.getHostname());
		mqConnectionFactory.setPort(mqGatewayProperties.getPort());
		mqConnectionFactory.setChannel(mqGatewayProperties.getChannelname());
		mqConnectionFactory.setQueueManager(mqGatewayProperties.getName());
		mqConnectionFactory.setTransportType(WMQ_CM_CLIENT);
		mqConnectionFactory.setCCSID(UTF_8_WITH_PUA);
		mqConnectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
		mqConnectionFactory.setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);
		mqConnectionFactory.setStringProperty(USERID, serviceuserProperties.getUsername());
		UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(mqConnectionFactory);

		PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
		pooledFactory.setConnectionFactory(adapter);
		pooledFactory.setMaxConnections(10);
		pooledFactory.setMaximumActiveSessionPerConnection(10);

		adapter.setUsername(serviceuserProperties.getUsername());
		adapter.setPassword(serviceuserProperties.getPassword());

		return pooledFactory;
	}
}
