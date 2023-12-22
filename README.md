# Hoover: Download GitHub directories without checking out the whole repository

## Motivation
- Big repos sometimes have interesting bits in it (e.g. sample projects) that are independent of the larger project.
- There is no convenient way to download these subdirectories via just a simple link

## Approach

- Use GitHub API to retrieve individual files from subdirectory
- Use Java's Standard Library Magic™ to zip them up and serve them at an endpoint

## Important characteristics
- Preserve file permissions (e.g. `gradlew` will remain executable)
- Support for branch names with slashes

## How to use this

- Put [your GitHub token](https://github.com/settings/tokens) in a file called `key.local` in the root of this project
- Run the main function from `Routing.kt`
- Or use the webinterface: `http://0.0.0.0:8080/zip/repo?user=JetBrains&name=compose-multiplatform&branch=master&path=/`

## TODO

- [x] Traverse directories and collect all files from subdirectory
- [x] Download all files (concurrently! ⚡️)
- [x] Store them in ZIP file with correct permisisons! 🎱
- [ ] Figure out why Detekt refuses to mark unused Sequences and Flows (non-terminal operators only) even when
  the https://detekt.dev/docs/rules/potential-bugs#ignoredreturnvalue inspection is on by default
- [ ] Write a blogpost about how Apache Commons Compress uses Octals to specify UNIX permissions
- [ ] Contribute to Apache Commons Compress Documentation(?)
- [ ] Wire up endpoint to serve actual ZIP file
- [ ] Make sure ZIP file is stored in memory (rather than on disk)
- [ ] Introduce allowlist for which repositories can be downloaded from
- [ ] Introduce caching of generated ZIP files
- [ ] Ensure that multiple requests for the same ZIP file don't cause parallel (re)generation
- [ ] Provide diagnostic UI that shows which ZIP files are already cached
- [ ] (Maybe) keep individual files in cache (keyed on their SHA, available from the GitHub API)
