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
- **Avisos** (web): notifica 5/15/30 minutos, 1 o 2 horas, o 1 día
  antes de la nota (se pueden elegir varios), con notificación del
  navegador, aviso interno y sonido mientras la página esté abierta.
- Categorías: crear, editar y eliminar (con confirmación).
- 6 temas: Claro, Oscuro, Azul noche, Bosque, Pastel y Café.
- Usuarios independientes con avatar emoji.
- La versión web guarda todo en `localStorage`; la Android en
  `SharedPreferences` (respaldo automático del sistema).

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
