package com.parkinghole.api_parkinghole.servicios;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import com.parkinghole.api_parkinghole.repositorio.UsuarioRepositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServicioUsuario {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    // --- LÓGICA DE GOOGLE (EVITA DUPLICADOS) ---
    public Usuario loginOModuloGoogle(Usuario usuarioGoogle) {
        // 1. Intentamos buscar si ya existe un usuario con ese Google ID
        return usuarioRepositorio.findByGoogleId(usuarioGoogle.getGoogleId())
                .map(usuarioExistente -> {
                    // Caso A: Ya es usuario de Google. Devolvemos sus datos actuales.
                    return usuarioExistente;
                })
                .orElseGet(() -> {
                    // 2. Si no tiene Google ID, buscamos si ya existe el CORREO en la DB
                    return usuarioRepositorio.findByCorreo(usuarioGoogle.getCorreo())
                            .map(usuarioTradicional -> {
                                // Caso B: El correo ya existe (cuenta tradicional).
                                // VINCULAMOS: Le ponemos el Google ID a la cuenta que ya existía.
                                usuarioTradicional.setGoogleId(usuarioGoogle.getGoogleId());
                                return usuarioRepositorio.save(usuarioTradicional);
                            })
                            .orElseGet(() -> {
                                // Caso C: No existe ni por ID ni por Correo. Registro nuevo total.
                                if (usuarioGoogle.getRol() == null) {
                                    usuarioGoogle.setRol("usuario");
                                }
                                return usuarioRepositorio.save(usuarioGoogle);
                            });
                });
    }

    // --- LÓGICA DE LOGIN TRADICIONAL ---
    public Usuario login(String correo, String contrasena) throws Exception {
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreo(correo);
        if (!usuarioOpt.isPresent()) {
            throw new Exception("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();

        // Verificamos que no sea una cuenta de Google intentando entrar por contraseña
        if (usuario.getContrasenaHash() == null) {
            throw new Exception("Esta cuenta usa Google Login. Entra con Google.");
        }

        if (!usuario.getContrasenaHash().equals(hashPassword(contrasena))) {
            throw new Exception("Contraseña incorrecta");
        }
        return usuario;
    }

    // --- REGISTRO TRADICIONAL ---
    public Usuario registrarUsuario(Usuario usuario) throws Exception {
        if (usuarioRepositorio.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new Exception("El correo ya está registrado");
        }
        // Hasheamos la contraseña antes de guardar
        usuario.setContrasenaHash(hashPassword(usuario.getContrasenaHash()));
        return usuarioRepositorio.save(usuario);
    }

    // --- MÉTODOS AUXILIARES ---
    public Optional<Usuario> findByGoogleId(String googleId) {
        return usuarioRepositorio.findByGoogleId(googleId);
    }

    public Usuario save(Usuario usuario) {
        return usuarioRepositorio.save(usuario);
    }

    public List<Usuario> findAll() {
        return usuarioRepositorio.findAll();
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}