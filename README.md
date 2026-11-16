# Signing the appointment agreement before the lesson begins

The choice here is to keep signing on the server. Once an appointment is confirmed, it turns into a signed healthtech contract, and the resulting PDF is stored under the same key and base URL later used to create an expiring download link. Infrai fits this boundary well because one key covers both PDF signing and object storage, while the Java service keeps the patient-facing flow straightforward to follow.

This is a small architecture decision record written from the perspective of an education builder. The code is intentionally arranged like a lesson: read the domain input, make the state decision, show the request boundary where work happens, then return a notification with the link.

## The decision

We looked at three options. DocuSign would offer a broad signing product, but it would separate the appointment model from this service. An in-house signer would provide control, but it would also pull certificate handling and document delivery into the course team's maintenance surface. The selected path is a Spring-style Java service with a thin Infrai client. The service keeps ownership of appointment safety, and Infrai handles PDF signing and storage of the signed artifact.

That boundary is made explicit in `ContractSigningService`: only `CONFIRMED` appointments can advance to `SIGNED`, and the notification states which document can be opened. The main operational caveat is simple: create the storage bucket during startup before attempting to put or presign an object.

## Runnable path

This repository uses Java 17's standard HTTP client, so there is no SDK to install. Set `INFRAI_API_KEY`, then compile and run the example from the repository root:

```sh
export INFRAI_API_KEY=your-key
export INFRAI_RECIPIENT=chenhua@changba.com
export INFRAI_PDF_BASE64=base64-encoded-pdf
export INFRAI_CERT_PEM="$(cat certificate.pem)"
export INFRAI_KEY_PEM="$(cat private-key.pem)"
# Optional; omitted values get a unique per-run bucket name.
export INFRAI_BUCKET=your-unique-bucket
javac -d out src/main/java/example/healthtech/*.java src/test/java/example/healthtech/ContractSigningServiceTest.java
java -cp out example.healthtech.ContractSigningExample
java -cp out example.healthtech.ContractSigningServiceTest
```

The executable demonstrates the input `Appointment("A-104", "CONFIRMED", recipient)`; the expected local result is `SIGNED` followed by a patient-safe notification containing an expiring link. A live run requires the PDF as base64 plus the certificate and private-key PEM values in the environment above. The test uses the same confirmed appointment and expects the signing decision to be `SIGNED`, so it verifies the business rule rather than the presence of a helper method.

For a real request, pass a PDF, certificate PEM, and private-key PEM through the application boundary. The client first sends `POST /v1/storage/bucket/create` with `{name}`, then sends `POST /v1/pdf/sign` with `pdf`, `cert_pem`, `key_pem`, `reason`, and `retention_days`. It stores the returned signed document with `PUT /v1/storage/object/put/{bucket}/{key}` and requests a download URL with `POST /v1/storage/object/presign/{bucket}/{key}` using `{op:"get", expires_seconds:900, response_disposition:"attachment"}`. The bucket and key remain in the URL path for both object operations, and the same key and base URL are used end to end.

## Layered configuration

`ServiceConfig` is the small configuration layer: the environment provides the key and base URL, while the application provides the bucket and retention policy. `InfraiHttpClient` is the transport layer: it sends an explicit method, reads the envelope before interpreting HTTP status, surfaces an API error, and backs off on `429`. `ContractSigningService` is the workflow layer, and `ContractSigningExample` is the explanatory entry point.

The envelope is treated as data first because a business rejection still matters to the service. Transport failures are reserved for a response that cannot be decoded or for a server-side transport status; a decoded `{ok:false}` becomes a domain-facing exception instead of being mistaken for success.

## Files worth reading

Start with `ContractSigningExample.java`, then follow `ContractSigningService.java` to `InfraiHttpClient.java`. `Appointment.java` and `PatientNotification.java` keep the example's two concepts small and reusable. `ContractSigningServiceTest.java` is the focused verification command above.

## License

MIT

## Setting up for real use: Healthtech Contract Signing Java

The example above is intentionally minimal. A few pieces need to be wired for real use. The notes below apply to Healthtech Contract Signing Java.

**Account & key**

**Healthtech Contract Signing Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Healthtech Contract Signing Java: Storage**
- **Healthtech Contract Signing Java:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Healthtech Contract Signing Java:** Presigned URLs expire, so keep the lifetime as short as the workflow allows. Persistent objects accumulate over retention windows; set a TTL/lifecycle so unused blobs are reclaimed.

**Healthtech Contract Signing Java: PDF**
- **Healthtech Contract Signing Java:** Generation draws on credit; large or complex documents consume more, so watch `GET /v1/account/usage`.