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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackServiceTest {

	private static final String FEILMELDING = "Sdist003 feilet under behandling av kvitteringer med exception=no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException";

	@Mock
	MethodsClient slackClient;

	SlackService slackService;

	@BeforeEach
	void setup() throws SlackApiException, IOException {
		slackService = new SlackService(dokdistdpiProperties(), slackClient);
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
	void skalSendeToSlackmeldingerHvisDetHarGaattLangNokTidMellomDem() throws SlackApiException, IOException, InterruptedException {
		slackService.sendMelding(FEILMELDING);
		Thread.sleep(1100);
		slackService.sendMelding(FEILMELDING);

		verify(slackClient, times(2)).chatPostMessage(any(ChatPostMessageRequest.class));
	}

	private DokdistdpiProperties dokdistdpiProperties() {
		DokdistdpiProperties dokdistdpiProperties = new DokdistdpiProperties();
		dokdistdpiProperties.getSlack().setToken("test-token");
		dokdistdpiProperties.getSlack().setEnabled(true);
		return dokdistdpiProperties;
	}
}