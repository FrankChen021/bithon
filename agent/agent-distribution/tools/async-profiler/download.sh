#!/bin/bash

# Script to download async-profiler from GitHub

# Ask for version
read -p "Enter async-profiler version to download (e.g., 4.1): " VERSION

# Save version to RELEASE file
echo "$VERSION" > RELEASE
echo "Version $VERSION saved to RELEASE file"

# Base URL
BASE_URL="https://github.com/async-profiler/async-profiler/releases/download/v${VERSION}"

# Create directories if they don't exist
rm -fr linux-amd64 linux-arm64 macos
mkdir -p linux-amd64 linux-arm64 macos

# Download and extract Linux x64 version
echo "Downloading async-profiler-${VERSION}-linux-x64.tar.gz..."
curl -L -o async-profiler-${VERSION}-linux-x64.tar.gz "${BASE_URL}/async-profiler-${VERSION}-linux-x64.tar.gz"
if [ $? -eq 0 ]; then
    echo "Extracting to linux-amd64 directory..."
    tar -xzf async-profiler-${VERSION}-linux-x64.tar.gz -C linux-amd64 --strip-components=1
    rm async-profiler-${VERSION}-linux-x64.tar.gz
else
    echo "Failed to download Linux x64 version"
fi

# Download and extract Linux ARM64 version
echo "Downloading async-profiler-${VERSION}-linux-arm64.tar.gz..."
curl -L -o async-profiler-${VERSION}-linux-arm64.tar.gz "${BASE_URL}/async-profiler-${VERSION}-linux-arm64.tar.gz"
if [ $? -eq 0 ]; then
    echo "Extracting to linux-arm64 directory..."
    tar -xzf async-profiler-${VERSION}-linux-arm64.tar.gz -C linux-arm64 --strip-components=1
    rm async-profiler-${VERSION}-linux-arm64.tar.gz
else
    echo "Failed to download Linux ARM64 version"
fi

# Download and extract macOS version
echo "Downloading async-profiler-${VERSION}-macos.zip..."
curl -L -o async-profiler-${VERSION}-macos.zip "${BASE_URL}/async-profiler-${VERSION}-macos.zip"
if [ $? -eq 0 ]; then
    echo "Extracting to macos directory..."
    unzip -o async-profiler-${VERSION}-macos.zip -d macos
    # Move contents from the created directory to macos/
    mv macos/async-profiler-${VERSION}-macos/* macos/
    # Remove the empty directory
    rmdir macos/async-profiler-${VERSION}-macos
    rm async-profiler-${VERSION}-macos.zip
else
    echo "Failed to download macOS version"
fi

echo "Download and extraction completed."