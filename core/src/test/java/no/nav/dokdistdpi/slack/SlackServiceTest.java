package no.nav.dokdistdpi.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackServiceTest {

	private static final String FEILMELDING = "Sdist003 feilet under behandling av kvitteringer med exception=no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException";

	public static final ZoneId EUROPE_OSLO = ZoneId.of("Europe/Oslo");
	private static final Instant TIDSPUNKT_FOR_SLACKVARSEL = LocalDateTime.of(2025, 1, 1, 13, 37, 15)
			.atZone(EUROPE_OSLO)
			.toInstant();

	@Mock
	Clock clock;

	@Mock
	MethodsClient slackClient;

	SlackService slackService;

	@BeforeEach
	void setup() throws SlackApiException, IOException {
		slackService = new SlackService(dokdistdpiProperties(), slackClient, clock);
		when(clock.instant()).thenReturn(TIDSPUNKT_FOR_SLACKVARSEL);
		when(slackClient.chatPostMessage(any(ChatPostMessageRequest.class))).thenReturn(new ChatPostMessageResponse());
	}

	@Test
	void skalKunSendeÉnSlackmeldingHvisDetErKortTidMellomDem() throws SlackApiException, IOException {
		slackService.sendMelding(FEILMELDING);
		slackService.sendMelding(FEILMELDING);
		slackService.sendMelding(FEILMELDING);

		verify(slackClient, times(1)).chatPostMessage(any(ChatPostMessageRequest.class));
	}

	@Test
	void skalSendeToSlackmeldingerHvisDetHarGaattLangNokTidMellomDem() throws SlackApiException, IOException {
		slackService.sendMelding(FEILMELDING);

		var toSekunderSenere = TIDSPUNKT_FOR_SLACKVARSEL.plusMillis(2000);
		when(clock.instant()).thenReturn(toSekunderSenere);
		slackService.sendMelding(FEILMELDING);

		verify(slackClient, times(2)).chatPostMessage(any(ChatPostMessageRequest.class));
	}

	private DokdistdpiProperties dokdistdpiProperties() {
		DokdistdpiProperties dokdistdpiProperties = new DokdistdpiProperties();
		dokdistdpiProperties.getSlack().setToken("test-token");
		dokdistdpiProperties.getSlack().setEnabled(true);
		dokdistdpiProperties.getSlack().setMinimumAntallSekunderMellomSlackvarsel(1);
		return dokdistdpiProperties;
	}

}