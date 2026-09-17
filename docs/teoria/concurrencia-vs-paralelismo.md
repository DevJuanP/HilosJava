---
title: Concurrencia vs paralelismo en programacion
area: desarrollo
tags: [desarrollo, programacion/javascript, arquitectura]
created: 2026-09-17
updated: 2026-09-17
related: ["./como-una-app-atiende-multiples-clientes.md", "./hilos-de-ejecucion-en-profundidad.md"]
---

# Concurrencia vs paralelismo en programacion

Dos conceptos que suelen confundirse: concurrencia es manejar muchas tareas a la vez de forma intercalada, paralelismo es ejecutarlas literalmente al mismo tiempo en varios nucleos. Un sistema paralelo siempre es concurrente, pero uno concurrente no necesariamente es paralelo.

## 1. Definiciones con analogia

* **Concurrencia:** manejar muchas cosas a la vez (intercaladas). Es una forma de **estructurar** el programa: dividir el trabajo en tareas independientes que pueden avanzar sin esperar a que otra termine. No necesita multiples CPUs, solo saber pausar y retomar.
* **Paralelismo:** hacer muchas cosas literalmente al mismo tiempo. Es una forma de **ejecutar**: usar 2 o mas hilos o nucleos de CPU a la vez para calcular mas rapido.

Analogia del cocinero:

> Concurrencia = 1 cocinero que alterna entre picar verduras, vigilar la olla y meter pan al horno. Nada se quema porque cambia rapido de tarea.
>
> Paralelismo = 3 cocineros, cada uno con una tarea, trabajando simultaneamente.

## 2. Concurrencia en detalle

Sirve para **esperar sin bloquear**.

No requiere multiples nucleos. La clave es que cuando una tarea se queda esperando (red, disco, base de datos, timer), el programa la pausa y avanza con otra, y la retoma cuando la espera termina.

En JavaScript esto es el comportamiento por defecto gracias al **Event Loop**: hay un solo hilo principal, pero las esperas se delegan al sistema y cuando terminan se retoma el codigo con callbacks, Promises o `async-await`.

```js
// Concurrente: lanzo 2 peticiones sin bloquearme,
// avanzan intercaladas aunque JS tenga 1 solo hilo.
const usuarios = fetch("/api/usuarios");
const productos = fetch("/api/productos");
const [u, p] = await Promise.all([usuarios, productos]);
```

Es el 90% de lo que se hace en JS: `async-await`, `Promise.all`, `fetch`, lectura de archivos, consultas a base de datos.

## 3. Paralelismo en detalle

Sirve para **calcular sin congelar**.

Requiere de verdad 2 o mas hilos o nucleos. Si haces un `for` pesadisimo de 5 segundos en el hilo principal, no hay concurrencia que te salve: la pagina o el servidor se congelan.

Como el hilo principal de JS es unico, para paralelismo real hay que salir de el:

* Navegador: `Web Workers`.
* Node.js: `worker_threads`, `child_process`, `cluster`.

Idea conceptual: el hilo principal crea Worker 1 para procesar imagen A y Worker 2 para imagen B. Los 3 corren en paralelo en distintos nucleos.

Casos tipicos: procesar video o imagenes, cifrado, parsear CSV gigante, fisica de un juego, IA en cliente.

Se puede combinar: lanzar 4 Workers en paralelo y coordinarlos de forma concurrente con `Promise.all`.

## 4. Similitudes

1. Ambos buscan aprovechar mejor el tiempo y los recursos.
2. Ambos trabajan con tareas independientes.
3. Ambos obligan a pensar en orden, coordinacion y errores (que pasa si fallan 2 tareas a la vez).

## 5. Diferencias

| Dimension | Concurrencia | Paralelismo |
| :--- | :--- | :--- |
| Objetivo | No bloquearse esperando | Terminar calculos mas rapido |
| Como | Intercalar tareas | Dividir trabajo en CPUs o hilos |
| Hardware | Funciona con 1 nucleo | Requiere 2+ nucleos o hilos |
| En JS | `async-await`, `Promise.all`, `fetch` | `Worker`, `worker_threads` |
| Problema tipico | Orden de llegada, condicion de carrera leve | Condicion de carrera real, memoria compartida |
| Ideal para | I/O-bound (red, disco) | CPU-bound (calculos) |

## 6. Cuando usar cada uno en JS

**Usa concurrencia (async) cuando esperas algo externo.** Ejemplos: 10 `fetch` a una API, leer archivos, consultar base de datos, timers. Es barato y es lo habitual.

**Usa paralelismo (Workers) cuando la CPU es el cuello de botella.** Ejemplos: procesar imagenes, comprimir video, cifrado, calculos pesados.

Regla practica:

> Si la app se queda esperando -> concurrencia. Si la app se pone lenta calculando -> paralelismo.

## 7. Que significa "bound"

"Bound" significa **limitado por**. Indica cual es el cuello de botella del programa.

* **I/O-bound:** el programa pasa el tiempo esperando entrada y salida (red, disco, base de datos). La CPU esta ociosa. Solucion: concurrencia. Ejemplo tipico: API con Express que hace 20 consultas.
* **CPU-bound:** el programa pasa el tiempo calculando. Los nucleos van al 100%. Solucion: paralelismo u optimizar el algoritmo. Ejemplo: comprimir imagenes en el navegador.
* **Memory-bound:** menos comun, el limite es la RAM o el ancho de banda de memoria. Ejemplo: procesar un array gigante que no cabe en cache y se vuelve lento aunque haya CPU libre.

Por eso se dice: "Node.js es genial para I/O-bound, malo para CPU-bound", porque su modelo concurrente de un hilo brilla esperando, pero sufre calculando.

## 8. Otra jerga del tema

* **Blocking / Non-blocking (bloqueante):** si una operacion congela el hilo hasta terminar o deja seguir. En JS casi todo debe ser non-blocking.
* **Sync / Async:** sync se ejecuta en orden y espera; async se lanza y avisa despues.
* **Event Loop, Task Queue, Microtask:** el mecanismo de JS para hacer concurrencia con un hilo. `await` y `Promise.then` van a microtasks (prioritarias), `setTimeout` va a tasks.
* **Thread (hilo) / Process (proceso):** hilo = unidad ligera de ejecucion que comparte memoria. Proceso = programa aislado con su propia memoria. Workers en Node son hilos, `child_process` son procesos.
* **Race Condition:** dos tareas acceden o modifican lo mismo y el resultado depende de quien llegue primero. El bug mas clasico de esto.
* **Deadlock:** tarea A espera a B, y B espera a A. Nadie avanza.
* **Starvation / Livelock:** una tarea nunca consigue turno, o dos tareas se esquivan eternamente sin avanzar.
* **Thread-safe / Atomic:** codigo que se puede usar desde varios hilos sin romperse. Operacion atomica = se completa de golpe o no se completa, sin intermedios. En JS: `SharedArrayBuffer + Atomics`.
* **Context Switch:** el costo de cambiar de una tarea o hilo a otro. Por eso crear mil hilos no siempre es mas rapido.
* **Throughput vs Latency:** throughput = cuantas tareas por segundo. Latency = cuanto tarda una tarea. Concurrencia suele mejorar throughput, paralelismo suele mejorar latency de calculos.
* **Scalability:** horizontal (mas maquinas o Workers) vs vertical (maquina mas potente).

Resumen para JS: JS es concurrente por defecto y paralelo por excepcion. `async-await` para el dia a dia (I/O-bound), Workers para cuando el CPU quema (CPU-bound).

## Ver también

* [Como una app atiende multiples clientes](como-una-app-atiende-multiples-clientes.md)
* [Hilos de ejecucion en profundidad](hilos-de-ejecucion-en-profundidad.md)
* [Concurrencia y paralelismo](_index.md)
* [Arquitectura](../_index.md)
* [Desarrollo de sistemas](../../_index.md)
