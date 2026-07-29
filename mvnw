#!/bin/bash
# Maven Wrapper - downloads and caches Maven automatically
MAVEN_VERSION="3.9.8"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
    echo "Downloading Maven ${MAVEN_VERSION}..."
    mkdir -p "$(dirname "${MAVEN_HOME}")"
    curl -sL "${MAVEN_URL}" | tar xz -C "$(dirname "${MAVEN_HOME}")" 2>/dev/null
    if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
        # Try alternate URL
        MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
        curl -sL "${MAVEN_URL}" | tar xz -C "$(dirname "${MAVEN_HOME}")" 2>/dev/null
    fi
fi

if [ -f "${MAVEN_HOME}/bin/mvn" ]; then
    exec "${MAVEN_HOME}/bin/mvn" "$@"
else
    echo "Error: Could not download Maven. Please install manually."
    exit 1
fi
