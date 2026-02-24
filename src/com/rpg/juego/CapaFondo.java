package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CapaFondo {
    private BufferedImage imagen;
    private double factorParallax;
    private int y; // Coordenada Y de renderizado estatico para profundidad

    // Dimensiones escaladas para el renderizado final de la capa
    private int anchoFinal;
    private int altoFinal;

    // Constructor de la capa de fondo interactiva (Parallax)
    public CapaFondo(BufferedImage imagen, double factorParallax, double escala, int y) {
        this.imagen = imagen;
        this.factorParallax = factorParallax;
        this.y = y;

        // Aplicacion de factor de escala para resolucion objetivo
        if (imagen != null) {
            this.anchoFinal = (int)(imagen.getWidth() * escala);
            this.altoFinal = (int)(imagen.getHeight() * escala);
        }
    }

    public void dibujar(Graphics g, int cameraX) {
        if (imagen == null) return;

        // 1. Calculo de desplazamiento relativo a la posicion de la camara
        int desplazamiento = (int) (cameraX * factorParallax);

        // 2. Calculo del punto de inicio mediante operacion modulo para generar efecto de fondo infinito
        int xInicio = -(desplazamiento % anchoFinal);

        // 3. Ajuste de transicion fluida para evitar cortes visuales
        if (xInicio > 0) {
            xInicio -= anchoFinal;
        }

        // 4. Ciclo de renderizado continuo para cubrir la totalidad del viewport
        int xActual = xInicio;
        int anchoPantalla = 1920; // Margen de seguridad para resoluciones extendidas

        while (xActual < anchoPantalla) {
            g.drawImage(imagen, xActual, y, anchoFinal, altoFinal, null);
            xActual += anchoFinal;
        }
    }
}