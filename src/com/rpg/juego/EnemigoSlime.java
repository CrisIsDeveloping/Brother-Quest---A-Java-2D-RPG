package com.rpg.juego;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class EnemigoSlime extends EnemigoBase {

    // Los 3 estados que puede tener el slime
    public static final int IDLE = 0;
    public static final int RUN = 1;
    public static final int DEAD = 2;

    private int cooldownAtaque = 0;
    private final int TIEMPO_ENTRE_GOLPES = 60;

    // Variables para hacer que el slime salte y rebote
    private boolean enElAire = false;
    private int COOLDOWN_SALTO = 60;
    private int DURACION_SALTO = 20;

    private boolean ultimoSaltoUno = true;
    private int timerSalto = 0;
    private int cooldownDisparo = 0;
    private final int TIEMPO_ENTRE_DISPAROS = 140; // Nerfeado de 100 a 140 para dar respiro en Horda 3
    private double velXActual = 0;
    private double velYActual = 0;

    // Stats que cambian según el color del slime
    private int cooldownSaltoBase;
    private int duracionSaltoBase;
    private int velocidadSalto;
    private String tipo; // Para saber de qué color es
    private boolean esTanque = false;

    public EnemigoSlime(int x, int y, String tipo) {
        super(x, y);
        this.tipo = tipo;
        // La caja de colisión del slime (reducida un 30% de 70x60)
        this.hitbox = new CajaColision(x, y, 49, 42);
        this.estadoActual = IDLE;
        this.aniSpeed = 15;

        // Stats base del slime (Nivel 1)
        this.vidaMax = 60;
        this.dano = 10;
        this.xpQueDa = 20;

        if (tipo.equals("ROJO") || tipo.equals("NEGRO")) {
            this.vidaMax = 90;
            this.dano = 15;
            this.xpQueDa = 35;
        }
        this.vida = this.vidaMax;
    }

    @Override
    public void iniciarMarchaCinematica(int metaX) {
        this.cinematicTargetX = metaX;
        this.llegoMetaCinematica = false;
        this.mirandoIzquierda = true;
    }

    @Override
    public void actualizarMarchaCinematica() {
        if (llegoMetaCinematica) {
            estadoActual = IDLE;
            return;
        }
        
        mirandoIzquierda = true; // Forzar que mire al jugador (izquierda)
        
        if (Math.abs(this.x - cinematicTargetX) <= 15) {
            this.x = cinematicTargetX;
            llegoMetaCinematica = true;
            estadoActual = IDLE;
            enElAire = false;
            return;
        }
        
        if (!enElAire) {
            timerSalto--;
            estadoActual = IDLE;
            if (timerSalto <= 0) {
                // Forzar salto hacia la meta
                iniciarSalto(cinematicTargetX, this.y, this.x, this.y);
            }
        } else {
            // Se mueve por el aire
            this.x += velXActual;
            this.y += velYActual;
            estadoActual = RUN;
            
            if (aniIndex >= getSpriteAmount(estadoActual)) {
                aniIndex = 0;
            }
            
            timerSalto--;
            if (timerSalto <= 0) {
                enElAire = false;
                aniIndex = 0;
                // Cooldown rapidísimo de marcha cinemática (2x más rápido)
                timerSalto = 15 + (int)(Math.random() * 15);
            }
        }
        
        hitbox.actualizar((int) this.x, (int) this.y);
        aplicarLimitesCarril();
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == DEAD)
            return;

        // Buscamos los centros para saber dónde está el jugador
        double jugadorCentroX = jugador.getX() + 20;
        double jugadorCentroY = jugador.getY() + 10;
        double miCentroX = this.x + 20;
        double miCentroY = this.y + 20;
        double distancia = Math.hypot(miCentroX - jugadorCentroX, miCentroY - jugadorCentroY);

        mirandoIzquierda = (jugadorCentroX < miCentroX);

        if (!enElAire) {
            timerSalto--;
            if (cooldownDisparo > 0)
                cooldownDisparo--;
            estadoActual = IDLE;

            // Elegimos el comportamiento según el color
            switch (this.tipo) {
                case "AZUL":
                    comportamientoAzul(distancia, jugadorCentroX, jugadorCentroY, miCentroX, miCentroY);
                    break;
                case "ROJO":
                    comportamientoRojo(distancia, jugadorCentroX, jugadorCentroY, miCentroX, miCentroY);
                    break;
                case "NEGRO":
                    comportamientoNegro(distancia, jugadorCentroX, jugadorCentroY, miCentroX, miCentroY);
                    break;
                default:
                    comportamientoVerde(distancia, jugadorCentroX, jugadorCentroY, miCentroX, miCentroY);
                    break;
            }
        } else {
            // Le sumamos la velocidad para que se mueva por el aire
            this.x += velXActual;
            this.y += velYActual;

            estadoActual = RUN;

            if (aniIndex >= getSpriteAmount(estadoActual))
                aniIndex = 0;

            timerSalto--;
            if (timerSalto <= 0) {
                enElAire = false;
                aniIndex = 0;
                // Le sumamos un tiempo MUCHO más aleatorio (entre 0 y 90 extra) para que ataquen rápido o lento (impredecible 1 a 2+ segs)
                timerSalto = cooldownSaltoBase + (int) (Math.random() * 90);
            }
        }

        // Actualizamos la hitbox para que siga al dibujo
        hitbox.actualizar((int) this.x, (int) this.y);

        // Evitamos que el slime se salga de la pista vertical
        aplicarLimitesCarril();
        
        // Jaula horizontal: si se salió en el aire, frenar la velocidad también
        if (limCameraIzq != Integer.MIN_VALUE) {
            if (this.x < limCameraIzq) {
                this.x = limCameraIzq;
                hitbox.x = limCameraIzq;
                velXActual = 0; // Frenamos para que no siga empujando
            }
            if (this.x > limCameraRight) {
                this.x = limCameraRight;
                hitbox.x = limCameraRight;
                velXActual = 0;
            }
        }
    }

    private void iniciarSalto(double targetX, double targetY, double miX, double miY) {
        enElAire = true;
        timerSalto = duracionSaltoBase;

        if (ultimoSaltoUno) {
            GestorSonidos.reproducir(GestorSonidos.SLIME_SALTO_1);
        } else {
            GestorSonidos.reproducir(GestorSonidos.SLIME_SALTO_2);
        }
        ultimoSaltoUno = !ultimoSaltoUno;

        // Calculamos el ángulo para saber hacia dónde brincar
        double angulo = Math.atan2(targetY - miY, targetX - miX);

        // Ajustamos la fuerza del salto según el color del slime
        velXActual = Math.cos(angulo) * velocidadSalto;
        velYActual = Math.sin(angulo) * velocidadSalto;
    }

    private void comportamientoVerde(double distancia, double tx, double ty, double mx, double my) {
        if (timerSalto <= 0 && distancia < rangoVision) {
            iniciarSalto(tx, ty, mx, my);
        }
    }

    private void comportamientoAzul(double distancia, double tx, double ty, double mx, double my) {
        if (timerSalto <= 0 && distancia < rangoVision) {
            iniciarSalto(tx, ty, mx, my);
        }
    }

    private void comportamientoRojo(double distancia, double tx, double ty, double mx, double my) {
        // El rojo dispara siempre que esté en rango (hasta 600px)
        if (distancia < 600) {
            if (cooldownDisparo <= 0) {
                disparar(tx, ty);
            }
        }

        // --- Lógica de Posicionamiento Inteligente ---
        if (timerSalto <= 0) {
            if (distancia < 180) {
                // Huida: Calculamos un punto opuesto pero con variación aleatoria en Y para no
                // atascarse
                double dirX = mx - tx;
                double dirY = (my - ty) + (Math.random() * 200 - 100); // Variación aleatoria en Y
                iniciarSalto(mx + dirX, my + dirY, mx, my);
            } else if (distancia > 320 && distancia < rangoVision) {
                // Acercamiento: Si el jugador se aleja, el slime salta hacia él para no perder
                // el rango
                iniciarSalto(tx, ty, mx, my);
            }
        }
    }

    private void comportamientoNegro(double distancia, double tx, double ty, double mx, double my) {
        // El negro es híbrido: dispara de lejos, ataca de cerca
        if (distancia > 300 && distancia < rangoVision) {
            if (cooldownDisparo <= 0) {
                disparar(tx, ty);
            }
        } else if (distancia <= 300 && timerSalto <= 0) {
            // Ataca como uno verde/azul si estás cerca
            iniciarSalto(tx, ty, mx, my);
        }
    }

    private void disparar(double targetX, double targetY) {
        cooldownDisparo = TIEMPO_ENTRE_DISPAROS + (int) (Math.random() * 30);

        // El color depende del tipo de slime
        Color colorBaba = Color.GREEN;
        if (tipo.equals("ROJO"))
            colorBaba = Color.RED;
        else if (tipo.equals("NEGRO"))
            colorBaba = new Color(50, 50, 50);
        else if (tipo.equals("AZUL"))
            colorBaba = Color.CYAN;

        // Calculamos la distancia horizontal real para escalar la velocidad
        double distH = Math.abs(targetX - this.x);

        // --- SISTEMA DE PUNTERÍA PARABÓLICO ---
        // El tiempo de vuelo en el aire con velZ=7f y gravedadZ=0.45f es ~31 frames
        float tiempoVuelo = (2 * 7.0f) / 0.45f;
        
        // Para que la bola recorra "distH" cruzando exactamente el tiempo de vuelo:
        float velocidadHRequerida = (float) (distH / tiempoVuelo);
        
        // Como la clase Proyectil usa un base de 7.8f, sacamos qué multiplicador necesitamos:
        float mult = velocidadHRequerida / 7.8f;
        
        // Topes para que no se vea extraño de muuuuuy lejos o absurdamente de cerca
        if (mult < 0.2f) mult = 0.2f;
        if (mult > 2.5f) mult = 2.5f;

        // Creamos la bola de baba apuntando un poco más abajo
        Proyectil baba = new Proyectil(
                (float) this.x + 15,
                (float) this.y + 5,
                (float) targetX,
                (float) targetY - 10, 
                this.dano,
                Proyectil.Emisor.ENEMIGO,
                Proyectil.Tipo.BABA_SLIME,
                colorBaba,
                mult);

        baba.setVelZ(7f); // velZ fijo, emparejado con nuestras matemáticas

        // Lo añadimos a la lista de proyectiles del juego
        GamePanel.getInstancia().getProyectiles().add(baba);
        GestorSonidos.reproducir(GestorSonidos.SLIME_SALTO_1);
    }

    @Override
    public void actualizar() {
        if (cooldownAtaque > 0)
            cooldownAtaque--;
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= getSpriteAmount(estadoActual)) {
                aniIndex = 0;
                if (estadoActual == DEAD)
                    aniIndex = getSpriteAmount(DEAD) - 1;
            }
        }
        hitbox.actualizar((int) x, (int) y);
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto)
            return;

        // Le quitamos vida
        this.vida -= cantidad;

        if (this.vida <= 0) {
            this.vida = 0;
            estadoActual = DEAD;
            muerto = true;
            aniIndex = 0;
            aniTick = 0;
        }
    }

    @Override
    public void establecerNivel(int nivelJugador) {
        // Llamamos al método padre para que calcule las stats básicas
        super.establecerNivel(nivelJugador);

        // Configuramos cómo salta cada color de slime
        switch (this.tipo) {
            case "AZUL": // Nerfeado MUCHO más
                this.vida = (int) (this.vida * 0.8); // Bajamos vida un 20% extra
                velocidadSalto = 7; // De 8 a 7
                cooldownSaltoBase = 90; // De 80 a 90
                duracionSaltoBase = 20;
                break;
            case "ROJO": // El arquero, gran nerf al daño para que el 1v4 sea jugable
                this.vida = (int) (this.vida * 0.7); // De 0.8 a 0.7
                this.dano = (int) (this.dano * 0.5); // De 0.6 a 0.5 (Mitad de daño base balanceado)
                velocidadSalto = 6;
                cooldownSaltoBase = 55; // De 45 a 55
                duracionSaltoBase = 18;
                break;
            case "NEGRO": // El tanque híbrido de LARGO ALCANCE
                this.vida = (int) (this.vida * 1.6);
                velocidadSalto = 6; // Pausado como el verde
                cooldownSaltoBase = 50;
                duracionSaltoBase = 20;
                break;
            default: // El slime verde normal
                this.vida = (int) (this.vida * 0.7);
                this.dano = (int) (this.dano * 0.8);
                velocidadSalto = 6;
                cooldownSaltoBase = 70;
                duracionSaltoBase = 20;
                break;
        }
    }

    private int getSpriteAmount(int estado) {
        switch (estado) {
            case IDLE:
                return 2;
            case RUN:
                return 4;
            case DEAD:
                return 4;
            default:
                return 1;
        }
    }

    @Override
    public boolean puedeAtacar() {
        return cooldownAtaque == 0 && estadoActual != DEAD && !isSpawning();
    }

    @Override
    public void reiniciarCooldown() {
        cooldownAtaque = TIEMPO_ENTRE_GOLPES;
    }

    @Override
    public Rectangle getAttackBox() {
        return null;
    } // El slime no tiene espada, te pega solo si te toca

    // --- Dibujamos el slime ---
    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        // Elegimos qué imagen usar dependiendo del color del slime
        BufferedImage[][] spritesActuales;

        switch (this.tipo) {
            case "AZUL":
                spritesActuales = GestorRecursos.animacionesSlimeAzul;
                break;
            case "ROJO":
                spritesActuales = GestorRecursos.animacionesSlimeRojo;
                break;
            case "NEGRO":
                spritesActuales = GestorRecursos.animacionesSlimeNegro;
                break;
            default:
                spritesActuales = GestorRecursos.animacionesSlime;
                break;
        }

        // Por si acaso algo sale mal, ponemos el verde
        if (spritesActuales == null) {
            spritesActuales = GestorRecursos.animacionesSlime;
        }

        if (spritesActuales != null) {
            int estado = (estadoActual < 3) ? estadoActual : 0;
            int amt = getSpriteAmount(estado);
            int idx = (aniIndex < amt) ? aniIndex : 0;
            BufferedImage img = spritesActuales[estado][idx];

            if (img != null) {
                // Reducimos el tamaño visual un 30% (de 120 a 84)
                int anchoBase = 84;
                int altoBase = 84;

                int anchoDibujo = esTanque ? (int) (anchoBase * 1.6) : anchoBase;
                int altoDibujo = esTanque ? (int) (altoBase * 1.6) : altoBase;

                // Ajustamos el dibujo para que cuadre con la nueva caja pequeña
                int ajusteX = esTanque ? 36 : 17;
                int ajusteY = esTanque ? 52 : 32;

                // Calculamos dónde dibujarlo restando la cámara
                int dx = hitbox.x - cameraX - ajusteX;
                int dy = hitbox.y - ajusteY;

                // Efecto de parpadeo al spawnear
                java.awt.Composite compOrig = g2.getComposite();
                float alpha = getSpawnAlpha();
                if (alpha < 1.0f) {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                }

                if (mirandoIzquierda) {
                    g2.drawImage(img, dx + anchoDibujo, dy, -anchoDibujo, altoDibujo, null);
                } else {
                    g2.drawImage(img, dx, dy, anchoDibujo, altoDibujo, null);
                }

                g2.setComposite(compOrig);
            }
        }

        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    @Override
    public boolean isAnimacionMuerteTerminada() {
        return muerto && estadoActual == DEAD && aniIndex >= getSpriteAmount(DEAD) - 1;
    }

    public void convertirEnTanque() {
        this.esTanque = true;

        // 1. Lo hacemos más lento porque es pesado
        this.velocidad = this.velocidad * 0.6f;

        // 2. Guardamos la altura anterior para poder ajustarlo
        int viejoAlto = this.hitbox.height;

        // Le hacemos la hitbox más grande (proporcional al nuevo tamaño)
        this.hitbox.width = 70;
        this.hitbox.height = 70;

        // 3. Lo subimos un poco para que no se hunda en el piso al crecer
        this.y -= (this.hitbox.height - viejoAlto);

        // 4. Lo corremos a la izquierda para que el dibujo no se salga de la caja
        this.x -= 20;

        // Actualizamos la hitbox a las nuevas medidas
        this.hitbox.x = (int) this.x;
        this.hitbox.y = (int) this.y;
    }
}