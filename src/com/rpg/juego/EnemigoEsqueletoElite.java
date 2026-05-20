package com.rpg.juego;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

// EnemigoEsqueletoElite: Un esqueleto más fuerte con sus propias animaciones y que te persigue mejor.
public class EnemigoEsqueletoElite extends EnemigoBase {

    // Tamaño del mono en pantalla
    private int anchoDibujo = 240;
    private int altoDibujo = 240;

    // Tamaño de su hitbox física
    private int anchoHitbox = 75;
    private int altoHitbox = 120;

    // Para cuadrarlo bien en el piso
    private int ajustePiesY = 60;

    // Correcciones en X porque el sprite viene medio descentrado
    private int ajusteDerecha = -5;
    private int ajusteIzquierda = 7;

    // La zona donde hace daño cuando pega
    private int anchoAtaque = 162;
    private int altoAtaque = 120;
    private int bajarAtaqueY = -20;

    // Variables para saber si corre, pega o qué hace
    private float velocidad = 1.8f;
    private boolean golpeRegistrado = false;
    private boolean atacando = false;

    private BufferedImage[][] animaciones;
    private static final int SPRITE_SIZE = 64;

    private static final int ATAQUE = 0;
    private static final int MUERTE = 1;
    private static final int CORRER = 2;
    private static final int QUIETO = 3;
    private static final int DAÑO = 4;

    private static final int[] CANTIDAD_FRAMES = {13, 13, 12, 4, 3};

    public EnemigoEsqueletoElite(int x, int y) {
        super(x, y);
        this.aniSpeed = 8;
        this.estadoActual = QUIETO;
        this.hitbox = new CajaColision((int)x, (int)y, anchoHitbox, altoHitbox);

        this.vidaMax = 400;
        this.vida = this.vidaMax;
        this.dano = 40;
        this.xpQueDa = 120;

        establecerNivel(9);
        cargarAnimaciones();
    }

    private void cargarAnimaciones() {
        try {
            InputStream is = null;

            // 1) Intento por classpath
            is = EnemigoEsqueletoElite.class.getResourceAsStream("/esqueleto_sheet.png");
            if (is == null) is = EnemigoEsqueletoElite.class.getResourceAsStream("/res/esqueleto_sheet.png");

            // 2) Fallback por filesystem (si el runner no copió `res/` al classpath)
            if (is == null) {
                File archivo = new File("res", "esqueleto_sheet.png");
                if (archivo.exists()) is = new FileInputStream(archivo);
            }

            if (is == null) {
                System.err.println("Falta la textura del EsqueletoElite: res/esqueleto_sheet.png");
                return;
            }

            BufferedImage img = ImageIO.read(is);
            animaciones = new BufferedImage[5][13];

            for (int j = 0; j < animaciones.length; j++) {
                int framesEnFila = (j < CANTIDAD_FRAMES.length) ? CANTIDAD_FRAMES[j] : 0;
                for (int i = 0; i < framesEnFila; i++) {
                    animaciones[j][i] = img.getSubimage(i * SPRITE_SIZE, j * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando la textura del EsqueletoElite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void iniciarMarchaCinematica(int metaX) {
        this.cinematicTargetX = metaX;
        this.llegoMetaCinematica = false;
        this.mirandoIzquierda = true;
        this.estadoActual = CORRER;
    }

    @Override
    public void actualizarMarchaCinematica() {
        if (llegoMetaCinematica) {
            estadoActual = QUIETO;
            return;
        }

        mirandoIzquierda = true;
        estadoActual = CORRER;

        if (Math.abs(this.x - cinematicTargetX) <= 15) {
            this.x = cinematicTargetX;
            llegoMetaCinematica = true;
            estadoActual = QUIETO;
            return;
        }

        // x2 velocidad de marcha
        this.x -= (velocidad * 2.0f);
        
        hitbox.actualizar((int) this.x, (int) this.y);
        aplicarLimitesCarril();
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == MUERTE) return;

        // Sacamos los centros de las hitboxes para ver qué tan lejos está del jugador
        Rectangle hbJugador = jugador.getHitbox();
        float centroEnemigoX = hitbox.x + hitbox.width / 2f;
        float centroJugadorX = hbJugador.x + hbJugador.width / 2f;
        float piesEnemigoY = hitbox.y + hitbox.height;
        float piesJugadorY = hbJugador.y + hbJugador.height;

        // Hacemos que mire al jugador si no está ocupado o si está cargando el ataque
        if (estadoActual != DAÑO) {
            if (estadoActual != ATAQUE) {
                mirandoIzquierda = (centroJugadorX < centroEnemigoX);
            } else if (aniIndex <= 4 || (aniIndex > 4 && aniIndex <= 8)) {
                mirandoIzquierda = (centroJugadorX < centroEnemigoX);
            }
        }

        float distX = Math.abs(centroJugadorX - centroEnemigoX);
        float distY = Math.abs(piesJugadorY - piesEnemigoY);

        // Lógica para seguir al jugador y pararse cuando lo alcanza

        // 1. Si está cerquita en X e Y, le pega (Aumentado rango a 115)
        if (distX <= 115 && distY <= 15) {
            if (estadoActual != ATAQUE && estadoActual != DAÑO) {
                estadoActual = ATAQUE;
                aniIndex = 0;
                aniTick = 0;
                atacando = true;

                if (Math.random() < 0.35) {
                    isDelayedAttack = true;
                    delayTimer = 30 + (int)(Math.random() * 60); // de 0.5 a 1.5 segundos
                    initialDelay = delayTimer;
                } else {
                    isDelayedAttack = false;
                }
            }
        }
        // 2. Si está lejos, lo persigue o frena si ya lo alcanzó
        else if (distX < 400 && estadoActual != DAÑO && estadoActual != ATAQUE) {
            estadoActual = CORRER;

            // Queremos que lleguen al choque pero sin fusionarse
            // Sumamos las mitades de las hitboxes para saber el punto exacto de choque
            float distanciaContacto = (hitbox.width / 2f) + (hbJugador.width / 2f) - 5;

            // Camina en X hasta que lo choca
            if (distX > distanciaContacto) {
                if (centroJugadorX < centroEnemigoX) this.x -= velocidad;
                else this.x += velocidad;
            } else {
                // Si ya lo chocó de frente, se queda quieto o mantiene posición
                if (distY <= 15) estadoActual = QUIETO;
            }

            // Lo sigue en el eje Y
            if (distY > 5) {
                if (piesJugadorY < piesEnemigoY) this.y -= (velocidad * 0.7f);
                else if (piesJugadorY > piesEnemigoY) this.y += (velocidad * 0.7f);
            }
        } else if (estadoActual != DAÑO && estadoActual != ATAQUE) {
            estadoActual = QUIETO;
        }

        // Actualizamos la caja de colisión a su nueva posición
        hitbox.actualizar((int) this.x, (int) this.y);
        aplicarLimitesCarril();
    }

    @Override
    public void actualizar() {
        actualizarTickAnimacion();
    }

    // Control de los frames de la animación y resets de los golpes
    private void actualizarTickAnimacion() {
        if (estadoActual == ATAQUE) {
            // El Elite puede cargar en el primer golpe (frame 2) o en el segundo (frame 7)
            if ((aniIndex == 2 || aniIndex == 7) && isDelayedAttack && delayTimer > 0) {
                if (delayTimer == initialDelay) {
                    GestorSonidos.reproducir(GestorSonidos.CARGA_ATAQUE);
                }
                delayTimer--;
                return;
            }
        }

        aniTick++;
        int velocidadActual = aniSpeed;
        if (estadoActual == MUERTE) velocidadActual = 6;
        else if (estadoActual == CORRER) velocidadActual = 6;
        if (aniTick >= velocidadActual) {
            aniTick = 0;
            aniIndex++;

            // Reiniciamos el golpe en el frame 4 para que pegue dos veces (el combo del frame 4 y 8)
            if (estadoActual == ATAQUE) {
                // Efecto de swing: se reproduce al soltar la espada (frames 3 y 8)
                if ((aniIndex == 3 || aniIndex == 8) && aniTick == 0) {
                    GestorSonidos.reproducir(GestorSonidos.SWING_ESPADA);
                }

                if (aniIndex == 0 || aniIndex == 6) {
                    golpeRegistrado = false;
                    
                    // Al iniciar el segundo swing del combo (frame 6), volvemos a sortear el delay
                    if (aniIndex == 6 && aniTick == 0) {
                        if (Math.random() < 0.35) {
                            isDelayedAttack = true;
                            delayTimer = 30 + (int)(Math.random() * 60);
                            initialDelay = delayTimer;
                        } else {
                            isDelayedAttack = false;
                            delayTimer = 0;
                        }
                    }
                }
            }

            if (aniIndex >= CANTIDAD_FRAMES[estadoActual]) {
                if (estadoActual == MUERTE) {
                    aniIndex = CANTIDAD_FRAMES[MUERTE] - 1; // Se queda en el último frame
                } else {
                    aniIndex = 0;
                    if (estadoActual == ATAQUE || estadoActual == DAÑO) {
                        estadoActual = QUIETO;
                        atacando = false;
                        golpeRegistrado = false;
                    }
                }
            }
        }
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto) return;
        vida -= cantidad;
        if (vida <= 0) {
            vida = 0;
            muerto = true;
            estadoActual = MUERTE;
            aniIndex = 0;
            aniTick = 0;
        } else if (estadoActual != ATAQUE && estadoActual != DAÑO) {
            estadoActual = DAÑO;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    // Creamos la caja de ataque justo en los frames 4 y 8 para que cuadre con el dibujo
    public Rectangle getAttackBox() {
        // Lo ajustamos a esos frames para que no se sienta raro el delay
        if (estadoActual == ATAQUE && (aniIndex == 4 || aniIndex == 8)) {
            int alcanceAtras = 50;
            int anchoExtra = 50;

            int xFinal = mirandoIzquierda ?
                    (hitbox.x + hitbox.width + alcanceAtras) - (anchoAtaque + anchoExtra) :
                    hitbox.x - alcanceAtras;

            return new Rectangle(xFinal, hitbox.y + bajarAtaqueY, anchoAtaque + anchoExtra, altoAtaque);
        }
        return null;
    }

    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        if (animaciones == null || animaciones[estadoActual][aniIndex] == null) return;

        BufferedImage img = animaciones[estadoActual][aniIndex];

        int drawX = hitbox.x - (anchoDibujo - hitbox.width) / 2 - cameraX;
        int drawY = (hitbox.y + hitbox.height) - altoDibujo + ajustePiesY;

        if (mirandoIzquierda) {
            g2.drawImage(img, drawX + anchoDibujo - ajusteIzquierda, drawY, -anchoDibujo, altoDibujo, null);
        } else {
            g2.drawImage(img, drawX - ajusteDerecha, drawY, anchoDibujo, altoDibujo, null);
        }

        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
            Rectangle ab = getAttackBox();
            if (ab != null) {
                g2.setColor(Color.YELLOW);
                g2.drawRect(ab.x - cameraX, ab.y, ab.width, ab.height);
            }
        }
    }

    public boolean isAtacando() { return atacando; }
    public void setAtacando(boolean b) { this.atacando = b; }
    public boolean isGolpeRegistrado() { return golpeRegistrado; }
    public void setGolpeRegistrado(boolean b) { this.golpeRegistrado = b; }

    @Override
    public boolean isAnimacionMuerteTerminada() {
        return muerto && estadoActual == MUERTE && aniIndex >= CANTIDAD_FRAMES[MUERTE] - 1;
    }
}