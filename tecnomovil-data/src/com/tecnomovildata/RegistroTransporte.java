package com.tecnomovildata;

import java.time.LocalDateTime;

/**
 * Representa un único registro capturado por el sistema TecnoMovil Data
 * (validador, sensor IoT o app móvil).
 *
 * Se implementa como "record" porque un record en Java es, por diseño,
 * INMUTABLE: una vez creado, ninguno de sus campos puede cambiar.
 * Esto encaja directamente con el requisito del caso de estudio de
 * "no modificar" los datos originales.
 */
public record RegistroTransporte(
        String idUsuario,
        String ruta,
        String estacion,
        String accion,      // "entrada" o "salida"
        LocalDateTime timestamp
) {
}
