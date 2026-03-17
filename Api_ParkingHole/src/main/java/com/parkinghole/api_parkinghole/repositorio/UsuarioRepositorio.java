package com.parkinghole.api_parkinghole.repositorio;

import com.parkinghole.api_parkinghole.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByGoogleId(String googleId);
}