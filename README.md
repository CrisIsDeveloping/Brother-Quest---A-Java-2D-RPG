# 🗡️ Brother Quest - A Java 2D RPG

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![2D RPG](https://img.shields.io/badge/Game-2D_RPG-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Estado-En_Desarrollo-success?style=for-the-badge)

¡Bienvenido a **Brother Quest**! Un RPG de acción en 2D desarrollado puramente con Java. Explora mapas, enfréntate a hordas de enemigos y sobrevive a combates épicos usando mecánicas avanzadas, habilidades mágicas y un motor construido completamente desde cero, ahora con una arquitectura robusta y modular.

<img src="https://github.com/user-attachments/assets/e938383b-6568-4d1c-b413-1345ed6f48ec" alt="Brother Quest preview" width="700" style="border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.3);">

---

## ✨ Características Actuales

El proyecto integra mecánicas robustas, entidades dinámicas y una arquitectura modular organizada:

* ⚔️ **Combate fluido y responsivo:** Sistema de ataque, mecánicas de guardia/escudo, evasión (roll) con i-frames y habilidades mágicas únicas.
* 👾 **Bestiario y Jefes:** Enfréntate a Slimes, Hongos (`EnemigoMushroom`), Esqueletos básicos y de Élite con IA de persecución. Incluye la batalla épica completamente funcional contra el jefe del Mundo 1: el **Demonio Slime** (`EnemigoJefeDemonio`), y el temible **NightBorne** (`EnemigoNightBorne`).
* 🕹️ **Tutorial Integrado:** Menú interactivo dentro del juego para consultar los controles en cualquier momento de la partida.
* 📜 **Consola de Comandos:** Consola integrada para depuración y trucos en tiempo real durante el juego (`GestorComandos.java`).
* 💰 **Sistema de Comercio e Inventario:** NPC Mercader funcional (`Trader.java`), recolección de objetos en el suelo (`ObjetoRecogible.java`) y consumo de pociones.
* 🎨 **Feedback Visual y Sonoro:** Textos de daño flotantes (`TextoDano.java`), barra de vida para jefes (`BarraJefe.java`) y efectos de sonido dinámicos gestionados por código.
* 🌍 **Entornos Dinámicos:** Portales funcionales (`Portal.java`) para el cambio de niveles, fondos multicapa con efecto parallax (`CapaFondo.java`), y generadores de enemigos (`Spawner.java`).

---

## 🕹️ Controles del Juego

Los controles están mapeados mediante un sistema de `InputBindings` para garantizar una respuesta precisa en cualquier pantalla del juego:

### Movimiento y Combate Básico
| Tecla | Acción |
| :--- | :--- |
| **W, A, S, D** o **Flechas** | Moverse (Arriba, Abajo, Izquierda, Derecha) |
| **Espacio** | Saltar (Con físicas de gravedad y salto integradas) |
| **J** | Atacar con el arma principal |
| **R** | Rodar / Esquivar (*Roll*) |
| **K** (Mantener) | Guardia / Bloquear ataques entrantes |
| **H** | Lanzar Habilidad Mágica Única |

### Interacción y Consumibles
| Tecla | Acción |
| :--- | :--- |
| **G** | Interactuar (Hablar con el mercader, cruzar portales, recoger ítems) |
| **Q / E** | Cambiar entre las pociones del inventario |
| **F** | Consumir la poción seleccionada |

### Navegación de Menús y Sistema
| Tecla | Acción |
| :--- | :--- |
| **W / S** o **Flechas ⬆️ ⬇️** | Navegar por las opciones de los menús |
| **Enter** | Aceptar / Confirmar opción seleccionada |
| **Escape (ESC)** | Pausar el juego / Volver al menú anterior |
| **F2** | Omitir Intro (Saltar directamente a la acción) |
| **F3** | Modo Debug (Muestra hitboxes de colisión y datos de IA en tiempo real) |
| **F4** | Abrir/Cerrar la Consola de Comandos |
| **F11** | Alternar Modo Pantalla Completa |

---

## 🧠 Arquitectura y Separación de Lógica (Refactorización OOP)

Para escalar el proyecto y mantener un código limpio (cumpliendo con el Principio de Responsabilidad Única), la lógica se ha modularizado en diversos **Gestores** y **Componentes**:

* 🛠️ **Gestores Especializados (Managers):**
  * **`GestorTeclas.java`**: Aísla por completo el registro y manejo de eventos de teclado (InputMap/ActionMap), independizándolo de la UI.
  * **`GestorComandos.java`**: Procesa e interpreta las instrucciones introducidas en la consola de desarrollador.
  * **`GestorHabilidades.java`**: Controla el enfriamiento (*cooldown*), coste y ejecución de las magias de los personajes.
  * **`GestorSonidos.java`** y **`GestorRecursos.java`**: Carga en memoria y reproducción eficiente de todos los *assets* (audio, sprites, tiles).
  * **`GestorContadores.java`**: Administra los temporizadores y eventos basados en tiempo de forma centralizada.

* ⚙️ **Motores Independientes:**
  * **`Colisiones.java`** y **`CajaColision.java`**: Todo el cálculo de intersecciones de *hitboxes*, detección de bordes del mapa y comportamiento de físicas se maneja exclusivamente aquí.
  * **`Dibujado.java`**: Centraliza el renderizado gráfico (`Graphics2D`), liberando a `GamePanel` de la enorme carga de pintar individualmente a las entidades, fondos, proyectiles y la interfaz de usuario.

* 🧬 **Jerarquía de Entidades:**
  * Todas las criaturas dinámicas, incluyendo al jugador (`Jugador.java`), heredan de **`Entidad.java`**, compartiendo un núcleo común de propiedades físicas, estados y animaciones.
  * Los enemigos derivan de **`EnemigoBase.java`**, que estandariza la Inteligencia Artificial básica, la detección del jugador y las rutinas de patrullaje, facilitando la creación de nuevas variaciones de enemigos de forma limpia y orientada a objetos.

---

## 🎮 Cómo Jugar (Jugadores)

¡No necesitas instalar Java ni compilar código! 
1. Ve a la sección de **[Releases](../../releases)** (a la derecha de esta página).
2. Descarga el archivo `.zip`.
3. Descomprímelo en tu PC y haz doble clic en `Juego.exe`.

---

## 📦 Instalación (Para Desarrolladores)

1. Clona el repositorio en tu máquina local:
   ```bash
   git clone https://github.com/CrisIsDeveloping/Brother-Quest---A-Java-2D-RPG.git
   ```
2. Abre tu IDE (Recomendado: IntelliJ IDEA o Eclipse) e importa la carpeta como un proyecto existente.
3. Ve a la configuración de estructura del proyecto y asegúrate de que el SDK esté asignado a **Java JDK 25**.
4. Localiza la clase `Main` dentro de la carpeta `src/com/rpg/juego/` y ejecuta el proyecto. ¡100% Core Java, cero librerías externas!

---

## 🤝 Cómo Contribuir

¡Toda ayuda es bienvenida! Si tienes ideas para nuevos enemigos, mecánicas o mejoras en el código:

1. Haz un Fork de este repositorio.
2. Crea una rama con tu nueva característica (`git checkout -b feature/NuevaMecanica`).
3. Sube tus cambios (`git commit -m 'Añadido nuevo hechizo'`).
4. Haz push a la rama (`git push origin feature/NuevaMecanica`).
5. Abre un **Pull Request** para revisar los cambios e integrarlos.

---

## 🚀 Próximamente (Roadmap)

- [ ] Crear el Mundo 2 y Mundo 3.
- [ ] Añadir más NPC interactivos y expandir el sistema de tiendas.
- [ ] Nuevas cinemáticas con *lore* del juego al terminar el primer mundo.
- [ ] Ampliar el inventario de objetos consumibles para el botón F.
- [ ] Ajustar la dificultad y equilibrar el balance del PVE para una experiencia justa.
