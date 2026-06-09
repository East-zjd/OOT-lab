package edu.lab.core.spell;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellCheckServiceFactoryTest {
    @AfterEach
    void cleanup() {
        System.clearProperty("spell.languagetool.endpoint");
        System.clearProperty("spell.languagetool.language");
    }

    @Test
    void fromSystemProperties_defaults_to_languagetool_http() {
        SpellCheckService svc = SpellCheckServiceFactory.fromSystemProperties();
        assertTrue(svc instanceof LanguageToolHttpAdapter);
    }

    @Test
    void fromSystemProperties_uses_custom_endpoint_and_language() {
        System.setProperty("spell.languagetool.endpoint", "https://example.com/check");
        System.setProperty("spell.languagetool.language", "en-GB");
        SpellCheckService svc = SpellCheckServiceFactory.fromSystemProperties();
        assertTrue(svc instanceof LanguageToolHttpAdapter);
    }
}
