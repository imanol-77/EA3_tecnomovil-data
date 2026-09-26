package com.tecnomovildata;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Módulo funcional de procesamiento y agregación de datos diarios para
 * TecnoMovil Data.
 *
 * REGLAS DE DISEÑO (pedidas explícitamente en el caso de estudio):
 *   - Todas las funciones son PURAS: para la misma entrada siempre
 *     devuelven la misma salida y no dependen de ni modifican estado
 *     externo (no hay variables globales, no hay "System.out.println"
 *     dentro de estos métodos, no hay campos mutables en esta clase).
 *   - INMUTABILIDAD: ninguna lista de entrada se modifica; siempre se
 *     construyen y devuelven colecciones nuevas.
 *   - Todo se resuelve con Streams + lambdas + funciones de orden
 *     superior (map, filter, reduce, groupingBy, etc.), nunca con
 *     bucles for/while imperativos ni contadores mutables manuales.
 */
public final class AnalizadorTransporte {

    private AnalizadorTransporte() {
        // Clase de utilidades: solo métodos estáticos, no se instancia.
    }

    /**
     * a) Cálculo de afluencia por estación.
     * Cuenta cuántos usuarios ENTRAN a cada estación durante el día.
     *
     * Pipeline: filter -> groupingBy -> counting
     * (usa parallelStream() para mostrar que, al ser una operación
     * pura y sin efectos secundarios, es segura de paralelizar).
     */
    public static Map<String, Long> calcularAfluenciaPorEstacion(
            List<RegistroTransporte> registros) {

        return registros.parallelStream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::estacion,
                        Collectors.counting()));
    }

    /**
     * b) Identificación de horas pico.
     * Agrupa TODOS los registros por hora del día (0-23) y cuenta
     * cuántos ocurrieron en cada hora, para detectar los momentos
     * de mayor flujo.
     *
     * Pipeline: groupingBy (por hora) -> counting
     */
    public static Map<Integer, Long> identificarHorasPico(
            List<RegistroTransporte> registros) {

        return registros.stream()
                .collect(Collectors.groupingBy(
                        r -> r.timestamp().getHour(),
                        Collectors.counting()));
    }

    /**
     * Utilidad sobre el resultado anterior: devuelve la hora (u horas)
     * con mayor número de registros. Se deja como función aparte para
     * que cada paso siga siendo una función pura de una sola
     * responsabilidad.
     */
    public static List<Integer> horaConMayorFlujo(Map<Integer, Long> horasPico) {
        long maximo = horasPico.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        return horasPico.entrySet().stream()
                .filter(entrada -> entrada.getValue() == maximo)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * c) Rutas más utilizadas.
     * Cuenta cuántos registros tiene cada ruta y las ordena de mayor
     * a menor uso.
     *
     * Pipeline: groupingBy -> counting -> sorted (por valor, descendente)
     */
    public static List<Map.Entry<String, Long>> rutasMasUtilizadas(
            List<RegistroTransporte> registros) {

        Map<String, Long> conteoPorRuta = registros.stream()
                .collect(Collectors.groupingBy(
                        RegistroTransporte::ruta,
                        Collectors.counting()));

        return conteoPorRuta.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    /**
     * d) Patrones de viaje por usuario.
     * Para cada usuario, genera la lista de estaciones visitadas EN EL
     * ORDEN en que aparecen los registros (por eso primero se ordena
     * por timestamp antes de agrupar).
     *
     * Pipeline: sorted (por timestamp) -> groupingBy (LinkedHashMap
     * para conservar el orden) -> mapping (a solo el nombre de estación)
     */
    public static Map<String, List<String>> patronesViajePorUsuario(
            List<RegistroTransporte> registros) {

        return registros.stream()
                .sorted(Comparator.comparing(RegistroTransporte::timestamp))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::idUsuario,
                        LinkedHashMap::new,
                        Collectors.mapping(RegistroTransporte::estacion,
                                Collectors.toList())));
    }

    /**
     * e) Cálculo de tiempo promedio entre estaciones (por usuario).
     * Ordena los registros de cada usuario por tiempo, calcula la
     * diferencia (en minutos) entre estaciones consecutivas y saca
     * el promedio.
     *
     * Pipeline: groupingBy -> (dentro) sorted -> map a diferencias ->
     * averagingLong
     */
    public static Map<String, Double> tiempoPromedioEntreEstaciones(
            List<RegistroTransporte> registros) {

        Map<String, List<RegistroTransporte>> porUsuario = registros.stream()
                .collect(Collectors.groupingBy(RegistroTransporte::idUsuario));

        return porUsuario.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entrada -> promedioDeDiferenciasEnMinutos(entrada.getValue())
                ));
    }

    /**
     * Función pura auxiliar: recibe los registros de UN usuario y
     * devuelve el promedio (en minutos) entre timestamps consecutivos.
     * No modifica la lista recibida (crea una copia ordenada nueva).
     */
    private static double promedioDeDiferenciasEnMinutos(
            List<RegistroTransporte> registrosDeUnUsuario) {

        List<LocalDateTime> tiempos = registrosDeUnUsuario.stream()
                .sorted(Comparator.comparing(RegistroTransporte::timestamp))
                .map(RegistroTransporte::timestamp)
                .collect(Collectors.toList());

        return java.util.stream.IntStream.range(1, tiempos.size())
                .mapToLong(i -> Duration.between(tiempos.get(i - 1), tiempos.get(i)).toMinutes())
                .average()
                .orElse(0.0);
    }

    /**
     * f) Detección de sobrecarga en rutas.
     * Si una ruta supera el umbral de ocupación indicado (número de
     * registros que se usa aquí como aproximación simulada a
     * "cantidad de pasajeros"), se marca como "crítica"; en caso
     * contrario, como "normal".
     *
     * Pipeline: groupingBy -> counting -> map a etiqueta ("crítica"/"normal")
     */
    public static Map<String, String> detectarSobrecargaEnRutas(
            List<RegistroTransporte> registros, long umbral) {

        Map<String, Long> conteoPorRuta = registros.stream()
                .collect(Collectors.groupingBy(
                        RegistroTransporte::ruta,
                        Collectors.counting()));

        return conteoPorRuta.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entrada -> entrada.getValue() > umbral ? "crítica" : "normal"
                ));
    }
}
