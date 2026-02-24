package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EnemigoMushroom extends EnemigoBase {

    public static final int IDLE = 0;
    public static final int RUN = 1;
    public static final int ATTACK = 2;
    public static final int HURT = 3;
    public static final int DEAD = 4;
    public static final int STUN = 5;

    private int anchoDibujo = 160;
    private int altoDibujo = 128;
    private int anchoHitbox = 45;
    private int altoHitbox = 60;
    private int ajusteX = 55;
    private int ajusteY = 68;
    private int ajusteDerecha = 0;

    private boolean mirandoDerecha = true;
    private int cooldownAtaque = 0;
    private boolean golpeRegistrado = false;
    private boolean atacando = false;
    private boolean animacionMuerteTerminada = false;

    private boolean enElAire = false;
    private boolean saltoCalculado = false;
    private int timerSalto = 0;
    private double velXActual = 0;
    private double velYActual = 0;

    // --- BUFOS DE SALTO ---
    private double velocidadSalto = 5.5; // Antes 4.0. Ahora sale disparado con más fuerza.

    public EnemigoMushroom(int x, int y) {
        super(x, y);
        this.estadoActual = IDLE;
        this.aniSpeed = 8;

        this.vidaMax = 60;
        this.dano = 10;
        this.xpQueDa = 20;
        this.vida = this.vidaMax;

        this.hitbox = new Rectangle((int)x, (int)y, anchoHitbox, altoHitbox);
    }

    public void setDano(int dano) { this.dano = dano; }
    public int getDano() { return this.dano; }
    public void setVidaMax(int vidaMax) { this.vidaMax = vidaMax; }
    public int getVidaMax() { return this.vidaMax; }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == DEAD || estadoActual == HURT || estadoActual == STUN) return;

        float centroEnemigoX = hitbox.x + hitbox.width / 2f;
        float centroJugadorX = jugador.getHitbox().x + jugador.getHitbox().width / 2f;
        float piesEnemigoY = hitbox.y + hitbox.height;
        float piesJugadorY = jugador.getHitbox().y + jugador.getHitbox().height;

        float distX = Math.abs(centroJugadorX - centroEnemigoX);
        float distY = Math.abs(piesJugadorY - piesEnemigoY);

        if (distX > 15 && estadoActual != ATTACK) {
            mirandoDerecha = (centroJugadorX > centroEnemigoX);
        }

        if (cooldownAtaque > 0) cooldownAtaque--;

        // --- BUFOS DE DISTANCIA Y VELOCIDAD ---
        int rangoAtaqueX = 180; // Antes 120. Se lanza desde más lejos.
        int margenY = 40;
        float velocidad = 2.2f; // Antes 1.5f. Camina más rápido.

        if (distX <= rangoAtaqueX && distY <= margenY) {
            if (cooldownAtaque <= 0 && estadoActual != ATTACK) {
                estadoActual = ATTACK;
                aniIndex = 0;
                aniTick = 0;
                atacando = true;
                golpeRegistrado = false;
                saltoCalculado = false;
                enElAire = false;
                cooldownAtaque = 120;
            }
        }
        else if (distX < 750 && estadoActual != ATTACK) { // Antes 500. Te detecta de más lejos.
            estadoActual = RUN;
            if (centroJugadorX > centroEnemigoX) this.x += velocidad;
            else this.x -= velocidad;

            if (distY > 10) {
                if (piesJugadorY < piesEnemigoY) this.y -= (velocidad * 0.7f);
                else if (piesJugadorY > piesEnemigoY) this.y += (velocidad * 0.7f);
            }
        }
        else if (estadoActual != ATTACK) {
            estadoActual = IDLE;
        }

        if (estadoActual == ATTACK) {
            if (aniIndex < 4) {
                // Frames 0, 1, 2, 3: Quieto cargando el salto.
            } else if (aniIndex == 4 && !saltoCalculado) {
                saltoCalculado = true;
                enElAire = true;
                timerSalto = 26; // Antes 24. Ahora pasa un poco más de tiempo volando hacia ti.

                double angulo = Math.atan2(piesJugadorY - piesEnemigoY, centroJugadorX - centroEnemigoX);
                velXActual = Math.cos(angulo) * velocidadSalto;
                velYActual = Math.sin(angulo) * velocidadSalto;
            }

            if (enElAire) {
                this.x += velXActual;
                this.y += velYActual;
                timerSalto--;
                if (timerSalto <= 0) {
                    enElAire = false;
                }
            }
        }

        hitbox.x = (int) this.x;
        hitbox.y = (int) this.y;

        aplicarLimitesCarril();
    }

    @Override
    public void actualizar() {
        actualizarTickAnimacion();
    }

    private void actualizarTickAnimacion() {
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;

            int framesActuales = getCantidadFrames(estadoActual);

            if (estadoActual == ATTACK && aniIndex == 3) {
                golpeRegistrado = false;
            }

            if (aniIndex >= framesActuales) {
                if (estadoActual == DEAD) {
                    animacionMuerteTerminada = true;
                    aniIndex = framesActuales - 1;
                } else {
                    aniIndex = 0;
                    if (estadoActual == ATTACK || estadoActual == HURT || estadoActual == STUN) {
                        estadoActual = IDLE;
                        atacando = false;
                        golpeRegistrado = false;
                        enElAire = false;
                        saltoCalculado = false;
                    }
                }
            }
        }
    }

    private int getCantidadFrames(int estado) {
        switch(estado) {
            case IDLE: return 7;
            case RUN: return 8;
            case ATTACK: return 10;
            case HURT: return 5;
            case DEAD: return 15;
            case STUN: return 18;
            default: return 1;
        }
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto || estadoActual == DEAD) return;

        vida -= cantidad;
        if (vida <= 0) {
            vida = 0;
            muerto = true;
            estadoActual = DEAD;
            aniIndex = 0;
            aniTick = 0;
            enElAire = false;
        } else if (estadoActual != ATTACK) {
            estadoActual = HURT;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    @Override
    public boolean puedeAtacar() {
        return !golpeRegistrado && estadoActual == ATTACK;
    }

    @Override
    public void reiniciarCooldown() {
        this.golpeRegistrado = true;
    }

    public Rectangle getAttackBox() {
        boolean esFrameAtaque = (aniIndex >= 4 && aniIndex <= 7);

        if (estadoActual == ATTACK && esFrameAtaque && !golpeRegistrado) {
            int anchoAtk = 50;
            int altoAtk = 40;
            int offsetY = 10;

            int xAtk = mirandoDerecha ? hitbox.x + hitbox.width : hitbox.x - anchoAtk;

            return new Rectangle(xAtk, hitbox.y + offsetY, anchoAtk, altoAtk);
        }
        return null;
    }

    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        if (animacionMuerteTerminada) return;

        BufferedImage imgFrame = null;
        if (GestorRecursos.animacionesMushroom != null && GestorRecursos.animacionesMushroom[estadoActual] != null) {
            int framesValidos = getCantidadFrames(estadoActual);
            int indiceSeguro = Math.min(aniIndex, framesValidos - 1);
            imgFrame = GestorRecursos.animacionesMushroom[estadoActual][indiceSeguro];
        }

        int drawX = hitbox.x - cameraX - ajusteX;
        int drawY = hitbox.y - ajusteY;

        if (imgFrame != null) {
            if (mirandoDerecha) {
                g2.drawImage(imgFrame, drawX + anchoDibujo, drawY, -anchoDibujo, altoDibujo, null);
            } else {
                g2.drawImage(imgFrame, drawX, drawY, anchoDibujo, altoDibujo, null);
            }
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
    public void setAtacando(boolean atacando) { this.atacando = atacando; }
    public boolean isGolpeRegistrado() { return golpeRegistrado; }
    public void setGolpeRegistrado(boolean golpeRegistrado) { this.golpeRegistrado = golpeRegistrado; }
    public boolean isAnimacionMuerteTerminada() { return animacionMuerteTerminada; }
}