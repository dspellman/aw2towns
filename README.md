# AW2 Town Prototype

Prototype abstract town economy mod inspired by Ancient Warfare 2.

This is a Minecraft 1.21.1 Architectury project with shared common code and Fabric/NeoForge platform loaders.

## Modules

- `common`: shared block, UI, registry, and town economy code.
- `fabric`: Fabric entrypoints and platform packaging.
- `neoforge`: NeoForge entrypoint and platform packaging.

## Build

```powershell
.\gradlew.bat build
```

Built release jars are emitted under:

- `fabric/build/libs/aw2towns-fabric-0.1.0.jar`
- `neoforge/build/libs/aw2towns-neoforge-0.1.0.jar`

## Development Notes

- Current Minecraft version: 1.21.1
- Current mod version: 0.1.0
- Current loaders: Fabric and NeoForge
- License: All Rights Reserved

## Publishing

CurseForge and Modrinth publishing should use generated API tokens stored as GitHub Actions secrets. Do not commit tokens, account passwords, launcher credentials, or local Prism instance paths.
