package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CapaFondo {
    private BufferedImage imagen;
    private double factorParallax;
    private int y; // Posicion Y en la pantalla

    // Tamaño final de la imagen ya escalada
    private int anchoFinal;
    private int altoFinal;

    // Constructor para crear cada capa del fondo
    public CapaFondo(BufferedImage imagen, double factorParallax, double escala, int y) {
        this.imagen = imagen;
        this.factorParallax = factorParallax;
        this.y = y;

        // Ajustamos el tamaño de la imagen según la escala del juego
        if (imagen != null) {
            this.anchoFinal = (int)(imagen.getWidth() * escala);
            this.altoFinal = (int)(imagen.getHeight() * escala);
        }
    }

    public void dibujar(Graphics g, int cameraX) {
        if (imagen == null) return;

        // Calculamos cuánto se debe mover el fondo según la cámara
        int desplazamiento = (int) (cameraX * factorParallax);

        // Usamos modulo para que la imagen se repita y parezca infinita
        int xInicio = -(desplazamiento % anchoFinal);

        // Arreglamos el inicio para que no se note un corte brusco en la imagen
        if (xInicio > 0) {
            xInicio -= anchoFinal;
        }

        // Dibujamos el fondo varias veces seguidas para llenar la pantalla
        int xActual = xInicio;
        int anchoPantalla = 1920; // Ancho de sobra por si la pantalla es muy grande

        while (xActual < anchoPantalla) {
            g.drawImage(imagen, xActual, y, anchoFinal, altoFinal, null);
            xActual += anchoFinal;
        }
    }
}