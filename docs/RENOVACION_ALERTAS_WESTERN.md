# Renovación visual – Alertas comunitarias

Se actualizó la presentación de `AlertasActivity` para mantener la identidad visual western del menú, chat, ventas y anuncios.

## Cambios visuales

- Cabecera de sheriff con el título «Alertas comunitarias».
- Indicador MQTT en un banner de pergamino. El texto sigue siendo dinámico y lo actualiza `MqttManager`.
- Panel de emergencia con mensaje y decoración western.
- Botón «Activar alerta» conservado como control Android real, con su listener original.
- Cabecera y tarjetas del historial con los recursos PNG de alertas.
- Estado visible, UID/identificador y fecha de cada alerta.
- Las alertas más recientes se muestran primero.
- Estado vacío cuando todavía no existen alertas.
- Botón Volver conservado como `ImageButton` real.

## Archivos principales modificados

- `app/src/main/res/layout/activity_alertas.xml`
- `app/src/main/res/layout/item_alerta.xml` (nuevo)
- `app/src/main/res/drawable/bg_alert_status.xml` (nuevo)
- `app/src/main/res/drawable-nodpi/alerts_*.png` (recursos gráficos)
- `app/src/main/res/drawable-nodpi/alerts_screen_background.png` (fondo del Oeste)
- `app/src/main/java/com/example/comunidadwestern/AlertasActivity.java`

## Lógica conservada

- Ruta de historial: `alerts/{communityId}`.
- Consulta limitada a los últimos 50 registros.
- Publicación de eventos MQTT, QoS y suscripción gestionados por `MqttManager`.
- El estado `PENDIENTE` se guarda antes de publicar y cambia a `ENVIADA` cuando MQTT confirma la publicación.
- Las recepciones se guardan en `alertReceipts`.
- Los administradores pueden consultar confirmaciones y marcar alertas como `ATENDIDA` o `CERRADA`.
- Los vecinos pueden confirmar que vieron alertas ajenas; no pueden gestionar su propia alerta.
- Se mantiene la detección local de identificadores MQTT duplicados.

## IDs conservados

- `txtEstadoMqtt`
- `btnActivarAlerta`
- `listaAlertas`
- `btnVolverAlertas`

La lista utiliza una presentación personalizada para cada `DataSnapshot`, sin convertir el historial en contenido estático.

## Validación realizada

- Todos los XML del proyecto se analizaron correctamente.
- Se comprobaron las referencias a recursos gráficos.
- Se comprobó que los IDs anteriores permanecen presentes.
- La compilación completa no pudo verificarse en este entorno porque Gradle 9.5.0 no estaba disponible en caché y no se pudo descargar. Ejecutar `:app:assembleDebug` en Android Studio antes de usar esta versión como entrega final.
