package com.medsimple.media.scheduler;

import com.medsimple.ai.service.AiTriageService;
import com.medsimple.media.service.MediaProcessingService;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaProcessingSchedulerTest {

    @Mock MediaProcessingService mediaProcessingService;
    @Mock AiTriageService aiTriageService;
    @Mock SymptomEpisodeRepository episodeRepository;
    @InjectMocks MediaProcessingScheduler scheduler;

    private void noPending() {
        when(mediaProcessingService.findPendingSubmissionIds()).thenReturn(List.of());
    }

    private void noStuck() {
        lenient().when(episodeRepository.findIdsByStatusAndUpdatedAtBefore(
                eq(EpisodeStatus.SUBMITTED), any(OffsetDateTime.class))).thenReturn(List.of());
    }

    @Test
    void nothingPendingOrStuck_doesNothing() {
        noPending();
        noStuck();

        scheduler.pollAndProcess();

        verify(mediaProcessingService, never()).process(any());
        verify(aiTriageService, never()).triageEpisodeIfReady(any());
    }

    @Test
    void processesEachPendingSubmission() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID();
        when(mediaProcessingService.findPendingSubmissionIds()).thenReturn(List.of(s1, s2));
        noStuck();

        scheduler.pollAndProcess();

        verify(mediaProcessingService).process(s1);
        verify(mediaProcessingService).process(s2);
    }

    @Test
    void triagesEachStuckEpisode() {
        UUID epA = UUID.randomUUID(), epB = UUID.randomUUID();
        noPending();
        when(episodeRepository.findIdsByStatusAndUpdatedAtBefore(
                eq(EpisodeStatus.SUBMITTED), any(OffsetDateTime.class))).thenReturn(List.of(epA, epB));

        scheduler.pollAndProcess();

        verify(aiTriageService).triageEpisodeIfReady(epA);
        verify(aiTriageService).triageEpisodeIfReady(epB);
    }

    @Test
    void processingFailure_doesNotStopRemainingProcessingOrTheTriageSweep() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID();
        UUID epA = UUID.randomUUID();
        when(mediaProcessingService.findPendingSubmissionIds()).thenReturn(List.of(s1, s2));
        when(mediaProcessingService.process(s1)).thenThrow(new RuntimeException("provider down"));
        when(episodeRepository.findIdsByStatusAndUpdatedAtBefore(
                eq(EpisodeStatus.SUBMITTED), any(OffsetDateTime.class))).thenReturn(List.of(epA));

        scheduler.pollAndProcess();

        verify(mediaProcessingService).process(s2);          // failure of s1 didn't abort the loop
        verify(aiTriageService).triageEpisodeIfReady(epA);   // nor the triage sweep
    }

    @Test
    void triageFailureForOneEpisode_doesNotStopTheOthers() {
        UUID epA = UUID.randomUUID(), epB = UUID.randomUUID();
        noPending();
        when(episodeRepository.findIdsByStatusAndUpdatedAtBefore(
                eq(EpisodeStatus.SUBMITTED), any(OffsetDateTime.class))).thenReturn(List.of(epA, epB));
        doThrow(new RuntimeException("triage down")).when(aiTriageService).triageEpisodeIfReady(epA);

        scheduler.pollAndProcess(); // must not propagate

        verify(aiTriageService).triageEpisodeIfReady(epA);
        verify(aiTriageService).triageEpisodeIfReady(epB);
    }
}
