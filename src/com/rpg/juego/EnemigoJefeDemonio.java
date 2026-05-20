package com.rpg.juego;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class EnemigoJefeDemonio extends EnemigoBase {

    
    
    
    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int CLEAVE = 2;
    public static final int TAKE_HIT = 3;
    public static final int DEATH = 4;

    private static final int[] FRAME_COUNT = { 6, 12, 15, 5, 22 };

    
    private boolean activo = false;
    private int cooldownAtaque = 0;
    private static final int TIEMPO_ENTRE_GOLPES = 45;

    
    
    private int cooldownTakeHit = 0;
    private static final int TIEMPO_ENTRE_TAKE_HIT = 240;

    private boolean golpeRegistradoBoss = false;

    
    private static final int RANGO_VISION = 1000; 
    private static final int RANGO_PARAR = 150; 
    private static final int RANGO_ATAQUE_X = 150; 
    private static final int RANGO_ATAQUE_Y = 80; 
    private static final float VELOCIDAD = 1.8f;

    
    private static final int DRAW_W = 576; 
    private static final int DRAW_H = 320; 
    
    
    private static final int HBOX_OFFSET_X = 15;

    
    public EnemigoJefeDemonio(int x, int y) {
        super(x, y);
        this.vidaMax = 2500;
        this.vida = 2500;
        this.dano = 44; 
        
        this.hitbox = new CajaColision(x + HBOX_OFFSET_X, y, 100, 220);
        this.estadoActual = IDLE;
        this.aniSpeed = 6;
        
        this.mirandoIzquierda = true;
        this.sombraAncho = 0;
    }

    private boolean enFase2 = false;
    private int tiempoMuerto = 0;

    
    public void setActivo(boolean a) {
        this.activo = a;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isGolpeRegistradoBoss() {
        return golpeRegistradoBoss;
    }

    public void setGolpeRegistradoBoss(boolean b) {
        this.golpeRegistradoBoss = b;
    }

    
    @Override
    public void actualizarIA(Jugador jugador) {
        if (!activo)
            return;
        if (estadoActual == DEATH || estadoActual == TAKE_HIT)
            return;

        double centroJugX = jugador.getBounds().x + jugador.getBounds().width / 2.0;
        double centroEneX = hitbox.x + hitbox.width / 2.0;
        double distX = Math.abs(centroJugX - centroEneX);

        if (distX > 20) {
            if (estadoActual != CLEAVE) {
                mirandoIzquierda = (centroJugX < centroEneX);
            } else if (aniIndex <= 11) {
                // Permite girar hacia el jugador hasta el último frame activo del ataque
                mirandoIzquierda = (centroJugX < centroEneX);
            }
        }

        if (estadoActual == CLEAVE)
            return; 

        if (cooldownAtaque > 0)
            cooldownAtaque--;

        boolean fase2 = this.vida <= (this.vidaMax / 2);
        float velActual = fase2 ? VELOCIDAD * 2.0f : VELOCIDAD;

        
        double piesEnemY = hitbox.y + hitbox.height;
        double piesJugY = jugador.getBounds().y + jugador.getBounds().height;

        double distY = Math.abs(piesJugY - piesEnemY);

        
        if (distX <= RANGO_ATAQUE_X && distY <= RANGO_ATAQUE_Y && cooldownAtaque <= 0) {
            estadoActual = CLEAVE;
            aniIndex = 0;
            aniTick = 0;
            golpeRegistradoBoss = false;
            GestorSonidos.reproducir(GestorSonidos.BOSS_ATTACK);

            
            if (Math.random() < 0.35) {
                isDelayedAttack = true;
                delayTimer = 40 + (int) (Math.random() * 50);
                initialDelay = delayTimer;
            } else {
                isDelayedAttack = false;
            }

            
        } else if (distX <= RANGO_PARAR) {
            estadoActual = IDLE;
            
            if (distY > RANGO_ATAQUE_Y) {
                if (piesJugY < piesEnemY)
                    this.y -= velActual * 0.4;
                else
                    this.y += velActual * 0.4;
                hitbox.actualizar((int) x + HBOX_OFFSET_X, (int) y);
            }

            
        } else if (distX < RANGO_VISION) {
            estadoActual = WALK;
            
            if (distX > RANGO_PARAR) {
                if (centroJugX < centroEneX)
                    this.x -= velActual;
                else
                    this.x += velActual;
            }
            
            if (distY > RANGO_ATAQUE_Y) {
                if (piesJugY < piesEnemY)
                    this.y -= velActual * 0.6;
                else
                    this.y += velActual * 0.6;
            }
            hitbox.actualizar((int) x + HBOX_OFFSET_X, (int) y);

        } else {
            estadoActual = IDLE;
        }
    }

    
    @Override
    public void actualizar() {
        
        actualizarInvulnerabilidad();
        if (cooldownTakeHit > 0)
            cooldownTakeHit--;

        
        if (estadoActual != CLEAVE || aniIndex < 9 || aniIndex > 11) {
            golpeRegistradoBoss = false;
        }

        
        if (estadoActual == CLEAVE && aniIndex == 8 && isDelayedAttack && delayTimer > 0) {
            if (delayTimer == initialDelay) {
                GestorSonidos.reproducir(GestorSonidos.CARGA_ATAQUE);
            }
            delayTimer--;
            return;
        }

        aniTick++;
        int vel = aniSpeed;
        if (estadoActual == IDLE) vel = 7;
        else if (estadoActual == DEATH) vel = 5;

        if (aniTick >= vel) {
            aniTick = 0;
            aniIndex++;

            int frames = FRAME_COUNT[estadoActual];
            if (aniIndex >= frames) {
                switch (estadoActual) {
                    case DEATH:
                        aniIndex = frames - 1;
                        tiempoMuerto++;
                        break;
                    case CLEAVE:
                        estadoActual = IDLE;
                        boolean fase2 = this.vida <= (this.vidaMax / 2);
                        cooldownAtaque = fase2 ? TIEMPO_ENTRE_GOLPES / 2 : TIEMPO_ENTRE_GOLPES;
                        isDelayedAttack = false;
                        break;
                    case TAKE_HIT:
                        estadoActual = IDLE;
                        break;
                    default:
                        aniIndex = 0;
                }
            }
        }

        hitbox.actualizar((int) x + HBOX_OFFSET_X, (int) y);
    }

    
    @Override
    public void dibujarHUD(Graphics2D g2, int cameraX) {
        
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto)
            return;
        
        
        if (tiempoInvulnerable > 0)
            return;
        tiempoInvulnerable = 8;

        this.vida -= cantidad;

        if (this.vida <= 0) {
            this.vida = 0;
            muerto = true;
            estadoActual = DEATH;
            aniIndex = 0;
            aniTick = 0;
            GestorSonidos.reproducir(GestorSonidos.BOSS_DEATH);
            return;
        }

        if (this.vida <= this.vidaMax / 2 && !enFase2) {
            enFase2 = true;
            GestorSonidos.reproducir(GestorSonidos.BOSS_ANGRY);
        }

        
        
        
        if (cooldownTakeHit <= 0 && estadoActual != CLEAVE) {
            estadoActual = TAKE_HIT;
            aniIndex = 0;
            aniTick = 0;
            cooldownTakeHit = TIEMPO_ENTRE_TAKE_HIT; 
        }
        
        
    }

    
    
    @Override
    public Rectangle getAttackBox() {
        if (estadoActual == CLEAVE && aniIndex >= 9 && aniIndex <= 11) {
            int anchoAtk = 250;
            int altoAtk = 180;
            // Desplazamos 60px hacia el centro del jefe para que golpee
            // a jugadores parados en sus pies (antes quedaban fuera del área)
            int atkX = mirandoIzquierda
                    ? hitbox.x - anchoAtk + 60
                    : hitbox.x + hitbox.width - 60;
            int atkY = hitbox.y + 10;
            return new Rectangle(atkX, atkY, anchoAtk, altoAtk);
        }
        return null;
    }

    @Override
    public boolean puedeAtacar() {
        return estadoActual != DEATH && estadoActual != TAKE_HIT;
    } 

    @Override
    public void reiniciarCooldown() {
        cooldownAtaque = TIEMPO_ENTRE_GOLPES;
    }

    @Override
    public boolean isAnimacionMuerteTerminada() {
        return muerto && estadoActual == DEATH && aniIndex >= FRAME_COUNT[DEATH] - 1 && tiempoMuerto > 60; 
    }

    @Override
    protected void aplicarLimitesCarril() {
    }

    @Override
    public void iniciarMarchaCinematica(int metaX) {
    }

    @Override
    public void actualizarMarchaCinematica() {
    }

    
    @Override
    public void dibujar(Graphics2D g, int cameraX) {
        if (GestorRecursos.animacionesDemon == null)
            return;

        BufferedImage[][] anim = GestorRecursos.animacionesDemon;
        int estado = (estadoActual >= 0 && estadoActual < anim.length) ? estadoActual : IDLE;
        int frames = FRAME_COUNT[estado];
        int idx = Math.min(aniIndex, frames - 1);

        BufferedImage img = (anim[estado] != null && idx < anim[estado].length)
                ? anim[estado][idx]
                : null;

        
        if (estadoActual == DEATH && idx >= frames - 1) {
            img = null;
        }

        if (img != null) {
            
            int renderX = hitbox.x - cameraX - (DRAW_W / 2) + (hitbox.width / 2);
            int renderY = hitbox.y - (DRAW_H - hitbox.height);

            
            
            
            if (mirandoIzquierda) {
                g.drawImage(img, renderX, renderY, DRAW_W, DRAW_H, null);
            } else {
                g.drawImage(img, renderX + DRAW_W, renderY, -DRAW_W, DRAW_H, null);
            }
        } else if (estadoActual != DEATH) {
            g.setColor(Color.RED);
            g.fillRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }

        if (GamePanel.debugActivado) {
            
            g.setColor(Color.RED);
            g.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
            
            Rectangle atk = getAttackBox();
            if (atk != null) {
                g.setColor(Color.YELLOW);
                g.drawRect(atk.x - cameraX, atk.y, atk.width, atk.height);
            }
        }
    }
}
