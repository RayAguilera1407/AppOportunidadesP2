# Backend XAMPP — catálogo de eventos

API REST en PHP sobre MySQL (tabla `eventos_catalogo`), consumida por la app con Retrofit.

## Instalación
1. Copia esta carpeta a `C:\xampp\htdocs\activaciones_api\`.
2. En el XAMPP Control Panel inicia **Apache** y **MySQL**.
3. Abre `http://localhost/activaciones_api/eventos.php`: crea la base `activaciones_db`, la tabla y 3 eventos de ejemplo.
   (También puedes importar `activaciones_db.sql` en phpMyAdmin.)

## Endpoints (`eventos.php`)
| Método | URL | Acción |
|---|---|---|
| GET | `eventos.php` | Lista los eventos |
| GET | `eventos.php?id=1` | Un evento |
| POST | `eventos.php` | Registra (JSON) |
| PUT | `eventos.php?id=1` | Actualiza (JSON) |
| DELETE | `eventos.php?id=1` | Elimina |

Desde el emulador la app usa `http://10.0.2.2/activaciones_api/` (`RetrofitInstance.URL_XAMPP`).
