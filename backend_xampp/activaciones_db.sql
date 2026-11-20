-- Script equivalente (opcional): la API crea esto sola la primera vez.
CREATE DATABASE IF NOT EXISTS activaciones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE activaciones_db;
CREATE TABLE IF NOT EXISTS eventos_catalogo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    fecha DATE NOT NULL,
    lugar VARCHAR(120) NOT NULL,
    cupos INT NOT NULL DEFAULT 0,
    descripcion TEXT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
