# Template del equipo (Java)

Servidor de pronóstico del taller (puerto **3916**). El sitio del taller lo opera el facilitador; tú solo levantas este repo.

## Setup

Desde la raíz del repo:

**Windows**

```bat
scripts\windows\setup.cmd
```

**Ubuntu**

```bash
bash scripts/ubuntu/setup.sh
```

Luego edita `.env` y pon **`MAIN_SERVER_URL`** con la URL del facilitador. Regístrate en el taller (`/register`) con la IP de tu PC.

## Run

**Windows**

```bat
scripts\windows\run.cmd
```

**Ubuntu**

```bash
bash scripts/ubuntu/run.sh
```

## Windows y la LAN del taller

**`run.cmd`** arranca el servidor. Si la LAN falla: **`run.cmd --fix-permissions`** (UAC) corrige **java.exe** en red privada y arranca.

## Setup manual

El script de setup hace: crear `.env` si no existe, comprobar JDK 21+, resolver dependencias con Maven (y descargar Maven a `.tools/` si hace falta).

Si lo haces a mano:

| Requisito | Windows | Ubuntu |
| --- | --- | --- |
| **JDK 21+** | `winget install Microsoft.OpenJDK.21` o [Adoptium](https://adoptium.net/) | `sudo apt install openjdk-21-jdk` |
| **Maven 3.9+** | En PATH o deja que `setup` lo ponga en `.tools/` | Igual |
| **Config** | Copia `.env.example` → `.env` y ajusta `MAIN_SERVER_URL` | Igual |

Arranque manual:

```bash
mvn -q exec:java
```

**`run.cmd`** recompila y reinicia al guardar en `src/main/java` (como `bun --watch` / uvicorn `--reload`). Sin recarga: `run.cmd --no-watch`.

## TensorFlow Java en WSL (GPU) + proxy LAN

Misma orquestación que la plantilla TypeScript: servidor en **WSL**, proxy Windows **:3916** → WSL **:3917** con `npm run start:wsl`.

| Paso | Comando |
| --- | --- |
| Activar WSL (primera vez) | `npm run setup:wsl:host` |
| JDK 21 + Maven + perfil `tf-gpu` si hay NVIDIA | `npm run setup:wsl` |
| Recompilar con `-Ptf-gpu` | `npm run setup:wsl:gpu` |
| Sitio + proxy + servidor Java en WSL | `npm run start:wsl:stack` |
| Solo proxy + WSL | `npm run start:wsl` |

Perfiles Maven: **`tf-cpu`** (default) y **`tf-gpu`** (Linux + NVIDIA). `.env`: `TF_BACKEND=gpu|cpu`, `MAIN_SERVER_URL`, `PORT=3916`.
