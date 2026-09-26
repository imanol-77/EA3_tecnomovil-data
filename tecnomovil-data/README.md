# TecnoMovil Data — Módulo funcional de procesamiento de datos

Solución al caso de estudio "Procesamiento funcional de datos para un sistema
de gestión de transporte urbano" (Alcaldía de TecnoValle).

## Integrantes del grupo

- Imanol Mejia Lotero
- Luis Fernando Castaño
- Andres Jeronimo Parra Bastidas
- Juan Andres Cadavid Correa

## Estructura del proyecto

```
tecnomovil-data/
├── src/com/tecnomovildata/
│   ├── RegistroTransporte.java   -> modelo inmutable de un registro
│   ├── GeneradorDatos.java       -> genera datos simulados
│   ├── AnalizadorTransporte.java -> las 6 operaciones funcionales pedidas
│   └── Main.java                 -> ejecuta todo y muestra resultados
└── README.md
```

## Cómo compilar y ejecutar

No se usan librerías externas ni frameworks: solo Java estándar (JDK 17+).

```bash
# Desde la carpeta tecnomovil-data/
javac -d out src/com/tecnomovildata/*.java
java -cp out com.tecnomovildata.Main
```

Si los acentos se ven como `?` en la consola, es solo un tema de
configuración regional del terminal (no del código); ejecuta con:

```bash
java -Dfile.encoding=UTF-8 -cp out com.tecnomovildata.Main
```

## Relación entre los requerimientos del caso y la implementación

| Requerimiento del caso | Método | Ubicación |
|---|---|---|
| a) Afluencia por estación | `calcularAfluenciaPorEstacion` | AnalizadorTransporte |
| b) Horas pico | `identificarHorasPico` / `horaConMayorFlujo` | AnalizadorTransporte |
| c) Rutas más utilizadas | `rutasMasUtilizadas` | AnalizadorTransporte |
| d) Patrones de viaje por usuario | `patronesViajePorUsuario` | AnalizadorTransporte |
| e) Tiempo promedio entre estaciones | `tiempoPromedioEntreEstaciones` | AnalizadorTransporte |
| f) Detección de sobrecarga en rutas | `detectarSobrecargaEnRutas` | AnalizadorTransporte |

## Conceptos de programación funcional aplicados

Esta sección está pensada para copiarse (y ampliarse con capturas propias)
en el informe del grupo, en la parte de "Desarrollo".

### 1. Funciones puras
Todos los métodos de `AnalizadorTransporte` son **funciones puras**: para
la misma lista de entrada siempre devuelven exactamente el mismo resultado,
y no producen ningún efecto secundario (no imprimen nada, no escriben
archivos, no dependen de variables globales ni de la hora del sistema).
Esto es justo lo que pide el caso de estudio ("la construcción de las
operaciones debe evitar totalmente efectos secundarios"). El único lugar
del programa con efectos secundarios es `Main`, que es intencionalmente
la única capa que "toca el mundo exterior" (imprime en consola).

### 2. Inmutabilidad
- `RegistroTransporte` es un `record`: en Java, un record es inmutable por
  diseño, sus campos no se pueden reasignar después de crearlo.
- `GeneradorDatos.generar(...)` devuelve la lista envuelta en
  `List.copyOf(...)`, lo que produce una lista que lanza una excepción si
  alguien intenta modificarla.
- Ninguna operación de `AnalizadorTransporte` modifica la lista que recibe:
  siempre se construyen colecciones (`Map`, `List`) completamente nuevas
  como resultado.

### 3. Funciones de orden superior
Una función de orden superior es aquella que recibe otra función como
parámetro o devuelve una función. En este proyecto se usan constantemente
a través de los métodos de `Stream`, por ejemplo:
- `filter(r -> r.accion().equals("entrada"))` recibe una función (un
  predicado) como argumento.
- `Collectors.groupingBy(RegistroTransporte::estacion, ...)` recibe una
  función (una "referencia a método") que le indica cómo agrupar.
- `Collectors.mapping(RegistroTransporte::estacion, Collectors.toList())`
  combina dos funciones de orden superior para transformar y luego
  recolectar.

### 4. Lambdas
Las expresiones lambda (`r -> r.accion().equals("entrada")`,
`(estacion, total) -> System.out.println(...)`, etc.) son la forma
compacta de escribir esas funciones que se pasan como parámetro, en vez de
crear una clase o un método aparte para cada una. Cuando la lambda es solo
"llamar a un método existente sobre el parámetro", se usa la forma aún más
corta de referencia a método (`RegistroTransporte::ruta`).

### 5. Streams
Cada operación se resuelve como un **pipeline declarativo** de Stream, es
decir, se describe *qué* se quiere obtener (filtrar, agrupar, contar,
ordenar) en vez de *cómo* recorrer manualmente la lista con un `for`. Por
ejemplo, el pipeline de "rutas más utilizadas" es:

```
registros.stream()
    .collect(groupingBy(ruta, counting()))   // 1. contar por ruta
    .entrySet().stream()
    .sorted(comparingByValue().reversed())   // 2. ordenar de mayor a menor
    .collect(toList());                      // 3. materializar resultado
```

### 6. Preparado para paralelización
`calcularAfluenciaPorEstacion` usa `parallelStream()` en vez de `stream()`
para mostrar que, al no haber estado mutable compartido ni efectos
secundarios, la misma operación puede ejecutarse en paralelo sin cambiar
ni una línea de lógica ni arriesgar condiciones de carrera — exactamente
el problema que tenía el sistema imperativo original de la Alcaldía.

## Evidencia de ejecución

Para el informe, ejecuta el programa como se indica arriba y toma una
captura de pantalla de la salida por consola (que muestra los resultados
de las 6 tareas a), b), c), d), e) y f)).
