-- 1. TABLA DE USUARIOS
-- Con Google Auth y registro normal.
CREATE TABLE Usuario (
    UID SERIAL PRIMARY KEY,
    google_id VARCHAR(255) UNIQUE,              -- ID login Google
    Nombre VARCHAR(100) NOT NULL,
    Correo VARCHAR(150) UNIQUE NOT NULL,
    Contrasena_Hash TEXT,                       -- NULL si viene de Google
    Rol VARCHAR(20) DEFAULT 'usuario',
    FechaRegistro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UltimoAcceso TIMESTAMP,
    Activo BOOLEAN DEFAULT TRUE,
    -- Gestión de pagos
    stripe_customer_id VARCHAR(255),            -- Token de cliente en Stripe
    metodo_pago_preferido VARCHAR(255),         -- ID del método de pago guardado
    tarjeta_brand VARCHAR(20),                  -- (Tipo de pago)
    tarjeta_ultimos_cuatro VARCHAR(4)           -- Para mostrar: "**** 0105"
);

-- 2. TABLA DE COCHES
CREATE TABLE Coche (
    CID SERIAL PRIMARY KEY,
    UID_Dueno INT NOT NULL REFERENCES Usuario(UID) ON DELETE CASCADE,
    Marca VARCHAR(50) NOT NULL,
    Modelo VARCHAR(50) NOT NULL,
    Matricula VARCHAR(15) UNIQUE NOT NULL,
    Color VARCHAR(30),
    Imagen_URL TEXT,
    Activo BOOLEAN DEFAULT TRUE
);

-- 3. TABLA DE INTERCAMBIOS
CREATE TABLE Intercambio (
    ID SERIAL PRIMARY KEY,
    
    -- Participantes
    Id_vendedor INT NOT NULL REFERENCES Usuario(UID),
    Id_comprador INT REFERENCES Usuario(UID), -- NULL hasta que alguien acepta
    Id_Coche_Vendedor INT NOT NULL REFERENCES Coche(CID),
    Id_Coche_Comprador INT REFERENCES Coche(CID),
    -- Valores/Economia (Comisión del 15%)
    Precio_Total_Comprador DECIMAL(10, 2) NOT NULL, -- Lo que paga el comprador
    Comision_Servicio DECIMAL(10, 2) NOT NULL,      -- El % que va para ParkingHole
    Ganancia_Vendedor DECIMAL(10, 2) NOT NULL,      -- Lo que recibe el vendedor
    -- Logística del Intercambio
    Momento_Intercambio_Previsto TIMESTAMP NOT NULL,
    Cortesia_Minutos INT DEFAULT 5,
    -- Ubicación GPS (Precisión para encontrar el hueco)
    Plaza_lat DECIMAL(10, 8) NOT NULL,
    Plaza_long DECIMAL(11, 8) NOT NULL,
    Plaza_Direccion_Texto TEXT,
    -- Estados del intercambio
	--Antes de la finalizacion
    -- Estados: 'Esperando', 'Reservado', 'En curso', 'Finalizado', 'Cancelado'
    Estado_intercambio VARCHAR(20) DEFAULT 'Esperando',
	-- Una vez termiado
    -- Resultados: 'Exito', 'No_presentado_comprador', 'No_presentado_vendedor', 'Cancelado_vendedor'
    Estado_resultado VARCHAR(20),
    -- Feedback
    Calificacion_Al_Vendedor INT CHECK (Calificacion_Al_Vendedor BETWEEN 1 AND 5),
    Calificacion_Al_Comprador INT CHECK (Calificacion_Al_Comprador BETWEEN 1 AND 5),
    Observaciones_Del_Comprador TEXT,
    Observaciones_Del_Vendedor TEXT,
    
    -- Seguimiento
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. ÍNDICES DE RENDIMIENTO
-- Para que la búsqueda de plazas libres sea instantánea por ubicación.
CREATE INDEX idx_plazas_disponibles ON Intercambio (Plaza_lat, Plaza_long) 
WHERE Estado_intercambio = 'Esperando';

-- 5. TRIGGER PARA ACTUALIZACIÓN AUTOMÁTICA DE 'UpdatedAt'
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.UpdatedAt = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_update_intercambio_time
BEFORE UPDATE ON Intercambio
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();