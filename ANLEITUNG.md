# ⚓ Ports of Call Android-Export & APK-Compilation

In diesem Export-Paket findest du alles, was du brauchst, um deine Retro-Handelssimulation als eigenständige App auf deinem Android-Smartphone zu spielen!

Es gibt **zwei Wege**, um an deine fertige `.apk` Installationsdatei zu kommen:
* **Variante A (Ohne PC / Keine Installation)**: Über die automatische **GitHub Actions Cloud-Kompilierung** (Empfohlen, wenn du kein Android Studio hast!).
* **Variante B (Für Entwickler)**: Lokal über **Android Studio** auf deinem PC.

---

## ☁️ Variante A: Kostenlos & Automisch in der Cloud (Über GitHub Actions)

Mit dieser Methode nutzt du das mitgelieferte Toolchain-Skript. GitHub compiles deine App auf ihren Servern und stellt dir die fertige Installationsdatei direkt als Download zur Verfügung.

### Schritt-für-Schritt Anleitung:

1. **Projekt herunterladen**:
   * Klicke oben rechts im AI Studio auf das Einstellungs-Zahnrad und wähle **Export to ZIP**.
   * Entpacke die ZIP-Datei auf deinem Computer. Du findest den Android-Code im Ordner `android_project`.

2. **Kostenloses GitHub Repository erstellen**:
   * Gehe auf [github.com](https://github.com) (erstelle einen kostenlosen Account, falls du noch keinen hast).
   * Klicke auf **New** (Neues Repository erstellen).
   * Schalte das Repository auf **Public** oder **Private** und klicke auf **Create Repository**.

3. **Dateien hochladen**:
   * Du kannst entweder das **komplette entpackte ZIP-Archiv** (mit allen Dateien) hochladen oder **nur den Inhalt des Ordners `android_project`**. Beides funktioniert einwandfrei!
   * *Hintergrund*: Unsere Actions-Konfig ist intelligent genug, um beide Ordnerstrukturen automatisch zu erkennen, in das richtige Verzeichnis zu navigieren und das APK erfolgreich zu kompilieren! Der Ordner `.github/workflows/android-build.yml` muss sich dabei in der hochgeladenen Struktur befinden.

4. **Der Cloud-Build startet automatisch**:
   * Sobald die Dateien hochgeladen sind, wechselst du auf GitHub auf den Tabulator **Actions**.
   * Du siehst dort einen gelben Punkt der blinkt – das ist der Cloud-Server, der gerade dein Android-Projekt kompiliert! Das dauert ca. 2 bis 3 Minuten.
   * Sobald der Punkt grün wird, klicke auf den Build-Namen (z.B. "Build Android APK").

5. **APK herunterladen & installieren**:
   * Scrolle nach unten zum Bereich **Artifacts** (Artefakte).
   * Dort findest du eine Datei namens `Ports-Of-Call-Retro-Game`. Klicke darauf, um sie herunterzuladen.
   * Entpacke die ZIP-Datei auf deinem PC oder sende sie direkt an dein Android-Handy.
   * Drücke auf die entpackte `.apk` Datei auf deinem Handy, um das Spiel zu installieren. (Falls dein Handy warnt, dass es sich um eine unbekannte Quelle handelt, klicke auf "Trotzdem installieren", da es deine eigene selbstkompilierte App ist!).

---

## 🛠️ Variante B: Lokal über Android Studio importieren

Wenn du Android Studio auf deinem Rechner installiert hast oder es ausprobieren möchtest:

### Voraussetzungen:
1. **Android Studio** (Hedgehog 2023.1.1 oder neuer)
2. **Java JDK 17** oder das in Android Studio eingebaute Gradle JDK.

### Schritt-für-Schritt Import-Prozess:

1. **Projekt öffnen**:
   * Starte Android Studio.
   * Wähle **Open** und navigiere zum Ordner `android_project`. Klicke auf **OK**.
   * Android Studio erkennt das Projekt automatisch und beginnt mit der Synchronisation der Gradle-Abhängigkeiten.

2. **Projektdateien im Editor studieren**:
   * **MainActivity.kt** (`app/src/main/java/com/example/portsofcall/MainActivity.kt`): Der Systemübergang und Initialisierung der nativen Sound-Engine.
   * **GameViewModel.kt** (`app/src/main/java/com/example/portsofcall/GameViewModel.kt`): Die gesamte Spiellogik, Reittabellen, Finanzen, Bunkern und Schiffsfahrten.
   * **PortsOfCallApp.kt** (`app/src/main/java/com/example/portsofcall/PortsOfCallApp.kt`): Die Benutzeroberfläche in modernem Jetpack Compose.
   * **SoundPlayer.kt** (`app/src/main/java/com/example/portsofcall/SoundPlayer.kt`): Die prozedurale Klangsynthese, die authentische Retro-Signale über den Lautsprecher erzeugt!

3. **APK manuell lokal erstellen**:
   * Klicke oben im Hauptmenü von Android Studio auf **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   * Nach ca. 1 Minute erscheint unten rechts eine Meldung. Klicke auf **locate**, um den Ordner mit deiner fertigen `app-debug.apk` zu öffnen!

4. **Kabellose Installation (USB-Debugging)**:
   * Verbinde dein Android-Handy per USB-Kabel mit dem PC.
   * Schalte den Entwicklermodus und USB-Debugging auf dem Handy frei.
   * Klicke in Android Studio auf den grünen **Play-Pfeil**, um die App direkt auf deinem Handy zu starten und zu debuggen!

Viel Spaß beim Spielen deines eigenen Reederei-Klassikers auf dem Handy! ⚓🚢
