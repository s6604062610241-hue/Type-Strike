package typestrike;

import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioSystem;
import typestrike.audio.SoundEffectPlayer;
import typestrike.audio.SoundEffectPlayer.Effect;

/** ตรวจไฟล์เสียงและจำลองการเล่น รวมกรณีมีคำสั่งรอขณะเปิดอุปกรณ์เสียง */
public final class SoundEffectPlayerTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static final class Line implements SoundEffectPlayer.Playback {
        int plays;
        boolean playing;
        final CountDownLatch closed;
        Line(CountDownLatch closed) { this.closed = closed; }
        public void play() { plays++; playing = true; }
        public void stop() { playing = false; }
        public void close() { playing = false; closed.countDown(); }
    }

    public static void main(String[] args) throws Exception {
        for (Effect effect : Effect.values()) {
            try (var raw = SoundEffectPlayerTest.class.getResourceAsStream(effect.resource)) {
                check(raw != null, "Bundled " + effect);
                try (var decoded = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                    var format = decoded.getFormat();
                    check(format.getChannels() == 1 && format.getSampleSizeInBits() == 16 &&
                            format.getSampleRate() == 22050 && !format.isBigEndian(), "Compact PCM " + effect);
                    double duration = decoded.getFrameLength() / format.getFrameRate();
                    check(duration > 0.05 && duration < 1.6, "Short complete cue " + effect);
                    byte[] samples = decoded.readAllBytes();
                    int peak = 0;
                    for (int i = 0; i + 1 < samples.length; i += 2) {
                        int sample = (short) ((samples[i] & 255) | (samples[i + 1] << 8));
                        peak = Math.max(peak, Math.abs(sample));
                    }
                    check(peak > 100 && peak <= 13763, "Non-silent, softened amplitude " + effect);
                }
            }
        }
        var lines = new EnumMap<Effect, Line>(Effect.class);
        var closed = new CountDownLatch(5);
        var player = new SoundEffectPlayer(effect -> {
            Line line = new Line(closed); lines.put(effect, line); return line;
        });
        player.awaitIdle();
        check(lines.size() == 5, "Load five clips once ahead of input");
        player.play(Effect.HIT); player.awaitIdle();
        check(lines.get(Effect.HIT).plays == 0, "Menu starts silent");
        player.setPaused(false);
        for (Effect effect : Effect.values()) { player.play(effect); player.awaitIdle(); }
        check(lines.values().stream().allMatch(line -> line.playing && line.plays == 1), "Dispatch all cues");
        player.play(Effect.HIT); player.awaitIdle();
        check(lines.get(Effect.HIT).plays == 2 && lines.size() == 5, "Reuse cached clip");
        player.toggleMuted(); player.awaitIdle();
        check(player.isMuted() && lines.values().stream().noneMatch(line -> line.playing), "Mute stops tails");
        player.play(Effect.HIT); player.awaitIdle();
        check(lines.get(Effect.HIT).plays == 2, "Discard muted events");
        player.setPaused(true); player.toggleMuted(); player.awaitIdle();
        player.play(Effect.HIT); player.awaitIdle();
        check(lines.get(Effect.HIT).plays == 2, "Unmute while paused stays silent");
        player.setPaused(false); player.awaitIdle();
        check(lines.get(Effect.HIT).plays == 2, "Resume does not replay missed cues");
        player.play(Effect.SLOW); player.awaitIdle();
        check(lines.get(Effect.SLOW).playing, "New cue after resume");
        player.setPaused(true); player.awaitIdle();
        check(lines.values().stream().noneMatch(line -> line.playing), "Pause stops tails");
        player.close(); player.close(); player.play(Effect.HIT);
        check(closed.await(5, TimeUnit.SECONDS), "Close all clips once; later requests harmless");

        var broken = new SoundEffectPlayer(effect -> { throw new IllegalStateException("No device"); });
        broken.awaitIdle(); broken.setPaused(false); broken.play(Effect.HIT); broken.awaitIdle();
        check(!broken.getWarning().isBlank(), "Hardware failure does not block game");
        broken.close();

        var opening = new CountDownLatch(1);
        var proceed = new CountDownLatch(1);
        Line late = new Line(new CountDownLatch(1));
        var racing = new SoundEffectPlayer(effect -> {
            if (effect == Effect.HIT) {
                opening.countDown();
                if (!proceed.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                return late;
            }
            return new Line(new CountDownLatch(1));
        });
        check(opening.await(5, TimeUnit.SECONDS), "Load asynchronously");
        racing.setPaused(false); racing.play(Effect.HIT);
        racing.setPaused(true); racing.setPaused(false);
        proceed.countDown(); racing.awaitIdle();
        check(late.plays == 0, "Pause/resume during loading discards stale requests");
        racing.play(Effect.HIT); racing.awaitIdle();
        check(late.plays == 1, "Fresh event after loading works");
        racing.close();
        System.out.println("PASS: " + checks + " SFX checks; WAVs, cache, dispatch, mute, pause, stale events, cleanup and hardware failure.");
    }
}
