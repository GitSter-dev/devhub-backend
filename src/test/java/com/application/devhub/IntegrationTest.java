package com.application.devhub;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@SpringBootTest(properties = {"devhub.scheduling.enabled=false", "devhub.rate-limit.enabled=false",
        "devhub.push.enabled=false"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    private static final Path KEYS_DIR = generateKeys();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void truncateTables() {
        jdbcTemplate.execute("TRUNCATE users, outbox_events, idempotency_records, devices, user_topics, follows, held_usernames, posts, post_likes CASCADE");
    }

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("devhub.jwt.public-key", () -> "file:" + KEYS_DIR.resolve("public.pem"));
        registry.add("devhub.jwt.private-key", () -> "file:" + KEYS_DIR.resolve("private.pem"));
    }

    public static KeyPair newRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path generateKeys() {
        KeyPair keyPair = newRsaKeyPair();
        try {
            Path dir = Files.createTempDirectory("devhub-test-keys");
            dir.toFile().deleteOnExit();
            writePem(dir.resolve("public.pem"), "PUBLIC KEY", keyPair.getPublic().getEncoded());
            writePem(dir.resolve("private.pem"), "PRIVATE KEY", keyPair.getPrivate().getEncoded());
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writePem(Path file, String type, byte[] der) throws IOException {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        Files.writeString(file, "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n");
        file.toFile().deleteOnExit();
    }
}
