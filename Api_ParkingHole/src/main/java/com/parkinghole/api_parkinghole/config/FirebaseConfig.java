package com.parkinghole.api_parkinghole.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;

            // 1. Intentamos leer la variable de entorno (para Google Cloud)
            String jsonConfig = System.getenv("FIREBASE_CONFIG_JSON");

            if (jsonConfig != null && !jsonConfig.isEmpty()) {
                serviceAccount = new ByteArrayInputStream(jsonConfig.getBytes(StandardCharsets.UTF_8));
                System.out.println("✅ Cargando Firebase desde variable de entorno.");
            } else {
                // 2. Si no hay variable, buscamos el archivo (para tu PC local)
                serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
                System.out.println("🏠 Cargando Firebase desde archivo local.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 Firebase Admin SDK inicializado correctamente.");
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR crítico al inicializar Firebase Admin SDK:");
            // No imprimimos todo el e.printStackTrace() para no ensuciar los logs de la nube
            System.err.println("Causa: " + e.getMessage());
        }
    }
}