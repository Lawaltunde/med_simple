package com.medsimple.appointment.service;

import com.medsimple.appointment.entity.Appointment;
import com.medsimple.clinic.entity.Clinic;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentIcsTest {

    private Appointment appointment(String reason, int minutes) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .clinic(Clinic.builder().name("MedSimple Demo Family Practice").build())
                .scheduledAt(OffsetDateTime.of(2026, 7, 1, 14, 30, 0, 0, ZoneOffset.UTC))
                .durationMinutes(minutes)
                .reason(reason)
                .build();
    }

    @Test
    void buildsAValidVeventWithStartAndEnd() {
        String ics = AppointmentIcs.build(appointment("Follow-up on throat", 20));

        assertThat(ics).contains("BEGIN:VCALENDAR").contains("END:VCALENDAR");
        assertThat(ics).contains("BEGIN:VEVENT").contains("END:VEVENT");
        assertThat(ics).contains("DTSTART:20260701T143000Z");
        assertThat(ics).contains("DTEND:20260701T145000Z"); // +20 minutes
        assertThat(ics).contains("LOCATION:MedSimple Demo Family Practice");
        assertThat(ics).contains("SUMMARY:MedSimple appointment");
        assertThat(ics).endsWith("\r\n");
    }

    @Test
    void escapesCommasAndSemicolonsInTheReason() {
        String ics = AppointmentIcs.build(appointment("Review labs, imaging; then plan", 15));
        assertThat(ics).contains("DESCRIPTION:Review labs\\, imaging\\; then plan");
    }

    @Test
    void fallsBackWhenReasonIsNull() {
        String ics = AppointmentIcs.build(appointment(null, 15));
        assertThat(ics).contains("DESCRIPTION:Follow-up appointment");
    }
}
