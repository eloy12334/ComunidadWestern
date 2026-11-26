# Comunidad Western — EV5 + Duelo del Oeste

Proyecto Android independiente en **Java + XML + Gradle Groovy**, basado en Empty Views Activity.

## Configuración Android
- Paquete: `com.example.comunidadwestern`
- `minSdk 27` (Android 8.1)
- `compileSdk 37`, `targetSdk 35`
- Gradle Wrapper 9.5.0, Android Gradle Plugin 9.3.3, JDK 17
- Firebase Authentication, Realtime Database, Cloud Messaging y Cloud Functions
- Eclipse Paho MQTT para las alertas de la comunidad

## Funciones de la comunidad
- Registro, inicio de sesión, recuperación de contraseña y perfil.
- Chat general en Realtime Database.
- Publicaciones de venta comunitarias.
- Anuncios oficiales de administrador y notificaciones FCM.
- Alertas MQTT, historial y confirmación de lectura.
- Mejora de alertas: antirrepetición, deduplicación y confirmación de lectura.

## Nuevo: Duelo del Oeste
- Minijuego **1 vs. 1 en línea**, exclusivo de esta variante Western.
- Un jugador crea una sala y comparte un código de 6 caracteres; el rival entra con ese código.
- Modos: mejor de 1, mejor de 3 y mejor de 5.
- Cada ronda usa una cuenta regresiva aleatoria de **3 a 5 segundos**.
- El botón de disparo se puede pulsar durante la cuenta: si el servidor recibe el disparo antes del instante habilitado, se registra falsa salida y el jugador pierde esa ronda.
- En mejor de 1, perder esa ronda significa perder el duelo completo.
- La primera acción válida recibida por el servidor después del tiempo habilitado gana la ronda.
- El estilo visual usa ilustración plana, colores cálidos, siluetas de vaqueros y escenario de desierto; no usa imágenes realistas.

### Cómo jugar
1. Inicia sesión en la aplicación.
2. Abre **Duelo del Oeste**.
3. El anfitrión escoge mejor de 1, 3 o 5 y presiona **Crear sala**.
4. Comparte el código con otro usuario que también haya iniciado sesión.
5. El segundo jugador introduce el código y presiona **Unirse al duelo**.
6. El anfitrión inicia la ronda. Espera a que el contador llegue a 0 y dispara.
7. Una falsa salida resta la ronda al jugador que disparó antes de tiempo.

### Arbitraje y latencia
La hora que decide una falsa salida y quién gana es la hora de recepción registrada por Cloud Functions, no el reloj local del dispositivo. La pantalla usa el desfase de hora del servidor de Realtime Database únicamente para dibujar el contador. Como en todo juego por Internet, la latencia puede afectar el momento en que llega la solicitud; el servidor evita que cada teléfono decida el resultado por su cuenta, pero no puede conocer con certeza el instante físico en que el dedo tocó la pantalla.

Si dos disparos llegan prácticamente al mismo tiempo, la transacción del servidor procesa uno primero y la ronda queda resuelta; el segundo intento ya no modifica el resultado.

## Configuración de Firebase (obligatoria)
1. Crea un proyecto en Firebase Console.
2. Registra una aplicación Android con paquete `com.example.comunidadwestern`.
3. Descarga `google-services.json` y colócalo dentro de `app/`. No lo subas a GitHub.
4. Activa Authentication > Email/Password.
5. Crea Realtime Database y publica `database.rules.json`.
6. Crea el usuario administrador y configura `roles/UID_DEL_ADMIN = "admin"` desde Firebase Console.
7. Configura Firebase CLI en la raíz del proyecto y despliega:
   ```bash
   cd functions
   npm install
   cd ..
   firebase deploy --only database,functions
   ```
8. Cloud Functions requiere un proyecto con facturación habilitada (plan Blaze). Las funciones usan `us-central1`; mantén coherente la ubicación configurada para Realtime Database y Functions.
9. La sala y el resultado del duelo se leen desde Realtime Database. Las escrituras de `/duels` están bloqueadas desde la app; solamente las funciones callable con Admin SDK pueden modificar las partidas.

## Configuración MQTT
En `local.properties` agrega valores propios:
```properties
MQTT_URI=ssl://TU_BROKER:8883
MQTT_USERNAME=TU_USUARIO
MQTT_PASSWORD=TU_PASSWORD
```
Usa un broker TLS con ACL limitada al tópico `comunidades/comunidad01/alertas`. No compartas `local.properties` ni credenciales.

## Abrir en Android Studio
1. Extrae el ZIP y abre la carpeta `ComunidadWestern`.
2. Configura Gradle JDK en 17.
3. Instala Android SDK Platform 37 y Build Tools desde SDK Manager.
4. Añade `google-services.json`, configura Firebase y despliega las funciones.
5. Ejecuta Sync Project with Gradle Files y prueba en dos dispositivos/emuladores con cuentas distintas.

## Antes de la demostración
- Prueba un duelo en dos dispositivos conectados a Internet.
- Comprueba mejor de 1, 3 y 5.
- Prueba disparar antes de 0 y verifica que pierde una ronda.
- Comprueba que en mejor de 1 la falsa salida cierra el duelo.
- Prueba también retrasos de red y reconexión.
- Mantén la pantalla Alertas abierta para la demostración MQTT; el comportamiento en segundo plano/cerrado se debe validar por separado.

## Límites de esta entrega
- Se entrega el proyecto fuente; no un APK compilado.
- El proyecto necesita el `google-services.json`, una base de datos Firebase, el despliegue de Cloud Functions y las credenciales de un broker MQTT real.
- El entorno de creación no tiene Android SDK instalado, por lo que no se pudo ejecutar una compilación Android aquí. Se validó la estructura del ZIP, los XML y JSON, y la sintaxis JavaScript de las funciones cuando Node está disponible.
- El duelo es en línea y necesita dos cuentas y dos dispositivos/emuladores para comprobarse de extremo a extremo.

## GitHub: secuencia sugerida
- v1.0 Comunidad Western (acceso, perfil, chat y ventas)
- v1.1 Anuncios y notificaciones FCM
- v1.2 Alertas MQTT y seguridad
- v1.3 Diseño ilustrado del oeste
- v1.4 Salas 1 vs. 1 y códigos de acceso
- v1.5 Cuenta 3–5 s, falsa salida y marcador por rondas
