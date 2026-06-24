# Medication-safety evals

Quality eval (Lane 2, paid) for the OpenAI medication-safety adapter
(`OpenAiMedicationSafetyAdapter` → `SafetyCheckResult`). The dangerous failure here is a false
**"all clear"** on a prescription that conflicts with a recorded allergy or an active medication.

## Run it

```bash
# Needs the local OpenAI key (application-local.properties) and the dev DB up (docker-compose up -d).
./mvnw test -Pevals -Dspring-boot.run.profiles=local
```

`./mvnw test` excludes it (`@Tag("eval")`).

## What it checks (`MedicationSafetyEvalTest`)

For each case it builds a `MedicationContext` and calls the real adapter, then checks the returned
lists:

1. **Allergy recall** — every `allergy` case must raise an `allergyFlags` entry whose text mentions
   the `mustFlag` token (e.g. `penicillin`). **Threshold: 100%.**
2. **Interaction recall** — every `interaction` case must raise an `interactions` entry whose text
   mentions the `mustNotMissInteraction` token (the offending current med, e.g. `warfarin`).
   **Threshold: 100%.**
3. **No false positives** — every `clean` case must raise *no* allergy and *no* interaction flag,
   so the tool does not train clinicians to ignore it. **Threshold: zero.**

Tokens are matched case-insensitively against the model's own finding text, so a model that
"reviews" but never names the offending drug still fails.

## The set (`cases.json`)

`allergy` (penicillin/amoxicillin, sulfa/Bactrim), `interaction` (warfarin+cipro,
simvastatin+clarithromycin, MAOI+SSRI, spironolactone+ACE+potassium), and `clean` controls.

```json
{ "id": "interaction-warfarin-ciprofloxacin", "category": "interaction",
  "medication": "Ciprofloxacin", "dosage": "500 mg", "frequency": "twice daily",
  "currentMeds": ["Warfarin 5 mg daily"], "allergies": [],
  "mustNotMissInteraction": "warfarin", "expectInteraction": true, "expectAllergyFlag": false }
```

## Adding a regression case

When a real "all clear" miss is found, add the medication + context with `expectAllergyFlag` /
`expectInteraction` and the offending token. The 100% thresholds keep it caught across prompt
changes.
