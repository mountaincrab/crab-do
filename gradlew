#!/bin/sh
#
# Gradle startup script for POSIX systems.
#
# Downloads the Gradle distribution on first run, then invokes GradleMain.
# The distribution is cached in ~/.gradle/wrapper/dists/ on subsequent runs.
#

set -e

die() {
    echo "ERROR: $1" >&2
    exit 1
}

# Locate java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ -x "$JAVACMD" ] || die "JAVA_HOME is set to an invalid directory: $JAVA_HOME"
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || die "JAVA_HOME is not set and no 'java' command found in PATH."
fi

GRADLE_VERSION="8.9"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

# This hash is the base-36 MD5 of the distribution URL, computed by the Gradle wrapper.
DIST_HASH="90cnw93cvbtalezasaz0blq0a"

DIST_DIR="${GRADLE_USER_HOME}/wrapper/dists/gradle-${GRADLE_VERSION}-bin/${DIST_HASH}"
GRADLE_HOME="${DIST_DIR}/gradle-${GRADLE_VERSION}"
LAUNCHER_JAR="${GRADLE_HOME}/lib/gradle-launcher-${GRADLE_VERSION}.jar"

if [ ! -f "$LAUNCHER_JAR" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
    mkdir -p "$DIST_DIR"
    TMP_ZIP=$(mktemp /tmp/gradle-XXXXXX.zip)
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 "$DIST_URL" -o "$TMP_ZIP"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DIST_URL" -O "$TMP_ZIP"
    else
        die "Neither curl nor wget found — cannot download Gradle."
    fi
    unzip -q "$TMP_ZIP" -d "$DIST_DIR"
    rm -f "$TMP_ZIP"
fi

exec "$JAVACMD" \
    -classpath "$LAUNCHER_JAR" \
    org.gradle.launcher.GradleMain "$@"
