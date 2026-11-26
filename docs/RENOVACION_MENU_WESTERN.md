# Renovación visual del menú principal

Se renovó `activity_home.xml` utilizando las ilustraciones PNG separadas del paquete visual.

## Cambios de interfaz
- Emblema Comunidad Western en la parte superior.
- Saludo y número de vivienda dinámicos conservando `txtBienvenida` y `txtViviendaHome`.
- Cartel de sección `Espacios de la comunidad`.
- Botones gráficos individuales de chat, ventas, anuncios, alertas, duelo, perfil y cerrar sesión.
- Fondo western con paisaje, lámparas, pueblo y detalles decorativos.
- Imágenes adaptadas a `drawable-nodpi` para evitar diferencias de densidad.
- Se conservaron los IDs que utiliza `HomeActivity.java`, de modo que los listeners existentes siguen conectados.
- El login usa el vaquero con globo de chat y el icono adaptable usa la misma identidad.

## Importante sobre credenciales
El proyecto se entrega sin `local.properties` ni archivos de cuenta de servicio de Firebase Admin.
Android Studio generará `local.properties` con la ruta del SDK. Después agrega las propiedades MQTT
según `local.properties.example`. La cuenta de servicio para `announcement-relay` debe mantenerse
fuera de esta carpeta y fuera de GitHub.

## Prueba
Abre el proyecto en Android Studio, ejecuta Gradle Sync y compila `:app:assembleDebug`.
No se alteró la lógica Java de navegación del menú. La validación visual definitiva debe realizarse
en el emulador, porque las proporciones finales dependen de la densidad y tamaño de pantalla.
