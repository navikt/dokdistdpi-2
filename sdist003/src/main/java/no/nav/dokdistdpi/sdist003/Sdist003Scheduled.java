package no.nav.dokdistdpi.sdist003;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.lederelection.LeaderElectionConsumer;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static java.util.concurrent.TimeUnit.SECONDS;

@ConditionalOnProperty(
		value = "dokdistdpi.sdist003.autostartup",
		havingValue = "true"
)
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

	@Scheduled(initialDelay = 1, fixedDelayString = "#{@'dokdistdpi-no.nav.dokdistdpi.config.prop.DokdistdpiProperties'.getSdist003().getPolldelay().toSeconds()}", timeUnit = SECONDS)
	public Publisher<Void> sdist003Publisher() {
		return Flux.just("sdist003")
				.filterWhen(p -> leaderElectionConsumer.isLeaderAsync())
				.flatMap(s -> sdist003Service.behandleKvitteringer());
	}
}
