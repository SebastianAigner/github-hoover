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

## Installation

1. Clone this repository to your machine:

   ```shell
   git clone https://github.com/SebastianAigner/github-hoover.git
   ```

2. Change into the project directory:

   ```shell
   cd github-hoover
   ```

3. Build the project using Gradle:

   ```shell
   ./gradlew build
   ```

4. Run the application:

   ```shell
   ./gradlew run
   ```

5. The API will be accessible at `http://0.0.0.0:8080`.

## How to Use

1. **Set Up GitHub Token:**
  - Obtain a GitHub token from [here](https://github.com/settings/tokens).
  - Put your GitHub token in a file named `key.local` located in the root of this project.
- For production, set the `GITHUB_TOKEN` environment variable to your token.

2. **Set Up Allowlist**
    - You can find a default allowlist by navigating to `http://localhost:8080/defaultAllowlist`.
    - Set the `HOOVER_ALLOWLIST` environment variable to the repositories for which you'd like to enable downloads.
    - If you'd like to allow all repositories under a given user or all paths under a given repository, leave those
      fields blank.
    - Example: `[{"user":"SebastianAigner","name":"","branch":"","path":""}]`
2. **API Endpoints:**
  - To interact with the API, send HTTP requests to the following endpoints:

      - `GET /download-zip/repo?user=JetBrains&name=amper&branch=0.1&path=/examples/compose-ios`
      - Retrieves the zipped file of the GitHub folder you requested.
      - Requires additional parameters:
        - `branch`: Specify the branch of the repository.
        - `folder`: Specify the folder within the repository.

   Example:
   ```http request
   GET /download-zip/repo?user=JetBrains&name=amper&branch=0.1&path=/examples/compose-ios
   ```

## TODO

- [x] Traverse directories and collect all files from subdirectory
- [x] Download all files (concurrently! ⚡️)
- [x] Store them in ZIP file with correct permisisons! 🎱
- [x] Wire up endpoint to serve actual ZIP file
- [x] Make sure ZIP file is created in memory (rather than on disk)
- [x] Explore if Ktor's typesafe routing allows us to specify nicer API endpoints?
- [x] Introduce allowlist for which repositories can be downloaded from
- [x] Introduce caching of generated ZIP files
- [x] Make sure that caching both API requests AND ZIP files don't result in us never updating the caches when new
  commits are available in the repo (fixed by evicting cache after time period)
- [x] Ensure that multiple requests for the same ZIP file don't cause parallel (re)generation
- [ ] (Maybe) Provide diagnostic UI that shows which ZIP files are already cached
- [ ] (Maybe) keep individual files in cache (keyed on their SHA, available from the GitHub API)
- [ ] (Maybe) Figure out why Detekt refuses to mark unused Sequences and Flows (non-terminal operators only) even when
  the https://detekt.dev/docs/rules/potential-bugs#ignoredreturnvalue inspection is on by default
- [ ] (Maybe) Write a blogpost about how Apache Commons Compress uses Octals to specify UNIX permissions
- [ ] (Maybe) Contribute to Apache Commons Compress Documentation(?)
