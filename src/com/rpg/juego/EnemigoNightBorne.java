package com.rpg.juego;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class EnemigoNightBorne extends EnemigoBase {

    // --- Matriz de estados y conteo de frames correspondientes ---
    public static final int IDLE = 0;         // Fila 1 (4 frames)
    public static final int SPEED_ATTACK = 1; // Fila 2 (9 frames)
    public static final int RUN = 2;          // Fila 3 (6 frames)
    public static final int ATTACK = 3;       // Fila 4 (12 frames)
    public static final int DAMAGE = 4;       // Fila 5 (5 frames)
    public static final int DEATH = 5;        // Fila 6 (22 frames)

    private int cooldownAtaque = 0;
    private final int TIEMPO_ENTRE_GOLPES = 90;

    // <--- CAMBIO AQUÍ: Rango de visión reducido de 800 a 400 --->
    private int rangoVision = 400;

    private boolean golpeRegistrado = false;

    public EnemigoNightBorne(int x, int y) {
        super(x, y);
        this.hitbox = new Rectangle(x, y, 60, 110);
        this.estadoActual = IDLE;

        // --- Modificadores de movimiento y logica interna ---
        this.aniSpeed = 4;
        this.velocidad = 4.6F; // Indice de velocidad escalado superior a la clase Jugador

        // Inicializacion de estadisticas de jefe
        this.vidaMax = 2000;
        this.vida = 2000;
        this.dano = 45;
        this.xpQueDa = 500;

        // --- AJUSTE MANUAL DE SOMBRA PARA EL NIGHTBORNE ---
        this.sombraAncho = 120;     // Lo hacemos más ancho que su hitbox
        this.sombraAlto = 30;       // Hacemos el óvalo un poco más grueso
        this.sombraOffsetX = -27;   // Lo movemos un poco a la izquierda para centrarlo
        this.sombraOffsetY = -22;    // Lo bajamos/subimos un poco respecto a sus pies
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == DEATH || estadoActual == DAMAGE) return;

        Rectangle hitJugador = jugador.getBounds();
        double centroEnemigoX = this.hitbox.x + this.hitbox.width / 2.0;
        double centroJugadorX = hitJugador.x + hitJugador.width / 2.0;

        double piesEnemigoY = this.hitbox.y + this.hitbox.height;
        double piesJugadorY = hitJugador.y + hitJugador.height;

        double distX = Math.abs(centroJugadorX - centroEnemigoX);
        double distY = Math.abs(piesJugadorY - piesEnemigoY);

        // 1. Zona muerta (Deadzone) para prevencion de solapamiento inestable
        if (distX > 15) {
            mirandoIzquierda = (centroJugadorX < centroEnemigoX);
        }

        // 2. Evaluacion de umbrales espaciales para ejecucion ofensiva
        if (distX <= 100 && distY <= 90) {
            if (cooldownAtaque <= 0 && estadoActual != ATTACK) {
                estadoActual = ATTACK;
                aniIndex = 0;
            } else if (estadoActual != ATTACK) {
                estadoActual = IDLE;
            }
        }
        else if (distX < rangoVision && estadoActual != ATTACK) {
            estadoActual = RUN;

            // Resolucion de interpolacion en el eje X
            if (distX > 80) {
                if (mirandoIzquierda) this.x -= velocidad;
                else this.x += velocidad;
            }

            // Resolucion de interpolacion en el eje Y
            if (distY > 30) {
                if (piesJugadorY < piesEnemigoY) this.y -= (velocidad * 0.7);
                else if (piesJugadorY > piesEnemigoY) this.y += (velocidad * 0.7);
            }

        } else if (estadoActual != ATTACK) {
            estadoActual = IDLE;
        }

        hitbox.x = (int) this.x;
        hitbox.y = (int) this.y;

        // Validacion cruzada con el sistema de fronteras del nivel
        aplicarLimitesCarril();
    }

    @Override
    public void actualizar() {
        if (cooldownAtaque > 0) cooldownAtaque--;

        if (estadoActual != ATTACK || aniIndex != 10) {
            golpeRegistrado = false;
        }

        aniTick++;

        // Dinamica de interpolacion condicional para ciclos de animacion especificos
        int velocidadAnimacionActual = (estadoActual == IDLE) ? (aniSpeed * 2) : aniSpeed;

        if (aniTick >= velocidadAnimacionActual) {
            aniTick = 0;
            aniIndex++;

            if (aniIndex >= getSpriteAmount(estadoActual)) {
                if (estadoActual == DEATH) {
                    aniIndex = getSpriteAmount(DEATH) - 1;
                }
                else if (estadoActual == ATTACK) {
                    estadoActual = IDLE;
                    reiniciarCooldown();
                }
                else if (estadoActual == DAMAGE) {
                    estadoActual = IDLE;
                }
                else {
                    aniIndex = 0;
                }
            }
        }
        hitbox.x = (int) x;
        hitbox.y = (int) y;
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto) return;
        this.vida -= cantidad;

        if (this.vida <= 0) {
            this.vida = 0;
            estadoActual = DEATH;
            muerto = true;
            aniIndex = 0;
            aniTick = 0;
        } else if (estadoActual != ATTACK && estadoActual != SPEED_ATTACK) {
            estadoActual = DAMAGE;
            aniIndex = 0;
        }
    }

    private int getSpriteAmount(int estado) {
        switch (estado) {
            case IDLE: return 4;
            case SPEED_ATTACK: return 9;
            case RUN: return 6;
            case ATTACK: return 12;
            case DAMAGE: return 5;
            case DEATH: return 22;
            default: return 1;
        }
    }

    @Override
    public boolean puedeAtacar() {
        // Evaluacion del "Active Frame" y validacion de redundancia de impacto
        if (estadoActual == ATTACK && aniIndex == 10 && !golpeRegistrado) {
            golpeRegistrado = true;
            return true;
        }
        return false;
    }

    @Override
    public void reiniciarCooldown() { cooldownAtaque = TIEMPO_ENTRE_GOLPES; }

    @Override
    public Rectangle getAttackBox() {
        int anchoAtaque = 90;
        int altoAtaque = hitbox.height + 40;
        // Calculo de proyeccion ofensiva segun vector de orientacion
        int atkX = mirandoIzquierda ? hitbox.x - anchoAtaque : hitbox.x + hitbox.width;
        int atkY = hitbox.y - 20;

        return new Rectangle(atkX, atkY, anchoAtaque, altoAtaque);
    }

    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        if (GestorRecursos.animacionesNightBorne == null) return;

        int frameSprite = aniIndex;
        if (frameSprite >= getSpriteAmount(estadoActual)) {
            frameSprite = Math.max(0, getSpriteAmount(estadoActual) - 1);
        }

        BufferedImage img = GestorRecursos.animacionesNightBorne[estadoActual][frameSprite];

        // Resolucion de offsets para el renderizado del componente visual principal
        int anchoDibujo = 300;
        int altoDibujo = 300;
        int dx = hitbox.x - cameraX - 120;
        int dy = hitbox.y - 135;

        // Renderizado condicional del sprite segun orientacion
        if (img != null) {
            if (mirandoIzquierda) {
                g2.drawImage(img, dx + anchoDibujo, dy, -anchoDibujo, altoDibujo, null);
            } else {
                g2.drawImage(img, dx, dy, anchoDibujo, altoDibujo, null);
            }
        }

        // Capa de depuracion visual para logica de colisiones
        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
            if (estadoActual == ATTACK) {
                g2.setColor(Color.ORANGE);
                Rectangle atk = getAttackBox();
                g2.drawRect(atk.x - cameraX, atk.y, atk.width, atk.height);
            }
        }
    }

    public boolean isAtacando() {
        return estadoActual == ATTACK || estadoActual == SPEED_ATTACK;
    }

    public void setGolpeRegistrado(boolean b) {
        this.golpeRegistrado = b;
    }

    public boolean isGolpeRegistrado() {
        return this.golpeRegistrado;
    }
}