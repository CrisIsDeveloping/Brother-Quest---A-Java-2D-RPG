<<<<<<< HEAD
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
    protected int rangoVision = 500;
    
    // Efecto parpadeo al spawnear (2 segundos = 120 frames en 60fps)
    protected int spawnFlashTicks = 120; // dura ~120 frames parpadeando
    
    // Límites de cámara (se actualizan cada frame desde GamePanel)
    protected int limCameraIzq = Integer.MIN_VALUE;
    protected int limCameraRight = Integer.MAX_VALUE;

    // Mecánica de Delayed Attack
    protected boolean isDelayedAttack = false;
    protected int delayTimer = 0;
    protected int initialDelay = 0;

    protected final int LIMITE_ARRIBA = 470;
    protected final int LIMITE_ABAJO = 610;

    // Stats básicos que van subiendo según la dificultad o el nivel
    protected int vidaMax;
    protected int vida;
    protected int dano;

    // Para ajustar la sombra a mano si hace falta y no queda bien centrada
    protected int sombraOffsetX = 0;   // Mueve la sombra a la izquierda/derecha
    protected int sombraOffsetY = -10; // Sube o baja la sombra
    protected int sombraAncho = -1;    // Si le ponemos -1, usa el ancho normal de la caja de colisión
    protected int sombraAlto = 20;     // Altura (grosor) del óvalo de la sombra

    public EnemigoBase(float x, float y) {
        super();
        this.x = x;
        this.y = y;
    }

    // --- Soporte para la nueva Cinemática de Marcha ---
    protected int cinematicTargetX = 0;
    protected boolean llegoMetaCinematica = false;
    
    public abstract void iniciarMarchaCinematica(int metaX);
    public abstract void actualizarMarchaCinematica();
    
    public boolean isLlegoMetaCinematica() { return llegoMetaCinematica; }

    public abstract void actualizarIA(Jugador jugador);
    public abstract void dibujar(Graphics2D g2, int cameraX);
    public abstract Rectangle getAttackBox();
    public abstract boolean isAnimacionMuerteTerminada();

    // Dibujamos la barra de vida chiquita encima de la cabeza del enemigo
    public void dibujarHUD(Graphics2D g2, int cameraX) {
        if (muerto || vida <= 0 || hitbox == null) return;

        int barW = 50;
        int barH = 6;

        // Centramos la barrita justo arriba de su hitbox
        int barX = hitbox.x - cameraX + (hitbox.width / 2) - (barW / 2);
        int barY = hitbox.y - 20;

        // Fondo negrito de la barra
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

        // La barra roja que queda de fondo al perder vida
        g2.setColor(Color.RED);
        g2.fillRect(barX, barY, barW, barH);

        // La barra verde con la vida que le queda actualmente
        double porcentajeVida = (double) vida / vidaMax;
        if (porcentajeVida < 0) porcentajeVida = 0; // Para que no se dibuje vida en negativo si le pegan muy duro
        g2.setColor(Color.GREEN);
        g2.fillRect(barX, barY, (int)(barW * porcentajeVida), barH);

        // Texto con el nivel del bicho
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        String txt = "Lv." + nivel;
        int txtW = g2.getFontMetrics().stringWidth(txt);

        // Le ponemos sombrita al texto para que resalte y se lea mejor
        g2.setColor(Color.BLACK);
        g2.drawString(txt, barX + (barW/2) - (txtW/2) + 1, barY - 5 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(txt, barX + (barW/2) - (txtW/2), barY - 5);
    }

    public CajaColision getHitbox() { return hitbox; }

    /** Actualiza los límites de la jaula de cámara para este enemigo */
    public void setLimitesCamara(int izq, int der) {
        this.limCameraIzq = izq;
        this.limCameraRight = der;
    }

    /**
     * Clampea la posición X directamente a los límites dados.
     * Se usa desde GamePanel después de actualizar, para garantizar que
     * ninguna física interna (this.x +=) pueda sacar al enemigo de la pantalla.
     */
    public void clampearEnPantalla(int izq, int der) {
        if (this.x < izq) {
            this.x = izq;
            if (this.hitbox != null) this.hitbox.x = izq;
        }
        if (this.x > der) {
            this.x = der;
            if (this.hitbox != null) this.hitbox.x = der;
        }
    }

    public void setX(int x) {
        this.x = x;
        if (this.hitbox != null) this.hitbox.x = x;
    }

    public void establecerNivel(int nuevoNivel) {
        this.nivel = nuevoNivel;

        // Escalar las stats multiplicando su valor base por un factor del nivel
        // Cada nivel sube un 25% extra de sus atributos base (Nivel 1 es 100%)
        float multiplicador = 1.0f + ((nuevoNivel - 1) * 0.25f);

        this.vidaMax = (int)(this.vidaMax * multiplicador);
        if (this.vidaMax < 1) this.vidaMax = 1; // Seguridad
        this.vida = this.vidaMax;

        this.dano = (int)(this.dano * multiplicador);
        this.xpQueDa = (int)(this.xpQueDa * multiplicador);
    }


    // Dibujamos la sombra. Cambia de tamaño según la hitbox del enemigo.
    public void dibujarSombra(Graphics g, int cameraX) {
        if (!muerto && hitbox != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 100)); // Color de la sombra

            // Si dejamos el ancho en -1, copiamos el de la hitbox. Si no, usamos el que le pasamos manual.
            int anchoSombraFinal = (sombraAncho == -1) ? hitbox.width : sombraAncho;

            // Sumamos los ajustes manuales por si la sombra queda chueca
            int sombraX = hitbox.x - cameraX + sombraOffsetX;
            int sombraY = hitbox.y + hitbox.height + sombraOffsetY;

            g2.fillOval(sombraX, sombraY, anchoSombraFinal, sombraAlto);
        }
    }

    protected void aplicarLimitesCarril() {
        if (hitbox == null) return;

        // Sacamos dónde están los pies del enemigo para que no se salga de la pista
        float piesY = this.y + hitbox.height;
        
        int limiteArriba = GamePanel.getInstancia().getLimiteArribaGlobal();
        int limiteAbajo = GamePanel.getInstancia().getLimiteAbajoGlobal();

        // Chequeamos que no se pase por arriba de la calle
        if (piesY < limiteArriba) {
            // Lo empujamos para abajo si se pasó
            this.y = limiteArriba - hitbox.height;
        }
        // Lo mismo pero por abajo
        else if (piesY > limiteAbajo) {
            this.y = limiteAbajo - hitbox.height;
        }

        // Actualizamos la hitbox para que coincida con la nueva posición obligada
        hitbox.y = (int) this.y;
    }

    public void actualizarInvulnerabilidad() {
        if (tiempoInvulnerable > 0) {
            tiempoInvulnerable--;
        }
        if (spawnFlashTicks > 0) {
            spawnFlashTicks--;
        }
    }

    public boolean puedeRecibirDano() {
        return tiempoInvulnerable <= 0 && spawnFlashTicks <= 0;
    }

    public boolean isSpawning() {
        return spawnFlashTicks > 0;
    }

    public boolean isVulnerablePorDelay() {
        return delayTimer > 0;
    }

    public void darInvulnerabilidad(int frames) {
        this.tiempoInvulnerable = frames;
    }

    /**
     * Devuelve el alpha que debe usarse para el parpadeo de spawn.
     * Durante los primeros 40 frames alterna entre 255 y 80 cada 5 frames.
     * Cuando spawnFlashTicks llega a 0 devuelve 255 (visible normal).
     */
    public float getSpawnAlpha() {
        if (spawnFlashTicks <= 0) return 1.0f;
        // Parpadea rápido: visible 5 frames, semitransparente 5 frames
        return (spawnFlashTicks % 10 < 5) ? 1.0f : 0.25f;
    }

    // Getters y Setters de toda la vida
    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }
    public int getXpQueDa() { return xpQueDa; }
    public int getValorOro() { return valorOro; }
    public boolean isMuerto() { return muerto; }
    public boolean isLootSoltado() { return lootSoltado; }
    public void setLootSoltado(boolean b) { this.lootSoltado = b; }
    public int getEstadoActual() { return estadoActual; }
    public int getAniIndex() { return aniIndex; }

    // Más getters y setters para la vida y daño
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
=======
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
>>>>>>> da25f6dd6bf3c69498f22ffaa92c786d38130149
}