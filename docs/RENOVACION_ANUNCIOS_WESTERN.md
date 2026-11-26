# Renovación visual: Anuncios oficiales

Se renovó la pantalla de anuncios tomando como referencia el mockup western entregado para esta iteración.

## Archivos visuales

- `app/src/main/res/layout/activity_anuncios.xml`: cabecera, descripción, formulario de administración, tablón de anuncios y botón volver.
- `app/src/main/res/layout/item_anuncio.xml`: tarjeta visual de cada anuncio en el listado.
- `app/src/main/res/layout/activity_anuncio_detalle.xml`: vista de lectura de un anuncio individual.
- `app/src/main/res/drawable-nodpi/ann_*.png`: recursos PNG de anuncios oficiales.
- `app/src/main/res/drawable-nodpi/ann_wood_background.png`: textura de madera para el fondo.
- `app/src/main/res/drawable/bg_ann_admin_panel.xml`: panel del formulario de administrador.
- `app/src/main/res/drawable/bg_ann_official_stamp.xml`: sello decorativo de anuncio oficial.

## Integración y comportamiento conservado

`AnunciosActivity.java` mantiene la lectura y publicación en Firebase Realtime Database, la consulta del rol `admin`, la validación de título y contenido, el límite de 100 anuncios cargados y la apertura de `AnuncioDetalleActivity` al tocar una tarjeta. El listado muestra los anuncios recientes primero y presenta título, autor, vivienda, contenido y fecha.

Se mantienen los IDs de controles utilizados por el flujo de anuncios: `panelAdminAnuncio`, `txtTituloAnuncio`, `txtCuerpoAnuncio`, `btnPublicarAnuncio`, `listaAnuncios` y `btnVolverAnuncios`. También se mantienen los IDs de la pantalla de detalle.

No se cambió el servicio de FCM, el relay local, la configuración MQTT, las reglas de Firebase ni la estructura de datos de anuncios.

## Comprobaciones realizadas

- XML del proyecto parseados correctamente.
- Referencias a recursos de las tres pantallas revisadas; no se encontraron recursos faltantes.
- IDs de navegación, publicación y detalle conservados.
- No se pudo ejecutar `:app:assembleDebug` en el entorno de preparación porque Gradle Wrapper intentó descargar Gradle 9.5.0 y no había conexión al servidor de distribución. Se debe compilar y probar desde Android Studio en el equipo de desarrollo.
