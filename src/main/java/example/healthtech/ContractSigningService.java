package example.healthtech;

public final class ContractSigningService {
    private final SigningGateway infrai;

    public ContractSigningService(SigningGateway infrai) {
        this.infrai = infrai;
    }

    public PatientNotification signConfirmed(Appointment appointment, String pdf, String cert, String key) {
        if (!appointment.isConfirmed()) {
            throw new IllegalArgumentException("Only confirmed appointments receive a contract");
        }
        String link = infrai.signAndPresign(pdf, cert, key, "appointments/" + appointment.id() + ".pdf");
        return new PatientNotification(appointment.patientEmail(), "Your appointment agreement is signed: " + link);
    }
}
