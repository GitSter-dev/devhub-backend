package com.application.devhub.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnBooleanProperty(name = "devhub.push.enabled")
public class FirebaseConfig {

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(PushProperties properties) throws IOException {
        if (properties.credentials() == null || properties.credentials().isBlank()) {
            throw new IllegalStateException("devhub.push.enabled=true requires FIREBASE_CREDENTIALS (service account JSON path)");
        }
        try (InputStream credentials = new FileInputStream(properties.credentials())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
