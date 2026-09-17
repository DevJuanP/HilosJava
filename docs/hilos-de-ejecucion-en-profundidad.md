---
title: Hilos de ejecucion en profundidad
area: desarrollo
tags: [desarrollo, arquitectura, programacion/java]
created: 2026-09-17
updated: 2026-09-17
related: ["./concurrencia-vs-paralelismo.md", "./como-una-app-atiende-multiples-clientes.md"]
---

# Hilos de ejecucion en profundidad

Un hilo es la unidad minima que la CPU puede ejecutar. Vive dentro de un proceso, tiene su propio stack y estado de ejecucion, pero comparte la memoria del proceso con los demas hilos. Entender esto explica por que existen la concurrencia, el paralelismo, las race conditions y los tres modelos con los que un servidor atiende a muchos clientes.

## 1. Proceso vs hilo

Un programa en disco no hace nada. Al ejecutarlo, el sistema operativo crea un **proceso**: un contenedor aislado con su propia memoria virtual, archivos abiertos y permisos.

Dentro de ese proceso viven los **hilos**:

* Cada hilo tiene lo suyo: program counter (que instruccion toca ahora), registros de CPU y stack propio (variables locales, llamadas a funciones).
* Todos los hilos del mismo proceso comparten: heap (objetos, arrays), codigo y archivos o sockets abiertos.

Analogia:

> Proceso = casa con paredes. Hilo = persona dentro de la casa. Cada persona tiene su mochila (stack), pero todas comparten cocina y bano (heap).

Un proceso siempre tiene al menos 1 hilo (el principal). Puede crear 10, 100 o miles mas.

## 2. Correccion clave: los hilos SI pueden pisarse

Es un error comun pensar que cada hilo "tiene permisos" y no puede tocar lo mismo que otro. Es al reves: en un mismo proceso, **todos los hilos SI pueden tocar lo mismo**, y el sistema no lo impide.

La analogia de la cebolla lo deja claro: si dos cocineros intentan picar la misma cebolla a la vez, la cebolla queda destrozada. En codigo eso es una `race condition`: dos hilos modifican el mismo dato y el resultado depende de quien llegue primero, quedando corrupto.

Lo privado de cada hilo es solo su mochila (stack, registros, instruccion actual). Todo lo demas es cocina compartida, asi que el programador debe poner reglas: locks o, mejor, no compartir memoria y comunicarse por mensajes.

Los que si estan aislados con permisos son los **procesos**, no los hilos. Un proceso no toca la memoria de otro sin mediacion del sistema operativo.

## 3. Como la CPU ejecuta cientos de hilos con pocos nucleos

Con 8 nucleos y 500 hilos activos, el sistema hace **scheduling** con reparto de tiempo (time slicing):

1. Da a cada hilo unos milisegundos de CPU (quantum).
2. Cuando se acaba, le quita la CPU, guarda sus registros (context switch) y pone otro hilo.
3. Cambia tan rapido que parece simultaneo.

Solo hay paralelismo real cuando dos hilos estan en **dos nucleos distintos en el mismo instante**. Todo lo demas es concurrencia por turnos.

El `context switch` no es gratis: guardar y cargar registros e invalidar cache cuesta. Por eso 10.000 hilos del sistema van lento: se gasta mas tiempo cambiando que trabajando.

Estados de un hilo: nuevo, listo, corriendo, bloqueado (esperando red, disco o un lock) y terminado. Cuando se bloquea esperando I/O, el sistema lo saca y mete otro. Esa es la base de todo servidor.

## 4. Paralelismo real vs concurrencia: ejemplo de 4 nucleos y 400 hilos

Pregunta tipica: si un CPU tiene 4 nucleos y cada nucleo ejecuta 100 hilos, esos hilos estan en paralelismo.

Respuesta: son **las dos cosas a la vez**.

* Paralelismo real: maximo 4. En un nanosegundo dado, solo 4 hilos corren de verdad, uno por nucleo.
* Concurrencia: 400. Los 100 hilos de cada nucleo se turnan rapidisimo. El nucleo hace: hilo 1 unos ms, guarda, hilo 2, guarda, hilo 3, y asi.

Es como 4 cocinas (nucleos) con 100 cocineros cada una esperando turno. Solo 4 cocinan al mismo instante, pero los 400 avanzan intercalados.

Reglas que salen de aqui:

* Paralelismo maximo = numero de nucleos. Crear mas hilos que nucleos no da mas paralelismo.
* Si la tarea es CPU-bound, 400 hilos en 4 nucleos van igual o mas lento que 4 hilos por el costo de cambiar.
* Si la tarea es I/O-bound, muchos hilos si sirven: mientras uno espera base de datos, otro usa el nucleo. Ver [Concurrencia vs paralelismo](concurrencia-vs-paralelismo.md).

## 5. Tipos de hilos que existen

No todos los "hilos" son hilos del sistema operativo:

1. **Kernel threads (hilos del OS):** los de verdad. Java clasico, `pthread` en C, `worker_threads` en Node. Potentes, pero cada uno consume alrededor de 1 a 2 MB de stack y cambiar entre ellos es costoso. Mas de unos miles ya duele.
2. **Event Loop + threadpool:** el truco de Node, Nginx y Redis. Un hilo principal intercala miles de conexiones y una piscinita pequena (libuv usa 4 por defecto) atiende por detras lo que bloquea (archivos, crypto, DNS). No se crea un hilo por cliente, se reutilizan.
3. **Hilos virtuales o ligeros:** el runtime crea millones de mini hilos baratos y los mapea a pocos hilos del OS. Ejemplos: goroutines en Go, Virtual Threads en Java 21, tasks de asyncio. Dan la comodidad de "un hilo por cliente" sin el costo.

JavaScript vive en el modelo 2, con escapes al modelo 1 cuando necesita fuerza de calculo.

## 6. Los tres modelos de servidor en profundidad

### 6.1 Thread por cliente

Como en Apache con PHP clasico, Java viejo o Rails clasico. Llega un cliente, se acepta la conexion, se toma 1 hilo del sistema y ese hilo ejecuta todo: parsea HTTP, consulta la base bloqueandose, responde y muere.

Ventaja: codigo lineal y facil de razonar. Cada peticion es un mundo aislado.

Problemas: 10k conexiones son 10k stacks de 1 MB, unas 10 GB solo en hilos. El sistema se ahoga cambiando entre ellos y la base de datos colapsa si solo aguanta 100 conexiones. Hoy solo se usa con pool limitado (por ejemplo 200 hilos mas cola de espera): si la cola se llena, se responde 503.

Tiene sentido con logica muy de CPU o librerias bloqueantes viejas donde no se puede usar async.

### 6.2 Event Loop

Como en Node.js, Nginx o Redis. Un solo hilo principal en bucle infinito: acepta conexiones sin bloquear, delega esperas al sistema (epoll en Linux, kqueue en macOS) y ejecuta uno por uno los callbacks que ya estan listos.

Ventaja: con poca memoria atiende 10k a 50k conexiones keep-alive y casi no hay context switch.

Precio: todo el codigo debe ser non-blocking. Un solo `for` pesado o un parse gigante congela a todos los clientes. Ademas solo usa 1 nucleo por proceso. Por eso Node delega red al sistema (cero hilos) y archivos, crypto y DNS a un pool pequeno configurable con `UV_THREADPOOL_SIZE`.

Es ideal para APIs, chats, streaming y gateways: todo I/O-bound.

### 6.3 Hibrido moderno

Lo que usa una app real: Event Loop para atender miles de sockets barato, pool o Workers para calcular, multiples procesos para usar todos los nucleos (cluster) y multiples maquinas para aguantar carga (balanceador).

En Node se ve asi: el Loop atiende, si la ruta es rapida la resuelve ahi, si es procesar imagen la manda a `worker_threads`, si hay 8 nucleos se lanzan 8 procesos con Cluster o PM2, y si hay 3 servidores Nginx o el balanceador reparte.

Go y Java con Loom hacen lo mismo pero automatico: se escribe codigo lineal como en el modelo 1 y el runtime lo ejecuta como el modelo 2, con pocos hilos del OS y miles virtuales. Ver [Como una app real atiende multiples clientes](como-una-app-atiende-multiples-clientes.md).

## 7. Como se coordinan los hilos sin romperse

Cuando hay memoria compartida hace falta sincronizacion:

* **Mutex o Lock:** solo 1 hilo entra a la vez a la zona critica. Simple, pero si se olvida liberarlo hay deadlock.
* **Semaphore:** entran N a la vez. Ejemplo: pool de 20 conexiones a base de datos.
* **Operacion atomica:** se hace de golpe, sin intermedio. En JS: `Atomics.add(sharedArray, i, 1)` sobre `SharedArrayBuffer`.
* **Mensajes en vez de memoria:** los Workers no comparten objetos, se mandan mensajes con `postMessage`. Sin memoria compartida no hay carreras. Solo se usa memoria compartida si de verdad hace falta velocidad extrema.

Otros terminos que aparecen aqui: deadlock (A espera a B y B espera a A), starvation (una tarea nunca consigue turno), livelock (dos tareas se esquivan sin avanzar) y thread-safe (codigo usable desde varios hilos sin romperse).

Regla de oro: si puedes, no compartas; comunicate.

## 8. Ejemplo de la imagen: 4 hilos que tardan 20 segundos y no 50

La imagen del Main Thread con Thread 1 a 4 y procesos de 15, 10, 20 y 5 segundos muestra justo esto. Es lo que hace Java con `Thread`, `ExecutorService` o Virtual Threads, y lo que hace cualquier lenguaje con hilos.

* Sin hilos (secuencial): 15 + 10 + 20 + 5 = 50 segundos.
* Con hilos: los 4 corren intercalados o en paralelo, el total es el del mas largo = 20 segundos. El Main Thread los lanza y los espera (join).

Condiciones para que sea verdad: las tareas deben ser independientes, y si son CPU-bound debe haber nucleos libres. Con 1 solo nucleo y puro calculo, seguiria tardando cerca de 50 segundos aunque intercalado. Si son I/O-bound (esperan red o disco), si se logra cerca de 20 segundos incluso con pocos nucleos, porque mientras una espera, otra corre.

La advertencia de la imagen tambien es correcta: crear hilos sin criterio (por ejemplo mil hilos para tareas de 1 ms) gasta mas en crearlos y cambiar entre ellos de lo que ahorra, y el programa va mas lento y consume mas recursos.

## 9. Hilos en JavaScript, resumido

* Hilo principal: 1, con Event Loop, corre el JS. No bloquearlo.
* Pool de libuv: unos 4 hilos invisibles para archivos y crypto.
* `worker_threads` y `Web Workers`: hilos del OS de verdad para CPU-bound. Se hablan por mensajes.
* `child_process` y `cluster`: procesos separados, no hilos, cada uno con su loop y memoria. Asi se escala en Node para usar todos los nucleos.
* En navegador la UI corre en el hilo principal: un calculo largo congela el scroll, por eso se mueve a un Worker.

Pregunta diagnostico: el hilo principal pasa el tiempo esperando o calculando. Si espera, el Event Loop basta. Si calcula, hace falta otro hilo, proceso o maquina.

## Ver también

* [Concurrencia vs paralelismo en programacion](concurrencia-vs-paralelismo.md)
* [Como una app real atiende multiples clientes](como-una-app-atiende-multiples-clientes.md)
* [Concurrencia y paralelismo](_index.md)
* [Arquitectura](../_index.md)
* [Desarrollo de sistemas](../../_index.md)
