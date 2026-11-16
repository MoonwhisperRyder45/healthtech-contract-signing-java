package example.healthtech;

public final class ContractSigningServiceTest {
    public static void main(String[] args) {
        Appointment appointment = new Appointment("A-104", "CONFIRMED", "patient@example.org");
        PatientNotification result = new ContractSigningService((pdf, cert, key, objectKey) -> "signed-url")
                .signConfirmed(appointment, "pdf", "cert", "key");
        if (!result.message().contains("signed-url")) {
            throw new AssertionError("confirmed appointment must produce a signed link");
        }
        System.out.println("PASS: confirmed appointment becomes a signed notification");
    }
}
