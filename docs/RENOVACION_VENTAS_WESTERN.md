# Renovación visual – Ventas comunitarias

## Qué cambió
- Se renovó `app/src/main/res/layout/activity_ventas.xml` con la identidad gráfica western: cartel superior, pergamino, panel de formulario, marcos de campos, botón de publicación, sección de publicaciones y botón Volver.
- Se creó `app/src/main/res/layout/item_venta.xml` para representar cada publicación en una ficha de pergamino con ilustración de producto, título, descripción, precio, vendedor, vivienda, fecha y sello de VENDIDO.
- Se incorporaron los 16 PNG de `app/src/main/res/drawable-nodpi/` con prefijo `sales_`.
- Se añadieron a `values/colors.xml` los cinco colores western sugeridos, manteniendo los colores ya existentes.
- `VentasActivity.java` se actualizó para usar un adaptador propio y mostrar los datos existentes de Firebase en las nuevas fichas.

## Funciones que se conservaron
- La ruta Realtime Database sigue siendo `sales/{communityId}`.
- Se mantiene la consulta ordenada por `createdAt` y limitada a los últimos 100 productos.
- Se conservan los campos y sus IDs: `txtProducto`, `txtDescripcionProducto`, `txtPrecioProducto`, `btnPublicarProducto`, `listaVentas` y `btnVolverVentas`.
- La validación sigue usando `AppValidation.isValidSale`.
- La publicación conserva los campos `title`, `description`, `price`, `sellerUid`, `sellerName`, `housing`, `status` y `createdAt`.
- El usuario que publicó un producto puede marcarlo como vendido mientras esté disponible; el comportamiento de permisos de Firebase no se modificó.
- Se mantiene la liberación del listener al destruir la actividad.

## Nota sobre fotografías de productos
La estructura actual de ventas no almacena una fotografía del producto. Por eso la ficha usa un cajón western como ilustración genérica; no se agregó una función de carga de imágenes ni se modificó el esquema de Firebase.

## Prueba sugerida en Android Studio
1. Abrir el proyecto y ejecutar Gradle Sync.
2. Compilar con `:app:assembleDebug`.
3. Iniciar sesión con un usuario y abrir Ventas comunitarias.
4. Publicar un producto con nombre, descripción y precio válidos.
5. Confirmar que aparece la ficha en el listado y que se sincroniza en otro emulador.
6. Tocar el producto con la misma cuenta que lo publicó y marcarlo como vendido.
7. Confirmar que aparece el sello VENDIDO.

La validación XML y de referencias de recursos fue realizada sobre este paquete. La compilación Android debe confirmarse en el entorno local con Android SDK instalado.
