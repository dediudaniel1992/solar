# Backend

This project uses Kotlin and Spring Boot. The build is configured to create a Windows executable using the [org.beryx.jlink](https://github.com/beryx/badass-jlink-plugin) plugin.

## Building

Run the following command to create an executable installer (requires a JDK that includes the `jpackage` tool and Windows build environment):

```bash
./gradlew jpackage
```

The resulting `.exe` can be found in `build/jpackage`.
