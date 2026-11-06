---
name: csc301-java-environment-setup
description: Set up and verify the Windows Java build environment for the CSC301 Monopoly project (Temurin JDK 25 LTS, JAVA_HOME/PATH, VS Code Java pack, AI-off workspace) and log it in AI-Usage-log.txt.
---

# CSC301 Monopoly - Software Environment Setup

Use this skill when the CSC301 Monopoly project needs its Java toolchain
installed, repaired or re-verified on a Windows computer (a new member's
laptop, a lab PC, or after a reinstall).

## Target environment

| Component | Choice | Why |
|---|---|---|
| JDK | Eclipse Temurin JDK 25 (LTS), winget id `EclipseAdoptium.Temurin.25.JDK` | Current LTS on the Oracle Java SE roadmap; free, TCK-certified OpenJDK build |
| Compile target | `javac --release 21` | Runs on the instructor's machine even if it has an older JDK; blocks newer language features |
| Editor | VS Code + Extension Pack for Java (`vscjava.vscode-java-pack`) | Compile, run, debug, inspect node links in the debugger |
| Version control | Git | Change history for the AI reflection ("what the group rewrote") |
| AI in editor | OFF (`chat.disableAIFeatures: true`, Copilot and inline suggestions off) | Brief 7.3: code that appears in no AI log counts as unlogged AI use. The Claude Project is the single AI of record |

## Files this skill uses (project folder C:\Claude\Monopoly)

- `setup\Setup-Environment.bat` - double-click launcher (bypasses the PowerShell execution policy for this one script only)
- `setup\Setup-Environment.ps1` - idempotent installer and verifier
- `setup\setup-verification.txt` - report written by the script
- `setup\Check-Java.bat` - run in a NEW session; writes `setup\java-path-check.txt` showing which Java PATH resolves first
- `.vscode\settings.json` - AI-off workspace settings and Java source/output paths (`code`, `bin`) and the pinned Temurin runtime

## Procedure

1. **Check what is already there (read-only).** List folder names under
   `C:\Program Files`, `C:\Program Files (x86)`, `%LOCALAPPDATA%\Programs`
   and `%USERPROFILE%\.vscode\extensions`. Look for `Eclipse Adoptium`,
   `Java`, `Zulu`, `Amazon Corretto`, `Microsoft\jdk-*`, `~\.jdks`, and JDKs on other drives (e.g. `F:\Android\jdk-17`, Android Studio). JREs
   bundled inside other apps (KNIME, PyCharm JBR) do not count: they have
   no `javac` and are not on PATH.
2. **Make sure the scripts exist** in `setup\` and that `.vscode\settings.json`
   exists with `"chat.disableAIFeatures": true`.
3. **Ask the user to run the script.** The AI cannot type into
   Windows terminals and cannot approve UAC. The user double-clicks
   `setup\Setup-Environment.bat` and approves the UAC prompt for the
   Temurin MSI. The script:
   - checks winget;
   - installs Temurin JDK 25 if no `jdk-25*` folder exists under
     `C:\Program Files\Eclipse Adoptium`;
   - sets `JAVA_HOME` (user scope) and prepends `%JAVA_HOME%\bin` to the
     user PATH;
   - installs `vscjava.vscode-java-pack` with the VS Code CLI;
   - checks the AI-off workspace settings;
   - runs `java -version`, `javac -version`, `where java`, `git --version`;
   - compiles and runs `EnvCheck.java` with `--release 21` and expects
     the line `ENVCHECK|OK|<version>`.
4. **Verify in a fresh session.** The installer prepends the JDK to its own session PATH, so its `where java` result is not proof. Run `setup\Check-Java.bat` after reopening Explorer/terminals; Temurin 25 must be listed first.
5. **Verify the report.** Read `setup\setup-verification.txt`. Pass criteria:
   - `java` and `javac` both report version 25.x;
   - `where java` lists the Temurin path first (no Oracle `javapath`
     shim or older JDK ahead of it);
   - the extension list contains `vscjava.vscode-java-pack` (and its
     members such as `redhat.java`);
   - the smoke test prints `ENVCHECK|OK|25...`.
6. **Pin VS Code to Temurin.** Set `java.configuration.runtimes` (JavaSE-25, default) and `java.jdt.ls.java.home` in `.vscode\settings.json`, so another JDK on PATH is never used.
7. **Finish VS Code by hand.** In the Extensions view, disable the
   Claude Code and ChatGPT/Codex extensions with **Disable (Workspace)**
   for this folder. Restart VS Code so PATH and settings reload.
8. **Log it.** Append the prompt, actions, results and any failures to
   `C:\Claude\Monopoly\AI-Usage-log.txt` under the heading
   **SOFTWARE ENVIRONMENT SETUP**, and update the log index row.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `winget` not found | App Installer missing or outdated | Install or update "App Installer" from the Microsoft Store |
| `javac` not recognised after install | Old console still open | Close and reopen the terminal and VS Code |
| `where java` shows another Java first | Older JDK or Oracle `javapath` earlier on the machine PATH | Remove the older entry from the System PATH, or uninstall the old JDK |
| Script blocked | Execution policy | Run through the `.bat` launcher, not the `.ps1` directly |
| winget fails behind a proxy | Corporate network | Download the Temurin 25 MSI from adoptium.net and install with "Set JAVA_HOME" and "Add to PATH" ticked |
| VS Code shows "unknown setting chat.disableAIFeatures" | Older VS Code | Update VS Code, or rely on the Copilot and inline-suggestion settings |

## Rules

- Do not install any banned or extra libraries. The game uses only the
  JDK, plus `java.util` ArrayList/Scanner/Random/Collections and `java.io`.
- Do not generate game code as part of this skill.
- Every run of this skill is an AI-assisted activity and must be logged.

## References

- Oracle Java SE Support Roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- Eclipse Adoptium installation guide: https://adoptium.net/installation/
- VS Code AI settings reference: https://code.visualstudio.com/docs/agents/reference/ai-settings
- CSC301 Monopoly Assignment brief, Sections 4.2, 7.3, 9
