# 📅 Mi Calendario — Notas y festivos de Colombia

Aplicación de calendario con notas/recordatorios por categoría, festivos
colombianos en rojo (Ley 51 de 1983, con traslado a lunes), domingos
destacados como día de descanso, 6 temas visuales y usuarios independientes
(cada uno con sus propias notas, categorías y tema).

Incluye **dos versiones**:

| Versión | Carpeta | Tecnología |
|---|---|---|
| Web | `./` (index.html) | HTML + CSS + JavaScript |
| Android | `android/` | Kotlin + Jetpack Compose (Material 3) |

## 📲 Descargar la APK (Android)

Descarga el archivo `MiCalendario-Colombia-v1.0.apk` desde:

**https://github.com/nacho2436/calendario/releases/latest**

En el celular: permite instalar aplicaciones de orígenes desconocidos
si el sistema lo pide. Requiere **Android 8.0 (API 26)** o superior.

## ✨ Funciones

- Calendario mensual (la semana inicia el lunes).
- **Festivos de Colombia en rojo** con su nombre, calculados para
  cualquier año (incluye Semana Santa y los traslados a lunes).
- **Domingos destacados** en tono rojo suave (día de descanso).
- Notas/recordatorios con título, fecha, hora, descripción, categoría
  y **color personalizado** (o el de su categoría). Se marcan como
  completadas.
- **Notas repetitivas** (web): todos los días, cada semana, cada mes
  o cada año; los títulos se ven en letra pequeña dentro de cada día
  del calendario.
- **Avisos**: notifica 5/15/30 minutos, 1 o 2 horas, o 1 día antes
  de la nota (se pueden elegir varios). En la web suenan mientras la
  página esté abierta; en Android además llegan como notificación del
  sistema mientras la app esté abierta. En ambas hay una ventana
  central que hay que aceptar.
- **Vistas de calendario** (v1.1): Hoy (agenda del día), Semana
  (cuadros altos) o Mes, con notas en letra pequeña ajustadas dentro
  de cada cuadro y hora en formato 12 h con am/pm.
- Categorías: crear, editar y eliminar (con confirmación).
- 6 temas: Claro, Oscuro, Azul noche, Bosque, Pastel y Café.
- Usuarios independientes con avatar emoji.
- La versión web guarda todo en `localStorage`; la Android en
  `SharedPreferences` (respaldo automático del sistema).

## 🖥️ Servidor (web con SQLite y sincronización)

```bash
./menu.sh      # menú interactivo: arrancar / detener / reiniciar / ver datos
./iniciar.sh   # arranque directo
./detener.sh   # detención con confirmación
```

Al arrancar muestra las direcciones: en el PC `http://localhost:8177`
y en el celular (misma red WiFi) `http://IP:8177`. Los datos viven en
`datos/calendario.sqlite` y se sincronizan con la app Android
(Usuarios → Sincronización con el servidor).

## 🔨 Compilar la APK en local

Requisitos: JDK 17, Android SDK (platform 35 + build-tools 35.0.0)
y Gradle 8.9+.

```bash
cd android
gradle assembleRelease
# resultado: app/build/outputs/apk/release/app-release.apk
```

La firma release se configura en `~/.gradle/gradle.properties`
(propiedades `CALENDARIO_*`), fuera del repositorio por seguridad.
El keystore vive en `~/gitea/keystores/calendario-android.jks`.
