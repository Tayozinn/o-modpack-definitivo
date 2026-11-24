package com.mythiccontracts.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AudioAndAssetIntegrityTest {

    private static final Path ASSETS_DIR = Path.of("src/main/resources/assets/mythic_contracts");

    @Test
    void allThirtySoundEventsExistAreMono44100HzOggAndHaveLocalizedSubtitles() throws IOException {
        JsonObject sounds = JsonParser.parseString(
                Files.readString(ASSETS_DIR.resolve("sounds.json"), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        JsonObject ptBr = JsonParser.parseString(
                Files.readString(ASSETS_DIR.resolve("lang/pt_br.json"), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        JsonObject enUs = JsonParser.parseString(
                Files.readString(ASSETS_DIR.resolve("lang/en_us.json"), StandardCharsets.UTF_8)
        ).getAsJsonObject();

        assertEquals(30, sounds.size(), "Expected 30 custom sound events in sounds.json");

        for (Map.Entry<String, com.google.gson.JsonElement> entry : sounds.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            assertTrue(obj.has("subtitle"), "Missing subtitle in sounds.json for " + entry.getKey());
            String subtitleKey = obj.get("subtitle").getAsString();
            assertTrue(ptBr.has(subtitleKey), "Missing pt_br subtitle: " + subtitleKey);
            assertTrue(enUs.has(subtitleKey), "Missing en_us subtitle: " + subtitleKey);

            JsonArray files = obj.getAsJsonArray("sounds");
            assertFalse(files.isEmpty());
            for (int i = 0; i < files.size(); i++) {
                String soundId = files.get(i).getAsString().replace("mythic_contracts:", "");
                Path oggPath = ASSETS_DIR.resolve("sounds/" + soundId + ".ogg");
                assertTrue(Files.isRegularFile(oggPath), "Missing OGG file: " + oggPath);
                verifyMono44100OggVorbis(oggPath);
            }
        }
    }

    @Test
    void v21ParticlesTexturesAndPlayerAnimationsExistAndParseCleanly() throws IOException {
        List<String> newParticles = List.of(
                "subjugation_rune_ring",
                "subjugation_chain_link",
                "subjugation_sigil",
                "qi_ribbon_wisp"
        );
        for (String name : newParticles) {
            Path jsonPath = ASSETS_DIR.resolve("particles/" + name + ".json");
            assertTrue(Files.isRegularFile(jsonPath), "Missing particle JSON: " + jsonPath);
            JsonObject pJson = JsonParser.parseString(Files.readString(jsonPath, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(pJson.getAsJsonArray("textures").size() >= 1);

            Path pngPath = ASSETS_DIR.resolve("textures/particle/" + name + ".png");
            assertTrue(Files.isRegularFile(pngPath), "Missing particle PNG: " + pngPath);
            byte[] png = Files.readAllBytes(pngPath);
            assertTrue(png.length > 32);
            assertEquals((byte) 0x89, png[0]);
            assertEquals((byte) 'P', png[1]);
            assertEquals((byte) 'N', png[2]);
            assertEquals((byte) 'G', png[3]);
        }

        List<String> animations = List.of(
                "divine_cast",
                "subjugation_channel",
                "martial_strike",
                "body_guard",
                "lotus_meditation"
        );
        for (String anim : animations) {
            Path animPath = ASSETS_DIR.resolve("player_animation/" + anim + ".json");
            assertTrue(Files.isRegularFile(animPath), "Missing player animation JSON: " + animPath);
            JsonObject root = JsonParser.parseString(Files.readString(animPath, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("1.8.0", root.get("format_version").getAsString());
            assertTrue(root.getAsJsonObject("animations").has(anim),
                    "Animation key mismatch inside " + anim + ".json");
        }
    }

    private static void verifyMono44100OggVorbis(Path oggPath) throws IOException {
        byte[] bytes = Files.readAllBytes(oggPath);
        assertTrue(bytes.length > 64, "OGG too small: " + oggPath);
        assertEquals('O', (char) bytes[0], "Not OggS stream: " + oggPath);
        assertEquals('g', (char) bytes[1]);
        assertEquals('g', (char) bytes[2]);
        assertEquals('S', (char) bytes[3]);

        byte[] marker = new byte[]{0x01, 'v', 'o', 'r', 'b', 'i', 's'};
        int idx = indexOf(bytes, marker);
        assertTrue(idx >= 0 && idx + 16 < bytes.length, "Missing Vorbis id header in " + oggPath);

        // Layout: [0x01,'v','o','r','b','i','s'] (7 bytes) + [vorbis_version: uint32] + [channels: uint8] + [sample_rate: uint32 LE]
        int channels = Byte.toUnsignedInt(bytes[idx + 11]);
        int sampleRate = ByteBuffer.wrap(bytes, idx + 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        assertEquals(1, channels, "Sound must be MONO (1 channel) for 3D spatial audio: " + oggPath);
        assertEquals(44100, sampleRate, "Sound must be 44100 Hz: " + oggPath);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
