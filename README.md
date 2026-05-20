# 🗡️ Brother Quest - A Java 2D RPG

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![2D RPG](https://img.shields.io/badge/Game-2D_RPG-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Estado-En_Desarrollo-success?style=for-the-badge)

¡Bienvenido a **Brother Quest**! Este es un proyecto de RPG de acción en 2D creado puramente con Java. Explora mapas, enfréntate a hordas de enemigos y sobrevive a combates épicos usando mecánicas avanzadas y un motor creado desde cero.

<p align="center">
  <img src="https://github.com/user-attachments/assets/e938383b-6568-4d1c-b413-1345ed6f48ec" alt="Brother Quest preview" width="700">
</p>

---

## ✨ Características Actuales

* ⚔️ **Combate fluido:** Ataca, esquiva y lanza habilidades mágicas.
* 👾 **Variedad de enemigos:** Desde clásicos slimes y hongos escurridizos, hasta esqueletos de élite con inteligencia artificial de persecución.
* 👹 **Batalla de Jefes:** Enfréntate al temible NightBorne, ¡y prepárate para el futuro jefe del mundo 1, el Demonio Slime!
* 🎨 **Arte Pixelado:** Animaciones cuadro por cuadro y gestión de cámara dinámica.
* ⚙️ **Físicas y mecánicas:** Sistema de saltos con físicas y mecánicas de evasión al puro estilo *Dark Souls*.

---

## 🎮 Cómo Jugar (Jugadores)

¡No necesitas instalar Java ni compilar código! 
1. Ve a la sección de **[Releases](../../releases)** (a la derecha de esta página).
2. Descarga el archivo `.zip`.
3. Descomprímelo en tu PC y haz doble clic en `Juego.exe`.

### 🕹️ Controles

| Tecla | Acción |
| :--- | :--- |
| **W, A, S, D** | Moverse |
| **J** | Atacar |
| **K / Espacio** | Escudo / Saltar (Físicas incluidas) |
| **R** | **Roll** (Rueda para esquivar con animación incluida) |
| **Q - E** | Cambiar slot de pociones |
| **F** | Consumir pociones |
| **1, 2, 3** | Cargar barra de energía / Habilidades (Hab. 1 lista) |
| **F2** | Skip Intro (¡Salta a la acción de una vez!) |
| **F3** | Modo Desarrollador (Ver hitboxes y debug de IA) |

---

## 🧠 Arquitectura y Motor del Juego

El juego no utiliza motores de terceros (como Unity o Godot). Está construido sobre un **motor personalizado en Java 2D** usando librerías nativas (`java.awt` y `javax.swing`). Esto garantiza un control absoluto sobre el rendimiento y las físicas.



* **Game Loop (Bucle Principal):** Implementado con `Runnable` y un `Thread` dedicado para asegurar **60 FPS constantes**. Utiliza un cálculo de *Delta Time* para que las físicas de salto y movimiento sean independientes de los fotogramas.
* **Sistema de Entidades:** El jugador, los enemigos (Slimes, Esqueletos) y los proyectiles heredan de una clase base común. Comparten lógicas de colisión mediante intersección de rectángulos (`Rectangle` hitboxes) y gestión de animaciones por frames.
* **Gestión de Estados (State Machine):** Interfaz fluida que cambia entre el Menú, el Juego en sí y las pantallas de pausa/tutorial sin recargar recursos innecesarios.
* **Renderizado y Cámara:** Uso intensivo de `Graphics2D` para dibujar el mapa basado en *Tiles* (cuadrículas) y un sistema de cámara dinámica que rastrea las coordenadas del jugador en mapas más grandes que la resolución de la pantalla.

---

## 🛠️ Requisitos Técnicos

Para abrir, modificar o compilar el código fuente:
* **Lenguaje:** Java JDK 25.
* **IDE Recomendado:** IntelliJ IDEA o Eclipse.
* **Librerías externas:** Ninguna. ¡100% Core Java!

---

## 📦 Instalación (Para Desarrolladores)

1. Clona el repositorio en tu máquina local:
   ```bash
   git clone [https://github.com/CrisIsDeveloping/Brother-Quest---A-Java-2D-RPG.git](https://github.com/CrisIsDeveloping/Brother-Quest---A-Java-2D-RPG.git)

2. Abre tu IDE e importa la carpeta como un proyecto existente.

3. Ve a la configuración de estructura del proyecto (Project Structure) y asegúrate de que el SDK esté asignado a Java 25.

4. Localiza tu clase Main dentro de la carpeta src y ejecuta el proyecto.

🤝 Cómo Contribuir
¡Toda ayuda es bienvenida! Si tienes ideas para nuevos enemigos, mecánicas o mejoras en el código:

1. Haz un Fork de este repositorio.

2. Crea una rama con tu nueva característica (git checkout -b feature/NuevaMagia).

3. Sube tus cambios (git commit -m 'Añadido nuevo hechizo de fuego').

4. Haz push a la rama (git push origin feature/NuevaMagia).

5. Abre un Pull Request para revisar los cambios e integrarlos.

## 🚀 Próximamente (Roadmap)
- [ ] Menú in-game para el tutorial de controles.
- [ ] Nuevos objetos consumibles para el botón F.
- [ ] Finalizar el desarrollo de las habilidades 2 y 3 del caballero.
- [ ] Implementar la épica batalla contra el Demonio Slime.
- [ ] Crear mundo 2, mundo 3
- [ ] Implementar sonidos, soundtracks
- [ ] Ajustar dificultad, PVE justo
- [ ] Agregar NPC, abre una tienda para comprar armaduras, espadas, pociones
- [ ] Nuevas cinematicas, con lore del juego, al terminar mundo 1... etc
