# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Until 1.0.0, minor versions may contain breaking API changes.

## [Unreleased]

## [0.1.0] - 2026-11-06

### Added
- `Modifier.melt()` for any composable, built on the `Modifier.Node` API.
- Style presets: `Drip`, `Wax`, `Goo`, `Slump`, `Wavy`, plus fully custom `MeltStyle`.
- Speed presets: `Slow`, `Normal`, `Fast`, `VeryFast`, plus custom `MeltSpeed`.
- `MeltConfig` with delay, easing, repeat mode (`Once`, `Restart`, `Reverse`), seed and reduced-motion support.
- Drips that follow the content's real bottom outline and take its colour.

[Unreleased]: https://github.com/gbilgeturk/compose-melt/compare/0.1.0...HEAD
[0.1.0]: https://github.com/gbilgeturk/compose-melt/releases/tag/0.1.0
