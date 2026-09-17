# Guía de lectura del código — ¿por dónde empiezo?

Ruta ordenada para entender el proyecto. Cada paso usa lo del anterior. Rutas de código relativas a la raíz del repo.

## 1. Las 2 formas de crear hilos (5 min)

Lee `src/main/java/Hilos/Hilo.java` y luego `src/main/java/Hilos/MiHilo.java`. Solo fíjate en: `extends Thread` vs `implements Runnable`, y que todo arranca con `start()`, no con `run()` directo.

## 2. El punto de entrada (2 min)

Lee `src/main/java/Hilos/MainServidor.java`. Son 3 líneas: crea `ServidorArchivos(1250)` y lo arranca. Pregúntate: ¿este `main` atiende clientes? No — solo lanza el hilo servidor.

## 3. El hilo que espera (10 min)

Lee `src/main/java/Hilos/ServidorArchivos.java`, el `run()` de arriba a abajo: abre `ServerSocket` → `accept()` bloqueante en loop → por cada conexión, `new ClassThreadCliente(...).start()`. Clave: él nunca envía archivos, solo delega y vuelve a esperar.

## 4. El que pide (10 min)

Lee `src/main/java/Hilos/Cliente.java` siguiendo el protocolo en orden: `new Socket` → `writeUTF(nombre)` → `readLong(tamaño)` → loop de bloques hasta completar → guarda en `C:\cibertec\`. Si el tamaño es `-1`, el archivo no existe.

## 5. El que atiende (10 min)

Lee `src/main/java/Hilos/ClassThreadCliente.java` como espejo del cliente: `readUTF` → busca en `C:\cibertec\` → `writeLong(tamaño)` → bloques de 8192 → o `-1`. Fíjate que al cerrar el socket el hilo muere (`hio finalizado`): no se reutiliza.

## 6. Correlo en ese mismo orden (15 min)

Primero `MainServidor` (verás `Esperando conexiones`), luego un `Cliente` pidiendo `prueba.txt` (ver tabla de pruebas en el [README](../readme.md#qué-pedir-en-cada-cliente-guía-de-pruebas)), después dos clientes a la vez (`prueba.txt` + `archivo-ejemplo.txt`), luego `documento-grande.txt` para ver los 8 chunks, y al final `noexiste.txt` para ver el `-1`.

## 7. Cierra con la teoría (10 min)

Relee solo el Modelo 1 ([como-una-app-atiende-multiples-clientes.md](teoria/como-una-app-atiende-multiples-clientes.md)) y el §6.1 ([hilos-de-ejecucion-en-profundidad.md](teoria/hilos-de-ejecucion-en-profundidad.md)) — ahora cada línea del código tiene dónde anclarse — y abre el [diagrama de secuencia](diagrama/hilos-servidor-sequence.html) para ver el flujo completo de un vistazo.
