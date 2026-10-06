package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** File synthesis needs no speakers: verifies the offline voice works. */
class SpeechServiceTest {

    private final SpeechService speech = new SpeechService();

    @AfterEach
    void shutdown() {
        speech.shutdown();
    }

    @Test
    void synthesizeToWav_producesAudio(@TempDir Path temp) {
        assumeTrue(SpeechService.isAvailable(), "Windows speech only");
        File wav = temp.resolve("hello.wav").toFile();
        assertThat(speech.synthesizeToWav("This is Maria, your daughter.", wav)).isTrue();
        assertThat(wav.isFile()).isTrue();
        assertThat(wav.length()).isGreaterThan(1024);
    }

    @Test
    void speak_ignoresBlankAndDisabled() {
        speech.speak(null);
        speech.speak("   ");
        speech.setEnabled(false);
        speech.speak("Should never be queued.");
        assertThat(speech.isEnabled()).isFalse();
    }
}
