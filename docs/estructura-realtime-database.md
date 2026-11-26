# Estructura de Realtime Database

| Ruta | Contenido | Acceso |
|---|---|---|
| `profiles/{uid}` | nombre, vivienda, email y fecha de alta | el usuario propietario y administrador pueden leer; propietario edita |
| `roles/{uid}` | `admin` o `vecino` | el usuario puede leer su propio rol; solo Admin SDK/Console lo modifica |
| `messages/{communityId}/{id}` | uid, nombre, vivienda, texto y fecha | usuarios autenticados leen; el emisor crea |
| `sales/{communityId}/{id}` | título, descripción, precio, vendedor, estado y fecha | usuarios autenticados leen; vendedor crea y marca vendido |
| `announcements/{communityId}/{id}` | título, cuerpo, autor, fecha | autenticados leen; administrador publica |
| `alerts/{communityId}/{alertId}` | id, comunidad, uid, identidad, fecha y estado | autenticados leen; usuario crea; administrador gestiona estado |

El rol administrativo se provisiona manualmente desde Firebase Console o Admin SDK. Nunca se acepta `role` desde el formulario de registro.

`alertReceipts/{communityId}/{alertId}/{uid}` almacena una confirmación única de lectura por vecino, con nombre, vivienda y fecha. Cada usuario solo puede crear su propia confirmación; el administrador puede consultar quién confirmó.
