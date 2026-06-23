package com.medsimple.appointment.service;

import com.medsimple.appointment.entity.Appointment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Builds a standard iCalendar (.ics) event so a patient or physician can add the appointment to
// Google / Apple / Outlook themselves. No OAuth, no third-party tokens, no PHI persisted by us at
// a calendar provider — the file is handed to whoever requested it for their own calendar.
public final class AppointmentIcs {

    private AppointmentIcs() {}

    private static final DateTimeFormatter UTC_BASIC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static String build(Appointment a) {
        OffsetDateTime start = a.getScheduledAt().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(a.getDurationMinutes());
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(UTC_BASIC);
        String location = a.getClinic() != null ? a.getClinic().getName() : "MedSimple";

        return String.join("\r\n",
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//MedSimple//Appointments//EN",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "BEGIN:VEVENT",
                "UID:" + a.getId() + "@medsimple",
                "DTSTAMP:" + now,
                "DTSTART:" + start.format(UTC_BASIC),
                "DTEND:" + end.format(UTC_BASIC),
                "SUMMARY:MedSimple appointment",
                "DESCRIPTION:" + escape(a.getReason() != null ? a.getReason() : "Follow-up appointment"),
                "LOCATION:" + escape(location),
                "STATUS:CONFIRMED",
                "END:VEVENT",
                "END:VCALENDAR") + "\r\n";
    }

    // iCalendar text escaping (RFC 5545): backslash, semicolon, comma, and newlines.
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
