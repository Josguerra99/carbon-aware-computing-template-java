# Template del equipo (Java)

Servidor de pronóstico del taller (puerto **3916**). El sitio del taller lo opera el facilitador; tú solo levantas este repo.

## Setup

Desde la raíz del repo. Crea `.env` si no existe, comprueba JDK 21+, descarga Maven a `.tools/` si no está en PATH y resuelve dependencias con Maven.

**Windows**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/setup.ps1
```

**Ubuntu**

```bash
bash scripts/ubuntu/setup.sh
```

Después de setup, edita `.env` y pon **`MAIN_SERVER_URL`** con la URL que te dé el facilitador. Regístrate en el sitio del taller (`/register`) con la IP de tu PC y el puerto **3916**.

## Run

Carga `.env` y arranca el servidor (`mvn exec:java`, escucha en `0.0.0.0:PORT`).

**Windows**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/run.ps1
```

**Ubuntu**

```bash
bash scripts/ubuntu/run.sh
```

## Setup manual

| Requisito | Windows | Ubuntu |
| --- | --- | --- |
| **JDK 21+** | `winget install Microsoft.OpenJDK.21` o [Adoptium](https://adoptium.net/) | `sudo apt install openjdk-21-jdk` |
| **Maven 3.9+** | En PATH o deja que `setup` lo ponga en `.tools/` | Igual |
| **Config** | Copia `.env.example` → `.env` y ajusta `MAIN_SERVER_URL` | Igual |

Arranque manual:

```bash
mvn -q -DskipTests compile exec:java
```
