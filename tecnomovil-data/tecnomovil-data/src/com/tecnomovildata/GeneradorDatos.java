package com.tecnomovildata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera un conjunto de datos simulados para poder probar el módulo
 * sin depender de una base de datos real. El caso de estudio dice que
 * el sistema real produce entre 6 y 12 millones de registros diarios;
 * aquí generamos una muestra pequeña (configurable) que representa
 * el mismo tipo de información.
 *
 * Es una función pura en el sentido de que, dado el mismo "seed",
 * siempre devuelve exactamente los mismos datos (no depende de
 * variables externas ni del reloj del sistema), lo que hace que
 * las pruebas sean reproducibles.
 */
public final class GeneradorDatos {

    private static final List<String> RUTAS =
            List.of("R101", "R102", "R103", "R204", "R305");

    private static final List<String> ESTACIONES =
            List.of("Estacion Central", "Estacion Norte", "Estacion Sur",
                    "Estacion Oriente", "Estacion Occidente");

    private GeneradorDatos() {
        // Clase de utilidades: no se debe instanciar.
    }

    public static List<RegistroTransporte> generar(int cantidadUsuarios,
                                                     int registrosPorUsuario,
                                                     long seed) {
        Random random = new Random(seed);
        List<RegistroTransporte> registros = new ArrayList<>();

        for (int usuario = 1; usuario <= cantidadUsuarios; usuario++) {
            String idUsuario = "U" + String.format("%03d", usuario);
            LocalDateTime momentoActual = LocalDateTime.of(2026, 9, 26, 5, 0)
                    .plusMinutes(random.nextInt(60));

            for (int i = 0; i < registrosPorUsuario; i++) {
                String ruta = RUTAS.get(random.nextInt(RUTAS.size()));
                String estacion = ESTACIONES.get(random.nextInt(ESTACIONES.size()));
                String accion = (i % 2 == 0) ? "entrada" : "salida";

                registros.add(new RegistroTransporte(
                        idUsuario, ruta, estacion, accion, momentoActual));

                // Avanzamos el reloj simulado para el próximo registro
                // de este mismo usuario (entre 5 y 40 minutos después).
                momentoActual = momentoActual.plusMinutes(5 + random.nextInt(35));
            }
        }

        // Devolvemos una lista INMUTABLE: nadie que reciba esta lista
        // podrá agregarle, quitarle o modificar elementos.
        return List.copyOf(registros);
    }
}
