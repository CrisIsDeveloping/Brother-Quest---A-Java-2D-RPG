package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Entidad ObjetoRecogible
 * Gestiona el ciclo de vida, la física de rebote y el renderizado de consumibles y monedas.
 * Implementa un modelo de partículas con restitución cinética para simular impacto con el suelo.
 */
public class ObjetoRecogible {

    // Identificadores constantes de tipos de entidad recolectable
    public static final int TIPO_MONEDA = 0;
    public static final int TIPO_POCION_VIDA = 1;
    public static final int TIPO_POCION_FUERZA = 2;
    public static final int TIPO_POCION_VELOCIDAD = 3;

    private float x, y; // Cambiado a float para mayor precisión en cálculos físicos
    private int tipo;
    private boolean activo = true;

    // --- Parámetros del Modelo Físico Interactivo ---
    private float velocidadX;
    private float velocidadY;
    private float gravedad = 0.5f;
    private int sueloY;
    private boolean enSuelo = false;
    private Rectangle hitbox;

    // --- Funciones Matemáticas para Simulación Visual (Idle) ---
    private int floatOffset = 0;
    private boolean subiendo = true;
    private int tick = 0;
    private boolean mostrarTag = false;

    public ObjetoRecogible(int x, int y, int tipo, int sueloY) {
        this.x = x;
        this.y = y;
        this.tipo = tipo;
        this.sueloY = sueloY;

        /* * AJUSTE DE IMPULSO:
         * Se reduce el rango de dispersión horizontal de 8 a 3 píxeles.
         * El impulso vertical se suaviza para evitar saltos excesivos.
         */
        this.velocidadX = (float)(Math.random() * 3 - 1.5f);
        this.velocidadY = (float)(Math.random() * -3 - 3);

        this.hitbox = new Rectangle(x, y, 20, 20);
    }

    /**
     * Actualiza la cinemática del objeto aplicando fuerzas de gravedad y fricción.
     */
    public void actualizar() {
        if (!enSuelo) {
            velocidadY += gravedad;
            y += velocidadY;
            x += velocidadX;

            // Detección de colisión con el plano horizontal (Suelo)
            if (y >= sueloY) {
                y = sueloY;
                if (velocidadY > 2) {
                    // Simulación de rebote con pérdida de energía (0.4f)
                    velocidadY = -velocidadY * 0.4f;
                    velocidadX *= 0.5f; // Fricción ambiental al impactar
                } else {
                    // Transición a estado de reposo absoluto
                    velocidadY = 0;
                    velocidadX = 0;
                    enSuelo = true;
                }
            }
        } else {
            // Algoritmo de oscilación armónica simple para feedback visual (Levitación)
            tick++;
            if (tick > 5) {
                tick = 0;
                if (subiendo) {
                    floatOffset--;
                    if (floatOffset < -5) subiendo = false;
                } else {
                    floatOffset++;
                    if (floatOffset > 0) subiendo = true;
                }
            }
        }

        // Sincronización del bounding box con la posición actual y el offset visual
        hitbox.x = (int) x;
        hitbox.y = (int) y + floatOffset;
    }

    /**
     * Renderizado de sombra proyectada con offset dinámico para profundidad.
     */
    public void dibujarSombra(Graphics2D g2, int cameraX) {
        g2.setColor(new Color(0, 0, 0, 100));

        // Ajuste fino para centrar la sombra bajo la base del item
        int sombraX = (int) x - cameraX + 2;
        int sombraY = sueloY + 19; // Anclaje fijo al sueloY del constructor

        int anchoSombra = 16;
        int altoSombra = 6;

        g2.fillOval(sombraX, sombraY, anchoSombra, altoSombra);
    }

    public void dibujar(Graphics g, int cameraX) {
        int drawX = (int) x - cameraX;
        int drawY = (int) y + floatOffset;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String nombreTag = "";
        Color colorTag = Color.WHITE;

        switch (tipo) {
            case TIPO_MONEDA:
                if (GestorRecursos.monedaImg != null) {
                    g2.drawImage(GestorRecursos.monedaImg, drawX, drawY, 20, 20, null);
                } else {
                    renderizarFallbackMoneda(g2, drawX, drawY);
                }
                break;

            case TIPO_POCION_VIDA:
                dibujarBotella(g2, drawX, drawY, Color.RED);
                nombreTag = "Poción Vida";
                colorTag = new Color(255, 100, 100);
                break;

            case TIPO_POCION_FUERZA:
                dibujarBotella(g2, drawX, drawY, new Color(138, 43, 226));
                nombreTag = "Poción Fuerza";
                colorTag = new Color(180, 100, 255);
                break;

            case TIPO_POCION_VELOCIDAD:
                dibujarBotella(g2, drawX, drawY, new Color(0, 255, 255));
                nombreTag = "Poción Velocidad";
                colorTag = Color.CYAN;
                break;
        }

        if (mostrarTag && !nombreTag.isEmpty()) {
            renderizarUIProximidad(g2, nombreTag, colorTag, drawX, drawY);
        }
    }

    private void renderizarFallbackMoneda(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(x, y, 16, 16);
        g2.setColor(Color.BLACK);
        g2.drawOval(x, y, 16, 16);
    }

    private void renderizarUIProximidad(Graphics2D g2, String texto, Color color, int x, int y) {
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        int anchoTexto = g2.getFontMetrics().stringWidth(texto);
        int tagX = x + 10 - (anchoTexto / 2);
        int tagY = y - 10;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(tagX - 4, tagY - 11, anchoTexto + 8, 14, 5, 5);
        g2.setColor(color);
        g2.drawString(texto, tagX, tagY);
    }

    private void dibujarBotella(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(x + 4, y, 12, 18, 5, 5);
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(x + 7, y - 4, 6, 4);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x + 4, y, 12, 18, 5, 5);
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillRect(x + 6, y + 4, 2, 8);
    }

    // --- Getters y Setters ---
    public Rectangle getHitbox() { return hitbox; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean b) { this.activo = b; }
    public int getTipo() { return tipo; }
    public void setMostrarTag(boolean b) { this.mostrarTag = b; }
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
}