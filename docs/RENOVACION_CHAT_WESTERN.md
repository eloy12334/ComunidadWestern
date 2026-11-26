# Renovación visual del Chat General — Comunidad Western

Se integró la propuesta visual de chat western en el proyecto Android.

## Cambios incluidos

- `app/src/main/res/layout/activity_chat.xml`: nueva composición de la pantalla con cartel de Chat general, faroles, descripción, paisaje, barra de escritura y botón Volver.
- `app/src/main/res/layout/item_chat_message.xml`: tarjeta dinámica individual para cada mensaje.
- `app/src/main/java/com/example/comunidadwestern/ChatActivity.java`: adaptador visual propio para mostrar remitente, vivienda, texto y fecha sobre tarjetas de pergamino. Se conserva el uso de Firebase Realtime Database, el tópico/ruta de la comunidad, el límite de 50 mensajes, el envío, las validaciones y la liberación del listener.
- `app/src/main/res/drawable-nodpi/`: se incorporaron los PNG entregados para cabecera, burbujas, botones, faroles y adornos.
- `app/src/main/res/drawable/`: fondos XML para pergamino, marco del campo de escritura, etiqueta del remitente y medallón.
- `AndroidManifest.xml`: se añadió `windowSoftInputMode="adjustResize"` a ChatActivity para que el teclado no tape el campo de escritura.

## IDs conservados

- `listaMensajes`
- `txtMensaje`
- `btnEnviarMensaje`
- `btnVolverChat`

Los listeners del resto de la aplicación no se cambiaron.

## Prueba

Abrir el proyecto en Android Studio, ejecutar Gradle Sync y compilar con `:app:assembleDebug`. Después probar en dos emuladores: enviar un mensaje desde cada cuenta, comprobar nombre/vivienda/fecha, recibir el mensaje en tiempo real y verificar el envío con el botón Enviar y el botón Volver.

La vista previa incluida en el paquete es ilustrativa; el render final depende del tamaño de pantalla, densidad y fuente del dispositivo.
