# Magi: Djinn RPG (Forge 1.20.1) — v0.3.0

A Magi-inspired Djinn RPG expansion built to live inside the Soulrend modpack (Epic Fight, Iron's Spells, Apotheosis), and to run without them.

## Build the jar
Needs JDK 17 and Gradle 8.1.1.
- **Local:** in this folder run `gradle build --no-daemon` (or `C:\gradle\gradle-8.1.1\bin\gradle build --no-daemon`). Jar: `build/libs/magi-0.3.0.jar`.
- **GitHub:** push this folder; the workflow in `.github/workflows/build.yml` builds it and uploads the jar as an artifact.

## Install
Drop the jar into the `mods` folder of a Forge 1.20.1 profile (remove older Magi jars). Install on both server and clients.

## Commands (op)
`/magi djinn grant|revoke|equip <player> <djinn>`, `unequip|list <player>`, `setlevel <player> <djinn> <level>`, `info <djinn>`

## Keys
**G** cast selected Djinn art, **H** next art, **J** Djinn signature ability (rebindable under Controls).

## Art
All item models/textures are generated: `python3 tools/model_gen.py previews/` (needs Python 3 + Pillow). Edit the script, rerun, rebuild.

## Docs
`docs/DESIGN.md` (architecture and plan), `docs/TESTING.md` (checklist), `CHANGELOG.md`.
