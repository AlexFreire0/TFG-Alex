package com.parkinghole.api_parkinghole.servicios;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    /**
     * Envía una notificación PUSH al dispositivo destino utilizando su Token FCM.
     */
    public String sendPushNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isEmpty()) {
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
                    .build();

            // Esto enviará la notificación y devolverá el message_id de confirmación
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Notificación enviada con éxito. Mensaje_ID: " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error enviando FCM push: " + e.getMessage();
        }
    }
}
