package com.medsimple.referral.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackSpecialtySuggestionAdapterTest {

    private FallbackSpecialtySuggestionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FallbackSpecialtySuggestionAdapter();
    }

    @Test
    void chestPain_suggestsCardiology() {
        assertThat(adapter.suggest("Recurrent chest pain on exertion").specialty())
                .isEqualTo("Cardiology");
    }

    @Test
    void skinRash_suggestsDermatology() {
        assertThat(adapter.suggest("Persistent skin rash on forearms").specialty())
                .isEqualTo("Dermatology");
    }

    @Test
    void migraine_suggestsNeurology() {
        assertThat(adapter.suggest("Frequent migraine with aura").specialty())
                .isEqualTo("Neurology");
    }

    @Test
    void unrecognized_defaultsToInternalMedicine() {
        assertThat(adapter.suggest("General fatigue and malaise").specialty())
                .isEqualTo("Internal Medicine");
    }

    @Test
    void reasoningIsAlwaysProvided() {
        assertThat(adapter.suggest("knee joint pain").reasoning()).isNotBlank();
    }

    @Test
    void nullSummary_defaultsToInternalMedicine() {
        assertThat(adapter.suggest(null).specialty()).isEqualTo("Internal Medicine");
    }
}
