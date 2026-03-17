package com.parkinghole.api_parkinghole.controladores;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.servicios.ServicioUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios") // Añadimos /api/ por convención profesional
public class UsuarioControlador {

    @Autowired
    private ServicioUsuario servicioUsuario; // Minúscula por convención

    // 1. Google Login (Llamando a la lógica de vinculación)
    @PostMapping("/google-login")
    public ResponseEntity<?> loginConGoogle(@RequestBody Usuario usuarioGoogle) {
        try {
            Usuario resultado = servicioUsuario.loginOModuloGoogle(usuarioGoogle);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en el login de Google");
        }
    }

    // 2. Login Tradicional con manejo de errores
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {
        try {
            Usuario usuarioLogueado = servicioUsuario.login(usuario.getCorreo(), usuario.getContrasenaHash());
            return ResponseEntity.ok(usuarioLogueado);
        } catch (Exception e) {
            // Si las credenciales fallan, enviamos un 401 (No autorizado)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 3. Registro Tradicional
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = servicioUsuario.registrarUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/vertodos")
    public List<Usuario> listarUsuarios() {
        return servicioUsuario.findAll();
    }
}