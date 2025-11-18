package no.nav.dokdistdpi.certificate;

import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.utils.CertificateUtils.itestVirksomhetssertifikatBase64AppCertificate;
import static no.nav.dokdistdpi.utils.CertificateUtils.itestVirksomhetssertifikatAppCertificate;
import static org.junit.jupiter.api.Assertions.*;

class AppCertificateTest {

	@Test
	void shouldLoadPKCS12KeyStore() {
		AppCertificate appCertificate = (itestVirksomhetssertifikatAppCertificate());
		assertNotNull(appCertificate.getX509Certificate());
	}

	@Test
	void shouldLoadPKCS12KeyStoreAsBase64() {
		AppCertificate appCertificate = (itestVirksomhetssertifikatBase64AppCertificate());
		assertNotNull(appCertificate.getX509Certificate());
	}

}