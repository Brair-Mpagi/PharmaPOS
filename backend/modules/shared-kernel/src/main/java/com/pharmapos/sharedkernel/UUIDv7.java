package com.pharmapos.sharedkernel;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Utility class for generating UUIDv7 (time-ordered) identifiers per RFC 9562.
 *
 * <p>UUIDv7 encodes a Unix epoch millisecond timestamp in the most significant bits,
 * giving natural chronological ordering that is ideal for database primary keys.
 * The remaining bits are filled with cryptographically strong random data.
 *
 * <p>Uses the uuid-creator library (com.github.f4b6a3:uuid-creator) for generation.
 */
public final class UUIDv7 {

    private UUIDv7() {
        // utility class — not instantiable
    }

    /**
     * Generates a new UUIDv7 with the current timestamp and random suffix.
     *
     * <p>Thread-safe; can be called from any context, including offline BLN devices.
     *
     * @return a new time-ordered UUID
     */
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * Extracts the Unix epoch millisecond timestamp embedded in a UUIDv7.
     *
     * @param uuid a UUIDv7
     * @return the embedded timestamp in milliseconds since the Unix epoch
     * @throws IllegalArgumentException if the UUID is not version 7
     */
    public static long extractTimestamp(UUID uuid) {
        if (uuid.version() != 7) {
            throw new IllegalArgumentException(
                    "Expected UUIDv7 (version 7) but got version " + uuid.version());
        }
        // UUIDv7: bits 0-47 of the most significant long are the 48-bit Unix timestamp in ms
        return uuid.getMostSignificantBits() >>> 16;
    }

    /**
     * Validates whether the given UUID is a version 7 UUID.
     *
     * @param uuid the UUID to check
     * @return true if the UUID is version 7
     */
    public static boolean isV7(UUID uuid) {
        return uuid != null && uuid.version() == 7;
    }
}
