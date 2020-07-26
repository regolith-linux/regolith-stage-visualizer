# Regolith Stage Visualizer

Generates a web page that lists all packages and their versions across the PPAs unstable, stable, and release.

## Build

```bash
$ ./gradlew shadowJar
```

## Run

```bash
$ java -jar build/libs/regolith-stage-visualizer-1.0-SNAPSHOT.jar
```
HTML page will be returned to stdout.