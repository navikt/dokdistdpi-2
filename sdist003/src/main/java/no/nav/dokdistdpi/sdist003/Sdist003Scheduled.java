package no.nav.dokdistdpi.sdist003;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.lederelection.LeaderElectionConsumer;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static java.util.concurrent.TimeUnit.MINUTES;

@ConditionalOnProperty(
		value = "dokdistdpi.sdist003.autostartup",
		havingValue = "true"
)
@ConditionalOnBean(DokdistdpiProperties.class)
@Slf4j
@Component
public class Sdist003Scheduled {

	private final LeaderElectionConsumer leaderElectionConsumer;
	private final Sdist003Service sdist003Service;

	public Sdist003Scheduled(LeaderElectionConsumer leaderElectionConsumer,
							 Sdist003Service sdist003Service) {
		this.leaderElectionConsumer = leaderElectionConsumer;
		this.sdist003Service = sdist003Service;
	}

	@Scheduled(fixedDelayString = "#{@dokdistdpi-no.nav.dokdistdpi.config.prop.DokdistdpiProperties.getSdist003().getPolldelay().toMinutes()}", timeUnit = MINUTES)
	public Publisher<Void> sdist003Publisher() {
		if (leaderElectionConsumer.isLeader()) {
			log.info("Sdist003 starter å hente kvitteringer");
			return sdist003Service.behandleKvitteringer();
		}
		return Mono.empty();
	}
}
