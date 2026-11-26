# Renovación visual — Duelo del Oeste

Se renovaron las pantallas del menú del minijuego y de la sala usando recursos PNG occidentales individuales, controles Android reales y paneles de pergamino/madera.

## Recursos y reutilización

Los PNG se guardan en `app/src/main/res/drawable-nodpi/` con prefijo `duel_` para evitar colisiones de nombres con recursos existentes. No había duplicados binarios exactos dentro del catálogo de recursos que ya utilizaba el proyecto. Los mismos botones de volver y paneles se reutilizan entre las pantallas del propio minijuego, en lugar de guardar copias.

## Funcionamiento conservado

- Selección Mejor de 1, 3 o 5.
- Código hexadecimal de sala de seis caracteres.
- Creación y unión de salas mediante Realtime Database.
- Cuenta aleatoria entre 3 y 5 segundos.
- Penalización por falsa salida.
- Sincronización de puntuaciones y resultados mediante transacciones.
- Botón disparar mantiene su listener y envía la acción a la lógica existente.

## Cómo jugar

Se renovó el diálogo de instrucciones con un panel de pergamino. Se eliminó la frase solicitada sobre la primera jugada aceptada por Firebase. No se modificó la lógica del juego.

## Archivos principales

- `res/layout/activity_duelo.xml`
- `res/layout/activity_duelo_sala.xml`
- `res/layout/dialog_duelo_reglas.xml`
- `DueloActivity.java` (solo presentación del diálogo de reglas)
- `res/drawable-nodpi/duel_*.png`
- `res/values/duel_colors.xml`
