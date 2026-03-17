CREATE SCHEMA IF NOT EXISTS public;

SET search_path TO public;

-- FUNCIÓN PARA UPDATED_AT AUTOMÁTICO

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TABLA USUARIO

CREATE TABLE usuario (
    uid BIGSERIAL PRIMARY KEY,
    google_id VARCHAR(255) UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255),
    rol VARCHAR(255) DEFAULT 'usuario',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    stripe_customer_id VARCHAR(255),
    metodo_pago_preferido VARCHAR(255),
    tarjeta_brand VARCHAR(255),
    tarjeta_ultimos_cuatro VARCHAR(255),
    stripe_connect_id VARCHAR(255)
);


-- TABLA COCHE

CREATE TABLE coche (
    cid BIGSERIAL PRIMARY KEY,
    uid_dueno BIGINT NOT NULL,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    color VARCHAR(255),
    imagen_url VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,

    CONSTRAINT coche_uid_dueno_fkey
        FOREIGN KEY (uid_dueno)
        REFERENCES usuario(uid)
        ON DELETE CASCADE
);


-- TABLA INTERCAMBIO

CREATE TABLE intercambio (
    id BIGSERIAL PRIMARY KEY,
    id_vendedor BIGINT NOT NULL,
    id_comprador BIGINT,
    id_coche_vendedor BIGINT NOT NULL,
    id_coche_comprador BIGINT,

    precio_total_comprador DOUBLE PRECISION NOT NULL,
    comision_servicio DOUBLE PRECISION NOT NULL,
    ganancia_vendedor DOUBLE PRECISION NOT NULL,

    momento_intercambio_previsto TIMESTAMP NOT NULL,
    cortesia_minutos INTEGER DEFAULT 5,

    plaza_lat DOUBLE PRECISION NOT NULL,
    plaza_long DOUBLE PRECISION NOT NULL,
    plaza_direccion_texto VARCHAR(255),

    estado_intercambio VARCHAR(255) DEFAULT 'Esperando',
    estado_resultado VARCHAR(255),

    calificacion_al_vendedor INTEGER CHECK (calificacion_al_vendedor BETWEEN 1 AND 5),
    calificacion_al_comprador INTEGER CHECK (calificacion_al_comprador BETWEEN 1 AND 5),

    observaciones_del_comprador TEXT,
    observaciones_del_vendedor TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    codigo_verificacion VARCHAR(4),
    payment_intent_id VARCHAR(255),

    CONSTRAINT intercambio_id_vendedor_fkey
        FOREIGN KEY (id_vendedor)
        REFERENCES usuario(uid),

    CONSTRAINT intercambio_id_comprador_fkey
        FOREIGN KEY (id_comprador)
        REFERENCES usuario(uid),

    CONSTRAINT intercambio_id_coche_vendedor_fkey
        FOREIGN KEY (id_coche_vendedor)
        REFERENCES coche(cid),

    CONSTRAINT intercambio_id_coche_comprador_fkey
        FOREIGN KEY (id_coche_comprador)
        REFERENCES coche(cid)
);


-- ÍNDICE PARA BÚSQUEDA DE PLAZAS DISPONIBLES

CREATE INDEX idx_plazas_disponibles
ON intercambio (plaza_lat, plaza_long)
WHERE estado_intercambio = 'Esperando';


-- TRIGGER PARA UPDATED_AT

CREATE TRIGGER trg_update_intercambio_time
BEFORE UPDATE ON intercambio
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
