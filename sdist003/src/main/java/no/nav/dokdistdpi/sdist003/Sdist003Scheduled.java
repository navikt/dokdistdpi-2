package no.nav.dokdistdpi.sdist003;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.lederelection.LeaderElectionConsumer;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;

@ConditionalOnProperty(
		value = "dokdistdpi.sdist003.autostartup",
		havingValue = "true"
)
@Slf4j
@Component
public class Sdist003Scheduled {

	static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	private final LeaderElectionConsumer leaderElectionConsumer;
	private final Sdist003Service sdist003Service;

	public Sdist003Scheduled(LeaderElectionConsumer leaderElectionConsumer,
							 Sdist003Service sdist003Service) {
		this.leaderElectionConsumer = leaderElectionConsumer;
		this.sdist003Service = sdist003Service;
	}

	@Scheduled(initialDelay = 1, timeUnit = SECONDS, fixedDelayString = "#{@'dokdistdpi-no.nav.dokdistdpi.config.prop.DokdistdpiProperties'.getSdist003().getPolldelay().toSeconds()}")
	public Publisher<String> sdist003Publisher() {
		return leaderElectionConsumer.isLeaderAsync()
				.filter(aBoolean -> aBoolean)
				.flatMapMany(aBoolean -> sdist003Service.behandleKvitteringer())
				.contextWrite(ctx -> ctx.put(CALL_ID, "sdist003-poll-" + LocalDateTime.now().format(DATE_TIME_FORMATTER)));
	}
}
