package typestrike.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** เก็บคะแนนสามอันดับในเครื่อง โดยพาธไฟล์ไม่ขึ้นกับโฟลเดอร์ที่ใช้เปิดเกม */
public final class HighScoreStore {
    private final Path file;
    private final List<Integer> scores = new ArrayList<>();
    private String warning = "";

    public HighScoreStore(Path file) {
        this.file = file;
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    try {
                        int score = Integer.parseInt(line.trim());
                        if (score > 0) scores.add(score);
                    } catch (NumberFormatException ignored) { /* ข้ามรายการคะแนนที่อ่านเป็นตัวเลขไม่ได้ */ }
                }
                trim();
            }
        } catch (IOException | SecurityException e) {
            warning = "Cannot read records. This session can still be played.";
        }
    }

    private void trim() {
        scores.sort(Comparator.reverseOrder());
        if (scores.size() > 3) scores.subList(3, scores.size()).clear();
    }

    public boolean record(int score) {
        if (score <= 0) return true;
        scores.add(score);
        trim();
        Path temp = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, "records-", ".tmp");
            Files.write(temp, scores.stream().map(String::valueOf).toList(), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            warning = "";
            return true;
        } catch (IOException | SecurityException e) {
            warning = "Record could not be saved. Scores are kept for this session.";
            return false;
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException | SecurityException ignored) { }
            }
        }
    }

    public List<Integer> getScores() { return List.copyOf(scores); }
    public int getBest() { return scores.isEmpty() ? 0 : scores.get(0); }
    public String getWarning() { return warning; }
}
