package example.healthtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiHttpClient implements SigningGateway {
    private final ServiceConfig config;
    private final HttpClient http;

    public InfraiHttpClient(ServiceConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String signAndPresign(String pdfBase64, String certPem, String keyPem, String objectKey) {
        request("POST", "/storage/bucket/create", jsonObject("name", config.bucket()));
        String signedResponse = request("POST", "/pdf/sign", "{" + field("pdf", pdfBase64)
                + "," + field("cert_pem", certPem) + "," + field("key_pem", keyPem)
                + "," + field("reason", "Appointment contract") + ",\"retention_days\":"
                + config.retentionDays() + "}");
        String signed = value(signedResponse, "signed_pdf", "pdf", "data_base64", "artifact");
        request("PUT", "/storage/object/put/" + config.bucket() + "/" + objectKey,
                "{\"data_base64\":\"" + signed + "\"}");
        String linkResponse = request("POST", "/storage/object/presign/" + config.bucket() + "/" + objectKey,
                "{\"op\":\"get\",\"expires_seconds\":900,\"response_disposition\":\"attachment\"}");
        return value(linkResponse, "url", "presigned_url", "download_url", "link");
    }

    private String jsonObject(String name, String value) {
        return "{" + field(name, value) + "}";
    }

    private String field(String name, String value) {
        return "\"" + escapeJson(name) + "\":\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String value(String envelope, String... names) {
        for (String name : names) {
            String marker = "\"" + name + "\"";
            int key = envelope.indexOf(marker);
            if (key >= 0) {
                int colon = envelope.indexOf(':', key + marker.length());
                int start = envelope.indexOf('"', colon + 1);
                int end = start + 1;
                while (end < envelope.length()) {
                    end = envelope.indexOf('"', end);
                    if (end < 0 || envelope.charAt(end - 1) != '\\') break;
                    end++;
                }
                if (start >= 0 && end >= 0) return envelope.substring(start + 1, end);
            }
        }
        throw new IllegalStateException("Infrai response did not contain a required artifact value: " + envelope);
    }

    private String request(String method, String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IllegalStateException("Infrai request rejected: " + envelope);
            }
            if (response.statusCode() == 429) {
                throw new IllegalStateException("Infrai request should be retried after the server delay");
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            return envelope;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Request transport failed", e);
        }
    }
}
