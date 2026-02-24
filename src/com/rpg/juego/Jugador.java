package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.Timer;

public class Jugador extends Entidad {

    // --- Parametros de renderizado y posicionamiento espacial ---
    private final int ANCHO_VISUAL = 240;
    private final int ALTO_VISUAL = 240;
    private int xDrawOffset = 100;
    private int yDrawOffset = 169;

    // --- Dimensiones y offsets de la caja de colision (Bounding Box) ---
    private final int anchoHitbox = 40;
    private final int altoHitbox = 70;
    private final int offsetHitboxY = 60;

    // --- Constantes fisicas y estado de la mecanica de salto ---
    private double z = 0;
    private double velZ = 0;
    private boolean enAire = false;
    private final double GRAVEDAD = 0.5;
    private final double FUERZA_SALTO = 10.0;
    private final int COSTO_STAMINA_SALTO = 20;
    private int cooldownHabilidad1 = 0;

    // --- Parametros de la mecanica de evasion (Roll) ---
    private boolean rodando = false;
    private float velocidadRodar = 6.5f;
    private final int COSTO_STAMINA_ROLL = 25;

    // --- Gestion de inventario y modificadores de estado temporales (Buffs) ---
    private int pocionesVida = 0;
    private int pocionesFuerza = 0;
    private int tiempoFuerza = 0;
    private int pocionesVelocidad = 0;
    private boolean velocidadActiva = false;
    private float velocidad = 4.0f;
    private int pocionSeleccionada = 0;

    private long ultimoUsoVida = 0, ultimoUsoFuerza = 0, ultimoUsoVelocidad = 0;
    private final long COOLDOWN_TIEMPO = 10000;

    // --- Variables de gestion de recursos de combate ---
    private float stamina = 100, maxStamina = 100;
    private boolean escudoRoto = false;
    private long tiempoSinCorrer = 0;

    private Color colorDestello;
    private float alfaDestello = 0f;

    private int aniTick, aniIndex, aniSpeed = 6;
    private int oro = 0;

    // --- Constantes de la maquina de estados de animacion ---
    private static final int IDLE = 0;
    private static final int RUN = 1;
    private static final int ATTACK = 4;
    private static final int HIT = 5;
    private static final int DEAD = 6;
    private static final int BLOCK = 7;
    private static final int ROLL = 8;
    private static final int MAGIC_ATTACK = 9;

    private int playerAction = IDLE;
    private boolean moving = false;
    private boolean mirandoDerecha = true;

    private final int LIMITE_ARRIBA = 470;
    private final int LIMITE_ABAJO = 610;

    private boolean atacando = false;
    private boolean defendiendo = false;
    private boolean inicioAtaque = false;

    private int anchoAtaque = 50, altoAtaque = 50, offsetAtaqueY = 10;
    private int cargaHabilidad = 300;
    private final int MAX_CARGA = 300;
    private boolean lanzandoPoder = false;

    public enum Estado { VIVO, MUERTO }
    private Estado estado = Estado.VIVO;

    private int velX = 5, velY = 4, nivel = 1, xp = 0, xpParaSiguienteNivel = 100;

    public Jugador() {
        super();
        this.x = 100;
        this.y = 490;
        this.hitbox = new Rectangle(0, 0, anchoHitbox, altoHitbox);
        this.vidaMax = 100;
        this.vida = 100;
        this.dano = 25;
    }

    // --- Logica de cinematica y resolucion de movimiento bidimensional ---
    public void mover(boolean w, boolean s, boolean a, boolean d) {
        if (estado == Estado.MUERTO || rodando) return;

        moving = false;
        // Modificador de friccion/penalizacion de velocidad segun accion activa
        float factorVelocidad = defendiendo ? 0.5f : (atacando ? 0.8f : 1.0f);

        int vx = (int)(velocidad * factorVelocidad);
        int vy = (int)(velocidad * factorVelocidad);

        if (w) { y -= vy; moving = true; }
        if (s) { y += vy; moving = true; }
        if (a) { x -= vx; moving = true; mirandoDerecha = false; }
        if (d) { x += vx; moving = true; mirandoDerecha = true; }

        if (y < LIMITE_ARRIBA) y = LIMITE_ARRIBA;
        if (y > LIMITE_ABAJO) y = LIMITE_ABAJO;
        if (x < 0) x = 0;
    }

    public void moverAutomatico(boolean w, boolean s, boolean a, boolean d) {
        mover(w, s, a, d);
        if (a || d) {
            this.moving = true;
            if (d) this.mirandoDerecha = true;
            if (a) this.mirandoDerecha = false;
        } else {
            this.moving = false;
        }
    }

    public void pararMovimiento() {
        mover(false, false, false, false);
        this.moving = false;
        this.aniIndex = 0;
    }

    public void saltar() {
        // Validacion cruzada de estados para autorizacion de impulso vertical
        if (!enAire && !rodando && estado != Estado.MUERTO && !atacando && !defendiendo) {
            if (stamina >= COSTO_STAMINA_SALTO) {
                stamina -= COSTO_STAMINA_SALTO;
                velZ = FUERZA_SALTO;
                enAire = true;
                tiempoSinCorrer = System.currentTimeMillis();
            }
        }
    }

    public void rodar() {
        if (atacando || lanzandoPoder || defendiendo) return;

        if (!rodando && !enAire && stamina >= COSTO_STAMINA_ROLL && estado == Estado.VIVO) {
            stamina -= COSTO_STAMINA_ROLL;
            rodando = true;
            aniIndex = 0;
            aniTick = 0;
            tiempoSinCorrer = System.currentTimeMillis();
        }
    }

    @Override
    public void actualizar() {
        hitbox.setLocation((int)x, (int)(y - z - offsetHitboxY));
        actualizarAnimacionTick();
        setAnimacion();

        // Resolucion de integracion fisica para caida libre
        if (enAire) {
            z += velZ;
            velZ -= GRAVEDAD;
            if (z <= 0) { z = 0; velZ = 0; enAire = false; }
        }

        if (rodando) {
            float direccion = mirandoDerecha ? velocidadRodar : -velocidadRodar;
            x += direccion;
            if (x < 0) x = 0;
        }

        actualizarStamina();
        actualizarBuffs();

        if (alfaDestello > 0) {
            alfaDestello -= 0.05f;
            if (alfaDestello < 0) alfaDestello = 0f;
        }

        if (cooldownHabilidad1 > 0) {
            cooldownHabilidad1--;
        }
    }

    private void actualizarStamina() {
        long tiempoActual = System.currentTimeMillis();
        if (defendiendo) {
            stamina -= 0.5f;
            tiempoSinCorrer = tiempoActual;
            // Evaluacion de agotamiento de escudo
            if (stamina <= 0) { stamina = 0; defendiendo = false; escudoRoto = true; }
        } else {
            if (escudoRoto) {
                if (tiempoActual - tiempoSinCorrer > 1500) stamina += 0.5f;
            } else if (stamina < maxStamina && (tiempoActual - tiempoSinCorrer > 500)) {
                stamina += 0.3f;
            }
            // Restauracion de guardia tras periodo de recuperacion
            if (stamina > 30) escudoRoto = false;
        }
    }

    public void actualizarBuffs() {
        if (tiempoFuerza > 0) tiempoFuerza--;
    }

    private void actualizarAnimacionTick() {
        aniTick++;
        int velocidadAnimacionActual = (playerAction == ROLL) ? 3 : aniSpeed;

        if (aniTick >= velocidadAnimacionActual) {
            aniTick = 0;
            aniIndex++;
            int maxFrames = getSpriteAmount(playerAction);

            // Logica especifica para mantener el frame estatico en accion de bloqueo
            if (playerAction == BLOCK) {
                if (aniIndex >= maxFrames) aniIndex = maxFrames - 1;
            } else if (aniIndex >= maxFrames) {
                aniIndex = 0;
                // Transicion a estado de reposo post-accion
                if (playerAction == ROLL) rodando = false;
                else if (playerAction == ATTACK) atacando = false;
                else if (playerAction == MAGIC_ATTACK) lanzandoPoder = false;
            }
        }
    }

    private void setAnimacion() {
        int startAni = playerAction;

        // Asignacion jerarquica de estados de animacion
        if (estado == Estado.MUERTO) playerAction = DEAD;
        else if (rodando) playerAction = ROLL;
        else if (lanzandoPoder) playerAction = MAGIC_ATTACK;
        else if (atacando) playerAction = ATTACK;
        else if (defendiendo) playerAction = BLOCK;
        else if (moving) playerAction = RUN;
        else playerAction = IDLE;

        if (startAni != playerAction) {
            aniTick = 0;
            aniIndex = 0;
            if (playerAction == ATTACK || playerAction == MAGIC_ATTACK) inicioAtaque = true;
        }
    }

    public void recibirGolpe(int d) {
        if (rodando || estado == Estado.MUERTO) return;
        if(!defendiendo) {
            vida -= d;
            if(vida <= 0) { vida = 0; estado = Estado.MUERTO; aniIndex = 0; }
        }
    }

    // --- Metodos accesores (Getters) ---
    public Rectangle getHitbox() { return hitbox; }
    public int getAniIndex() { return aniIndex; }
    public boolean isMirandoDerecha() { return mirandoDerecha; }
    public boolean isAtacando() { return atacando; }
    public boolean isDefendiendo() { return defendiendo; }
    public boolean isEscudoRoto() { return escudoRoto; }
    public boolean isLanzandoPoder() { return lanzandoPoder; }
    public boolean isRodando() { return rodando; }
    public Estado getEstado() { return estado; }
    public int getNivel() { return nivel; }
    public int getXp() { return xp; }
    public int getXpParaSiguienteNivel() { return xpParaSiguienteNivel; }
    public int getOro() { return oro; }
    public int getCargaHabilidad() { return cargaHabilidad; }
    public int getMaxCarga() { return MAX_CARGA; }
    public int getPocionSeleccionada() { return pocionSeleccionada; }
    public int getPocionesVida() { return pocionesVida; }
    public int getPocionesFuerza() { return pocionesFuerza; }
    public int getPocionesVelocidad() { return pocionesVelocidad; }
    public long getUltimoUsoVida() { return ultimoUsoVida; }
    public long getUltimoUsoFuerza() { return ultimoUsoFuerza; }
    public long getUltimoUsoVelocidad() { return ultimoUsoVelocidad; }
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public double getZ() { return z; }

    public BufferedImage getSpriteActual() {
        if (GestorRecursos.animacionesJugador != null) {
            BufferedImage img = GestorRecursos.animacionesJugador[playerAction][aniIndex];
            if (img != null) {
                return img;
            } else {
                return GestorRecursos.animacionesJugador[1][aniIndex % getSpriteAmount(1)];
            }
        }
        return null;
    }

    // --- Metodos mutadores controlados (Setters) ---
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setLanzandoPoder(boolean b) { this.lanzandoPoder = b; }

    public void setAtacando(boolean b) {
        // Prevencion de cancelacion de animacion prioritaria
        if (b && (defendiendo || rodando)) return;

        if (b && !this.atacando) this.inicioAtaque = true;
        this.atacando = b;
    }

    public void setDefendiendo(boolean b) {
        // Validacion de estado mutuo excluyente para acciones de combate
        if (b && (atacando || lanzandoPoder)) return;

        if (!escudoRoto) this.defendiendo = b;
    }

    // --- Resolucion de logica de interaccion ---
    public void recogerObjeto(int tipo) {
        switch (tipo) {
            case 0: this.oro += 15; break;
            case 1: this.pocionesVida++; break;
            case 2: this.pocionesFuerza++; break;
            case 3: this.pocionesVelocidad++; break;
        }
    }

    @Override
    public int getDano() {
        int danoCalculado = this.dano;
        // Aplicacion de multiplicador escalar por modificador activo
        if (tiempoFuerza > 0) {
            danoCalculado *= 2;
        }
        return danoCalculado;
    }

    public void ganarCarga(int cantidad) { cargaHabilidad = Math.min(MAX_CARGA, cargaHabilidad + cantidad); }

    public void ganarXP(int c) {
        xp += c;
        if(xp >= xpParaSiguienteNivel) {
            nivel++; xp=0; vida=vidaMax; dano+=5; xpParaSiguienteNivel*=1.5;
        }
    }

    public void cambiarPocion(int dir) { pocionSeleccionada = (pocionSeleccionada + dir + 3) % 3; }

    public void usarPocionSeleccionada() {
        if (pocionSeleccionada == 0) usarPocionVida();
        else if (pocionSeleccionada == 1) usarPocionFuerza();
        else if (pocionSeleccionada == 2) usarPocionVelocidad();
    }

    public void usarPocionVida() {
        if (pocionesVida > 0 && vida < vidaMax && (System.currentTimeMillis() - ultimoUsoVida) >= COOLDOWN_TIEMPO) {
            pocionesVida--; vida = Math.min(vidaMax, vida + 50); dispararDestello(Color.GREEN); ultimoUsoVida = System.currentTimeMillis();
        }
    }

    public void usarPocionFuerza() {
        if (pocionesFuerza > 0 && (System.currentTimeMillis() - ultimoUsoFuerza) >= COOLDOWN_TIEMPO) {
            pocionesFuerza--; tiempoFuerza = 600; ultimoUsoFuerza = System.currentTimeMillis(); dispararDestello(new Color(180, 50, 255));
        }
    }

    public void usarPocionVelocidad() {
        if (pocionesVelocidad > 0 && !velocidadActiva && (System.currentTimeMillis() - ultimoUsoVelocidad) >= COOLDOWN_TIEMPO) {
            pocionesVelocidad--; velocidadActiva = true; ultimoUsoVelocidad = System.currentTimeMillis(); dispararDestello(Color.CYAN);
            float vOrig = 4.0f; this.velocidad = vOrig * 1.7f;
            Timer t = new Timer(5000, e -> { this.velocidad = vOrig; this.velocidadActiva = false; });
            t.setRepeats(false); t.start();
        }
    }

    public boolean usarHabilidad(int costoBarras) {
        int costo = costoBarras * 100;
        if (cargaHabilidad >= costo) { cargaHabilidad -= costo; lanzandoPoder = true; return true; }
        return false;
    }

    public void dispararDestello(Color color) { this.colorDestello = color; this.alfaDestello = 0.8f; }

    public int getSpriteAmount(int action) {
        switch (action) {
            case IDLE: return 5;
            case RUN: return 8;
            case ATTACK: return 6;
            case HIT: return 1;
            case DEAD: return 7;
            case BLOCK: return 2;
            case MAGIC_ATTACK: return 6;
            case ROLL: return 8;
            default: return 1;
        }
    }

    public Rectangle getAttackBox() {
        // Evaluacion del "Active Frame" correspondiente al impacto logico de la animacion
        boolean esFrameDeGolpe = (aniIndex == 2);

        if ((atacando || lanzandoPoder) && esFrameDeGolpe) {
            // Proyeccion del vector ofensivo segun orientacion de la entidad
            int xAtaque = mirandoDerecha ? hitbox.x + hitbox.width : hitbox.x - anchoAtaque;
            return new Rectangle(xAtaque, hitbox.y + offsetAtaqueY, anchoAtaque, altoAtaque);
        }

        // Retorno nulo para evitar colisiones fantasma en frames pasivos
        return null;
    }

    @Override
    public void dibujarSombra(Graphics g, int cameraX) {
        // Sombreado dinamico especializado para el jugador
        if (estado != Estado.MUERTO) {
            g.setColor(new Color(0, 0, 0, 100));
            // Usamos la variable 'y' base que NO se ve afectada por 'z' (el salto)
            g.fillOval((int)(x - cameraX - 11), (int)(y), 60, 20);
        }
    }

    public void dibujar(Graphics g, int cameraX) {

        int xPantalla = (int) (x - xDrawOffset) - cameraX;
        int yPantalla = (int) (y - yDrawOffset - z);

        BufferedImage img = getSpriteActual();
        if (img != null) {
            if (mirandoDerecha) g.drawImage(img, xPantalla, yPantalla, ANCHO_VISUAL, ALTO_VISUAL, null);
            else g.drawImage(img, xPantalla + ANCHO_VISUAL, yPantalla, -ANCHO_VISUAL, ALTO_VISUAL, null);
        }

        // Composicion de efectos visuales (VFX) con gradientes radiales
        if (alfaDestello > 0 && colorDestello != null) {
            Graphics2D g2 = (Graphics2D) g;
            int radio = 80;
            int cX = (int) (x - cameraX) + 20;
            int cY = (int) (y - z) - 30;
            Composite originalComposite = g2.getComposite();
            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfaDestello));
                float[] dist = {0.0f, 0.5f, 1.0f};
                Color cPrincipal = colorDestello;
                Color cTransparente = new Color(cPrincipal.getRed(), cPrincipal.getGreen(), cPrincipal.getBlue(), 0);
                Color[] colors = {Color.WHITE, cPrincipal, cTransparente};

                RadialGradientPaint p = new RadialGradientPaint(cX, cY, radio, dist, colors);
                g2.setPaint(p);
                g2.fillOval(cX - radio, cY - radio, radio * 2, radio * 2);

            } catch (Exception e) {
            } finally {
                g2.setComposite(originalComposite);
            }
        }

        // Modulo de visualizacion de estructuras logicas para depuracion
        if (GamePanel.debugActivado) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLUE);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
            Rectangle ab = getAttackBox();
            if (ab != null) { g2.setColor(Color.YELLOW); g2.drawRect(ab.x - cameraX, ab.y, ab.width, ab.height); }
        }
    }
    public boolean habilidad1Lista() {
        return cooldownHabilidad1 <= 0;
    }

    public void usarCooldownHabilidad1() {
        this.cooldownHabilidad1 = 60; // Periodo de enfriamiento predeterminado
    }
}