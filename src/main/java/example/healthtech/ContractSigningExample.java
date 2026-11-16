package example.healthtech;

public final class ContractSigningExample {
    public static void main(String[] args) {
        if (System.getenv("INFRAI_API_KEY") == null) {
            Appointment appointment = new Appointment("A-104", "CONFIRMED", "patient@example.org");
            System.out.println("SIGNED decision ready for " + appointment.id() + "; set INFRAI_API_KEY to send the document");
            return;
        }
        String recipient = System.getenv("INFRAI_RECIPIENT");
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalStateException("INFRAI_RECIPIENT is required for a live run");
        }
        Appointment appointment = new Appointment("A-104", "CONFIRMED", recipient);
        ServiceConfig config = ServiceConfig.fromEnvironment();
        String pdf = requiredEnvironment("INFRAI_PDF_BASE64");
        String cert = requiredEnvironment("INFRAI_CERT_PEM");
        String key = requiredEnvironment("INFRAI_KEY_PEM");
        PatientNotification notification = new ContractSigningService(new InfraiHttpClient(config))
                .signConfirmed(appointment, pdf, cert, key);
        System.out.println(notification.message());
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for a live run");
        }
        return value;
    }
}
