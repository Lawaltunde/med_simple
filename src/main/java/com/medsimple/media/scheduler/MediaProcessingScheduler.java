package com.medsimple.media.scheduler;

import com.medsimple.ai.service.AiTriageService;
import com.medsimple.media.service.MediaProcessingService;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Background worker for the async media story. Each poll does two things:
 *
 * <ol>
 *   <li><b>Process</b> PENDING voice/photo submissions — transcribe/analyze them out-of-band.</li>
 *   <li><b>Triage backstop</b> — assess episodes left in SUBMITTED past a grace window. The
 *       synchronous patient flow chains process→triage in the browser; when that chain doesn't
 *       complete (slow transcription, a dropped connection, the patient navigating away), the
 *       media still gets transcribed but the episode is never triaged. This sweep catches those so
 *       a voice/photo episode is never stuck transcribed-but-not-assessed.</li>
 * </ol>
 *
 * <p>The grace window is what keeps the backstop from racing an in-flight sync flow: a freshly
 * created episode is left alone until it is clearly stuck. Disabled in tests via
 * media.processing.poller.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.processing.poller.enabled", havingValue = "true", matchIfMissing = true)
public class MediaProcessingScheduler {

    private final MediaProcessingService mediaProcessingService;
    private final AiTriageService aiTriageService;
    private final SymptomEpisodeRepository episodeRepository;

    // How long an episode may sit in SUBMITTED before the backstop triages it. Long enough that a
    // normal synchronous flow (upload → transcribe → triage) has finished and moved it on, so the
    // sweep never double-triages an episode the browser is about to handle.
    @Value("${media.processing.triage-grace-seconds:120}")
    private long triageGraceSeconds;

    @Scheduled(fixedDelayString = "${media.processing.poller.interval-ms:10000}")
    public void pollAndProcess() {
        processPendingMedia();
        triageStuckEpisodes();
    }

    private void processPendingMedia() {
        List<UUID> pending = mediaProcessingService.findPendingSubmissionIds();
        if (pending.isEmpty()) return;
        log.info("Media poller: processing {} pending submission(s)", pending.size());
        for (UUID id : pending) {
            try {
                mediaProcessingService.process(id);
            } catch (Exception e) {
                log.error("Media poller: failed to process submission {}", id, e);
            }
        }
    }

    private void triageStuckEpisodes() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(triageGraceSeconds);
        List<UUID> stuck = episodeRepository.findIdsByStatusAndUpdatedAtBefore(EpisodeStatus.SUBMITTED, cutoff);
        if (stuck.isEmpty()) return;
        log.info("Media poller: triage backstop checking {} stuck episode(s)", stuck.size());
        for (UUID episodeId : stuck) {
            try {
                // Idempotent + conservative: skips episodes already assessed, waits for any
                // still-pending media, and needs at least one completed submission with text.
                aiTriageService.triageEpisodeIfReady(episodeId);
            } catch (Exception e) {
                log.error("Media poller: failed to triage stuck episode {}", episodeId, e);
            }
        }
    }
}
