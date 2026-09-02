# New-client

## Build the Windows launcher

The project uses Java 21 and JavaFX. A Windows runner can build a self-contained `.exe` installer with `jpackage`.

### GitHub Actions

1. Open the repository's **Actions** tab.
2. Select **Build Windows EXE**.
3. Choose **Run workflow**.
4. When the workflow finishes, download the `mapz-launcher-windows` artifact.

The generated installer is a Windows `.exe` and includes its Java runtime, so users do not need to install Java separately.

### Local Windows build

With JDK 21 and Gradle installed, run:

```text
gradle jpackage
```

The installer is generated under `build/jpackage/`.

> GitHub Codespaces are headless Linux environments, so the JavaFX launcher cannot be visually opened there. Use Windows for visual testing or the GitHub Actions build for packaging.
