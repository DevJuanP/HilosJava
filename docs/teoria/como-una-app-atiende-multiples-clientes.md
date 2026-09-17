---
title: Como una app real atiende multiples clientes
area: desarrollo
tags: [desarrollo, arquitectura, devops]
created: 2026-09-17
updated: 2026-09-17
related: ["./concurrencia-vs-paralelismo.md", "./hilos-de-ejecucion-en-profundidad.md"]
---

# Como una app real atiende multiples clientes

Una app real casi nunca atiende a todos literalmente al mismo instante. Crea esa ilusion combinando concurrencia (intercalar esperas) y paralelismo (replicar procesos y maquinas) en varias capas: runtime, sistema operativo e infraestructura.

## 1. La idea base: intercalar muy rapido

Como un mesero: no come con cada mesa al mismo tiempo, toma pedido de mesa 1, mientras cocina espera toma pedido de mesa 2, cuando cocina termina vuelve a mesa 1. Nadie nota la espera.

Un servidor hace lo mismo:

1. El cliente abre una conexion TCP (socket).
2. El servidor la acepta y dice: "te atiendo luego, sigue esperando sin bloquearme".
3. Cuando llegan datos o la base de datos responde, retoma esa peticion donde quedo.
4. Mientras tanto atiende otras 10.000 conexiones.

Eso es **concurrencia con I/O no bloqueante**. No necesita 10.000 hilos.

## 2. Como lo hace Node.js con un solo hilo

Node es el mejor ejemplo de que con 1 hilo se puede atender a miles, porque la mayoria del tiempo una API esta esperando red o base de datos, no calculando.

1. **Event Loop:** el hilo principal solo ejecuta tu JS. Nunca espera. Si haces `fetch`, consulta a base de datos o `readFile`, lo delega al sistema y sigue con otro cliente.
2. **El OS avisa:** Linux (epoll) o macOS (kqueue) le dicen "ya llego la respuesta del cliente 452". Node la pone en cola y la ejecuta un momentito.
3. **Threadpool de libuv:** para cosas que el OS no hace async bien (leer archivos, crypto, DNS), Node tiene una piscina pequena de hilos por detras (por defecto 4) para no congelar el principal.

Por eso Node brilla para apps **I/O-bound** (APIs, chats, e-commerce): 90% del tiempo esta esperando, no calculando.

Limite: si metes un `for` pesado de 3 segundos en una ruta, **bloqueas a todos los clientes**. Ahi se rompe la magia y hace falta paralelismo. Ver [Concurrencia vs paralelismo](concurrencia-vs-paralelismo.md).

## 3. Los 3 modelos que usan los backends reales

1. **Thread por cliente (Apache + PHP clasico, Java viejo):** cada cliente = 1 hilo. Simple de programar, pero 10k clientes = 10k hilos = mucha RAM y mucho `context switch`. Se cae.
2. **Event Loop (Node.js, Nginx):** 1 hilo intercala miles de conexiones. Muy eficiente en memoria para I/O. Malo para CPU pesada.
3. **Hibrido moderno (Go con goroutines, Java con Virtual Threads, Node + Workers):** intercala por defecto, y cuando hay calculo pesado lo manda a otros hilos. Es lo que hace una app seria hoy.

Una app Node real usa el modelo 2 mas un poco del 3: concurrencia para atender, paralelismo para no atascarse.

## 4. Cuando un proceso ya no alcanza: escalar

Un solo proceso Node usa 1 nucleo. Un servidor tiene 8 o 16, y hay miles de usuarios. Esto es lo que se hace en produccion:

**Dentro de 1 maquina:**

* **Cluster, PM2 o `child_process`:** lanzar 1 proceso Node por nucleo (8 copias de la app). Todos escuchan el mismo puerto y el sistema reparte clientes. Eso ya es paralelismo real.
* **Workers para CPU:** procesar imagen, PDF o cifrado se manda a `worker_threads` para no congelar las peticiones.

**Fuera de 1 maquina:**

* **Nginx como reverse proxy:** recibe todo, sirve archivos estaticos directo y reparte la API entre los procesos Node.
* **Load Balancer + replicas:** 3 servidores con la app, el balanceador reparte: cliente 1 al server A, cliente 2 al server B. Si uno muere, nadie lo nota.
* **App stateless + Redis o base compartida:** para que de igual que servidor toque, la sesion no vive en memoria del servidor, vive en Redis o base de datos.
* **Pool de conexiones a base de datos:** no se abren 10k conexiones a Postgres. Se abren 20 reutilizables y se comparten entre todas las peticiones con cola. Si no, se mata la base.
* **Colas (RabbitMQ, Kafka, SQS, BullMQ):** si algo tarda (enviar email, generar factura), se responde rapido "recibido" y se procesa despues en background.

## 5. Arquitectura tipica real

```text
10k clientes -> Load Balancer -> Nginx
 -> Node API replica 1, 2, 3 (cada una con cluster por nucleo)
 -> Pool de conexiones -> Postgres / Redis
 -> Cola -> Workers para tareas pesadas
```

Cada nivel aporta su concurrencia o paralelismo: el balanceador reparte, Node intercala, el pool comparte, la cola difiere.

## 6. Resumen practico

* No se atiende a todos a la vez, se **intercalan esperas** (concurrencia).
* Cuando hace falta fuerza bruta, se **replican procesos y maquinas** (paralelismo).
* En JS: `async-await` da lo primero gratis. Para lo segundo hacen falta Cluster, Load Balancer y no guardar estado en memoria.
* Si la API solo espera base o red: no hacen falta hilos, hace falta buen async + pool.
* Si la API calcula: hay que sacarlo del hilo principal o escalar horizontalmente.
* Ante una app lenta, la pregunta clave es: estamos limitados por espera (I/O-bound) o por calculo (CPU-bound).

## Ver también

* [Concurrencia vs paralelismo en programacion](concurrencia-vs-paralelismo.md)
* [Hilos de ejecucion en profundidad](hilos-de-ejecucion-en-profundidad.md)
* [Concurrencia y paralelismo](_index.md)
* [Arquitectura](../_index.md)
* [Desarrollo de sistemas](../../_index.md)
