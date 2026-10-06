package service;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Offline text-to-speech for memory assistance (reads names and
 * relationships aloud). Uses the built-in Windows System.Speech voices via
 * PowerShell, so there are no extra dependencies and no network needed.
 * All failures degrade silently: the app must never break for lack of audio.
 */
public class SpeechService {
    /** Hard cap for one utterance so a stuck voice cannot pile up. */
    private static final long SPEAK_TIMEOUT_SECONDS = 30;

    private final ExecutorService audio = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "imentia-tts");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean enabled = true;
    private volatile Future<?> current;
    private volatile Process currentProcess;

    /** Queues text to be spoken, dropping anything still waiting. Never throws. */
    public void speak(String text) {
        if (!enabled || !isAvailable() || text == null || text.isBlank()) {
            return;
        }
        cancelCurrent();
        final String sentence = text.trim();
        current = audio.submit(() -> speakNow(sentence, null));
    }

    /**
     * Synthesizes to a WAV file instead of the speakers (used by tests and
     * future voice-note export). Returns true when a non-empty file results.
     */
    public boolean synthesizeToWav(String text, File wavFile) {
        if (!isAvailable() || text == null || text.isBlank() || wavFile == null) {
            return false;
        }
        try {
            return speakNow(text.trim(), wavFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("SpeechService unavailable: " + e.getMessage());
            return false;
        }
    }

    private boolean speakNow(String sentence, String wavPath) {
        String escaped = sentence.replace("'", "''");
        String output = (wavPath == null)
                ? "$s.Speak($t)"
                : "$s.SetOutputToWaveFile($o); $s.Speak($t); $s.SetOutputToDefaultAudioDevice()";
        String[] command = {
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "Add-Type -AssemblyName System.Speech; "
                        + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                        + "$t = '" + escaped + "'; "
                        + (wavPath == null ? "" : "$o = '" + wavPath.replace("'", "''") + "'; ")
                        + output
        };
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            currentProcess = process;
            // Drain output so the pipe cannot block the voice.
            try (var in = process.getInputStream()) {
                in.transferTo(java.io.OutputStream.nullOutputStream());
            }
            boolean done = process.waitFor(SPEAK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!done) {
                process.destroyForcibly();
                return false;
            }
            if (wavPath == null) {
                return process.exitValue() == 0;
            }
            return process.exitValue() == 0 && Files.size(new File(wavPath).toPath()) > 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.out.println("SpeechService unavailable: " + e.getMessage());
            return false;
        } finally {
            currentProcess = null;
        }
    }

    /** True on Windows with PowerShell; false everywhere else (stays silent). */
    public static boolean isAvailable() {
        String os = System.getProperty("os.name", "");
        return os.startsWith("Windows");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            cancelCurrent();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void cancelCurrent() {
        Future<?> job = current;
        if (job != null && !job.isDone()) {
            job.cancel(true);
        }
        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    /** Releases the speech thread. Safe to call multiple times. */
    public void shutdown() {
        cancelCurrent();
        audio.shutdownNow();
    }
}
