package example.healthtech;

@FunctionalInterface
public interface SigningGateway {
    String signAndPresign(String pdfBase64, String certPem, String keyPem, String objectKey);
}
