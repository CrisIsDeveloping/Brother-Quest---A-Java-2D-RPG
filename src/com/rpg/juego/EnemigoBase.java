package com.rpg.juego;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.*;

public abstract class EnemigoBase extends Entidad {
    protected int nivel = 1;
    protected int xpQueDa;
    protected int valorOro;
    protected boolean muerto = false;
    protected int estadoActual;
    protected int aniTick, aniIndex, aniSpeed;
    protected boolean mirandoIzquierda = false;
    protected boolean lootSoltado = false;
    protected int tiempoInvulnerable = 0;

    protected final int LIMITE_ARRIBA = 470;
    protected final int LIMITE_ABAJO = 610;

    // --- Atributos base para el sistema de escalado de dificultad ---
    protected int vidaMax;
    protected int vida;
    protected int dano;

    // Mover sombra manualmente
    protected int sombraOffsetX = 0;   // Mueve la sombra a la izquierda/derecha
    protected int sombraOffsetY = -10; // Sube o baja la sombra
    protected int sombraAncho = -1;    // -1 significa "usar el ancho de la hitbox por defecto"
    protected int sombraAlto = 20;     // Altura (grosor) del óvalo de la sombra

    public EnemigoBase(float x, float y) {
        super();
        this.x = x;
        this.y = y;
    }

    public abstract void actualizarIA(Jugador jugador);
    public abstract void dibujar(Graphics2D g2, int cameraX);
    public abstract Rectangle getAttackBox();

    // --- Renderizado de interfaz de usuario (HUD) sobre la entidad ---
    public void dibujarHUD(Graphics2D g2, int cameraX) {
        if (muerto || vida <= 0 || hitbox == null) return;

        int barW = 50;
        int barH = 6;

        // Calculo de posicionamiento centrado respecto a la caja de colision real
        int barX = hitbox.x - cameraX + (hitbox.width / 2) - (barW / 2);
        int barY = hitbox.y - 20;

        // Renderizado de fondo oscuro
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

        // Renderizado de barra base (Color de fondo para daño)
        g2.setColor(Color.RED);
        g2.fillRect(barX, barY, barW, barH);

        // Renderizado de vitalidad proporcional
        double porcentajeVida = (double) vida / vidaMax;
        if (porcentajeVida < 0) porcentajeVida = 0; // Prevencion de desbordamiento visual negativo
        g2.setColor(Color.GREEN);
        g2.fillRect(barX, barY, (int)(barW * porcentajeVida), barH);

        // Renderizado de identificador de nivel
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        String txt = "Lv." + nivel;
        int txtW = g2.getFontMetrics().stringWidth(txt);

        // Sombreado de texto para contraste visual
        g2.setColor(Color.BLACK);
        g2.drawString(txt, barX + (barW/2) - (txtW/2) + 1, barY - 5 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(txt, barX + (barW/2) - (txtW/2), barY - 5);
    }

    public Rectangle getHitbox() { return hitbox; }

    public void setX(int x) {
        this.x = x;
        if(this.hitbox != null) this.hitbox.x = x;
    }

    public void establecerNivel(int nuevoNivel) {
        this.nivel = nuevoNivel;

        // --- Escalado matematico de estadisticas operativas ---

        // Formula de progresion de vitalidad base
        this.vidaMax = 60 + (nuevoNivel * 40);
        this.vida = this.vidaMax;

        // Formula de progresion de dano ofensivo
        this.dano = 10 + (nuevoNivel * 5);

        // Formula de progresion de recompensa de experiencia
        this.xpQueDa = 10 + (nuevoNivel * 20);
    }


    // Metodo base de sombreado. Escala dinamicamente con la Hitbox.
    public void dibujarSombra(Graphics g, int cameraX) {
        if (!muerto && hitbox != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 100)); // Color de la sombra

            // Si sombraAncho es -1, usamos el de la hitbox. Si no, usamos el valor manual.
            int anchoSombraFinal = (sombraAncho == -1) ? hitbox.width : sombraAncho;

            // Calculamos X e Y sumando los Offsets manuales
            int sombraX = hitbox.x - cameraX + sombraOffsetX;
            int sombraY = hitbox.y + hitbox.height + sombraOffsetY;

            g2.fillOval(sombraX, sombraY, anchoSombraFinal, sombraAlto);
        }
    }

    protected void aplicarLimitesCarril() {
        if (hitbox == null) return;

        // Identificacion del limite inferior de la entidad (Pies)
        float piesY = this.y + hitbox.height;

        // Evaluacion de limite superior del carril de navegacion
        if (piesY < LIMITE_ARRIBA) {
            // Correccion de posicion en eje Y respetando el margen de colision
            this.y = LIMITE_ARRIBA - hitbox.height;
        }
        // Evaluacion de limite inferior del carril de navegacion
        else if (piesY > LIMITE_ABAJO) {
            this.y = LIMITE_ABAJO - hitbox.height;
        }

        // Sincronizacion de caja de colision tras aplicar limites fisicos
        hitbox.y = (int) this.y;
    }

    public void actualizarInvulnerabilidad() {
        if (tiempoInvulnerable > 0) {
            tiempoInvulnerable--;
        }
    }

    public boolean puedeRecibirDano() {
        return tiempoInvulnerable <= 0;
    }

    public void darInvulnerabilidad(int frames) {
        this.tiempoInvulnerable = frames;
    }

    // --- Metodos de encapsulamiento ---
    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }
    public int getXpQueDa() { return xpQueDa; }
    public int getValorOro() { return valorOro; }
    public boolean isMuerto() { return muerto; }
    public boolean isLootSoltado() { return lootSoltado; }
    public void setLootSoltado(boolean b) { this.lootSoltado = b; }
    public int getEstadoActual() { return estadoActual; }
    public int getAniIndex() { return aniIndex; }

    // Accesores para logica externa y gestor de estado
    public int getVida() { return vida; }
    public void setVida(int vida) { this.vida = vida; }
    public int getVidaMax() { return vidaMax; }
    public void setVidaMax(int vidaMax) { this.vidaMax = vidaMax; }
    public int getDano() { return dano; }
    public void setDano(int dano) { this.dano = dano; }

    public boolean puedeAtacar() { return !muerto; }
    public void reiniciarCooldown() {}

    public boolean isAtacando() {
        return false;
    }
}