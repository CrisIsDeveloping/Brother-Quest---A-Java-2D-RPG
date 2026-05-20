package com.rpg.juego;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Trader extends Entidad {
    public static final int IDLE = 0;
    public static final int IDLE_2 = 1;
    public static final int GREETING = 2; // Idle_3
    public static final int DIALOGUE = 3;
    public static final int APPROVAL = 4;

    private int estadoActual = IDLE;
    private int aniIndex = 0;
    private int aniTick = 0;
    private int aniSpeed = 10;

    private boolean yaSaludo = false;
    private boolean mirandoIzquierda = true;

    public Trader(int x, int y) {
        this.x = x;
        this.y = y;
        this.hitbox = new CajaColision(x, y - 40, 60, 80, 0, 0);
    }

    public void actualizar(Jugador jugador, int cameraX) {
        int miX = getXLogico(cameraX);
        if (jugador != null) {
            // El npc mira al jugador según su posición X lógica proyectada al layer 1
            if (jugador.getXLogical() > miX + 30) {
                mirandoIzquierda = false; // El jugador está a la derecha
            } else {
                mirandoIzquierda = true;
            }
        }
        
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;

            int totalFrames = getCantidadFrames();
            if (aniIndex >= totalFrames) {
                aniIndex = 0;

                if (estadoActual == GREETING) {
                    estadoActual = IDLE;
                } else if (estadoActual == APPROVAL) {
                    estadoActual = DIALOGUE; // Vuelve a hablar despues de comprar
                } else if (estadoActual == IDLE && Math.random() < 0.2) {
                    estadoActual = IDLE_2;
                } else if (estadoActual == IDLE_2) {
                    estadoActual = IDLE;
                }
            }
        }

        if (!yaSaludo) {
            float dist = Math.abs(jugador.getX() - miX);
            if (dist < 250) {
                yaSaludo = true;
                setEstado(GREETING);
            }
        }
    }

    public int getXLogico(int cameraX) {
        return (int) x;
    }

    public void setEstado(int estado) {
        if (this.estadoActual != estado) {
            this.estadoActual = estado;
            this.aniIndex = 0;
            this.aniTick = 0;
        }
    }

    public int getEstadoActual() {
        return estadoActual;
    }

    private int getCantidadFrames() {
        if (estadoActual == GREETING) {
            return GestorRecursos.traderIdle3.length; // Usualmente 14 o 15
        }
        return 5;
    }

    public void dibujar(Graphics2D g2, int cameraX) {
        BufferedImage img = null;
        switch (estadoActual) {
            case IDLE:
                if (aniIndex < GestorRecursos.traderIdle.length)
                    img = GestorRecursos.traderIdle[aniIndex];
                break;
            case IDLE_2:
                if (aniIndex < GestorRecursos.traderIdle2.length)
                    img = GestorRecursos.traderIdle2[aniIndex];
                break;
            case GREETING:
                if (aniIndex < GestorRecursos.traderIdle3.length)
                    img = GestorRecursos.traderIdle3[aniIndex];
                break;
            case DIALOGUE:
                if (aniIndex < GestorRecursos.traderDialogue.length)
                    img = GestorRecursos.traderDialogue[aniIndex];
                break;
            case APPROVAL:
                if (aniIndex < GestorRecursos.traderApproval.length)
                    img = GestorRecursos.traderApproval[aniIndex];
                break;
        }

        if (img != null) {
            // El sprite original es de 128x128p
            // Escalamos a 1.5x (192x192) para que sea proporcional al caballero
            int visualWidth = 192;
            int visualHeight = 192;

            // Centramos el sprite de 192px sobre el CENTRO de la hitbox
            int centralX = (int) x + 30; // Desplazado -10px para centrar con la sombra
            int drawX = (int) (centralX - cameraX) - (visualWidth / 2);

            // Alineamos los pies (parte inferior del sprite) con el suelo (y)
            int drawY = (int) y - visualHeight + 40;

            if (!mirandoIzquierda) {
                // Dibujo normal
                g2.drawImage(img, drawX, drawY, visualWidth, visualHeight, null);
            } else {
                // Invertimos la imagen horizontalmente (Flip)
                g2.drawImage(img, drawX + visualWidth, drawY, -visualWidth, visualHeight, null);
            }
        }

        if (GamePanel.debugActivado) {
            g2.setColor(java.awt.Color.MAGENTA);
            int visualHitboxX = (int) (x - cameraX);
            g2.drawRect(visualHitboxX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    // Metodos vacios por heredar Entidad (si son obligatorios)
    @Override
    public void actualizar() {
    }

    public void dibujarSombra(Graphics2D g2, int cameraX) {
        g2.setColor(new java.awt.Color(0, 0, 0, 80)); // Sombra semitransparente
        int visualX = (int) (x - cameraX) + 30; 
        // La sombra se pinta a los pies de la hitbox (+40px debajo de la x)
        g2.fillOval(visualX - 30, hitbox.y + hitbox.height - 10, 60, 20);
    }
}
