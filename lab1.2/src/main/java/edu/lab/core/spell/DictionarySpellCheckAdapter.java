package edu.lab.core.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 适配内置小词典的拼写检查实现。
 */
public final class DictionarySpellCheckAdapter implements SpellCheckService {
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");

    private final Set<String> dictionary;
    private final Map<String, String> suggestions;

    public DictionarySpellCheckAdapter(Set<String> dictionary, Map<String, String> suggestions) {
        this.dictionary = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.dictionary.addAll(dictionary);
        this.suggestions = suggestions;
    }

    public static DictionarySpellCheckAdapter defaultEnglish() {
        Set<String> dict = Set.of(
                "a", "an", "and", "are", "as", "at", "be", "brown", "code", "contains", "day", "dog",
                "extra", "fox", "good", "hello", "is", "it", "jumps", "lazy", "line", "new", "of",
                "over", "quick", "spaces", "test", "the", "this", "today", "world", "write", "writing"
        );
        Map<String, String> fixes = Map.of(
            "recieve", "receive",
            "occured", "occurred",
            "itallian", "italian",
            "rowlling", "rowling"
        );
        return new DictionarySpellCheckAdapter(dict, fixes);
    }

    @Override
    public List<SpellCheckIssue> checkLines(List<String> lines) {
        List<SpellCheckIssue> issues = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = WORD.matcher(line);
            while (matcher.find()) {
                String word = matcher.group();
                String lower = word.toLowerCase(Locale.ROOT);
                if (!dictionary.contains(lower)) {
                    String suggestion = suggestions.getOrDefault(lower, word);
                    issues.add(new SpellCheckIssue(i + 1, matcher.start() + 1, word, suggestion));
                }
            }
        }
        return issues;
    }
}
