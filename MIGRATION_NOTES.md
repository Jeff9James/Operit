# Cactus SDK Migration Notes

## Overview
This document describes the migration from llama.cpp + MNN native inference to Cactus SDK 1.4.1-beta for local LLM inference.

## Migration Summary

### What Was Removed
- **llama.cpp JNI backend**: Native library loading via `LlamaLibraryLoader` and `LlamaWrapper` native library
- **MNN native JNI backend**: Native library loading via `MNNLibraryLoader` and `MNN`/`MNNWrapper` native libraries
- **Direct model file handling**: `.gguf` file loading for llama.cpp models, `llm.mnn` file detection for MNN models

### What Was Added
- **Cactus SDK 1.4.1-beta**: New Kotlin Multiplatform library for local inference
- **Model slug-based identification**: Models are now identified by slug (e.g., "qwen3-0.6", "gemma3-270m") rather than file paths
- **Automatic model management**: `CactusModelManager` for downloading, checking, and deleting models

### Provider Routing Changes
| Provider Type | Previous Backend | New Backend |
|---------------|------------------|-------------|
| `LLAMA_CPP` | llama.cpp + JNI | LlamaProvider + CactusLM (legacy alias) |
| `CACTUS_LOCAL` | N/A (new) | LlamaProvider + CactusLM |
| `MNN` | MNN Native + JNI | MNNProvider + CactusLM (compatibility wrapper) |

### Architecture Changes
- `CactusContextInitializer.initialize(context)` must be called in Activity.onCreate()
- Models are downloaded automatically via `CactusLM.downloadModel(slug)`
- Model initialization uses `CactusLM.initializeModel(CactusInitParams(...))`
- Inference via `CactusLM.generateCompletion(...)` with `List<ChatMessage>`

## Known Limitations
1. **No mid-inference cancellation**: Once a generation starts, it cannot be cancelled mid-stream
2. **No standalone token count API**: Token counting requires initializing a model

## Version Requirements
- Kotlin: 2.2.0
- Android Gradle Plugin: 8.7.3
- Compile SDK: 36
- Min SDK: 24 (Android 7.0)
- Cactus SDK: 1.4.1-beta

## Files Modified
- `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/LlamaProvider.kt` - Updated to use CactusLM
- `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/MNNProvider.kt` - Updated to use CactusLM
- `app/proguard-rules.pro` - Added Cactus SDK keep rules
- `mnn/src/main/java/com/ai/assistance/mnn/MNNLibraryLoader.kt` - Stub (no longer used)
- `llama/src/main/java/com/ai/assistance/llama/LlamaLibraryLoader.kt` - Stub (no longer used)

## Backward Compatibility
- `ApiProviderType` enum entries remain unchanged to preserve compatibility with saved configurations
- Existing saved configurations with `LLAMA_CPP` or `MNN` provider types will continue to work
- Model config fields (`mnnForwardType`, `mnnThreadCount`) are preserved for API compatibility but not used by Cactus SDK
