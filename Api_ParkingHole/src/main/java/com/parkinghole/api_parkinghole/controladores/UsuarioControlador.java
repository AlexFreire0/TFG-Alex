package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.AuthResponse;
import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.seguridad.JwtUtil;
import com.parkinghole.api_parkinghole.servicios.ServicioUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControlador {

    @Autowired
    private ServicioUsuario servicioUsuario;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/google-login")
    public ResponseEntity<?> loginConGoogle(@RequestBody Usuario usuarioGoogle) {
        try {
            Usuario resultado = servicioUsuario.loginOModuloGoogle(usuarioGoogle);
            String token = jwtUtil.generateToken(resultado.getCorreo(), resultado.getUid());
            return ResponseEntity.ok(new AuthResponse(token, resultado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en el login de Google");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {
        try {
            Usuario usuarioLogueado = servicioUsuario.login(usuario.getCorreo(), usuario.getContrasenaHash());
            String token = jwtUtil.generateToken(usuarioLogueado.getCorreo(), usuarioLogueado.getUid());
            return ResponseEntity.ok(new AuthResponse(token, usuarioLogueado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = servicioUsuario.registrarUsuario(usuario);
            String token = jwtUtil.generateToken(nuevoUsuario.getCorreo(), nuevoUsuario.getUid());
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, nuevoUsuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/vertodos")
    public List<Usuario> listarUsuarios() {
        return servicioUsuario.findAll();
    }

    @PutMapping("/actualizar-fcm")
    public ResponseEntity<?> actualizarFcm(@RequestParam String token, java.security.Principal principal) {
        System.out.println("--> Intento de actualizar FCM para: " + (principal != null ? principal.getName() : "Usuario Nulo (Sin Token/No Autenticado)"));
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            java.util.Optional<Usuario> usrOpt = servicioUsuario.findByCorreo(principal.getName());
            if (usrOpt.isPresent()) {
                Usuario usr = usrOpt.get();
                usr.setFcmToken(token);
                servicioUsuario.save(usr);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }
}