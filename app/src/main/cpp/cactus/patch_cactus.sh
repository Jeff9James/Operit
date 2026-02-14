#!/bin/bash

# Patch script to add telemetry stubs to Cactus Android build
# This script is called after cloning the cactus repository and before building

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACTUS_DIR="${1:-../cactus}"

echo "Patching Cactus at: $CACTUS_DIR"

# Check if cactus directory exists
if [ ! -d "$CACTUS_DIR" ]; then
    echo "Error: Cactus directory not found at $CACTUS_DIR"
    exit 1
fi

# Copy the telemetry stub file to cactus source
echo "Copying telemetry_stub.cpp to cactus source..."
cp "$SCRIPT_DIR/telemetry_stub.cpp" "$CACTUS_DIR/cactus/telemetry_stub.cpp"

# Check if CMakeLists.txt exists and modify it to include the stub
CMAKE_FILE="$CACTUS_DIR/android/CMakeLists.txt"
if [ -f "$CMAKE_FILE" ]; then
    # Check if telemetry_stub.cpp is already added
    if ! grep -q "telemetry_stub.cpp" "$CMAKE_FILE"; then
        echo "Adding telemetry_stub.cpp to CMakeLists.txt..."
        # Add the stub file to the source files
        # Look for the add_library command and add our file
        sed -i 's/cactus_complete.cpp cactus_index.cpp/cactus_complete.cpp cactus_index.cpp telemetry_stub.cpp/g' "$CMAKE_FILE"
    else
        echo "telemetry_stub.cpp already added to CMakeLists.txt"
    fi
else
    echo "Warning: CMakeLists.txt not found at $CMAKE_FILE"
    # Try alternative approach - add to the main source list
    MAIN_CMAKE="$CACTUS_DIR/cactus/CMakeLists.txt"
    if [ -f "$MAIN_CMAKE" ]; then
        if ! grep -q "telemetry_stub.cpp" "$MAIN_CMAKE"; then
            sed -i 's/cactus_complete.cpp cactus_index.cpp/cactus_complete.cpp cactus_index.cpp telemetry_stub.cpp/g' "$MAIN_CMAKE"
        fi
    fi
fi

echo "Patch applied successfully!"
