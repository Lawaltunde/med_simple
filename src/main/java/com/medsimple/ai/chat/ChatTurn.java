package com.medsimple.ai.chat;

/** One turn in the symptom-chat conversation. role is "user" (patient) or "assistant". */
public record ChatTurn(String role, String content) {}
