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
2. **API Endpoints:**
  - To interact with the API, send HTTP requests to the following endpoints:

    - `GET /download-zip/{owner}/{repository}?branch={branch}&folder={folder}`
      - Retrieves the zipped file of the GitHub folder you requested.
      - Requires additional parameters:
        - `branch`: Specify the branch of the repository.
        - `folder`: Specify the folder within the repository.

   Example:
   ```http request
   GET /download-zip/octocat/Hello-World?branch=main&folder=src
   ```

## TODO

- [x] Traverse directories and collect all files from subdirectory
- [x] Download all files (concurrently! ⚡️)
- [x] Store them in ZIP file with correct permisisons! 🎱
- [ ] Figure out why Detekt refuses to mark unused Sequences and Flows (non-terminal operators only) even when
  the https://detekt.dev/docs/rules/potential-bugs#ignoredreturnvalue inspection is on by default
- [ ] Write a blogpost about how Apache Commons Compress uses Octals to specify UNIX permissions
- [ ] Contribute to Apache Commons Compress Documentation(?)
- [x] Wire up endpoint to serve actual ZIP file
- [ ] Make sure ZIP file is stored in memory (rather than on disk)
- [ ] Introduce allowlist for which repositories can be downloaded from
- [ ] Introduce caching of generated ZIP files
- [ ] Ensure that multiple requests for the same ZIP file don't cause parallel (re)generation
- [ ] Provide diagnostic UI that shows which ZIP files are already cached
- [ ] (Maybe) keep individual files in cache (keyed on their SHA, available from the GitHub API)
