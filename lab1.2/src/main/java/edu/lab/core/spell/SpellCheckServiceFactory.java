package edu.lab.core.spell;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * 拼写检查服务工厂。
 */
public final class SpellCheckServiceFactory {
    private static final String ENDPOINT_KEY = "spell.languagetool.endpoint";
    private static final String LANGUAGE_KEY = "spell.languagetool.language";

    private SpellCheckServiceFactory() {
    }

    public static SpellCheckService fromSystemProperties() {
        String endpoint = System.getProperty(ENDPOINT_KEY, "https://api.languagetool.org/v2/check");
        String language = System.getProperty(LANGUAGE_KEY, "en-US");
        return new LanguageToolHttpAdapter(URI.create(endpoint), HttpClient.newHttpClient(), language);
    }
}
