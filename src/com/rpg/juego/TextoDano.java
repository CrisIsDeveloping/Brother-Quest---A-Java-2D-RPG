package com.rpg.juego;

import java.awt.*;
public class TextoDano {
    private float x, y;
    private String texto; // Almacenamiento generico del contenido representacional
    private Color color;
    private int vida = 60; // Ciclo de vida util (TTL) en cuadros logicos
    private boolean activo = true;

    // Sobrecarga de constructor orientada a resolucion de valores numericos (Evaluadores de daño o loot)
    public TextoDano(float x, float y, int valor, Color color) {
        this.x = x;
        this.y = y;
        this.texto = String.valueOf(valor); // Transformacion a cadena de caracteres generica
        this.color = color;
    }

    // Sobrecarga de constructor enfocada a la inyeccion directa de metadatos del sistema (Evasiones, Bloqueos)
    public TextoDano(float x, float y, String mensaje, Color color) {
        this.x = x;
        this.y = y;
        this.texto = mensaje;
        this.color = color;
    }

    public void actualizar() {
        y -= 1; // Transicion continua de traslacion ascendente
        vida--;
        if (vida <= 0) activo = false;
    }

    public void dibujar(Graphics g, int cameraX) {
        g.setColor(color);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        // Descarga del buffer al contexto de renderizado principal
        g.drawString(texto, (int)(x - cameraX), (int)y);
    }

    public boolean isActivo() { return activo; }
}