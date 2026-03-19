package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.servicios.NotificationService;
import com.parkinghole.api_parkinghole.servicios.ServicioUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TestNotificacionControlador {

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/test-notificacion")
    public ResponseEntity<?> testNotification(@RequestParam String email) {
        try {
            Optional<Usuario> usrOpt = servicioUsuario.findByCorreo(email);
            if (!usrOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Usuario no encontrado con el correo proporcionado.");
            }

            Usuario usuario = usrOpt.get();
            String token = usuario.getFcmToken();

            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: El usuario existe, pero no ha registrado ningún token en su cuenta. (¿Ha abierto la app?)");
            }

            String titulo = "¡Prueba de ParkingHole! \uD83D\uDE80";
            String mensaje = "Si lees esto, tu sistema de avisos está configurado correctamente.";

            String res = notificationService.sendPushNotification(token, titulo, mensaje);

            return ResponseEntity.ok("Notificación enviada con éxito al token: " + token + "\nRespuesta de Firebase: " + res);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }
}
