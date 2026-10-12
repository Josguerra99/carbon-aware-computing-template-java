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

En el **primer** **`run.cmd`**, Windows suele mostrar el aviso de **java.exe**: **Red privada** + **Permitir**. **`run.cmd`** solo pide UAC si quedó **Block** en Private.

## Setup manual

El script de setup hace: crear `.env` si no existe, comprobar JDK 21+, resolver dependencias con Maven (y descargar Maven a `.tools/` si hace falta).

Si lo haces a mano:

| Requisito | Windows | Ubuntu |
| --- | --- | --- |
| **JDK 21+** | `winget install Microsoft.OpenJDK.21` o [Adoptium](https://adoptium.net/) | `sudo apt install openjdk-21-jdk` |
| **Maven 3.9+** | En PATH o deja que `setup` lo ponga en `.tools/` | Igual |
| **Config** | Copia `.env.example` → `.env` y ajusta `MAIN_SERVER_URL` | Igual |
| **Firewall (LAN)** | `scripts\windows\open-lan-firewall.cmd 3916` (admin si hace falta) | Abre el puerto en `ufw` si usas firewall |

Arranque manual:

```bash
mvn -q exec:java
```
