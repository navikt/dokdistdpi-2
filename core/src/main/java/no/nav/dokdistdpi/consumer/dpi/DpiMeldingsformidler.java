package no.nav.dokdistdpi.consumer.dpi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.ForsendelseStatusResponse;
import no.nav.dokdistdpi.consumer.dpi.client.OppdaterForsendelseAndVarselRequest;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Dokumentpakkefingeravtrykk;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DigitalPostContentPackager;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.StandardBusinessDocumentMapper;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import org.apache.camel.Handler;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;

import javax.xml.crypto.dsig.DigestMethod;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.client.StatusType.OPPRETTET;
import static no.nav.dokdistdpi.consumer.dpi.client.StatusType.SENDT;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Slf4j
@Component
public class DpiMeldingsformidler {

	private final StandardBusinessDocumentMapper sbdMapper;
	private final DigitalPostContentPackager digitalPostContentPackager;
	private final AppCertificate appCertificate;
	private final ObjectMapper objectMapper;
	private final DpiClient dpiClient;

	@Autowired
	public DpiMeldingsformidler(@Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper,
								StandardBusinessDocumentMapper sbdMapper,
								DigitalPostContentPackager digitalPostContentPackager,
								AppCertificate appCertificate, DpiClient dpiClient) {
		this.objectMapper = dpiObjectMapper;
		this.sbdMapper = sbdMapper;
		this.digitalPostContentPackager = digitalPostContentPackager;
		this.appCertificate = appCertificate;
		this.dpiClient = dpiClient;
	}

	@Handler
	public OppdaterForsendelseAndVarselRequest sendMelding(Forsendelse forsendelse) {
		byte[] dokumentpakke = getKryptertDokumentpakke(forsendelse);

		StandardBusinessDocument standardBusinessDocument = sbdMapper.mapDigitalPostEnvelope(forsendelse,
				getDokumentpakkefingeravtrykk(dokumentpakke));

		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("forretningsmelding", generateStandardBusinessDocumentJWT(standardBusinessDocument));
		multipartBodyBuilder.part("dokumentpakke", dokumentpakke, APPLICATION_OCTET_STREAM);

		List<ForsendelseStatusResponse> forsendelseStatusResponses = dpiClient.sendDpiForsendelse(multipartBodyBuilder, forsendelse);
		ForsendelseStatusResponse forsendelseStatusResponse = forsendelseStatusResponses.stream()
				.filter(statusResponse -> SENDT.equals(statusResponse.getStatus()) || OPPRETTET.equals(statusResponse.getStatus()))
				.findAny().orElse(null);

		return forsendelseStatusResponse == null ? null : OppdaterForsendelseAndVarselRequest.builder()
				.status(forsendelseStatusResponse.getStatus())
				.forsendelseId(forsendelse.getForsendelseId())
				.digitalLeverandoeradresse(forsendelse.getDigitalPostLeverandoerAdresse())
				.digitalPostkasseadresse(forsendelse.getDigital().getMottaker().getPostkasseadresse())
				.varsler(forsendelse.getDigital().getVarsler())
				.distribusjonsTypeKode(forsendelse.getDistribusjonsTypeKode())
				.build();
	}

	private byte[] getKryptertDokumentpakke(Forsendelse forsendelse) {
		return digitalPostContentPackager.createKryptertDokumentpakke(forsendelse, appCertificate);
	}

	public Dokumentpakkefingeravtrykk getDokumentpakkefingeravtrykk(byte[] asicStream) {
		MessageDigest messageDigest = new SHA256.Digest();
		byte[] digest = messageDigest.digest(asicStream);
		return Dokumentpakkefingeravtrykk.builder()
				.digestMethod(DigestMethod.SHA256)
				.digestValue(Base64.getEncoder().encodeToString(digest))
				.build();
	}

	private String generateStandardBusinessDocumentJWT(StandardBusinessDocument sbd) {
		try {
			String sbdJson = objectMapper.writeValueAsString(new SimpleStandardBusinessDocument(sbd));
			JWTClaimsSet claims = JWTClaimsSet.parse(sbdJson);
			return GenerateJwt.generateJWT(claims, appCertificate);
		} catch (JsonProcessingException | ParseException e) {
			log.warn("SBD til JWT behandling feilet med feilmelding={}", e.getMessage());
			throw new JsonParserTechnicalException("SBD til JWT behandling feilet med feilmelding={}" + e.getMessage(), e);
		}
	}
}
