package com.rpg.juego;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class BarraJefe {

    private EnemigoBase jefe;
    private String nombre;
    private int x, y, ancho, alto;

    // Variable para el efecto visual de reduccion progresiva de vida tras recibir dano
    private float vidaRetrasada;

    public BarraJefe(EnemigoBase jefe, String nombre, int anchoPantalla) {
        this.jefe = jefe;
        this.nombre = nombre;
        // Dimensiones de la interfaz de la barra de jefe
        this.ancho = 450;
        this.alto = 22;

        // Calculo de posicionamiento centrado en el eje X superior de la pantalla
        this.x = (anchoPantalla / 2) - (this.ancho / 2) + 350;
        this.y = 50;

        if (jefe != null) {
            this.vidaRetrasada = jefe.getVidaMax();
        }
    }

    public void actualizar() {
        if (jefe == null || jefe.isMuerto()) return;

        // Logica de interpolacion para el efecto visual de daño recibido
        if (vidaRetrasada > jefe.getVida()) {
            vidaRetrasada -= 2.5f; // Tasa de decremento del indicador de daño residual
            if (vidaRetrasada < jefe.getVida()) {
                vidaRetrasada = jefe.getVida();
            }
        }
    }

    public void dibujar(Graphics2D g2) {
        if (jefe == null || jefe.isMuerto()) return;

        // --- Calculo de proporciones visuales ---
        float porcentajeVida = (float) jefe.getVida() / jefe.getVidaMax();
        float porcentajeRetrasado = vidaRetrasada / jefe.getVidaMax();

        int anchoRojo = (int) (ancho * porcentajeVida);
        int anchoAmarillo = (int) (ancho * porcentajeRetrasado);

        // 1. Renderizado del fondo de la barra
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRect(x, y, ancho, alto);

        // 2. Renderizado del indicador de dano residual (Efecto de impacto)
        g2.setColor(new Color(255, 180, 0));
        g2.fillRect(x, y, anchoAmarillo, alto);

        // 3. Renderizado de la vitalidad actual de la entidad
        g2.setColor(new Color(190, 20, 20));
        g2.fillRect(x, y, anchoRojo, alto);

        // 4. Renderizado del contorno estetico de la interfaz
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x, y, ancho, alto);

        // 5. Renderizado de la informacion textual (Nombre y valores absolutos)
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        String texto = nombre + "  (" + jefe.getVida() + "/" + jefe.getVidaMax() + ")";

        // Metricas de fuente para alineacion dinamica del texto
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int anchoTexto = fm.stringWidth(texto);

        // Ecuacion de centrado relativo al componente contenedor
        int xTextoCentrado = x + (ancho / 2) - (anchoTexto / 2);

        // Sombreado de texto para mejorar el contraste
        g2.setColor(Color.BLACK);
        g2.drawString(texto, xTextoCentrado + 2, y - 8);

        // Renderizado del texto principal
        g2.setColor(Color.WHITE);
        g2.drawString(texto, xTextoCentrado, y - 10);
    }

    // Metodo mutador para reasignar la entidad vinculada a la barra tras cambios de nivel
    public void setJefe(EnemigoBase nuevoJefe, String nuevoNombre) {
        this.jefe = nuevoJefe;
        this.nombre = nuevoNombre;
        if (nuevoJefe != null) {
            this.vidaRetrasada = nuevoJefe.getVidaMax();
        }
    }
}