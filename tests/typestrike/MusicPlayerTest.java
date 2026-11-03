package typestrike;

import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioSystem;
import typestrike.audio.MusicPlayer;

/** จำลองการเล่นเสียงเพื่อตรวจคำสั่ง โดยไม่ส่งเสียงจริงและไม่ต้องมีอุปกรณ์บนเครื่องทดสอบ */
public final class MusicPlayerTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static final class Line implements MusicPlayer.Playback {
        int starts, stops;
        boolean playing;
        final CountDownLatch closed;
        Line(CountDownLatch closed) { this.closed = closed; }
        public void startLoop() { starts++; playing = true; }
        public void stop() { stops++; playing = false; }
        public void close() { playing = false; closed.countDown(); }
    }
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"menu", "battle"}) {
            try (var raw = MusicPlayerTest.class.getResourceAsStream("/audio/" + name + ".wav")) {
                check(raw != null, "Music bundled: " + name);
                try (var decoded = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                    check(decoded.getFormat().getSampleSizeInBits() == 16 && decoded.getFormat().getChannels() == 1,
                            "Music decodes as compact PCM WAV");
                    check(decoded.getFrameLength() / decoded.getFormat().getFrameRate() > 40,
                            "Complete track rather than a missing/truncated sample");
                }
            }
        }
        var lines = new EnumMap<MusicPlayer.Track, Line>(MusicPlayer.Track.class);
        var closed = new CountDownLatch(2);
        MusicPlayer music = new MusicPlayer(track -> {
            Line line = new Line(closed); lines.put(track, line); return line;
        });
        music.play(MusicPlayer.Track.MENU); music.awaitIdle();
        Line menu = lines.get(MusicPlayer.Track.MENU);
        check(menu.playing && menu.starts == 1, "Menu starts looping");
        music.toggleMuted(); music.awaitIdle();
        check(music.isMuted() && !menu.playing, "Mute actually stops output");
        music.play(MusicPlayer.Track.BATTLE); music.awaitIdle();
        check(lines.size() == 1 && !menu.playing, "Muted screen changes remain silent and avoid loading");
        music.toggleMuted(); music.awaitIdle();
        Line battle = lines.get(MusicPlayer.Track.BATTLE);
        check(!menu.playing && battle.playing, "Unmute selects current screen music");
        music.setPaused(true); music.awaitIdle();
        check(!battle.playing, "Pause stops output");
        music.toggleMuted(); music.toggleMuted(); music.awaitIdle();
        check(!battle.playing, "Unmute while paused never resumes playback");
        music.setPaused(false); music.awaitIdle();
        check(battle.playing && battle.starts == 2, "Resume restarts the same open line");
        music.play(MusicPlayer.Track.MENU); music.awaitIdle();
        check(menu.playing && !battle.playing && lines.size() == 2, "One track at a time; cached lines reused");
        music.close(); music.close();
        check(closed.await(5, TimeUnit.SECONDS) && !menu.playing && !battle.playing, "Dispose closes both clips; idempotent");

        MusicPlayer broken = new MusicPlayer(track -> { throw new IllegalStateException("No audio device"); });
        broken.play(MusicPlayer.Track.MENU); broken.awaitIdle();
        check(!broken.getWarning().isBlank(), "Unavailable hardware degrades gracefully");
        broken.toggleMuted(); broken.toggleMuted(); broken.awaitIdle(); broken.close();

        CountDownLatch opening = new CountDownLatch(1), proceed = new CountDownLatch(1);
        Line late = new Line(new CountDownLatch(1));
        MusicPlayer racing = new MusicPlayer(track -> {
            opening.countDown();
            if (!proceed.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
            return late;
        });
        racing.play(MusicPlayer.Track.MENU);
        check(opening.await(5, TimeUnit.SECONDS), "Device open started asynchronously");
        racing.toggleMuted(); proceed.countDown(); racing.awaitIdle();
        check(late.starts == 0, "Mute during a slow device open cannot leak music");
        racing.close();
        System.out.println("PASS: " + checks + " music checks; real WAV decoding, track routing, mute, pause, cleanup and device failure.");
    }
}
