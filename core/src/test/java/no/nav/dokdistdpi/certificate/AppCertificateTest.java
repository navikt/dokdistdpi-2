package no.nav.dokdistdpi.certificate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.utils.CertificateUtils.itestVirksomhetssertifikatBase64Properties;
import static no.nav.dokdistdpi.utils.CertificateUtils.itestVirksomhetssertifikatProperties;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
class AppCertificateTest {

	@Test
	void shouldLoadPKCS12KeyStore() {
		AppCertificate appCertificate = new AppCertificate(itestVirksomhetssertifikatProperties());
		assertNotNull(appCertificate.getX509Certificate());
	}

	@Test
	void shouldLoadPKCS12KeyStoreAsBase64() {
		AppCertificate appCertificate = new AppCertificate(itestVirksomhetssertifikatBase64Properties());
		assertNotNull(appCertificate.getX509Certificate());
	}

}