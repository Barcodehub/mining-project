# Resultados de las Pruebas de Carga con K6 en Grafana

Este documento contiene las capturas de pantalla y análisis de las métricas obtenidas en Grafana tras ejecutar las pruebas de carga con K6 en modo InfluxDB.

## Captura 1: http_req_duration (Tiempo Total de Petición)

![http_req_duration](https://drive.google.com/file/d/1OAvkjKYOen-H0aXuH6l9ln2ypIZM03_L/view?usp=sharing)

### Análisis
- **Sistema estable:** No hay tendencia alcista en las métricas, lo que indica que el sistema mantiene un rendimiento consistente bajo carga sostenida.
- **Latencias aceptables:** El percentil 95 (p95) es menor a 200 ms, lo cual es excelente.

### Conclusión
El sistema mantiene latencias consistentes sin degradación progresiva bajo carga sostenida.

---

## Captura 2: http_req_blocked (Tiempo en Cola de Conexión)

![http_req_blocked](https://drive.google.com/file/d/1A4U0DHn1gJEjauvSA2uNF5pfyXjNbp64/view?usp=sharing)

### Análisis
- **Casi 0 ms:** La mayoría de las métricas muestran tiempos cercanos a 0 ms.
- **Sin contención:** No se observa contención en el pool de conexiones.

### Conclusión
No existe contención en el pool de conexiones, lo que demuestra que la arquitectura está bien dimensionada.

---

## Captura 3: http_req_connecting (Tiempo de Handshake TCP)

![http_req_connecting](https://drive.google.com/file/d/13xZhH8H61XUp0LOigQVeXVaKW_q1ppKl/view?usp=sharing)

### Análisis
- **Casi 0 ms:** Las métricas muestran tiempos de conexión cercanos a 0 ms.
- **Conexiones reutilizadas:** El uso de Keep-Alive es eficiente.

### Conclusión
El uso eficiente de Keep-Alive reduce el overhead de red al reutilizar conexiones existentes.

---

## Captura 6: http_req_waiting (Tiempo de Procesamiento Real)

![http_req_waiting](https://drive.google.com/file/d/14EtKvSYaULYlZ9kAaYH3G9cEUgqNZ0ZY/view?usp=sharing)

### Análisis
- **Similar a http_req_duration:** El tiempo de espera domina el tiempo total de respuesta.
- **Backend eficiente:** El tiempo de respuesta está dominado por el backend, no por la red.

### Conclusión
El tiempo de respuesta está dominado por el backend, lo que indica que la red no es un cuello de botella.
