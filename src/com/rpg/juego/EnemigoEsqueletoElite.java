package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Entidad EnemigoEsqueletoElite
 * Implementa una variante avanzada de la clase EnemigoBase con escalado dinámico,
 * una máquina de estados para animaciones complejas y lógica de IA basada en proximidad.
 */
public class EnemigoEsqueletoElite extends EnemigoBase {

    // --- Configuración de Renderizado y Escala ---
    private int anchoDibujo = 240;
    private int altoDibujo = 240;

    // --- Definición de Volúmenes de Colisión (Bounding Boxes) ---
    private int anchoHitbox = 75;
    private int altoHitbox = 120;

    // --- Parámetros de Ajuste Espacial ---
    private int ajustePiesY = 60;

    // Ajustes de compensación horizontal para corregir el descentrado del sprite
    private int ajusteDerecha = -5;
    private int ajusteIzquierda = 7;

    // --- Propiedades del Área de Efecto (Attack Box) ---
    private int anchoAtaque = 162;
    private int altoAtaque = 120;
    private int bajarAtaqueY = -20;

    // --- Atributos de Comportamiento y Estado ---
    private float velocidad = 3.2f;
    private boolean golpeRegistrado = false;
    private boolean atacando = false;

    private BufferedImage[][] animaciones;
    private static final int SPRITE_SIZE = 64;

    private static final int ATAQUE = 0;
    private static final int MUERTE = 1;
    private static final int CORRER = 2;
    private static final int QUIETO = 3;
    private static final int DAÑO = 4;

    private static final int[] CANTIDAD_FRAMES = {13, 13, 12, 4, 3};

    public EnemigoEsqueletoElite(int x, int y) {
        super(x, y);
        this.aniSpeed = 8;
        this.estadoActual = QUIETO;
        this.hitbox = new Rectangle((int)x, (int)y, anchoHitbox, altoHitbox);

        establecerNivel(9);
        cargarAnimaciones();
    }

    private void cargarAnimaciones() {
        try {
            String ruta = "/res/esqueleto_sheet.png";
            var is = getClass().getResourceAsStream(ruta);
            if (is == null) is = getClass().getResourceAsStream("/esqueleto_sheet.png");

            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                animaciones = new BufferedImage[5][13];

                for (int j = 0; j < animaciones.length; j++) {
                    int framesEnFila = (j < CANTIDAD_FRAMES.length) ? CANTIDAD_FRAMES[j] : 0;
                    for (int i = 0; i < framesEnFila; i++) {
                        animaciones[j][i] = img.getSubimage(i * SPRITE_SIZE, j * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == MUERTE) return;

        // Referencias de las Hitboxes para cálculos de proximidad
        Rectangle hbJugador = jugador.getHitbox();
        float centroEnemigoX = hitbox.x + hitbox.width / 2f;
        float centroJugadorX = hbJugador.x + hbJugador.width / 2f;
        float piesEnemigoY = hitbox.y + hitbox.height;
        float piesJugadorY = hbJugador.y + hbJugador.height;

        // Determinación de la orientación visual
        if (estadoActual != ATAQUE && estadoActual != DAÑO) {
            mirandoIzquierda = (centroJugadorX < centroEnemigoX);
        }

        float distX = Math.abs(centroJugadorX - centroEnemigoX);
        float distY = Math.abs(piesJugadorY - piesEnemigoY);

        // --- Lógica de Persecución y Frenado ---

        // 1. Verificamos si estamos en rango de ataque (en X e Y)
        if (distX <= 95 && distY <= 15) {
            if (estadoActual != ATAQUE && estadoActual != DAÑO) {
                estadoActual = ATAQUE;
                aniIndex = 0;
                aniTick = 0;
                atacando = true;
            }
        }
        // 2. Si no estamos atacando, decidimos si avanzar o frenar
        else if (distX < 400 && estadoActual != DAÑO && estadoActual != ATAQUE) {
            estadoActual = CORRER;

            // Distancia mínima deseada para que las hitboxes se "toquen" pero no se encimen
            // Usamos la mitad de las anchas de ambas hitboxes para calcular el contacto perfecto
            float distanciaContacto = (hitbox.width / 2f) + (hbJugador.width / 2f) - 5;

            // Solo se mueve en X si la distancia es mayor al punto de contacto
            if (distX > distanciaContacto) {
                if (centroJugadorX < centroEnemigoX) this.x -= velocidad;
                else this.x += velocidad;
            } else {
                // Si ya llegó al contacto en X, cambia a estado QUIETO o mantiene posición
                if (distY <= 15) estadoActual = QUIETO;
            }

            // Persecución en Y (se mantiene igual como pediste)
            if (distY > 5) {
                if (piesJugadorY < piesEnemigoY) this.y -= (velocidad * 0.7f);
                else if (piesJugadorY > piesEnemigoY) this.y += (velocidad * 0.7f);
            }
        } else if (estadoActual != DAÑO && estadoActual != ATAQUE) {
            estadoActual = QUIETO;
        }

        // Actualización de la hitbox física
        hitbox.x = (int) this.x;
        hitbox.y = (int) this.y;
        aplicarLimitesCarril();
    }

    @Override
    public void actualizar() {
        actualizarTickAnimacion();
    }

    /**
     * Gestión de frames y reset de lógica de combate para ataques múltiples.
     */
    private void actualizarTickAnimacion() {
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;

            // Reset de bandera justo después del primer golpe (frame 4)
            // para que el frame 8 pueda registrar el segundo daño.
            if (estadoActual == ATAQUE && (aniIndex == 0 || aniIndex == 6)) {
                golpeRegistrado = false;
            }

            if (aniIndex >= CANTIDAD_FRAMES[estadoActual]) {
                aniIndex = 0;
                if (estadoActual == ATAQUE || estadoActual == DAÑO) {
                    estadoActual = QUIETO;
                    atacando = false;
                    golpeRegistrado = false;
                }
            }
        }
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto) return;
        vida -= cantidad;
        if (vida <= 0) {
            vida = 0;
            muerto = true;
            estadoActual = MUERTE;
            aniIndex = 0;
            aniTick = 0;
        } else if (estadoActual != ATAQUE) {
            estadoActual = DAÑO;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    /**
     * Calcula la Attack Box sincronizada un frame antes (4 y 8) para máxima precisión.
     */
    public Rectangle getAttackBox() {
        // Ajustado a los frames 4 y 8 para compensar el retraso de 1 frame
        if (estadoActual == ATAQUE && (aniIndex == 4 || aniIndex == 8)) {
            int alcanceAtras = 50;
            int anchoExtra = 50;

            int xFinal = mirandoIzquierda ?
                    (hitbox.x + hitbox.width + alcanceAtras) - (anchoAtaque + anchoExtra) :
                    hitbox.x - alcanceAtras;

            return new Rectangle(xFinal, hitbox.y + bajarAtaqueY, anchoAtaque + anchoExtra, altoAtaque);
        }
        return null;
    }

    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        if (animaciones == null || animaciones[estadoActual][aniIndex] == null) return;

        BufferedImage img = animaciones[estadoActual][aniIndex];

        int drawX = hitbox.x - (anchoDibujo - hitbox.width) / 2 - cameraX;
        int drawY = (hitbox.y + hitbox.height) - altoDibujo + ajustePiesY;

        if (mirandoIzquierda) {
            g2.drawImage(img, drawX + anchoDibujo - ajusteIzquierda, drawY, -anchoDibujo, altoDibujo, null);
        } else {
            g2.drawImage(img, drawX - ajusteDerecha, drawY, anchoDibujo, altoDibujo, null);
        }

        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
            Rectangle ab = getAttackBox();
            if (ab != null) {
                g2.setColor(Color.YELLOW);
                g2.drawRect(ab.x - cameraX, ab.y, ab.width, ab.height);
            }
        }
    }

    public boolean isAtacando() { return atacando; }
    public void setAtacando(boolean b) { this.atacando = b; }
    public boolean isGolpeRegistrado() { return golpeRegistrado; }
    public void setGolpeRegistrado(boolean b) { this.golpeRegistrado = b; }
}