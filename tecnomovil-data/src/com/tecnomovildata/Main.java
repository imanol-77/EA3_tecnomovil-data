package com.tecnomovildata;

import java.util.List;
import java.util.Map;

/**
 * Punto de entrada del programa. Aquí SÍ hay efectos secundarios
 * (imprimir en consola), y eso es intencional: es la única capa del
 * programa que "toca el mundo exterior". Todo el procesamiento de
 * datos real ocurre en AnalizadorTransporte, que es 100% puro.
 */
public class Main {

    public static void main(String[] args) {

        // 1) Generamos datos simulados: 12 usuarios con 6 registros
        //    cada uno (equivalente en miniatura a los millones de
        //    registros diarios reales del sistema).
        List<RegistroTransporte> registros =
                GeneradorDatos.generar(12, 6, 42L);

        System.out.println("=== TecnoMovil Data: módulo funcional de análisis ===");
        System.out.println("Total de registros procesados: " + registros.size());
        System.out.println();

        // a) Afluencia por estación
        System.out.println("a) Afluencia por estación (entradas):");
        Map<String, Long> afluencia = AnalizadorTransporte.calcularAfluenciaPorEstacion(registros);
        afluencia.forEach((estacion, total) ->
                System.out.println("   - " + estacion + ": " + total + " usuarios"));
        System.out.println();

        // b) Horas pico
        System.out.println("b) Registros por hora del día:");
        Map<Integer, Long> horas = AnalizadorTransporte.identificarHorasPico(registros);
        horas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entrada ->
                        System.out.println("   - Hora " + entrada.getKey() + ":00 -> " + entrada.getValue() + " registros"));
        List<Integer> horaPico = AnalizadorTransporte.horaConMayorFlujo(horas);
        System.out.println("   >> Hora(s) pico: " + horaPico);
        System.out.println();

        // c) Rutas más utilizadas
        System.out.println("c) Rutas más utilizadas (de mayor a menor uso):");
        AnalizadorTransporte.rutasMasUtilizadas(registros)
                .forEach(entrada ->
                        System.out.println("   - " + entrada.getKey() + ": " + entrada.getValue() + " registros"));
        System.out.println();

        // d) Patrones de viaje por usuario
        System.out.println("d) Patrones de viaje por usuario:");
        Map<String, List<String>> patrones = AnalizadorTransporte.patronesViajePorUsuario(registros);
        patrones.forEach((usuario, estaciones) ->
                System.out.println("   - " + usuario + ": " + estaciones));
        System.out.println();

        // e) Tiempo promedio entre estaciones
        System.out.println("e) Tiempo promedio entre estaciones consecutivas (minutos):");
        Map<String, Double> tiempos = AnalizadorTransporte.tiempoPromedioEntreEstaciones(registros);
        tiempos.forEach((usuario, promedio) ->
                System.out.printf("   - %s: %.1f min%n", usuario, promedio));
        System.out.println();

        // f) Detección de sobrecarga en rutas
        long umbralSimulado = 10L; // ajustable según el volumen de datos generado
        System.out.println("f) Estado de las rutas (umbral simulado = " + umbralSimulado + " registros):");
        Map<String, String> estadoRutas = AnalizadorTransporte.detectarSobrecargaEnRutas(registros, umbralSimulado);
        estadoRutas.forEach((ruta, estado) ->
                System.out.println("   - " + ruta + ": " + estado));
    }
}
