# Scripts (Java)

Ayudantes de **setup** y **run** en Windows y Ubuntu (sin WSL). Ejecuta los comandos desde la **raíz del repo**.

## Comandos

| OS | Setup | Run |
| --- | --- | --- |
| Windows | `powershell -ExecutionPolicy Bypass -File scripts/windows/setup.ps1` | `powershell -ExecutionPolicy Bypass -File scripts/windows/run.ps1` |
| Ubuntu | `bash scripts/ubuntu/setup.sh` | `bash scripts/ubuntu/run.sh` |

## Qué instala / hace `setup`

| Paso | Windows | Ubuntu |
| --- | --- | --- |
| JDK | Comprueba Java 21+ en PATH; si falta, indica `winget install Microsoft.OpenJDK.21` | `openjdk-21-jdk` vía `apt` si falta |
| Maven | Usa `mvn` en PATH o descarga Maven 3.9.10 en `.tools/` | Igual |
| Dependencias | `mvn -q -DskipTests dependency:resolve compile` | Igual |
| Config | `scripts/ensure-env.*` → crea `.env` desde `.env.example` (o valores por defecto) | Igual |

La carpeta `.tools/` está en `.gitignore`.

## Qué hace `run`

1. Resuelve el ejecutable de Maven (PATH o `.tools/.../bin/mvn`).
2. Carga `.env`.
3. `mvn -q exec:java` → `ServerApp` en `0.0.0.0:PORT`.

## Requisitos previos (manual)

| Herramienta | Notas |
| --- | --- |
| **JDK 21+** | Obligatorio si `setup` no puede instalarlo solo (Windows). |
| **Maven 3.9+** | Opcional en PATH; `setup` puede descargarlo a `.tools/`. |
| **Sitio del taller** | Lo opera el facilitador. Pon su URL en `MAIN_SERVER_URL` (`.env`). |

## Archivos

```
scripts/
  README.md
  windows/
    setup.ps1
    run.ps1
  ubuntu/
    setup.sh
    run.sh
```
