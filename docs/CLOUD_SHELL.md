# Cloud Shell build instructions for the project

This file describes how to safely build the project in Google Cloud Shell (or any environment with limited $HOME space).

Steps:
1. Run the storage check script to inspect available space and inode usage:
   chmod +x scripts/check_storage.sh
   ./scripts/check_storage.sh

2. The script reports candidate directories and available space. If you confirm a directory (for example /tmp) has sufficient free space, run the helper wrapper which will automatically pick the best location and set GRADLE_USER_HOME:
   chmod +x gradlew-cloudshell
   ./gradlew-cloudshell clean assembleDebug --no-daemon --stacktrace

3. Alternatively, you may manually set GRADLE_USER_HOME to a path on a filesystem with sufficient space (e.g. /tmp/gradle-cache):
   export GRADLE_USER_HOME=/tmp/gradle-cache
   mkdir -p "$GRADLE_USER_HOME"
   ./gradlew clean assembleDebug --no-daemon --stacktrace

Notes and recommendations:
- The helper will pick among /tmp, $HOME, and the current working directory the filesystem with most available space.
- The helper does NOT change global permissions on your system; it creates the cache directory with default ownership and permissions.
- Ensure Java 17 is installed and available before building. Verify with:
   java -version
   ./gradlew --version

- The project intentionally keeps signing configuration out of the repo; do NOT commit keystore or google-services.json files.
