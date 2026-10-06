# IMentia — Architecture

Dementia assistance app: recognizes familiar faces through a camera and shows
patients who someone is (name, relationship, past meeting notes), with spoken
readouts. Java 17, Maven, Swing desktop app. Offline-first.

## Stack

| Concern       | Choice                                                                 |
|---------------|------------------------------------------------------------------------|
| Language/build| Java 17, Maven (`pom.xml`), jar packaging                              |
| UI            | Swing, hand-built in `MainPanel.buildFormUI()` (see note below)        |
| Face detect   | Haar cascade (`haarcascade_frontalface_default.xml` in resources)       |
| Face recognize| LBPH via JavaCV `opencv-platform:4.9.0-1.5.10` (all-OS bundle, ~1GB)  |
| Speech        | Windows built-in `System.Speech` via PowerShell subprocess, async      |
| Logging/tests | SLF4J + Logback, JUnit 5 + AssertJ (`mvn test`, ~20 tests)             |

**UI note:** `src/main/java/ui/MainPanel.form` exists but is REFERENCE ONLY.
Since commit `b73cd2a` the whole component tree is constructed in code
(`buildFormUI()`, generated from the form). Any compiler works; never rely on
IntelliJ GUI Designer injection.

## Module map

```
Main.java                     JFrame bootstrap, shutdown hook
ui/MainPanel.java             ~1,340 lines: screens, camera actions, dialogs (TO BE SPLIT, Phase 1)
ui/VideoProcessor.java        Camera thread, Haar detection, face-rect tracking
ui/CameraLifecycleManager.java Start/stop guard for the camera
ui/AnimatedBackgroundPanel.java Start-screen background
ui/AbstractMainPanel.java     Panel contract (getPanel, refresh, capture, delete…)
service/PersonRecognitionManager.java  FACADE: sole entry point for UI data ops
service/FaceRecognitionService.java    LBPH train/recognize, model persistence
service/SpeechService.java             Async TTS, silent fallback, shutdown()
people/Person.java            id, name, relationship, image, latest conversation
people/MeetingRecord.java     File-backed meeting notes (create/append/read)
util/PersonDataManager.java   CSV persistence (Person_File.csv)
util/FileHandler.java         DATA_FOLDER, capitalize, generateId, CSV quote/parse
util/ImageHandler.java        PNG <-> Mat file IO
util/ImageUtils.java          Mat<->BufferedImage, preprocessFace (single choke point)
util/exceptions/              PersonSaveException (+AlreadyExists), NoCamException
```

**Rule: UI code must only call `service/*`.** Nothing outside `service/`
touches `imentia_data/`, OpenCV, or recognition. (Two legacy `"imentia_data"`
literals remain in `MainPanel`; Phase 1 moves them behind the manager.)

## Data flow

```
Camera (VideoProcessor thread, Haar detection every 4th frame)
  -> clamped face Rect + frame clone
  -> CapturePhotoButton: 5-sample burst (~250ms apart, progress dialog)
  -> PersonRecognitionManager.recognizeFace(first sample)
       -> FaceRecognitionService.recognize(preprocessFace(crop)) -> person?
  -> hit: details screen + SpeechService "This is X, your Y."
  -> miss: confirm dialog -> PersonFormPanel -> registerNewPerson(List<Mat>)
       -> CSV append + PNGs + full retrain + model persist
```

## On-disk format (`imentia_data/`, git-ignored, CWD-relative)

- `Person_File.csv`: `id,name,relationship` per line. Fields with `,`/`"`/newline
  are double-quoted (`FileHandler.escapeCsv`/`parseCsvLine`). Ids look like `Person3`.
- `saved_faces/<id>.png`: display photo + first training sample.
- `saved_faces/<id>_1.png`, `<id>_2.png`, …: burst training samples.
- `Meeting_Notes/<id>.txt`: blocks delimited by `----- NOTE START -----` /
  `----- NOTE END -----`, each with date, 12h time, blank line, body.
- `LBPH_model.yml` + `LBPH_model.manifest`: persisted recognizer + fingerprint
  (`id|file|size|mtime` per image, training order). `tryLoadModel()` skips
  retraining only on exact manifest match; labels always align with
  `trainedPersons` order (imageless persons get NO label).

## Recognition invariants (do not break)

1. `preprocessFace()` (grayscale → 100×100 → equalizeHist) runs on BOTH train
   and recognize inputs. Preprocessing must never differ between the paths.
2. `trainedPersons[i]` is ALWAYS the person for recognizer label `i`.
3. Confidence threshold is a fixed 100.0 (`CONFIDENCE_THRESHOLD`).
4. `SpeechService` must never throw into UI code paths; shutdown on exit.

## Build / run / test

- IntelliJ: open `pom.xml`, Run `Main` (any build works since `b73cd2a`).
- Maven: `mvn test` (needs JDK 17 + `JAVA_HOME`; first run downloads OpenCV).
- Automation MUST build in a scratch copy (e.g. `Temp/opencode/IMentia-build`),
  never into the working `target/` — it would clobber IntelliJ outputs.
- Tests (`src/test/...`, 8 files): FileHandler (generateId, CSV quote/parse),
  PersonDataManager (CSV round-trip incl. comma names), MeetingRecord (note
  round-trip), ImageUtils (preprocess geometry), FaceRecognitionModel
  (train→persist→load→recognize on synthetic faces), PersonRecognitionManager
  (multi-sample register/recognize/duplicate/delete),
  MainPanelConstruction (full UI builds headlessly), SpeechService (wav synth).
