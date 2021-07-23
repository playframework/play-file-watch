## Releasing

This is released from the `main` branch (from version 1.1.13). Unless an older version needs patching, then it must be released from the maintenance branch, for instance `1.1.x` branch. If there is no maintenance branch for the release that needs patching, create it from the tag.

## Cutting the release

### Requires contributor access

- Check the [draft release notes](https://github.com/playframework/play-file-watch/releases) to see if everything is there
- Wait until [main branch build finished](https://travis-ci.com/github/playframework/play-file-watch/builds) after merging the last PR
- Update the [draft release](https://github.com/playframework/play-file-watch/releases) with the next tag version (eg. `2.2.0`), title and release description
- Check that Travis CI release build has executed successfully (Travis will start a [CI build](https://travis-ci.com/github/playframework/play-file-watch/builds) for the new tag and publish artifacts to Sonatype)

### Check Maven Central

- The artifacts will become visible at https://repo1.maven.org/maven2/com/lightbend/play/
