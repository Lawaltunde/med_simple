# Evals — the paid quality lane

Lane 2 of the two-lane model in `CLAUDE.md`. **Gate tests** (Lane 1) are deterministic, free, and
run on every commit (`./mvnw test`). **Evals** (this directory) call live OpenAI, cost money, and
run before ship / nightly with a pass threshold. Every AI surface has one.

```bash
# Run ALL evals (paid; needs the local OpenAI key in application-local.properties + dev DB up):
docker-compose up -d
./mvnw test -Pevals -Dspring-boot.run.profiles=local

# Run one surface:
./mvnw test -Pevals -Dtest=TriageUrgencyEvalTest -Dspring-boot.run.profiles=local
```

The default build excludes them (`-DexcludedGroups=eval`), so `./mvnw test` stays free and fast.
Each runner is `@Tag("eval") @SpringBootTest @ActiveProfiles("local")`, forces the OpenAI adapter
with `properties = "ai.<surface>.adapter=openai"`, applies deterministic must/must-not checks plus
(where useful) an LLM-as-judge, prints a report, and asserts thresholds.

## Surfaces and thresholds

| Dir | Adapter | Runner | Thresholds |
|---|---|---|---|
| `care-suggestion/` | `OpenAiTriageAdapter` (plan) | `CareSuggestionEvalTest` | 0 banned-term leaks; 100% emergencies; judge mean >= 4.0 |
| `triage-urgency/` | `OpenAiTriageAdapter` (level) | `TriageUrgencyEvalTest` | 100% emergencies classified EMERGENCY; accuracy >= 0.80 |
| `medication-safety/` | `OpenAiMedicationSafetyAdapter` | `MedicationSafetyEvalTest` | 100% allergy recall; 100% interaction recall; 0 false positives |
| `specialty-suggestion/` | `OpenAiSpecialtySuggestionAdapter` | `SpecialtySuggestionEvalTest` | accuracy >= 0.85 (set membership) |
| `result-summary/` | `OpenAiResultSummaryAdapter` | `ResultSummaryEvalTest` | 0 fabrication; 0 diagnosis/treatment; judge mean >= 4.0 |

### Why these thresholds

- **100% gates** sit on the failure that hurts a patient: a missed emergency, a false "all clear"
  on an allergy/interaction, a fabricated lab value. No band — a single miss fails.
- **Accuracy bands (0.80 / 0.85)** sit on routing-quality calls where reasonable clinicians
  disagree at the margins. The acceptable-set encodes that disagreement; the threshold gates the
  overall quality without pretending there is one right answer per case.
- **Judge mean >= 4.0** mirrors the existing `CareSuggestionEvalTest` bar for free-text quality
  (plan / summary), scored 1-5 by a separate model against gold themes.

## Deferred: vision + voice (`media/`)

`OpenAiPhotoAnalysisAdapter` and `OpenAiVoiceTranscriptionAdapter` take a `mediaUrl` and fetch the
binary via `UrlResource`. A real eval needs (a) image/audio fixtures and (b) a URL the model
provider can reach. Two blockers, deferred on purpose:

1. **No binaries in the repo.** `CLAUDE.md` bans committing binaries/model weights; image and audio
   fixtures would have to live in cloud storage with a pointer, which is infra not yet in place.
2. **Reachable URL.** The adapters hand a URL to OpenAI, not bytes, so a `file://` or localhost
   fixture will not work without a public (signed) URL or a media-server stub.

When the media-storage contract lands (signed URLs from the same store the intake pipeline uses),
add `evals/photo-analysis/` and `evals/voice-transcription/` mirroring this layout: a handful of
fixtures with gold descriptions/transcripts, a deterministic must-not-fabricate check on the photo
description, and word-error-rate on the transcript. Tracked here so it is not forgotten.
