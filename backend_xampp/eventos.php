<?php
// API REST de la tabla eventos_catalogo
//   GET    eventos.php          -> lista todos
//   GET    eventos.php?id=1     -> uno
//   POST   eventos.php          -> crea (JSON en el cuerpo)
//   PUT    eventos.php?id=1     -> actualiza (JSON en el cuerpo)
//   DELETE eventos.php?id=1     -> elimina
require __DIR__ . '/config.php';

$metodo = $_SERVER['REQUEST_METHOD'];
$id = isset($_GET['id']) ? (int)$_GET['id'] : null;

function leerCuerpo(): array {
    $datos = json_decode(file_get_contents('php://input'), true);
    if (!is_array($datos)) responder(['error' => 'El cuerpo debe ser JSON'], 400);
    return $datos;
}

function validar(array $d): array {
    $errores = [];
    if (trim($d['nombre'] ?? '') === '') $errores[] = 'nombre es obligatorio';
    if (trim($d['tipo'] ?? '') === '') $errores[] = 'tipo es obligatorio';
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $d['fecha'] ?? '')) $errores[] = 'fecha debe tener formato AAAA-MM-DD';
    if (trim($d['lugar'] ?? '') === '') $errores[] = 'lugar es obligatorio';
    if (!isset($d['cupos']) || !is_numeric($d['cupos']) || (int)$d['cupos'] < 0) $errores[] = 'cupos debe ser un número >= 0';
    if ($errores) responder(['error' => implode(', ', $errores)], 422);
    return [
        ':nombre' => trim($d['nombre']), ':tipo' => trim($d['tipo']), ':fecha' => $d['fecha'],
        ':lugar' => trim($d['lugar']), ':cupos' => (int)$d['cupos'], ':descripcion' => trim($d['descripcion'] ?? ''),
    ];
}

function buscar(PDO $pdo, int $id) {
    $st = $pdo->prepare('SELECT id, nombre, tipo, fecha, lugar, cupos, descripcion FROM eventos_catalogo WHERE id = ?');
    $st->execute([$id]);
    $fila = $st->fetch();
    if (!$fila) responder(['error' => 'Evento no encontrado'], 404);
    $fila['id'] = (int)$fila['id'];
    $fila['cupos'] = (int)$fila['cupos'];
    return $fila;
}

switch ($metodo) {
    case 'GET':
        if ($id) responder(buscar($pdo, $id));
        $filas = $pdo->query('SELECT id, nombre, tipo, fecha, lugar, cupos, descripcion FROM eventos_catalogo ORDER BY fecha')->fetchAll();
        foreach ($filas as &$f) { $f['id'] = (int)$f['id']; $f['cupos'] = (int)$f['cupos']; }
        responder($filas);

    case 'POST':
        $p = validar(leerCuerpo());
        $pdo->prepare('INSERT INTO eventos_catalogo (nombre, tipo, fecha, lugar, cupos, descripcion) VALUES (:nombre, :tipo, :fecha, :lugar, :cupos, :descripcion)')->execute($p);
        responder(buscar($pdo, (int)$pdo->lastInsertId()), 201);

    case 'PUT':
        if (!$id) responder(['error' => 'Falta el id'], 400);
        buscar($pdo, $id);
        $p = validar(leerCuerpo());
        $p[':id'] = $id;
        $pdo->prepare('UPDATE eventos_catalogo SET nombre=:nombre, tipo=:tipo, fecha=:fecha, lugar=:lugar, cupos=:cupos, descripcion=:descripcion WHERE id=:id')->execute($p);
        responder(buscar($pdo, $id));

    case 'DELETE':
        if (!$id) responder(['error' => 'Falta el id'], 400);
        buscar($pdo, $id);
        $pdo->prepare('DELETE FROM eventos_catalogo WHERE id = ?')->execute([$id]);
        http_response_code(204);
        exit;

    default:
        responder(['error' => 'Método no permitido'], 405);
}
