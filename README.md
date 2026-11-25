# Comunidad Western

Proyecto Android Java con Firebase Authentication, Realtime Database, FCM y MQTT.

## Preparación local
1. Abre esta carpeta en Android Studio.
2. Configura el Android SDK desde Android Studio (local.properties es local y no se incluye).
3. Coloca el archivo `google-services.json` correspondiente a tu proyecto Firebase dentro de `app/`.
4. Copia `local.properties.example` como `local.properties` y completa las credenciales MQTT entregadas para tu broker.
5. Sincroniza Gradle y ejecuta `:app:assembleDebug`.

## Relay local FCM
El relay se encuentra en `announcement-relay/`. Requiere Node.js 22 o superior.
Instala dependencias con `npm install` dentro de esa carpeta y configura `GOOGLE_APPLICATION_CREDENTIALS` apuntando a una clave de cuenta de servicio guardada fuera del repositorio.
Nunca incluyas claves de cuenta de servicio en GitHub.

## Seguridad
- `google-services.json`, `local.properties` y las claves privadas de Firebase Admin se mantienen fuera de Git.
- No incluyas `node_modules/`, `.gradle/` ni carpetas `build/`.
- Las reglas de Realtime Database están respaldadas en `database.rules.json`.
