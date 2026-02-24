package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Proyectil {
    private float x, y;
    private int vel = 12;
    private int distanciaRecorrida = 0;
    private int distanciaMax = 500;
    private boolean derecha;
    private int dano;
    private boolean activo = true;
    public Rectangle hitbox;

    // --- Parametros de control del ciclo de animacion interactiva ---
    private int aniTick = 0;
    private int aniIndex = 0;
    private int aniSpeed = 5; // Modulador de frecuencia temporal (Inversamente proporcional a la velocidad)

    public Proyectil(float x, float y, boolean derecha, int danoBase) {
        this.x = x;
        this.y = y;
        this.derecha = derecha;
        this.dano = danoBase * 2;

        // Instanciacion estructurada de la frontera de interaccion fisica
        this.hitbox = new Rectangle((int)x, (int)y - 10, 40, 80);
    }

    public void actualizar() {
        if (!activo) return;

        if (derecha) x += vel;
        else x -= vel;

        distanciaRecorrida += vel;
        if (distanciaRecorrida >= distanciaMax) activo = false;

        // Compensacion del punto de anclaje de la caja de colision en el eje vertical
        hitbox.setLocation((int)x, (int)y - 10);

        // Control algoritmico del secuenciador de frames graficos
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            if (aniIndex < 3) aniIndex++;
        }
    }

    public void dibujar(Graphics2D g2, int cameraX) {
        if (!activo) return;

        int drawX = (int)x - cameraX;

        // Algoritmo de resampleo grafico para ajuste visual sobre el contenedor logico
        // Sobrescritura forzada de la resolucion natural del asset
        int nuevoTamanoY = 84;
        int nuevoTamanoX = 84;
        int offsetVisualY = -12; // Compensacion de desplazamiento para centralizacion de coordenadas

        if (GestorRecursos.animacionOnda != null && GestorRecursos.animacionOnda[aniIndex] != null) {
            BufferedImage img = GestorRecursos.animacionOnda[aniIndex];

            if (derecha) {
                g2.drawImage(img, drawX, (int)y + offsetVisualY, nuevoTamanoX, nuevoTamanoY, null);
            } else {
                // Operacion matricial para inversion horizontal condicional
                g2.drawImage(img, drawX + nuevoTamanoX, (int)y + offsetVisualY, -nuevoTamanoX, nuevoTamanoY, null);
            }
        }

        // Capa de visualizacion de depuracion estructural
        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    public Rectangle getHitbox() { return hitbox; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getDano() { return dano; }
}