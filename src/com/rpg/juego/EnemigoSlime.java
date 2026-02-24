package com.rpg.juego;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.BasicStroke;

public class EnemigoSlime extends EnemigoBase {

    // Constantes de la maquina de estados de animacion
    public static final int IDLE = 0;
    public static final int RUN = 1;
    public static final int DEAD = 2;

    private int rangoVision = 400;
    private int cooldownAtaque = 0;
    private final int TIEMPO_ENTRE_GOLPES = 60;

    // Variables de control para calculo de trayectorias parabolicas
    private boolean enElAire = false;
    private int COOLDOWN_SALTO = 60;
    private int DURACION_SALTO = 20;

    private int timerSalto = 0;
    private double velXActual = 0;
    private double velYActual = 0;

    // --- Parametros de escalado dinamico por variante ---
    private int cooldownSaltoBase;
    private int duracionSaltoBase;
    private int velocidadSalto;
    private String tipo; // Inyeccion de la variante instanciada
    private boolean esTanque = false;

    public EnemigoSlime(int x, int y, String tipo) {
        super(x, y);
        this.tipo = tipo;
        // Inicializacion de la caja de colision (Hitbox) pre-ajustada
        this.hitbox = new Rectangle(x, y, 70, 60);
        this.estadoActual = IDLE;
        this.aniSpeed = 15;
    }

    @Override
    public void actualizarIA(Jugador jugador) {
        if (estadoActual == DEAD) return;

        // Calculo de centroides para resolucion de vectores de distancia
        double jugadorCentroX = jugador.getX() + 20;
        double jugadorCentroY = jugador.getY() + 10;
        double miCentroX = this.x + 20;
        double miCentroY = this.y + 20;
        double distancia = Math.hypot(miCentroX - jugadorCentroX, miCentroY - jugadorCentroY);

        mirandoIzquierda = (jugadorCentroX < miCentroX);

        if (!enElAire) {
            timerSalto--;
            estadoActual = IDLE;
            if (timerSalto <= 0 && distancia < rangoVision) {
                iniciarSalto(jugadorCentroX, jugadorCentroY, miCentroX, miCentroY);
            }
        } else {
            // Aplicacion de vectores de velocidad al componente de posicion
            this.x += velXActual;
            this.y += velYActual;

            estadoActual = RUN;

            if (aniIndex >= getSpriteAmount(estadoActual)) aniIndex = 0;

            timerSalto--;
            if (timerSalto <= 0) {
                enElAire = false;
                aniIndex = 0;
                // Introduccion de entropia (Random) para desincronizar los saltos de multiples entidades
                timerSalto = cooldownSaltoBase + (int)(Math.random() * 20);
            }
        }

        // Sincronizacion del modelo fisico con la capa logica
        hitbox.x = (int) this.x;
        hitbox.y = (int) this.y;

        // Aplicacion de fronteras operativas del entorno (Limites de carril)
        aplicarLimitesCarril();
    }

    private void iniciarSalto(double targetX, double targetY, double miX, double miY) {
        enElAire = true;
        timerSalto = duracionSaltoBase;

        // Resolucion trigonometrica del vector de direccion
        double angulo = Math.atan2(targetY - miY, targetX - miX);

        // Asignacion dinamica de parametros fisicos segun la variante
        velXActual = Math.cos(angulo) * velocidadSalto;
        velYActual = Math.sin(angulo) * velocidadSalto;
    }

    @Override
    public void actualizar() {
        if (cooldownAtaque > 0) cooldownAtaque--;
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= getSpriteAmount(estadoActual)) {
                aniIndex = 0;
                if (estadoActual == DEAD) aniIndex = getSpriteAmount(DEAD) - 1;
            }
        }
        hitbox.x = (int) x;
        hitbox.y = (int) y;
    }

    @Override
    public void recibirDano(int cantidad) {
        if (muerto) return; // Validacion de estado nulo

        // Resolucion de mutacion: Sustraccion explicita de puntos de salud
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
        // Ejecucion del metodo superclase para el calculo base de estadisticas
        super.establecerNivel(nivelJugador);

        // Configuracion del arbol de comportamientos de salto segun la variante instanciada
        switch (this.tipo) {
            case "AZUL": // Movimiento parabolico extendido, cadencia estandar
                velocidadSalto = 9;
                cooldownSaltoBase = 60;
                duracionSaltoBase = 25;
                break;
            case "ROJO": // Movimiento parabolico corto, alta frecuencia de ejecucion
                velocidadSalto = 6;
                cooldownSaltoBase = 20;
                duracionSaltoBase = 20;
                break;
            case "NEGRO": // Comportamiento hostil agresivo: alcance y frecuencia elevados
                velocidadSalto = 7;
                cooldownSaltoBase = 40;
                duracionSaltoBase = 20;
                break;
            default: // Variante base (Verde)
                velocidadSalto = 6;
                cooldownSaltoBase = 60;
                duracionSaltoBase = 20;
                break;
        }
    }

    private int getSpriteAmount(int estado) {
        switch (estado) { case IDLE: return 2; case RUN: return 4; case DEAD: return 4; default: return 1; }
    }

    @Override
    public boolean puedeAtacar() { return cooldownAtaque == 0 && estadoActual != DEAD; }

    @Override
    public void reiniciarCooldown() { cooldownAtaque = TIEMPO_ENTRE_GOLPES; }

    @Override
    public Rectangle getAttackBox() { return null; } // Entidad sin arma externa; el daño se calcula por solapamiento de Hitboxes (Contacto)

    // --- Modulo de renderizado desacoplado del GamePanel ---
    @Override
    public void dibujar(Graphics2D g2, int cameraX) {
        // 1. Declaracion de matriz de sub-imagenes
        BufferedImage[][] spritesActuales;

        // 2. Asignacion dinamica de referencias segun la variable de estado 'tipo'
        switch (this.tipo) {
            case "AZUL": spritesActuales = GestorRecursos.animacionesSlimeAzul; break;
            case "ROJO": spritesActuales = GestorRecursos.animacionesSlimeRojo; break;
            case "NEGRO": spritesActuales = GestorRecursos.animacionesSlimeNegro; break;
            default: spritesActuales = GestorRecursos.animacionesSlime; break;
        }

        // 3. Mecanismo de contingencia (Fallback) para prevencion de NullPointerException
        if (spritesActuales == null) {
            spritesActuales = GestorRecursos.animacionesSlime;
        }

        // 4. Inyeccion en el pipeline de renderizado
        if (spritesActuales != null) {
            int idx = (aniIndex < 4) ? aniIndex : 0;
            int estado = (estadoActual < 3) ? estadoActual : 0;
            BufferedImage img = spritesActuales[estado][idx];

            if (img != null) {
                int anchoBase = 120;
                int altoBase = 120;

                int anchoDibujo = esTanque ? (int)(anchoBase * 1.6) : anchoBase;
                int altoDibujo = esTanque ? (int)(altoBase * 1.6) : altoBase;

                // Ecuacion de compensacion espacial (Offset) para centrado de sprites escalados
                int ajusteX = esTanque ? 52 : 25;
                int ajusteY = esTanque ? 75 : 45;

                // Calculo de coordenadas absolutas considerando el desplazamiento de la camara
                int dx = hitbox.x - cameraX - ajusteX;
                int dy = hitbox.y - ajusteY;

                if (mirandoIzquierda) {
                    g2.drawImage(img, dx + anchoDibujo, dy, -anchoDibujo, altoDibujo, null);
                } else {
                    g2.drawImage(img, dx, dy, anchoDibujo, altoDibujo, null);
                }
            }
        }

        // Modulo de depuracion visual (Hitboxes)
        if (GamePanel.debugActivado) {
            g2.setColor(Color.RED);
            g2.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    public void convertirEnTanque() {
        this.esTanque = true;

        // 1. Modificacion del multiplicador de velocidad (Debuff)
        this.velocidad = this.velocidad * 0.6f;

        // 2. Persistencia temporal del estado espacial previo
        int viejoAlto = this.hitbox.height;

        // Redefinicion de dimensiones del Bounding Box para variante tipo Tanque
        this.hitbox.width = 100;
        this.hitbox.height = 100;

        // 3. Correccion del eje Y para evitar penetracion geometrica con el plano del suelo
        this.y -= (this.hitbox.height - viejoAlto);

        // 4. Correccion del centroide en el eje X
        this.x -= 30;

        // Aplicacion de la transformacion al contenedor fisico
        this.hitbox.x = (int) this.x;
        this.hitbox.y = (int) this.y;
    }
}