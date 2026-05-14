# MarisHomes

Folia/Bukkit homes plugin targeting Spigot API `26.1.2-R0.1-SNAPSHOT`.

## Build

Requires JDK 25 and Gradle 9.1.0.

```bash
gradle build
```

The build compiles with Java toolchain 25 and `--release 21` for modern Minecraft server compatibility.

## Runtime libraries

Libraries are declared in `plugin.yml` and are not shaded into the plugin jar:

- `com.zaxxer:HikariCP:7.0.2