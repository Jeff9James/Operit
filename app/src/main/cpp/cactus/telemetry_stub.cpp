// Telemetry stub implementations for Cactus Android build
// This file provides empty/stub implementations for telemetry functions
// that are called by the Cactus library but not defined in the open-source version

#include <cstddef>
#include <cstring>

// NOTE: CompletionMetrics struct is defined in cactus/telemetry/telemetry.h
// We only provide stub implementations for the telemetry functions

namespace cactus {
namespace telemetry {

// Stub implementations

void init(const char* api_key, const char* endpoint, const char* model) {
    // No-op stub - telemetry initialization
}

void setCloudDisabled(bool disabled) {
    // No-op stub - cloud telemetry disabled
}

void setStreamMode(bool enabled) {
    // No-op stub - stream mode set
}

void recordCompletion(const char* event_type, const CompletionMetrics& metrics) {
    // No-op stub - completion event recording
}

void recordInit(const char* model_name, bool success, double duration_ms, const char* error_msg) {
    // No-op stub - init event recording
}

void recordTranscription(
    const char* audio_data,
    bool success,
    double audio_duration_sec,
    double transcription_duration_sec,
    double cost_usd,
    int num_speakers,
    const char* language
) {
    // No-op stub - transcription event recording
}

void recordStreamTranscription(
    const char* session_id,
    bool is_final,
    double audio_duration_sec,
    double transcription_duration_sec,
    double cost_usd,
    int chunk_index,
    double time_to_first_token,
    double time_per_chunk,
    double total_transcription_time,
    int num_speakers,
    const char* language
) {
    // No-op stub - stream transcription event recording
}

} // namespace telemetry
} // namespace cactus
