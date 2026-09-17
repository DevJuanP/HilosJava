# HilosJava — Servidor de Archivos Multicliente (lo que hizo el profesor en clase)

Proyecto sencillo en Java que aterriza la teoría de concurrencia, paralelismo e hilos en algo concreto: **una app que atiende a múltiples clientes a la vez con hilos.**

## Cómo entender mejor este proyecto con la teoría

Lo que se ve en este proyecto se entiende mucho mejor con la teoría que aparece en los documentos de clase:

* **Modelo 1 — Thread por cliente:** explicado en [Como una app real atiende múltiples clientes — §3 Los 3 modelos que usan los backends reales](docs/como-una-app-atiende-multiples-clientes.md#3-los-3-modelos-que-usan-los-backends-reales). Este proyecto es literalmente ese modelo: cada cliente que llega = 1 hilo nuevo.
* **Hilos en profundidad §6.1 — Thread por cliente en detalle:** explicado en [Hilos de ejecución en profundidad — §6.1 Thread por cliente](docs/hilos-de-ejecucion-en-profundidad.md#61-thread-por-cliente). Ahí está el porqué funciona para pocos clientes y por qué se cae con 10k hilos (RAM + `context switch` + colapso de la base/disco).

Documentos completos en `docs/`:

* [como-una-app-atiende-multiples-clientes.md](docs/como-una-app-atiende-multiples-clientes.md) — idea base del mesero, Event Loop de Node, modelos, escalado con cluster / Nginx / balanceador / pool / colas.
* [concurrencia-vs-paralelismo.md](docs/concurrencia-vs-paralelismo.md) — definiciones, analogía del cocinero, tabla de diferencias, qué es `I/O-bound` vs `CPU-bound`.
* [hilos-de-ejecucion-en-profundidad.md](docs/hilos-de-ejecucion-en-profundidad.md) — proceso vs hilo, scheduling, tipos de hilos, los 3 modelos de servidor en profundidad, coordinación.

> Si solo puedes leer dos secciones, lee las dos de arriba: Modelo 1 y §6.1. Con eso todo el código de abajo cobra sentido.

---

## 1. Idea general en una frase

Sin hilos el servidor atendería de a uno (uno espera, todos esperan). Con hilos, el servidor principal solo acepta conexiones y **delega cada cliente a un hilo independiente**, así atiende a N a la vez de forma concurrente.

Es el mismo ejemplo de los docs: 4 tareas de 15, 10, 20 y 5 segundos no suman 50 segundos en secuencial, con hilos terminan en ~20 segundos (el más largo). Ver [Hilos en profundidad — §8 Ejemplo de la imagen](docs/hilos-de-ejecucion-en-profundidad.md#8-ejemplo-de-la-imagen-4-hilos-que-tardan-20-segundos-y-no-50).

```
Cliente 1 \                         / pide "foto.jpg" -> hilo 1 lee disco y responde
Cliente 2 -- Socket TCP :1250 ------  pide "doc.pdf"  -> hilo 2 lee disco y responde
Cliente 3 /  ServidorArchivos       \ pide "noexiste" -> hilo 3 responde -1
         accept() en loop, por cada accept() => new ClassThreadCliente(socket).start()
```

---

## 2. Estructura del proyecto

```
pom.xml (Java 17, Maven)
src/main/java/Hilos/
  Hilo.java              -> ejemplo 1: crear hilo heredando de Thread
  MiHilo.java            -> ejemplo 2: crear hilo implementando Runnable
  MainServidor.java      -> main que arranca el servidor en puerto 1250
  ServidorArchivos.java  -> hilo servidor: ServerSocket + accept() en loop
  ClassThreadCliente.java -> hilo por cliente: recibe nombre, envía archivo
  Cliente.java           -> programa cliente: pide nombre, recibe y guarda
src/main/java/org/example/Main.java -> Hello world por defecto de Maven, no se usa.
```

---

## 3. Paso a paso: qué hizo el profesor

### Paso 0 — Las 2 formas de crear hilos en Java

Primero mostró lo mínimo, en 2 archivos de prueba.

**`src/main/java/Hilos/Hilo.java:3`:** heredar de `Thread` y sobrescribir `run()`:

```java
public class Hilo extends Thread {
    @Override
    public void run(){
        System.out.println("Ejecutando el hilo de clase heredada");
    }
    public static void main(String[] args) {
        Hilo hilo = new Hilo();
        hilo.start();
    }
}
```

**`src/main/java/Hilos/MiHilo.java:3`:** implementar `Runnable` y envolverlo en un `Thread`:

```java
public class MiHilo implements Runnable {
    @Override
    public void run() {
        System.out.println("Otra forma de crear hilos");
    }
    public static void main(String[] args) {
      MiHilo hilo = new MiHilo();
      Thread interfaceHilo = new Thread(hilo);
      interfaceHilo.start();
    }
}
```

> Clave: `start()` crea el hilo de verdad, llamar a `run()` directo NO, solo sería una llamada normal secuencial.

Esto conecta con [Hilos en profundidad — §1 Proceso vs hilo](docs/hilos-de-ejecucion-en-profundidad.md#1-proceso-vs-hilo): cada hilo tiene su `stack` y `program counter` propios, pero comparten el `heap` del proceso. Y con [§2 Corrección clave: los hilos SÍ pueden pisarse](docs/hilos-de-ejecucion-en-profundidad.md#2-correccion-clave-los-hilos-si-pueden-pisarse): si compartieran memoria sin reglas habría `race condition`.

### Paso 1 — Arrancar el servidor (`MainServidor.java`)

**`src/main/java/Hilos/MainServidor.java:5`:**

```java
ServidorArchivos serv = new ServidorArchivos(1250);
serv.start();
```

Solo lanza el servidor en el puerto `1250` en su propio hilo. A partir de aquí hay al menos 2 hilos vivos: el `main` y el servidor.

### Paso 2 — El servidor que acepta sin atender (`ServidorArchivos.java`)

**`src/main/java/Hilos/ServidorArchivos.java:18` — `run()`:**

1. `new ServerSocket(1250)` → abre el puerto y escucha.
2. `while(!parar){ servidor.accept(); }` → `accept()` es **bloqueante**: el hilo se queda esperando ahí sin consumir CPU. Cuando el SO recibe una conexión TCP, lo despierta. Ver [Cómo una app atiende — §1 La idea base](docs/como-una-app-atiende-multiples-clientes.md#1-la-idea-base-intercalar-muy-rapido).
3. Cuando llega un cliente, `accept()` devuelve un `Socket nuevoCliente`.
4. Inmediatamente crea `new ClassThreadCliente(nuevoCliente)` y hace `start()`. **No lo atiende él, delega.**
5. Vuelve al `accept()` a esperar al siguiente.

Analogía de tus docs ([mesero](docs/como-una-app-atiende-multiples-clientes.md#1-la-idea-base-intercalar-muy-rapido) / [cocinero](docs/concurrencia-vs-paralelismo.md#1-definiciones-con-analogia)): es el mesero. No se queda en una mesa, toma el pedido y lo pasa a otro cocinero (hilo), y sigue atendiendo la puerta.

Este es el punto central: sin hilos, el servidor atendería 1 por 1. Con hilos, atiende a N a la vez.

### Paso 3 — El hilo que sí atiende (`ClassThreadCliente.java`)

**`src/main/java/Hilos/ClassThreadCliente.java:6`:** cada instancia es 1 hilo para 1 cliente, guarda su `Socket`.

En `run():15`:

1. Abre `DataInputStream / DataOutputStream` sobre el socket.
2. `dis.readUTF()` → espera el nombre del archivo que pide el cliente. También bloqueante.
3. Busca en disco: `new File("C:\\cibertec\\"+strFichero)`.
4. Protocolo súper simple que inventó el profesor:
   * Si existe: `dos.writeLong(fileSize)` + luego manda los bytes en trozos de `8192` con `dos.write(data, 0, byteLeidos)`.
   * Si no existe: `dos.writeLong(-1)`.
5. `while(!parar)` → en teoría podría atender varios pedidos del mismo cliente en loop, pero en la práctica `Cliente.java` pide 1 y cierra, entonces salta `IOException` y cierra todo en `parar():50`.

### Paso 4 — El cliente que pide (`Cliente.java`)

**`src/main/java/Hilos/Cliente.java:11`:**

1. `new Socket("localhost",1250)` → abre conexión TCP.
2. Pide por consola: `Subir el archivo que deseas recibir:` → `sc.nextLine()`.
3. `dos.writeUTF(strFichero)` → manda el nombre.
4. `dis.readLong()` → lee el tamaño.
   * Si `-1`: `El archivo solicitado no existe`.
   * Si `>0`: crea `C:\cibertec\ + nombre` local y copia en loop hasta completar `fileSize`, imprimiendo `bytesReceived` por chunk.

---

## 4. Cómo aterriza la teoría en este código

* **Concurrencia vs paralelismo:** esto es concurrencia para `I/O-bound`. La mayoría del tiempo los hilos están **bloqueados** esperando red o disco, no calculando. Por eso con 4 núcleos puedes tener 20 clientes avanzando intercalados. Ver [Concurrencia vs paralelismo — §2, §3 y §7](docs/concurrencia-vs-paralelismo.md#2-concurrencia-en-detalle). La pregunta diagnóstico de tus docs aplica aquí: ¿el hilo pasa el tiempo esperando o calculando? Aquí esperando → hilos sí ayudan.
* **Proceso vs hilo + scheduling:** con 8 núcleos y 500 hilos, el SO hace `time slicing` y `context switch`. Solo hay paralelismo real cuando 2 hilos están en 2 núcleos distintos al mismo instante. Ver [Hilos en profundidad — §3 y §4](docs/hilos-de-ejecucion-en-profundidad.md#3-como-la-cpu-ejecuta-cientos-de-hilos-con-pocos-nucleos).
* **Por qué no escala:** este es el modelo viejo Apache/PHP clásico. Funciona en clase con 5 clientes, pero con 10k clientes = 10k hilos = ~10GB solo en stacks + muchísimo `context switch`. En producción no se hace `new Thread()` por cliente, se usa pool limitado (`ExecutorService` con 200 hilos + cola, si se llena se responde 503), `Virtual Threads` de Java 21, o `Event Loop` como Node/Nginx. Ver [Hilos en profundidad — §5 Tipos de hilos y §6 Los tres modelos](docs/hilos-de-ejecucion-en-profundidad.md#5-tipos-de-hilos-que-existen) y [Cómo una app atiende — §4 Cuando un proceso ya no alcanza](docs/como-una-app-atiende-multiples-clientes.md#4-cuando-un-proceso-ya-no-alcanza-escalar).

---

## 5. Cómo probarlo

Requisitos: Java 17, Maven, Windows (por las rutas quemadas).

1. Crear carpeta `C:\Cibertec\` en tu PC (tal como pidió el profesor en clase).
2. Poner ahí un archivo de prueba en el lado servidor. Ya están creados y probados:
   * `C:\Cibertec\prueba.txt` (283 bytes, 1 chunk — prueba rápida).
   * `C:\Cibertec\archivo-ejemplo.txt` (para probar 2 clientes a la vez).
   * `C:\Cibertec\documento-grande.txt` (~63 KB, 8 chunks de 8192 — para ver varios `bytesReceived` en consola).
3. Terminal 1 — arrancar servidor:
   ```bash
   mvn compile exec:java -Dexec.mainClass="Hilos.MainServidor"
   # o desde tu IDE: Run MainServidor.main()
   # Verás: "Servidor Funcionando" / "Esperando conexiones puerto:1250"
   ```
4. Terminal 2 — arrancar uno o varios clientes:
   ```bash
   mvn compile exec:java -Dexec.mainClass="Hilos.Cliente"
   # Escribe: prueba.txt
   # Verás los bytes por chunk y "Archivo recibido correctamente"
   ```
5. Abre 2-3 clientes a la vez para ver la concurrencia: el servidor no se bloquea, cada uno tiene su hilo (`Client:... ha solicitado el archivo...` / `archivo enviado correctamente`).

---

## 6. Limitaciones (por hacerlo apurado en clase)

1. Ruta quemada `C:\cibertec\` en cliente y servidor — no es portable.
2. `pararServidor()` nunca se llama, el `while(!parar)` es infinito y el `servidor.close()` de `ServidorArchivos.java:30` es inalcanzable.
3. Sin pool: un ataque simple creando conexiones lo tumba. Faltaría `ExecutorService` o `Virtual Threads`. Ver [§6.1 problemas](docs/hilos-de-ejecucion-en-profundidad.md#61-thread-por-cliente).
4. Sin `pool de conexiones`, sin `stateless`, sin `cola` — todo lo que en producción se hace según [§4 y §5 de Cómo atiende una app](docs/como-una-app-atiende-multiples-clientes.md#5-arquitectura-tipica-real).
5. `org/example/Main.java` es el Hello World por defecto de Maven, no se usa.
6. El `while(!parar)` de `ClassThreadCliente.java:21` sugiere múltiples pedidos por conexión, pero `Cliente.java` cierra tras 1 archivo, así que siempre termina por excepción.

---

## 7. Glosario rápido para el examen

Ver definiciones completas en [Concurrencia vs paralelismo — §8 Otra jerga](docs/concurrencia-vs-paralelismo.md#8-otra-jerga-del-tema) y [Hilos — §7 Cómo se coordinan](docs/hilos-de-ejecucion-en-profundidad.md#7-como-se-coordinan-los-hilos-sin-romperse).

* **Blocking / Non-blocking:** `accept()` y `readUTF()` aquí son bloqueantes.
* **Thread / Process:** `ServidorArchivos` y cada `ClassThreadCliente` son hilos del mismo proceso Java.
* **Race Condition:** no pasa aquí porque cada hilo usa su propio buffer, pero pasaría si compartieran uno global.
* **Context Switch / Throughput vs Latency:** crear hilos mejora `throughput` para `I/O-bound`, no para `CPU-bound` puro con 1 núcleo.
* **I/O-bound vs CPU-bound:** este proyecto es `I/O-bound` (espera red/disco), por eso los hilos sí ayudan.
