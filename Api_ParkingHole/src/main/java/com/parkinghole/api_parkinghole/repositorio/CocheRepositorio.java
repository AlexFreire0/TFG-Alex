package com.parkinghole.api_parkinghole.repositorio;

import com.parkinghole.api_parkinghole.modelos.Coche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface CocheRepositorio extends JpaRepository<Coche, Long> {

    List<Coche> findByDuenoUidAndActivoTrue(Long uid);
    long countByDuenoUidAndActivoTrue(Long uid);

    @Transactional
    @Modifying
    @Query("UPDATE Coche c SET c.activo = false WHERE c.cid = ?1")
    void setActivoFalse(Long cid);
}