# Lab requisition → FHIR demo

Shows a diagnostic requisition leaving MedSimple as a real **FHIR R4 ServiceRequest** to a lab
endpoint. The endpoint here is a local HAPI FHIR sandbox; in production it is the lab network /
OLIS once onboarded. Same code path — only `lab.fhir.base-url` changes. Selection is driven by the
`LAB_ELECTRONIC` clinic capability (`RequisitionDeliveryResolver`); without it, the clinic gets the
printable-PDF fallback.

## Run it

```bash
# 1. Start the dev DB + the FHIR sandbox
docker compose up -d            # brings up Postgres (5333) and HAPI FHIR (8090)

# 2. Boot the app pointed at the sandbox
LAB_FHIR_BASE_URL=http://localhost:8090/fhir \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then, as the demo users (`admin@demo.medsimple` / `Admin123!`, `physician@demo.medsimple` /
`Physician123!`, `patient@demo.medsimple` / `Patient123!`):

1. **Admin** enables the capability: `POST /api/v1/clinics/{clinicId}/capabilities`
   `{"capability":"LAB_ELECTRONIC","enabled":true}`
2. **Patient** starts an episode; **physician** assigns it and creates a requisition
   (`POST /api/v1/requisitions`).
3. **Physician** sends it: `POST /api/v1/requisitions/{id}/send`. The response's `documentUrl` is
   the created FHIR resource, e.g. `http://localhost:8090/fhir/ServiceRequest/1000/_history/1`.

## See it in the sandbox

```bash
curl "http://localhost:8090/fhir/ServiceRequest?_sort=-_lastUpdated"
```

You get back a `ServiceRequest` with `status=active`, `intent=order`, the test as `code.text`
(+ LOINC codings), the patient as `subject`, the ordering physician as `requester`, and the
requisition id as a business `identifier` so a lab result can be correlated back.

## Production swap

Replace `lab.fhir.base-url` with the lab network / OLIS FHIR endpoint and add the provider auth
header in `ElectronicLabAdapter`. No change to callers, the mapper, or the capability switch.
