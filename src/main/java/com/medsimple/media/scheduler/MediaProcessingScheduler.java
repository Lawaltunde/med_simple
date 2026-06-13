package com.medsimple.media.scheduler;

import com.medsimple.media.service.MediaProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Background worker: periodically picks up PENDING voice/photo submissions and processes them.
 * This is the async story — a patient uploads a voice note/photo (PENDING), and the pipeline
 * transcribes/analyzes it out-of-band. Disabled in tests via media.processing.poller.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.processing.poller.enabled", havingValue = "true", matchIfMissing = true)
public class MediaProcessingScheduler {

    private final MediaProcessingService mediaProcessingService;

    @Scheduled(fixedDelayString = "${media.processing.poller.interval-ms:10000}")
    public void pollAndProcess() {
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
}
