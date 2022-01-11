package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScopeType {
	CONVERSATION_ID("ConversationId"),
	BESTILLINGS_ID("BestillingsId"),
	SENDER_REF("SenderRef"),
	RECEIVER_REF("ReceiverRef");

	private String fullname;
}
