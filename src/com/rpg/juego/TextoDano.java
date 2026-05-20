package com.rpg.juego;

import java.awt.*;
public class TextoDano {
    private float x, y;
    private String texto; // Lo guardamos como texto para poder escribir "BLOQUEADO" o los números
    private Color color;
    private int vida = 60; // Cuánto tiempo dura en pantalla antes de desaparecer
    private boolean activo = true;

    // Si le pasamos un número (como el daño que hicimos), lo convertimos a texto
    public TextoDano(float x, float y, int valor, Color color) {
        this.x = x;
        this.y = y;
        this.texto = String.valueOf(valor);
        this.color = color;
    }

    // Si le pasamos un texto directamente (como "ESQUIVADO")
    public TextoDano(float x, float y, String mensaje, Color color) {
        this.x = x;
        this.y = y;
        this.texto = mensaje;
        this.color = color;
    }

    public void actualizar() {
        y -= 1; // Hacemos que el texto vaya subiendo despacito
        vida--;
        if (vida <= 0) activo = false;
    }

    public void dibujar(Graphics g, int cameraX) {
        g.setColor(color);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        // Dibujamos las letras tomando en cuenta la cámara para que se queden en el lugar del golpe
        g.drawString(texto, (int)(x - cameraX), (int)y);
    }

    public boolean isActivo() { return activo; }
}