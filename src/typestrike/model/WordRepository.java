package typestrike.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Vocabulary is a bundled resource, so the JAR works outside the source directory. */
public final class WordRepository {
    private final List<String> words;

    public WordRepository() {
        InputStream stream = getClass().getResourceAsStream("/words.txt");
        if (stream == null) throw new IllegalStateException("Missing bundled words.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            words = reader.lines().map(String::trim).filter(s -> s.matches("[a-z]{3,10}"))
                    .distinct().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load vocabulary", e);
        }
        if (words.size() < 30) throw new IllegalStateException("At least 30 different words are required");
    }

    public String nextWord(Random random, Set<String> excluded) {
        List<String> available = new ArrayList<>();
        for (String word : words) if (!excluded.contains(word)) available.add(word);
        if (available.isEmpty()) throw new IllegalStateException("Vocabulary exhausted");
        return available.get(random.nextInt(available.size()));
    }

    public int size() { return words.size(); }
}
