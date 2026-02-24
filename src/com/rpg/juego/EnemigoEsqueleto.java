package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EnemigoEsqueleto extends EnemigoBase {

    // Constantes de la maquina de estados de animacion
    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int ATTACK_1 = 2;
    public static final int ATTACK_2 = 3;
    public static final int HURT = 4;
    public static final int DEAD = 5;

    // --- Parametros de escalado y renderizado visual ---
    private int anchoDibujo = 192;
    private int altoDibujo = 128;

    // Dimensiones de la caja de colision fisica (Hitbox)
    private int anchoHitbox = 45;
    private int altoHitbox = 95;

    // Offsets de alineacion espacial entre el sprite y la caja de colision
    private int ajusteX = 80;
    private int ajusteY = 35;
    private int ajusteDerecha = 10;

    private String tipo;

    // --- Variables de control logico e Inteligencia Artificial ---
    private boolean mirandoDerecha = true;
    private int cooldownAtaque = 0;
    private boolean golpeRegistrado = false;
    private boolean atacando = false;

    public EnemigoEsqueleto(int x, int y, String tipo) {
        super(x, y);
        this.tipo = tipo.toUpperCase();
        this.estadoActual = IDLE;
        this.aniSpeed = 6;

        if (this.tipo.equals("DORADO")) {
            this.anchoDibujo = 240;
            this.altoDibujo = 160;
            this.anchoHitbox = 60;
            this.altoHitbox = 110;
            this.ajusteX = 95;
            this.ajusteY = 50;
            this.ajusteDerecha = 15;

            this.vidaMax = 200;
            this.dano = 30;
            this.xpQueDa = 80;
        } else {
            this.vidaMax = 120;
            this.dano = 18;
            this.xpQueDa = 45;
            this.ajusteDerecha = 10;
        }

        this.vida = this.vidaMax;
        this.hitbox = new Rectangle((int)x, (int)y, anchoHitbox, altoHitbox);
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == DEAD || estadoActual == HURT) return;

        float centroEnemigoX = hitbox.x + hitbox.width / 2f;
        float centroJugadorX = jugador.getHitbox().x + jugador.getHitbox().width / 2f;
        float piesEnemigoY = hitbox.y + hitbox.height;
        float piesJugadorY = jugador.getHitbox().y + jugador.getHitbox().height;

        float distX = Math.abs(centroJugadorX - centroEnemigoX);
        float distY = Math.abs(piesJugadorY - piesEnemigoY);

        if (distX > 15 && estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
            mirandoDerecha = (centroJugadorX > centroEnemigoX);
        }

        if (cooldownAtaque > 0) cooldownAtaque--;

        int rangoAtaqueX = (tipo.equals("DORADO")) ? 80 : 65;
        int margenY = 35;
        float velocidad = (tipo.equals("DORADO")) ? 2.5f : 1.8f;

        if (distX <= rangoAtaqueX && distY <= margenY) {
            if (cooldownAtaque <= 0 && estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
                estadoActual = (Math.random() < 0.5) ? ATTACK_1 : ATTACK_2;
                aniIndex = 0;
                aniTick = 0;
                atacando = true;
                golpeRegistrado = false;
                cooldownAtaque = 80;
            } else if (estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
                estadoActual = IDLE;
            }
        }
        else if (distX < 600 && estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
            estadoActual = WALK;

            if (centroJugadorX > centroEnemigoX) this.x += velocidad;
            else this.x -= velocidad;

            if (distY > 10) {
                if (piesJugadorY < piesEnemigoY) this.y -= (velocidad * 0.7f);
                else if (piesJugadorY > piesEnemigoY) this.y += (velocidad * 0.7f);
            }
        }
        else if (estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
            estadoActual = IDLE;
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

            if (estadoActual == ATTACK_1 || estadoActual == ATTACK_2) {
                if (aniIndex == 5) golpeRegistrado = false;
            }

            if (aniIndex >= framesActuales) {
                if (estadoActual == DEAD) {
                    aniIndex = framesActuales - 1; // Se queda tirado en el último frame
                } else {
                    aniIndex = 0;
                    if (estadoActual == ATTACK_1 || estadoActual == ATTACK_2 || estadoActual == HURT) {
                        estadoActual = IDLE;
                        atacando = false;
                        golpeRegistrado = false;
                    }
                }
            }
        }
    }

    private int getCantidadFrames(int estado) {
        switch(estado) {
            case IDLE: return 8;
            case WALK: return 10;
            case ATTACK_1: return 10;
            case ATTACK_2: return 9;
            case HURT: return 5;
            case DEAD: return 13;
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
        } else if (estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
            estadoActual = HURT;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    public Rectangle getAttackBox() {
        boolean esFrameAtaque = (aniIndex >= 5 && aniIndex <= 7);

        if ((estadoActual == ATTACK_1 || estadoActual == ATTACK_2) && esFrameAtaque) {

            int anchoAtk = estadoActual == ATTACK_2 ? 70 : 50;
            int altoAtk = 40;
            int offsetY = 10;

            if (tipo.equals("DORADO")) {
                anchoAtk = estadoActual == ATTACK_2 ? 95 : 75;
                altoAtk = 60;
                offsetY = 15;
            }

            int xAtk = mirandoDerecha ? hitbox.x + hitbox.width : hitbox.x - anchoAtk;

            return new Rectangle(xAtk, hitbox.y + offsetY, anchoAtk, altoAtk);
        }
        return null;
    }

    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        BufferedImage[][] matrizActual = tipo.equals("DORADO") ?
                GestorRecursos.animacionesEsqOro :
                GestorRecursos.animacionesEsqBlanco;

        BufferedImage imgFrame = null;

        if (matrizActual != null && matrizActual[estadoActual] != null) {
            int framesValidos = getCantidadFrames(estadoActual);
            int indiceSeguro = Math.min(aniIndex, framesValidos - 1);
            imgFrame = matrizActual[estadoActual][indiceSeguro];
        }

        int drawX = hitbox.x - cameraX - ajusteX;
        int drawY = hitbox.y - ajusteY;

        if (imgFrame != null) {
            if (mirandoDerecha) {
                g2.drawImage(imgFrame, drawX + ajusteDerecha, drawY, anchoDibujo, altoDibujo, null);
            } else {
                g2.drawImage(imgFrame, drawX + anchoDibujo, drawY, -anchoDibujo, altoDibujo, null);
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
}