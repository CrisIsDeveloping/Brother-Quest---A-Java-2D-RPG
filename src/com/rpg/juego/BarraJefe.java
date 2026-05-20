package com.rpg.juego;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class BarraJefe {

    private EnemigoBase jefe;
    private String nombre;
    private int x, y, ancho, alto;

    // Variable para el efecto de que la barra baje lento al recibir daño
    private float vidaRetrasada;

    public BarraJefe(EnemigoBase jefe, String nombre, int anchoPantalla) {
        this.jefe = jefe;
        this.nombre = nombre;
        // Tamaño de la barra
        this.ancho = 450;
        this.alto = 22;

        // Centramos la barra arriba en la pantalla
        this.x = (anchoPantalla / 2) - (this.ancho / 2);
        this.y = 50;

        if (jefe != null) {
            this.vidaRetrasada = jefe.getVidaMax();
        }
    }

    public void actualizar() {
        if (jefe == null || jefe.isMuerto()) return;

        // Hacemos que la barra amarilla baje de forma adaptativa
        if (vidaRetrasada > jefe.getVida()) {
            vidaRetrasada -= Math.max(2.5f, jefe.getVidaMax() * 0.015f); // Se vacía como máximo en ~1 segundo
            if (vidaRetrasada < jefe.getVida()) {
                vidaRetrasada = jefe.getVida();
            }
        } else if (vidaRetrasada < jefe.getVida()) {
            vidaRetrasada = jefe.getVida(); // Por si el boss se cura
        }
    }

    public void dibujar(Graphics2D g2, Jugador jugador) {
        if (jefe == null || jefe.isMuerto()) return;

        // Ocultamos la barra si el jugador está demasiado lejos (ej. a más de 1000 píxeles)
        double distancia = Math.hypot(jugador.getX() - jefe.getX(), jugador.getY() - jefe.getY());
        if (distancia > 1000) return;

        // Calculamos los porcentajes de vida para dibujar las barras
        float porcentajeVida = (float) jefe.getVida() / jefe.getVidaMax();
        float porcentajeRetrasado = vidaRetrasada / jefe.getVidaMax();

        int anchoRojo = (int) (ancho * porcentajeVida);
        int anchoAmarillo = (int) (ancho * porcentajeRetrasado);

        // Fondo oscuro de la barra
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRect(x, y, ancho, alto);

        // Barra amarilla (daño recibido)
        g2.setColor(new Color(255, 180, 0));
        g2.fillRect(x, y, anchoAmarillo, alto);

        // Barra roja (vida actual)
        g2.setColor(new Color(190, 20, 20));
        g2.fillRect(x, y, anchoRojo, alto);

        // Borde gris de la barra
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x, y, ancho, alto);

        // Texto con el nombre y la vida
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        // Para el boss no mostramos nivel, solo nombre y HP
        String texto = nombre + "  " + jefe.getVida() + " / " + jefe.getVidaMax();

        // Medimos el texto para poder centrarlo
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int anchoTexto = fm.stringWidth(texto);

        // Calculamos el centro exacto para poner el texto
        int xTextoCentrado = x + (ancho / 2) - (anchoTexto / 2);

        // Sombra negra del texto para que se lea mejor
        g2.setColor(Color.BLACK);
        g2.drawString(texto, xTextoCentrado + 2, y - 8);

        // Texto principal en color blanco
        g2.setColor(Color.WHITE);
        g2.drawString(texto, xTextoCentrado, y - 10);
    }

    // Cambiamos el jefe de la barra si cambiamos de nivel o monstruo
    public void setJefe(EnemigoBase nuevoJefe, String nuevoNombre) {
        this.jefe = nuevoJefe;
        this.nombre = nuevoNombre;
        if (nuevoJefe != null) {
            this.vidaRetrasada = nuevoJefe.getVidaMax();
        }
    }
}