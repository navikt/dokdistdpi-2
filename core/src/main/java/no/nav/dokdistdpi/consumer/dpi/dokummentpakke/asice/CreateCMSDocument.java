package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.asice;

import no.nav.dokdistdpi.exception.technical.KonfigurasjonException;
import no.nav.dokdistdpi.exception.functional.RuntimeIOException;
import no.nav.dokdistdpi.exception.technical.DokumentpakkingException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import static java.security.Security.addProvider;
import static java.security.Security.getProvider;
import static java.util.Objects.isNull;
import static org.bouncycastle.asn1.DERNull.INSTANCE;
import static org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha256;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_RSAES_OAEP;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_mgf1;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_pSpecified;

@Component
public class CreateCMSDocument {

	private final AlgorithmIdentifier keyEncryptionScheme;
	private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;

	@Autowired
	public CreateCMSDocument() {
		if (isNull(getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
			addProvider(new BouncyCastleProvider());
		}
		this.keyEncryptionScheme = rsaesOaepIdentifier();
		this.cmsEncryptionAlgorithm = CMSAlgorithm.AES256_CBC;
	}

	private AlgorithmIdentifier rsaesOaepIdentifier() {
		AlgorithmIdentifier hash = new AlgorithmIdentifier(id_sha256, INSTANCE);
		AlgorithmIdentifier mask = new AlgorithmIdentifier(id_mgf1, hash);
		AlgorithmIdentifier p_source = new AlgorithmIdentifier(id_pSpecified, new DEROctetString(new byte[0]));
		ASN1Encodable parameters = new RSAESOAEPparams(hash, mask, p_source);
		return new AlgorithmIdentifier(id_RSAES_OAEP, parameters);
	}

	public byte[] createCMSByte(byte[] bytes, X509Certificate certificate) {
		try {
			JceKeyTransRecipientInfoGenerator recipientInfoGenerator = isNull(keyEncryptionScheme) ? new JceKeyTransRecipientInfoGenerator(certificate) :
					new JceKeyTransRecipientInfoGenerator(certificate, keyEncryptionScheme);

			CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
			envelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

			OutputEncryptor contentEncryptor = new JceCMSContentEncryptorBuilder(cmsEncryptionAlgorithm).build();
			CMSEnvelopedData cmsData = envelopedDataGenerator.generate(new CMSProcessableByteArray(bytes), contentEncryptor);

			return cmsData.getEncoded();
		} catch (CertificateEncodingException e) {
			throw new KonfigurasjonException("Feil med mottakers sertifikat", e);
		} catch (CMSException e) {
			throw new KonfigurasjonException("Kunne ikke generere Cryptographic Message Syntax for dokumentpakke", e);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	InputStream decryptCMSStreamed(InputStream encrypted, PrivateKey privateKey) {
		try {
			CMSEnvelopedDataParser cms;
			cms = new CMSEnvelopedDataParser(encrypted);
			RecipientInformationStore recipients = cms.getRecipientInfos();
			Collection<?> c = recipients.getRecipients();
			Iterator<?> it = c.iterator();
			if (it.hasNext()) {
				JceKeyTransEnvelopedRecipient recipient = new JceKeyTransEnvelopedRecipient(privateKey);
				RecipientInformation recipientInformation = (RecipientInformation) it.next();
				return recipientInformation.getContentStream(recipient).getContentStream();
			}
			throw new DokumentpakkingException("No recipients in CMS package.");
		} catch (CMSException | IOException e) {
			throw new DokumentpakkingException("Klarte ikke kryptere dokumentpakke", e);
		}
	}
}
