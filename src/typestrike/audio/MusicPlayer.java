package typestrike.audio;

import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** ดูแลเพลงแยกจากกติกาเกม และโหลดเพลงโดยไม่ให้หน้าจอ Swing ต้องรอ */
public final class MusicPlayer implements AutoCloseable {
    public enum Track {
        MENU("/audio/menu.wav"), BATTLE("/audio/battle.wav");
        final String resource;
        Track(String resource) { this.resource = resource; }
    }
    public interface Playback extends AutoCloseable {
        void startLoop();
        void stop();
        @Override void close();
    }
    @FunctionalInterface public interface Factory { Playback open(Track track) throws Exception; }
    private final Factory factory;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "type-strike-music");
        thread.setDaemon(true);
        return thread;
    });
    // ให้งานเสียงที่ทำแยกจากหน้าจอ เป็นผู้ดูแลอุปกรณ์เสียงและสถานะเพลงที่เปิดไม่ได้
    private final Map<Track, Playback> lines = new EnumMap<>(Track.class);
    private final Set<Track> failed = EnumSet.noneOf(Track.class);
    private Playback active;
    private volatile String warning = "";
    private Track track;
    private boolean muted;
    private boolean paused;
    private boolean closed;

    public MusicPlayer() { this(MusicPlayer::openClip); }
    public MusicPlayer(Factory factory) { this.factory = java.util.Objects.requireNonNull(factory); }

    private static Playback openClip(Track track) throws Exception {
        if (Boolean.getBoolean("typestrike.audio.disabled")) return new Playback() {
            public void startLoop() { }
            public void stop() { }
            public void close() { }
        };
        var resource = MusicPlayer.class.getResourceAsStream(track.resource);
        if (resource == null) throw new IllegalStateException("Missing " + track.resource);
        Clip clip = AudioSystem.getClip();
        try (var stream = new BufferedInputStream(resource);
             var decoded = AudioSystem.getAudioInputStream(stream)) {
            var format = decoded.getFormat();
            if (format.getSampleSizeInBits() != 16 || format.isBigEndian()
                    || !format.getEncoding().equals(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED)) {
                throw new IllegalStateException("Expected bundled 16-bit little-endian PCM music");
            }
            byte[] samples = decoded.readAllBytes();
            reduceVolume(samples);
            clip.open(format, samples, 0, samples.length);
            if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                var gain = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), -15f)));
            }
        } catch (Exception failure) {
            clip.close(); throw failure;
        }
        return new Playback() {
            public void startLoop() { clip.loop(Clip.LOOP_CONTINUOUSLY); }
            public void stop() { clip.stop(); }
            public void close() { clip.close(); }
        };
    }

    /** ลดระดับเสียงลง 30% แม้อุปกรณ์จะไม่มีตัวควบคุมระดับเสียงให้ใช้ */
    private static void reduceVolume(byte[] samples) {
        for (int i = 0; i + 1 < samples.length; i += 2) {
            short original = (short) ((samples[i] & 255) | (samples[i + 1] << 8));
            short quieter = (short) Math.round(original * 0.7);
            samples[i] = (byte) quieter;
            samples[i + 1] = (byte) (quieter >> 8);
        }
    }

    public synchronized void play(Track value) {
        if (closed) return;
        track = value;
        paused = false;
        worker.execute(this::reconcile);
    }
    public synchronized void setPaused(boolean value) {
        if (closed || paused == value) return;
        paused = value;
        worker.execute(this::reconcile);
    }
    public synchronized void toggleMuted() {
        if (closed) return;
        muted = !muted;
        worker.execute(this::reconcile);
    }
    public synchronized boolean isMuted() { return muted; }
    public String getWarning() { return warning; }

    private void reconcile() {
        Track wanted;
        synchronized (this) { wanted = closed || muted || paused ? null : track; }
        Playback next = wanted == null ? null : lines.get(wanted);
        if (wanted != null && next == null && !failed.contains(wanted)) {
            try { next = factory.open(wanted); lines.put(wanted, next); }
            catch (Exception failure) { failed.add(wanted); warning = "Music unavailable on this device"; }
        }
        // ระหว่างเปิดอุปกรณ์ ผู้เล่นอาจสั่งปิดเสียง พัก หรือปิดเกมได้
        synchronized (this) {
            if (closed || muted || paused || wanted != track) next = null;
            if (active == next) return;
            if (active != null) active.stop();
            active = next;
            if (active != null) active.startLoop();
        }
    }

    /** ใช้ในการทดสอบ เพื่อรอให้งานเสียงที่ค้างอยู่ทำเสร็จ โดยไม่ต้องมีอุปกรณ์เสียงจริง */
    public void awaitIdle() throws Exception { worker.submit(() -> { }).get(5, TimeUnit.SECONDS); }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        worker.execute(() -> {
            if (active != null) active.stop();
            lines.values().forEach(Playback::close);
            lines.clear();
            active = null;
        });
        worker.shutdown();
    }
}
