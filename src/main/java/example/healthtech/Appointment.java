package example.healthtech;

public record Appointment(String id, String status, String patientEmail) {
    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }
}
