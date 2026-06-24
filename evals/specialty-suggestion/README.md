# Specialty-suggestion evals

Quality eval (Lane 2, paid) for the OpenAI specialty-suggestion adapter
(`OpenAiSpecialtySuggestionAdapter` → `SpecialtySuggestion`). Measures whether a clinical summary
routes to the right specialty. A wrong referral burns a specialist slot and delays care.

## Run it

```bash
# Needs the local OpenAI key (application-local.properties) and the dev DB up (docker-compose up -d).
./mvnw test -Pevals -Dspring-boot.run.profiles=local
```

`./mvnw test` excludes it (`@Tag("eval")`).

## What it checks (`SpecialtySuggestionEvalTest`)

For each case it calls the real adapter and matches `SpecialtySuggestion.specialty` against the
case's `acceptableSpecialty` set. Match is case-insensitive and substring-tolerant in both
directions, so `Orthopedic Surgery` satisfies `Orthopedics` and `Otolaryngology` satisfies `ENT`.

- **Accuracy** = fraction of cases whose suggested specialty is in the acceptable set.
  **Threshold: >= 0.85.** Set membership (not LLM-judge) is used because the gold is a small,
  well-defined set of Canadian specialty names — a deterministic check is cheaper and exact.

## The set (`cases.json`)

One representative referral per major specialty: Cardiology, Dermatology, Gastroenterology,
Neurology, Orthopedics, Respirology, Endocrinology, Psychiatry, Ophthalmology, Urology,
Nephrology, ENT. Synonyms are listed in `acceptableSpecialty`.

```json
{ "id": "ent-1", "category": "ent",
  "clinicalSummary": "Unilateral nasal obstruction with recurrent epistaxis ...",
  "acceptableSpecialty": ["ENT", "Otolaryngology"] }
```

## Adding a regression case

When a referral is mis-routed in the wild, add the summary with the correct `acceptableSpecialty`
(include synonyms). Keep summaries PHI-stripped — symptom/condition text only.
