package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Proyectil {

    public enum Tipo { ONDA_MAGICA, FLECHA, DAGA, BABA_SLIME }
    public enum Emisor { JUGADOR, ENEMIGO }

    private float x, y;
    private float vx, vy;
    private int distanciaRecorrida = 0;
    private int distanciaMax = 800;
    private float z = 0;
    private float velZ = 0;
    private float gravedadZ = 0;
    private boolean derecha;
    private int dano;
    private Color colorPersonalizado;
    private boolean activo = true;
    private boolean isSplash = false;
    private int timerSplash = 0;
    public CajaColision hitbox;

    private Tipo tipo;
    private Emisor emisor;

    public java.util.Set<EnemigoBase> enemigosGolpeados = new java.util.HashSet<>();

    // --- Parametros de control del ciclo de animacion interactiva ---
    private int aniTick = 0;
    private int aniIndex = 0;
    private int aniSpeed = 5; 

    public Proyectil(float x, float y, float targetX, float targetY, int danoBase, Emisor emisor, Tipo tipo, Color color, float multVelocidad) {
        this.x = x;
        this.y = y;
        this.emisor = emisor;
        this.tipo = tipo;
        this.colorPersonalizado = color;

        // Calculamos la dirección fija hacia el objetivo original
        double angulo = Math.atan2(targetY - y, targetX - x);
        float velocidadBase = 5f;
        if (tipo == Tipo.BABA_SLIME) velocidadBase = 7.8f; 

        if (tipo == Tipo.ONDA_MAGICA) velocidadBase = 12f;
        else if (tipo == Tipo.FLECHA) velocidadBase = 15f;

        this.vx = (float) (Math.cos(angulo) * velocidadBase * multVelocidad);
        this.vy = (float) (Math.sin(angulo) * velocidadBase * multVelocidad);
        this.derecha = (vx >= 0);

        if (tipo == Tipo.ONDA_MAGICA) {
            this.dano = danoBase * 2;
            this.distanciaMax = 500;
            // Compensacion del punto de anclaje de la caja de colision en el eje vertical: -10 en offsetY
            this.hitbox = new CajaColision((int)x, (int)y, 40, 80, 0, -10);
        } else if (tipo == Tipo.FLECHA) {
            this.dano = danoBase; 
            this.distanciaMax = 800;
            this.hitbox = new CajaColision((int)x, (int)y, 30, 10, 0, 0);       
        } else if (tipo == Tipo.BABA_SLIME) {
            this.dano = (int)(danoBase * 1.2f);
            this.z = 0;
            this.velZ = 7; // Más impulso inicial para una parábola clara
            this.gravedadZ = 0.45f;
            this.distanciaMax = 700;
            this.hitbox = new CajaColision((int)x, (int)y, 25, 25, 0, 0);
        } else {
            // Predeterminado
            this.dano = danoBase;
            this.hitbox = new CajaColision((int)x, (int)y, 20, 20, 0, 0);
        }
    }

    public void actualizar() {
        if (!activo) return;

        if (isSplash) {
            timerSplash--;
            if (timerSplash <= 0) activo = false;
            return;
        }

        x += vx;
        y += vy;

        // Física de altura (Eje Z)
        z += velZ;
        velZ -= gravedadZ;
        
        // Si toca el suelo, explota (Splash)
        if (z < 0) {
            z = 0;
            isSplash = true;
            timerSplash = 15; // Se queda 15 frames en el suelo como mancha
            vx = 0; vy = 0; // Se queda quieto
        }

        distanciaRecorrida += Math.abs(vx);
        if (distanciaRecorrida >= distanciaMax) activo = false;

        // Actualización de la caja de impacto — sigue la posición VISUAL del proyectil (y - z)
        hitbox.actualizar((int)x, (int)(y - z));

        // Control algoritmico del secuenciador de frames graficos
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            if (tipo == Tipo.ONDA_MAGICA) {
                // La onda tiene 4 frames (0 a 3) y se queda en el frame 3 al llegar
                if (aniIndex < 3) aniIndex++;
            } else {
                // Otras animaciones (ej. flecha que gira) pueden loopear
                aniIndex++;
                if (aniIndex >= 4) aniIndex = 0;
            }
        }
    }

    public void dibujar(Graphics2D g2, int cameraX) {
        if (!activo) return;

        int drawX = (int)x - cameraX;
        int drawY = (int)(y - z); 

        int tamX = 25;
        int tamY = 25;

        // Calculamos el ángulo de rotación basado en el movimiento (vx y velZ)
        // Usamos Math.abs(vx) para que siempre apunte hacia adelante independientemente de si va a izq o der
        double anguloRotacion = Math.atan2(-velZ, Math.abs(vx));

        if (tipo == Tipo.BABA_SLIME) {
            // Dibujamos el slime tipo pixel art manual si no hay imagen externa
            Color colorBase = (colorPersonalizado != null) ? colorPersonalizado : new Color(150, 255, 150);
            
            // Guardamos el estado del lienzo para rotar solo el proyectil
            java.awt.geom.AffineTransform old = g2.getTransform();
            
            if (isSplash) {
                // Dibujamos la mancha en el suelo
                g2.setColor(new Color(colorBase.getRed(), colorBase.getGreen(), colorBase.getBlue(), 150));
                g2.fillOval(drawX - 10, (int)y - 5, 45, 15);
            } else {
                // Rotamos hacia la dirección del movimiento
                g2.translate(drawX + tamX/2, drawY + tamY/2);
                g2.rotate(derecha ? anguloRotacion : -anguloRotacion + Math.PI);
                
                // Cabeza del proyectil (Blob tipo pixel art)
                g2.setColor(colorBase);
                g2.fillRect(-tamX/2, -tamY/4, tamX, tamY/2); // Cuerpo alargado
                g2.fillRect(-tamX/4, -tamY/2, tamX/2, tamY); // Parte central
                
                // Bordes oscuros para estilo pixel art
                g2.setColor(colorBase.darker());
                g2.drawRect(-tamX/2, -tamY/4, tamX, tamY/2);
            }
            g2.setTransform(old);

        } else if (tipo == Tipo.ONDA_MAGICA) {
            // Restauramos el sprite animado de la onda
            if (GestorRecursos.animacionOnda != null && aniIndex < GestorRecursos.animacionOnda.length) {
                BufferedImage img = GestorRecursos.animacionOnda[aniIndex];
                if (img != null) {
                    // Para la onda, usamos un tamaño más grande y un ligero desfase
                    int wOnda = 84, hOnda = 84;
                    if (derecha) g2.drawImage(img, drawX, drawY - 12, wOnda, hOnda, null);
                    else g2.drawImage(img, drawX + wOnda, drawY - 12, -wOnda, hOnda, null);
                }
            } else {
                // Fallback si no hay imagen
                g2.setColor(Color.CYAN);
                g2.fillOval(drawX, drawY, 30, 30);
            }
        } else {
            // Otros proyectiles (Flechas, etc.) con rotación
            java.awt.geom.AffineTransform old = g2.getTransform();
            g2.translate(drawX + 10, drawY + 10);
            g2.rotate(derecha ? anguloRotacion : -anguloRotacion + Math.PI);
            
            g2.setColor(emisor == Emisor.ENEMIGO ? Color.RED : Color.WHITE);
            g2.fillRect(-10, -2, 20, 4); // Representación simple de flecha/daga
            
            g2.setTransform(old);
        }

        // Capa de visualizacion de depuracion estructural
        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    public boolean isSplash() { return isSplash; }
    public float getZ() { return z; }
    public float getY() { return y; }
    public CajaColision getHitbox() { return hitbox; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getDano() { return dano; }
    public Tipo getTipo() { return tipo; }
    public Emisor getEmisor() { return emisor; }
    /** Permite ajustar el impulso vertical inicial desde fuera (para proyectíl parabólico escalado). */
    public void setVelZ(float velZ) { this.velZ = velZ; }
}