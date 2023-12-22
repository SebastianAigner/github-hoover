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

- Put your GitHub token in a file called `key.local` in the root of this project
- Run the main function from `Routing.kt`
- Or use the webinterface: `http://0.0.0.0:8080/zip/repo?user=JetBrains&name=compose-multiplatform&branch=master&path=/`