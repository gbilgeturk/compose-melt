# Contributing to compose-melt

Thanks for taking the time to contribute!

## Reporting bugs and requesting features

Open an [issue](https://github.com/gbilgeturk/compose-melt/issues/new/choose) using one of the templates.
For bugs, include the library version, device / API level, a minimal code snippet and, if possible,
a screenshot or screen recording. The effect is visual, so a recording helps a lot.

## Development setup

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew :sample:installDebug   # run the sample app on a device or emulator
./gradlew check                  # unit tests, lint and API check
```

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep the change focused; open an issue first for larger changes so we can agree on the approach.
3. Add or update unit tests in `melt/src/test` when you change behaviour.
4. Try the change in the sample app, ideally on an old (API 21–25) and a recent API level.
5. Update `README.md` and the `[Unreleased]` section of `CHANGELOG.md` when users would notice the change.
6. Make sure `./gradlew check` passes.

### Public API

The library uses Kotlin [explicit API mode](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors)
and the [binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator).
If you intentionally change the public API, run `./gradlew apiDump` and commit the updated
`melt/api/melt.api` file. Every public declaration needs KDoc.

### Code style

Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
(`kotlin.code.style=official`); `.editorconfig` configures the IDE accordingly.

## Releasing (maintainers)

1. Move the `[Unreleased]` entries in `CHANGELOG.md` under the new version and update the version in
   `README.md` and the default in `melt/build.gradle.kts`.
2. Tag the commit with the bare version (no `v` prefix, since JitPack uses the tag as the version):
   `git tag 0.2.0 && git push origin 0.2.0`.
3. Create a GitHub release from the tag with the changelog entries.
4. Open `https://jitpack.io/#gbilgeturk/compose-melt` and check the build log for the new tag.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By participating you agree to uphold it.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
