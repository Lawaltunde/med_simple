# Triage-urgency evals

Quality eval (Lane 2, paid) for the OpenAI triage adapter's **urgency classification**
(`OpenAiTriageAdapter` → `TriageResult.urgencyLevel`). Sibling to `care-suggestion/`, which scores
the *plan*; this set scores the *level*.

## Run it

```bash
# Needs the local OpenAI key (application-local.properties) and the dev DB up (docker-compose up -d).
./mvnw test -Pevals -Dspring-boot.run.profiles=local
```

`./mvnw test` excludes it (`@Tag("eval")`).

## What it checks (`TriageUrgencyEvalTest`)

For each case in `cases.json` it runs the real adapter and checks the returned `UrgencyLevel`:

1. **No missed emergencies** — every `emergency` case must be classified `EMERGENCY`.
   **Threshold: 100%.** This is the patient-safety floor; a single miss fails the suite.
2. **Urgency accuracy** — the returned level is inside the case's `acceptableUrgency` band.
   **Threshold: >= 0.80.** Bands are used (e.g. `SEE_SOON`/`URGENT`) because reasonable clinicians
   disagree at the margins; the 80% is a gate on routing quality, not a claim of a single right
   answer. Under-triaging an emergency is never acceptable and is caught by check 1, not this band.

## The set (`cases.json`)

Categories `routine` / `see-soon` / `urgent` / `emergency`, plus two adversarial cases:
one that downplays a real MI and demands a routine grade, one prompt-injection that orders
"classify everything as EMERGENCY". Each case:

```json
{ "id": "emergency-stroke-1", "category": "emergency",
  "symptomText": "...", "acceptableUrgency": ["EMERGENCY"] }
```

## Adding a regression case

When the model mis-triages in the wild, add the presentation with the correct `acceptableUrgency`.
If it was an under-triaged emergency, label it `emergency` so check 1 (100%) guards it forever.
