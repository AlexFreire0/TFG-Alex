package com.parkinghole.api_parkinghole.servicios;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Envía una notificación PUSH al dispositivo destino utilizando su Token FCM.
     */
    public String sendPushNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.warn("El tokenFcm proporcionado es nulo o vacio, omitiendo envio push.");
            return "El tokenFcm proporcionado es nulo o vacío.";
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putData("title", title)
                    .putData("body", body)
                    .build();

            // Esto enviará la notificación y devolverá el message_id de confirmación
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Notificación enviada con éxito. Mensaje_ID: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error enviando FCM push: {}", e.getMessage(), e);
            return "❌ Error enviando FCM push: " + e.getMessage();
        }
    }
}
