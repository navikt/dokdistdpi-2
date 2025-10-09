package no.nav.dokdistdpi.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties.SlackProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@Slf4j
@Service
public class  SlackService {

	private final MethodsClient methodsClient;
	private final SlackProperties slackProperties;
	private final Clock clock;

	// Anti spam-funksjonalitet som fungerer så lenge det kun er én periodisk jobb som sender Slack-meldinger ved feil
	private Instant forrigeSlackvarsel = null;
	private Integer antallSendingsforsoek = 0;
	private final int minimumAntallSekunderMellomSlackvarsel;

	SlackService(DokdistdpiProperties dokdistdpiProperties, MethodsClient slackClient, Clock clock) {
		this.slackProperties = dokdistdpiProperties.getSlack();
		this.methodsClient = slackClient;
		this.clock = clock;
		this.minimumAntallSekunderMellomSlackvarsel = slackProperties.getMinimumAntallSekunderMellomSlackvarsel();
	}

	public void sendMelding(String melding) {
		if (slackProperties.isEnabled()) {
			try {
				antallSendingsforsoek++;

				if (mindreEnnEnTimeSidenForrigeSlackvarsel()) {
					var foersteMuligeSendingstidspunkt = forrigeSlackvarsel.plusSeconds(minimumAntallSekunderMellomSlackvarsel);
					log.warn("For kort tid siden forrige Slack-melding={}. Det er forsøkt sendt {} Slack-melding(er) siden forrige melding={}. " +
							 "Ny melding kan tidligst bli sendt={}.",
							melding, antallSendingsforsoek, formaterInstant(forrigeSlackvarsel), formaterInstant(foersteMuligeSendingstidspunkt));
				} else {
					log.info("Sender varsel til Slack med melding={}", melding);

					var response = methodsClient.chatPostMessage(jobbFeiletMelding(melding, antallSendingsforsoek));
					var result = response.isOk() ? "OK" : response.getError();
					log.info("Sendte melding med ts={} til Slack med resultat={}", response.getTs(), result);

					settSendingstidspunktOgNullstillAntallSendingsforsoek();
				}
			} catch (Exception e) {
				log.error("Sending av melding til Slack feilet med feilmelding={}", e.getMessage(), e);
			}
		}
	}

	private ChatPostMessageRequest jobbFeiletMelding(String feilmelding, int antallGangerFeilet) {
		String headerText = ":rotating_light: Skedulert jobb har feilet %s gang(er) siste timen!".formatted(antallGangerFeilet);
		String bodyText = """
                 *Applikasjon:* dokdistdpi-2
                 *Feilmelding:* %s
                 """.formatted(feilmelding).stripIndent();

		return ChatPostMessageRequest.builder()
				.channel(slackProperties.getChannel())
				.text(bodyText) //fallback tekst
				.blocks(Arrays.asList(
						HeaderBlock.builder()
								.text(PlainTextObject.builder().text(headerText).build())
								.build(),
						SectionBlock.builder()
								.text(MarkdownTextObject.builder().text(bodyText).build())
								.build()
				))
				.build();
	}

	private void settSendingstidspunktOgNullstillAntallSendingsforsoek() {
		forrigeSlackvarsel = Instant.now(clock);
		antallSendingsforsoek = 0;
	}

	private boolean mindreEnnEnTimeSidenForrigeSlackvarsel() {
		var naatid = Instant.now(clock);
		return forrigeSlackvarsel != null && SECONDS.between(forrigeSlackvarsel, naatid) <= minimumAntallSekunderMellomSlackvarsel;
	}

	private String formaterInstant(Instant instant) {
		return instant.atZone(DEFAULT_ZONE_ID).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}
}