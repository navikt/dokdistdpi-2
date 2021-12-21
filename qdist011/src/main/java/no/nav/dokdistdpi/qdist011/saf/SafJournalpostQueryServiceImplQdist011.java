package no.nav.dokdistdpi.qdist011.saf;

import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.consumer.saf.graphql.GraphQLRequest;
import no.nav.dokdistdpi.consumer.saf.graphql.SafGraphqlConsumer;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class SafJournalpostQueryServiceImplQdist011  implements SafJournalpostQueryService<JournalpostQdist011> {

	private final SafGraphqlConsumer safGraphqlConsumer;
	private final SafJournalpostValidatorQdist011 safJournalpostValidatorQdist011;
	private final JournalpostQdist011Mapper journalpostQdist011Mapper;

	@Autowired
	public SafJournalpostQueryServiceImplQdist011(SafGraphqlConsumer safGraphqlConsumer, SafJournalpostValidatorQdist011 safJournalpostValidatorQdist011,
												  JournalpostQdist011Mapper journalpostQdist011Mapper) {
		this.safGraphqlConsumer = safGraphqlConsumer;
		this.safJournalpostValidatorQdist011 = safJournalpostValidatorQdist011;
		this.journalpostQdist011Mapper = journalpostQdist011Mapper;
	}

	@Override
	public JournalpostQdist011 hentJournalpost(String journalpostid) {
		SafJournalpostResponse safJournalpost = safGraphqlConsumer.performQuery(GraphQLRequest.builder()
				.query(JOURNALPOST_QUERY)
				.operationName("journalpost")
				.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
				.build());

		safJournalpostValidatorQdist011.validate(safJournalpost, journalpostid);
		return journalpostQdist011Mapper.map(safJournalpost);
	}

	private static final String JOURNALPOST_QUERY =
			"query journalpost($queryJournalpostId: String!) {\n" +
					"  journalpost(journalpostId: $queryJournalpostId) {\n" +
					"    dokumenter {\n" +
					"      dokumentInfoId\n" +
					"      tittel\n" +
					"    }\n" +
					"  }\n" +
					"}\n";
}
