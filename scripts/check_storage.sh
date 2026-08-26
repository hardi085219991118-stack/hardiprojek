#!/usr/bin/env sh
# scripts/check_storage.sh
# Performs local storage checks helpful before choosing GRADLE_USER_HOME in Cloud Shell
set -euo pipefail

echo "== df -h (filesystems) =="
df -h || true

echo "\n== df -i (inode usage) =="
df -i || true

# Check typical gradle locations
echo "\n== du -sh ~/.gradle (if exists) =="
du -sh "$HOME/.gradle" 2>/dev/null || echo "~/.gradle not found or inaccessible"

echo "\n== du -sh .gradle (in repo) =="
du -sh .gradle 2>/dev/null || echo ".gradle not found in working directory"

# Recommend candidate mounts
CANDIDATES="/tmp ${HOME:-$HOME} $(pwd)"
printf "\nRecommended candidate free space:\n"
for d in $CANDIDATES; do
  if [ -d "$d" ]; then
    avail=$(df -Pk "$d" | awk 'NR==2 {print $4}')
    avail_h=$(df -hP "$d" | awk 'NR==2 {print $4}')
    printf "%s -> %s KB available (%s)\n" "$d" "$avail" "$avail_h"
  fi
done

printf "\nTo use the automated helper, run:\n  chmod +x gradlew-cloudshell scripts/check_storage.sh\n  ./scripts/check_storage.sh\n  ./gradlew-cloudshell clean assembleDebug --no-daemon --stacktrace\n"
