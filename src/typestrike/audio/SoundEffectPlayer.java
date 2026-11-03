package typestrike.audio;

import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** เก็บเสียงสั้นทั้งห้าไว้ใช้ซ้ำ โดยแยกระบบเสียงออกจากกติกาเกม */
public final class SoundEffectPlayer implements AutoCloseable {
    public enum Effect {
        HIT("hit"), ARMOR("armor"), DAMAGE("damage"), SLOW("slow"), BLAST("blast");
        public final String resource;
        Effect(String name) { resource = "/audio/sfx/" + name + ".wav"; }
    }

    public interface Playback extends AutoCloseable {
        void play();
        void stop();
        @Override void close();
    }
    @FunctionalInterface public interface Factory { Playback open(Effect effect) throws Exception; }

    private final Map<Effect, Playback> sounds = new EnumMap<>(Effect.class);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "type-strike-sfx");
        thread.setDaemon(true);
        return thread;
    });
    private boolean muted;
    private boolean paused = true;
    private boolean closed;
    // ยกเลิกคำสั่งเสียงที่รออยู่ เมื่อพัก ปิดเสียง เปลี่ยนหน้า หรือปิดเกม
    private long generation;
    private volatile String warning = "";

    public SoundEffectPlayer() { this(SoundEffectPlayer::openClip); }

    public SoundEffectPlayer(Factory factory) {
        Objects.requireNonNull(factory);
        // โหลดเสียงแต่ละไฟล์ไว้ครั้งเดียว โดยแยกงานโหลดออกจากเธรดหน้าจอ
        worker.execute(() -> {
            for (Effect effect : Effect.values()) {
                synchronized (this) { if (closed) return; }
                try { sounds.put(effect, factory.open(effect)); }
                catch (Exception failure) { warning = "Some sound effects are unavailable on this device"; }
            }
        });
    }

    private static Playback openClip(Effect effect) throws Exception {
        if (Boolean.getBoolean("typestrike.audio.disabled")) return new Playback() {
            public void play() { }
            public void stop() { }
            public void close() { }
        };
        var resource = SoundEffectPlayer.class.getResourceAsStream(effect.resource);
        if (resource == null) throw new IllegalStateException("Missing " + effect.resource);
        Clip clip = AudioSystem.getClip();
        try (var stream = new BufferedInputStream(resource);
             var decoded = AudioSystem.getAudioInputStream(stream)) {
            clip.open(decoded);
            // ไฟล์เสียงที่รวมไว้ลดระดับเสียงลง 30% แล้ว
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                var gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), -12f)));
            }
        } catch (Exception failure) { clip.close(); throw failure; }
        return new Playback() {
            public void play() {
                // เริ่มเสียงเดิมใหม่ แทนการเปิดเสียงชนิดเดียวกันซ้อนหลายชุด
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            }
            public void stop() { clip.stop(); }
            public void close() { clip.close(); }
        };
    }

    public synchronized void play(Effect effect) {
        Objects.requireNonNull(effect);
        if (closed || muted || paused) return;
        long requestedGeneration = generation;
        worker.execute(() -> {
            synchronized (this) {
                if (closed || muted || paused || generation != requestedGeneration) return;
                Playback sound = sounds.get(effect);
                if (sound != null) sound.play();
            }
        });
    }

    public synchronized void setPaused(boolean value) {
        if (closed || paused == value) return;
        paused = value;
        generation++;
        if (paused) worker.execute(() -> sounds.values().forEach(Playback::stop));
    }

    public synchronized void toggleMuted() {
        if (closed) return;
        muted = !muted;
        generation++;
        if (muted) worker.execute(() -> sounds.values().forEach(Playback::stop));
    }

    public synchronized boolean isMuted() { return muted; }
    public String getWarning() { return warning; }
    public void awaitIdle() throws Exception { worker.submit(() -> { }).get(5, TimeUnit.SECONDS); }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        generation++;
        worker.execute(() -> {
            sounds.values().forEach(sound -> { sound.stop(); sound.close(); });
            sounds.clear();
        });
        worker.shutdown();
    }
}
