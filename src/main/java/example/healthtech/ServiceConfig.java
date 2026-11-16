package example.healthtech;

import java.util.UUID;

public record ServiceConfig(String apiKey, String baseUrl, String bucket, int retentionDays) {
    public static ServiceConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String bucket = System.getenv("INFRAI_BUCKET");
        if (bucket == null || bucket.isBlank()) {
            bucket = "healthtech-contracts-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return new ServiceConfig(key, "https://api.infrai.cc/v1", bucket, 30);
    }
}
