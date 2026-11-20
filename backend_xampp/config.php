<?php
// Conexión a MySQL de XAMPP (usuario root sin clave, valores por defecto de XAMPP).
// Crea la base y la tabla la primera vez, así no hay que importar nada a mano.
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }

const DB_HOST = '127.0.0.1';
const DB_USER = 'root';
const DB_PASS = '';
const DB_NAME = 'activaciones_db';

function responder($datos, int $codigo = 200): void {
    http_response_code($codigo);
    echo json_encode($datos, JSON_UNESCAPED_UNICODE);
    exit;
}

try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';charset=utf8mb4', DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $pdo->exec('CREATE DATABASE IF NOT EXISTS ' . DB_NAME . ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    $pdo->exec('USE ' . DB_NAME);
    $pdo->exec("CREATE TABLE IF NOT EXISTS eventos_catalogo (
        id INT AUTO_INCREMENT PRIMARY KEY,
        nombre VARCHAR(120) NOT NULL,
        tipo VARCHAR(60) NOT NULL,
        fecha DATE NOT NULL,
        lugar VARCHAR(120) NOT NULL,
        cupos INT NOT NULL DEFAULT 0,
        descripcion TEXT NULL,
        creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB");
    // Datos de ejemplo solo si la tabla está vacía
    if ((int)$pdo->query('SELECT COUNT(*) FROM eventos_catalogo')->fetchColumn() === 0) {
        $pdo->exec("INSERT INTO eventos_catalogo (nombre, tipo, fecha, lugar, cupos, descripcion) VALUES
            ('Feria laboral Peñalolén', 'Feria institucional', '2026-12-10', 'Peñalolén', 40, 'Stands de empresas e instituciones'),
            ('Semana de bienestar Minera Andes', 'Actividad de trabajadores', '2026-12-15', 'Las Condes', 25, 'Charlas de beneficios para trabajadores'),
            ('Expo Emprende Santiago', 'Feria empresarial', '2027-01-20', 'Santiago', 60, 'Feria de emprendimiento con espacios comerciales')");
    }
} catch (PDOException $e) {
    responder(['error' => 'No se pudo conectar a MySQL. ¿Está iniciado en el panel de XAMPP?', 'detalle' => $e->getMessage()], 500);
}
