#!/usr/bin/env sh
# Lightweight Gradle 8.13 bootstrap. Downloads from services.gradle.org only if needed.
set -eu
GRADLE_VERSION="8.13"

if [ -n "${GRADLE_HOME:-}" ] && [ -x "${GRADLE_HOME}/bin/gradle" ]; then
    exec "${GRADLE_HOME}/bin/gradle" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual"
INSTALL_DIR="$BASE/gradle-$GRADLE_VERSION"
ZIP_PATH="$BASE/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -x "$INSTALL_DIR/bin/gradle" ]; then
    mkdir -p "$BASE"
    if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
        echo "Instale Gradle $GRADLE_VERSION, curl e unzip, ou use Android Studio." >&2
        exit 1
    fi
    curl --fail --location --silent --show-error "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" --output "$ZIP_PATH"
    unzip -q "$ZIP_PATH" -d "$BASE"
    rm -f "$ZIP_PATH"
fi
exec "$INSTALL_DIR/bin/gradle" "$@"
