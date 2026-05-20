package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EnemigoEsqueleto extends EnemigoBase {

    // Los estados que puede tener el esqueleto para saber qué animación ponerle
    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int ATTACK_1 = 2;
    public static final int ATTACK_2 = 3;
    public static final int HURT = 4;
    public static final int DEAD = 5;

    // Tamaño del sprite en pantalla
    private int anchoDibujo = 192;
    private int altoDibujo = 128;

    // Tamaño de la caja con la que nos chocamos y le pegamos
    private int anchoHitbox = 45;
    private int altoHitbox = 95;

    // Ajustes para que el dibujo cuadre bien con la hitbox física
    private int ajusteX = 80;
    private int ajusteY = 35;
    private int ajusteDerecha = 10;

    private String tipo;

    // Variables para saber qué hace, a dónde mira y si puede pegar
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
        this.hitbox = new CajaColision((int)x, (int)y, anchoHitbox, altoHitbox);
    }

    @Override
    public void iniciarMarchaCinematica(int metaX) {
        this.cinematicTargetX = metaX;
        this.llegoMetaCinematica = false;
        this.mirandoDerecha = false; // Van hacia la izquierda
        this.estadoActual = WALK;
    }

    @Override
    public void actualizarMarchaCinematica() {
        if (llegoMetaCinematica) {
            estadoActual = IDLE;
            return;
        }

        mirandoDerecha = false;
        estadoActual = WALK;

        // Marcha de cinemática más rápida (x2 aprox)
        float velocidadM = (tipo.equals("DORADO")) ? 5.0f : 3.6f;

        if (Math.abs(this.x - cinematicTargetX) <= 15) {
            this.x = cinematicTargetX;
            llegoMetaCinematica = true;
            estadoActual = IDLE;
            return;
        }

        this.x -= velocidadM;
        
        hitbox.actualizar((int) this.x, (int) this.y);
        aplicarLimitesCarril();
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

        if (distX > 15) {
            if (estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
                mirandoDerecha = (centroJugadorX > centroEnemigoX);
            } else if (aniIndex <= 5) {
                mirandoDerecha = (centroJugadorX > centroEnemigoX);
            }
        }

        if (cooldownAtaque > 0) cooldownAtaque--;

        int rangoAtaqueX = (tipo.equals("DORADO")) ? 130 : 115;
        int margenY = 15;
        float velocidad = (tipo.equals("DORADO")) ? 2.5f : 1.8f;

        if (distX <= rangoAtaqueX && distY <= margenY) {
            if (cooldownAtaque <= 0 && estadoActual != ATTACK_1 && estadoActual != ATTACK_2) {
                // Selección inteligente de ataque basado en la distancia
                if (distX <= 85) {
                    estadoActual = ATTACK_1; // Tajo rápido cuerpo a cuerpo (Rango ampliado a 85)
                } else {
                    estadoActual = ATTACK_2; // Estocada de largo alcance
                }
                aniIndex = 0;
                aniTick = 0;
                atacando = true;
                golpeRegistrado = false;
                cooldownAtaque = 60; // Cooldown reducido para un combate más fluido

                // Lógica de Delayed Attack
                if (Math.random() < 0.35) {
                    isDelayedAttack = true;
                    delayTimer = 30 + (int)(Math.random() * 60); // Casi 1 segundo de carga extra
                    initialDelay = delayTimer;
                } else {
                    isDelayedAttack = false;
                }
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

        hitbox.actualizar((int) this.x, (int) this.y);

        aplicarLimitesCarril();
    }

    @Override
    public void actualizar() {
        actualizarTickAnimacion();
    }

    private void actualizarTickAnimacion() {
        if (estadoActual == ATTACK_1 || estadoActual == ATTACK_2) {
            // Congelamos en el frame 2 para la pose de preparación
            if (aniIndex == 2 && isDelayedAttack && delayTimer > 0) {
                if (delayTimer == initialDelay) {
                    GestorSonidos.reproducir(GestorSonidos.CARGA_ATAQUE);
                }
                delayTimer--;
                return; // Evitamos que avance aniTick
            }
        }

        aniTick++;
        int velocidadActual = 15;
        if (estadoActual == WALK) velocidadActual = 9;
        else if (estadoActual == ATTACK_1) velocidadActual = 10;
        else if (estadoActual == ATTACK_2) velocidadActual = 8;
        else if (estadoActual == DEAD) velocidadActual = 6;
        
        if (aniTick >= velocidadActual) {
            aniTick = 0;
            aniIndex++;

            int framesActuales = getCantidadFrames(estadoActual);

            if (estadoActual == ATTACK_1 || estadoActual == ATTACK_2) {
                if (aniIndex == 2 && aniTick == 0) GestorSonidos.reproducirEspadaAleatoria();
                
                if (aniIndex == 5) golpeRegistrado = false;
            }

            if (aniIndex >= framesActuales) {
                if (estadoActual == DEAD) {
                    aniIndex = framesActuales - 1; // Dejamos la animación en el último frame para que quede muerto en el piso
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
            GestorSonidos.reproducir(GestorSonidos.MUERTE_ESQUELETO);
            aniIndex = 0;
            aniTick = 0;
        } else if (estadoActual != ATTACK_1 && estadoActual != ATTACK_2 && estadoActual != HURT) {
            estadoActual = HURT;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    public Rectangle getAttackBox() {
        boolean esFrameAtaque = (aniIndex == 5 || aniIndex == 6);

        if ((estadoActual == ATTACK_1 || estadoActual == ATTACK_2) && esFrameAtaque) {

            int anchoAtk = (estadoActual == ATTACK_1) ? 75 : 80;
            int altoAtk = 40;
            int offsetY = 10;

            if (tipo.equals("DORADO")) {
                anchoAtk = 100; // Ambos ataques dorados unificados a 100 (Estocada reducida -15)
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
            // Efecto de parpadeo al spawnear
            java.awt.Composite compOrig = g2.getComposite();
            float alpha = getSpawnAlpha();
            if (alpha < 1.0f) {
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            }

            if (mirandoDerecha) {
                g2.drawImage(imgFrame, drawX + ajusteDerecha, drawY, anchoDibujo, altoDibujo, null);
            } else {
                g2.drawImage(imgFrame, drawX + anchoDibujo, drawY, -anchoDibujo, altoDibujo, null);
            }

            g2.setComposite(compOrig);
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

    @Override
    public boolean isAnimacionMuerteTerminada() {
        return muerto && estadoActual == DEAD && aniIndex >= getCantidadFrames(DEAD) - 1;
    }
}