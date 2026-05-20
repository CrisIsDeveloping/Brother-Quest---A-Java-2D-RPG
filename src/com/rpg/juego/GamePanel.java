<<<<<<< HEAD
package com.rpg.juego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static GamePanel instancia;

    public static GamePanel getInstancia() {
        return instancia;
    }

    private int hitStopTicks = 0;
    private int shakeTicks = 0;
    private int maxShakeTicks = 1;
    private int shakeIntensity = 0;

    public void iniciarHitStop(int ticks) {
        this.hitStopTicks = ticks;
    }

    public void iniciarCameraShake(int frames, int intensidad) {
        this.shakeTicks = frames;
        this.maxShakeTicks = frames > 0 ? frames : 1;
        this.shakeIntensity = intensidad;
    }

    private static final int ANCHO = 1280;
    private static final int ALTO = 720;

    public static final int ESTADO_MENU = 0;
    public static final int ESTADO_INTRO = 1;
    public static final int ESTADO_JUEGO = 2;
    public static final int ESTADO_PAUSA = 3;
    public static final int ESTADO_DIALOGO_HORDA = 4;
    public static final int ESTADO_CINEMATICA_HORDA = 5;
    public static final int ESTADO_TIENDA = 6;
    public static final int ESTADO_DIALOGO_TIENDA = 7;
    public static final int ESTADO_BOSS_FIGHT = 8;
    public static final int ESTADO_CINEMATICA_BOSS = 9;
    public static final int ESTADO_DIALOGO_BOSS = 10;
    public static final int ESTADO_VICTORIA = 11;

    public int estadoActual = ESTADO_MENU;
    private List<Trader> traders;
    public Trader traderActivo = null;

    private Portal portalJefe;
    private EnemigoJefeDemonio jefeFinal;

    private int indiceDialogoTrader = 0;
    private final String[] dialogosTrader = {
            "*Tarareando musica epica*",
            "Ho-Hola aventurero, este lugar es peligroso",
            "Tengo pociones y mejoras que te vendrán muy bien",
            "¡Echa un vistazo!, cualquier compra va con ñapa"
    };

    private int seleccionMenu = 0;
    private int subEstadoMenu = 0;
    private final String[] opcionesMenu = { "EMPEZAR PARTIDA", "OPCIONES", "CERRAR EL JUEGO" };

    private int subEstadoPausa = 0;
    private int seleccionPausa = 0;

    public int opcionTiendaActiva = 0;
    public String[] itemsTienda = { "Mejora de Salud Maxima (+20%)", "Mejora de Daño (+20%)", "Pocion de Vida (+1)",
            "Pocion de Fuerza (+1)", "Pocion de Velocidad (+1)" };
    public int[] costosTienda = { 100, 150, 30, 50, 50 };

    private boolean enPantallaCompleta = true;
    private int cameraX = 0;
    private int menuScrollX = 0;
    private final int PUNTO_SCROLL = ANCHO / 2;

    private transient Thread gameThread;
    private volatile boolean running = true;
    private Jugador jugador;
    private List<CapaFondo> capasParallax;

    private List<EnemigoBase> enemigos;
    private Spawner spawner;
    private BarraJefe barraJefeNivel;

    private int faseIntro = 0;
    private int villanoX = 1400;
    private int villanoY = 480;
    private int hermanoX = 1500;
    private String textoDialogoLinea1 = "¡JAJAJA! ¡Eres tan debil, que no puedes proteger a tu hermano!";
    private String textoDialogoLinea2 = "¡Ahora sera convertido en el monstruo mas poderoso!";
    private boolean mostrarDialogo = false;

    private boolean enTransicion = false;
    private float alphaTransicion = 0f;
    private int estadoSiguiente = -1;
    private boolean oscureciendo = true;
    private float velTransicion = 0.03f;

    private int ultimaHordaDialogada = -1;
    private int faseDialogoHorda = 0;
    private int cinematicTargetX = 0;

    private int introAniTick = 0;
    private int introAniIndex = 0;
    private boolean villanoMirandoIzq = true;
    private int estadoVillanoIntro = 2;
    private int estadoHermanoIntro = 1;

    private boolean up, down, left, right;
    private boolean juegoActivo = true;
    private List<TextoDano> textosDano = new ArrayList<>();
    private ArrayList<ObjetoRecogible> objetosSuelo = new ArrayList<>();
    private List<Proyectil> proyectiles = new ArrayList<>();
    private GestorHabilidades gestorHabilidades;
    private Font fuenteTitulo;

    private final List<float[]> spritesVictoria = new java.util.ArrayList<>();
    private int timerSpawnVictoria = 60;
    private float victoriaCamX = 0f;

    private final Dibujado dibujado;
    private final Colisiones colisiones;

    public static boolean debugActivado = false;
    private boolean consolaAbierta = false;
    private StringBuilder textoConsola = new StringBuilder();
    private String ultimoResultado = "";
    private List<String> historialComandos = new ArrayList<>();
    private List<String> historialLogs = new ArrayList<>();
    private int indiceHistorial = -1;
    private int cursorConsola = 0;
    private List<String> sugerenciasActivas = new ArrayList<>();
    private int indiceSugerencia = -1;
    private String comandoAntesDeTab = "";

    private int faseCinematicaHorda = 0;

    public GamePanel() {
        instancia = this;
        this.setDoubleBuffered(true);
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        try {
            fuenteTitulo = new Font("Serif", Font.BOLD, 80);
        } catch (Exception e) {
        }

        GestorRecursos.cargarRecursos();
        GestorSonidos.inicializar();
        GestorSonidos.iniciarMusicasFondo();
        dibujado = new Dibujado();
        colisiones = new Colisiones();
        gestorHabilidades = new GestorHabilidades();
        initGame();

        new GestorTeclas(this, getInputMap(WHEN_IN_FOCUSED_WINDOW), getActionMap());

        gameThread = new Thread(this::gameLoop);
        gameThread.start();
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            boolean shouldRender = false;

            while (delta >= 1) {
                actualizarLogicaJuego();
                delta--;
                shouldRender = true;
            }

            if (shouldRender) {
                repaint();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void actualizarLogicaJuego() {
        if (estadoActual == ESTADO_MENU) {
            menuScrollX += 2;
        } else if (estadoActual == ESTADO_INTRO) {
            actualizarIntro();
        } else if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && juegoActivo
                && !consolaAbierta) {
            actualizarJuego();
        } else if (estadoActual == ESTADO_CINEMATICA_HORDA || estadoActual == ESTADO_DIALOGO_HORDA
                || estadoActual == ESTADO_CINEMATICA_BOSS || estadoActual == ESTADO_DIALOGO_BOSS) {
            actualizarCinematicaODialogo();
        } else if (estadoActual == ESTADO_VICTORIA) {
            actualizarSpritesVictoria();
        }

        gestionarSonidosEnemigos();

        boolean yendoAIntro = (estadoActual == ESTADO_INTRO) || (enTransicion && estadoSiguiente == ESTADO_INTRO);
        boolean yendoADialogos = yendoAIntro || (estadoActual == ESTADO_DIALOGO_TIENDA)
                || (enTransicion && estadoSiguiente == ESTADO_DIALOGO_TIENDA);
        boolean enTienda = (estadoActual == ESTADO_TIENDA) || (enTransicion && estadoSiguiente == ESTADO_TIENDA);

        GestorSonidos.TipoMusica tipoMusica = GestorSonidos.TipoMusica.NINGUNA;

        if (estadoActual == ESTADO_MENU && !yendoADialogos) {
            tipoMusica = GestorSonidos.TipoMusica.MENU;
        } else if (yendoADialogos) {
            tipoMusica = GestorSonidos.TipoMusica.NINGUNA;
        } else if (enTienda) {
            tipoMusica = GestorSonidos.TipoMusica.TIENDA;
        } else if (estadoActual == ESTADO_BOSS_FIGHT) {
            tipoMusica = GestorSonidos.TipoMusica.BOSS;
        } else if (estadoActual == ESTADO_VICTORIA) {
            tipoMusica = GestorSonidos.TipoMusica.VICTORIA;
        } else if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_PAUSA ||
                estadoActual == ESTADO_CINEMATICA_HORDA || estadoActual == ESTADO_DIALOGO_HORDA) {
            if (spawner != null && spawner.isHordaActiva()) {
                tipoMusica = GestorSonidos.TipoMusica.COMBATE;
            } else {
                tipoMusica = GestorSonidos.TipoMusica.JUEGO;
            }
        }

        GestorSonidos.setMusicaAmbiental(tipoMusica);
        GestorSonidos.actualizarFundidoMusica();

        if (estadoActual == ESTADO_VICTORIA) {
            GestorSonidos.detener(GestorSonidos.AMBIENTE_BOSQUE);
            GestorSonidos.detener(GestorSonidos.RUN_SONIDO);
            GestorSonidos.detener(GestorSonidos.RUN_ESQUELETO);
            GestorSonidos.detener(GestorSonidos.RUN_NIGHTBORNE);
        }

        actualizarTransicion();
    }

    private void initGame() {
        GestorContadores.reiniciar();
        jugador = new Jugador();
        capasParallax = new ArrayList<>();
        enemigos = new ArrayList<>();
        spawner = new Spawner(enemigos);

        traders = new ArrayList<>();

        traders.add(new Trader(17050, 470));
        portalJefe = new Portal(17200, 460);

        cargarCapas();
    }

    public void limpiarTradersTemporales() {

        traders.removeIf(t -> t.getXLogico(0) < 16000);
    }

    private void cargarCapas() {
        if (!GestorRecursos.capasFondo.isEmpty()) {
            double escala = 0.33334;
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(6), 0.0, escala, 0));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(5), 0.1, escala, 0));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(4), 0.2, escala, 5));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(3), 0.5, escala, 10));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(2), 0.7, escala, 290));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(1), 1.0, escala, 385));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(0), 1.0, escala, 600));
        }
    }

    private void setupTeclasDireccion(InputMap im, ActionMap am) {

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "acc_up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "acc_up");
        am.put("acc_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (consolaAbierta)
                    return;
                if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                    up = true;
                else
                    navegarMenu(-1);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "stop_up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "stop_up");
        am.put("stop_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                up = false;
                if (jugador != null)
                    jugador.pararMovimiento();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "acc_down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "acc_down");
        am.put("acc_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (consolaAbierta)
                    return;
                if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                    down = true;
                else
                    navegarMenu(1);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "stop_down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "stop_down");
        am.put("stop_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                down = false;
                if (jugador != null)
                    jugador.pararMovimiento();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "acc_left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "acc_left");
        am.put("acc_left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (consolaAbierta)
                    return;
                if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                    left = true;
                else if (isEnOpciones())
                    ajustarOpcion(false);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "stop_left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "stop_left");
        am.put("stop_left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                left = false;
                if (jugador != null)
                    jugador.pararMovimiento();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "acc_right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "acc_right");
        am.put("acc_right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (consolaAbierta)
                    return;
                if (consolaAbierta)
                    return;
                if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                    right = true;
                else if (isEnOpciones())
                    ajustarOpcion(true);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "stop_right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "stop_right");
        am.put("stop_right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                right = false;
                if (jugador != null)
                    jugador.pararMovimiento();
            }
        });
    }

    private void setupTeclasCombate(InputMap im, ActionMap am) {
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "jump");
        am.put("jump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && !consolaAbierta)
                    jugador.saltar();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0, false), "attack");
        am.put("attack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && !consolaAbierta
                        && !jugador.isAtacando())
                    jugador.setAtacando(true);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "roll");
        am.put("roll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && !consolaAbierta)
                    jugador.rodar();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "potion");
        am.put("potion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && !consolaAbierta)
                    jugador.usarPocionSeleccionada();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, false), "guard");
        am.put("guard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) && !consolaAbierta)
                    jugador.setDefendiendo(true);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, true), "stop_guard");
        am.put("stop_guard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                    jugador.setDefendiendo(false);
            }
        });
    }

    public boolean isEnOpciones() {
        return (estadoActual == ESTADO_PAUSA && subEstadoPausa == 1)
                || (estadoActual == ESTADO_MENU && subEstadoMenu == 1);
    }

    public void navegarMenu(int dir) {
        GestorSonidos.reproducir(GestorSonidos.SWITCH);
        if (estadoActual == ESTADO_MENU) {
            if (subEstadoMenu == 0) {
                seleccionMenu = (seleccionMenu + dir + opcionesMenu.length) % opcionesMenu.length;
            } else {
                seleccionPausa = (seleccionPausa + dir + 4) % 4;
            }
        } else if (estadoActual == ESTADO_PAUSA && subEstadoPausa != 2) {
            seleccionPausa = (seleccionPausa + dir + 5) % 5;
        } else if (estadoActual == ESTADO_TIENDA) {
            opcionTiendaActiva = (opcionTiendaActiva + dir + itemsTienda.length) % itemsTienda.length;
        }
    }

    private void resetFlechas() {
        up = false;
        down = false;
        left = false;
        right = false;
    }

    private void gestionarSeleccionOpcionesMenu() {
        GestorSonidos.reproducir(GestorSonidos.SELECT);
        switch (seleccionPausa) {
            case 2:
                toggleFullScreen();
                break;
            case 3:
                subEstadoMenu = 0;
                seleccionMenu = 1;
                break;
        }
    }

    private void gestionarSeleccionPausa() {
        if (subEstadoPausa == 0) {
            switch (seleccionPausa) {
                case 0:
                    estadoActual = ESTADO_JUEGO;
                    break;
                case 1:
                    subEstadoPausa = 1;
                    seleccionPausa = 0;
                    break;
                case 2:
                    subEstadoPausa = 2;
                    seleccionPausa = 0;
                    break;
                case 3:
                    cambiarEstado(ESTADO_MENU);
                    break;
                case 4:
                    System.exit(0);
                    break;
            }
        } else if (subEstadoPausa == 1) {
            switch (seleccionPausa) {
                case 1:
                    GestorSonidos.toggleMusica();
                    break;
                case 2:
                    toggleFullScreen();
                    break;
                case 3:
                    subEstadoPausa = 0;
                    seleccionPausa = 1;
                    break;
            }
        } else if (subEstadoPausa == 2) {
            subEstadoPausa = 0;
            seleccionPausa = 2;
        }
    }

    public void ajustarOpcion(boolean derecha) {
        if (isEnOpciones()) {
            if (seleccionPausa == 0) {
                float vol = GestorSonidos.getVolumenMaestroSFX();
                vol = derecha ? Math.min(6.0f, vol + 1.0f) : Math.max(-20.0f, vol - 1.0f);
                GestorSonidos.setVolumenMaestroSFX(vol);
            } else if (seleccionPausa == 1) {
                if (derecha && !GestorSonidos.isMusicaActivada()) {
                    GestorSonidos.toggleMusica();
                } else if (!derecha && GestorSonidos.isMusicaActivada()) {
                    GestorSonidos.toggleMusica();
                }
            }
        }
    }

    /** Alterna la visibilidad de la consola de comandos (F4). */
    public void toggleConsola() {
        consolaAbierta = !consolaAbierta;
        if (consolaAbierta) {
            textoConsola.setLength(0);
            cursorConsola = 0;
            ultimoResultado = "Introduce comando...";
            pararMovimientoJugador();
        } else {
            juegoActivo = true;
        }
    }

    public void setUp(boolean v) {
        up = v;
    }

    public void setDown(boolean v) {
        down = v;
    }

    public void setLeft(boolean v) {
        left = v;
    }

    public void setRight(boolean v) {
        right = v;
    }

    /** Lógica de la tecla ENTER delegada a GamePanel. */
    public void accionEnter() {
        if (consolaAbierta)
            return;

        if (estadoActual == ESTADO_MENU && !enTransicion) {
            if (subEstadoMenu == 0) {
                GestorSonidos.reproducir(GestorSonidos.SELECT);
                switch (seleccionMenu) {
                    case 0:
                        cambiarEstado(ESTADO_INTRO);
                        break;
                    case 1:
                        subEstadoMenu = 1;
                        seleccionMenu = 0;
                        break;
                    case 2:
                        System.exit(0);
                        break;
                }
            } else {
                gestionarSeleccionOpcionesMenu();
            }
        } else if (estadoActual == ESTADO_DIALOGO_TIENDA) {
            GestorSonidos.reproducir(GestorSonidos.NEXT);

            if (indiceDialogoTrader < dialogosTrader.length - 1) {
                indiceDialogoTrader++;
            } else {
                GestorSonidos.reproducir(GestorSonidos.MENU_OPEN);
                estadoActual = ESTADO_TIENDA;
            }
        } else if (estadoActual == ESTADO_TIENDA) {
            GestorSonidos.reproducir(GestorSonidos.SELECT);
            comprarItemTienda();
        } else if (estadoActual == ESTADO_INTRO && mostrarDialogo) {
            GestorSonidos.reproducir(GestorSonidos.NEXT);
            mostrarDialogo = false;
            faseIntro = 3;
        } else if (estadoActual == ESTADO_DIALOGO_HORDA) {
            GestorSonidos.reproducir(GestorSonidos.NEXT);
            if (faseDialogoHorda == 0) {
                faseDialogoHorda = 1;
            } else if (faseDialogoHorda == 1) {
                estadoActual = ESTADO_JUEGO;
                jugador.setVelocidadCinematica(4.0f);
                resetFlechas();
            }
        } else if (estadoActual == ESTADO_DIALOGO_BOSS) {
            GestorSonidos.reproducir(GestorSonidos.NEXT);
            if (faseDialogoHorda == 0) {
                faseDialogoHorda = 1;
            } else if (faseDialogoHorda == 1) {
                estadoActual = ESTADO_BOSS_FIGHT;
                juegoActivo = true;
                resetFlechas();
                jugador.setVelocidadCinematica(4.0f);
                if (jefeFinal instanceof EnemigoJefeDemonio) {
                    ((EnemigoJefeDemonio) jefeFinal).setActivo(true);
                }
                barraJefeNivel = new BarraJefe(jefeFinal, "Rey Demonio", 1280);
                GestorSonidos.reproducir(GestorSonidos.BOSS_LAUGHT);
            }
        } else if (estadoActual == ESTADO_PAUSA) {
            GestorSonidos.reproducir(GestorSonidos.SELECT);
            gestionarSeleccionPausa();
        } else if ((estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT)
                && !juegoActivo && !enTransicion) {

            cambiarEstado(ESTADO_MENU);
        } else if (estadoActual == ESTADO_VICTORIA && !enTransicion) {

            cambiarEstado(ESTADO_MENU);
        }
    }

    /** Lógica de la tecla ESCAPE delegada a GamePanel. */
    public void accionEscape() {
        if (consolaAbierta) {
            consolaAbierta = false;
        } else if (estadoActual == ESTADO_JUEGO) {
            GestorSonidos.reproducir(GestorSonidos.MENU_OPEN);
            estadoActual = ESTADO_PAUSA;
            subEstadoPausa = 0;
            pararMovimientoJugador();
        } else if (estadoActual == ESTADO_PAUSA) {
            if (subEstadoPausa == 1) {
                subEstadoPausa = 0;
                seleccionPausa = 1;
            } else {
                estadoActual = ESTADO_JUEGO;
            }
        } else if (estadoActual == ESTADO_TIENDA) {
            estadoActual = ESTADO_JUEGO;
            if (traderActivo != null)
                traderActivo.setEstado(Trader.IDLE);
            traderActivo = null;
        } else if (estadoActual == ESTADO_MENU && subEstadoMenu == 1) {
            subEstadoMenu = 0;
            seleccionMenu = 1;
        }
    }

    /** Lógica de la tecla G (interactuar) delegada a GamePanel. */
    public void accionInteractuar() {
        if (consolaAbierta || estadoActual == ESTADO_PAUSA)
            return;
        if (estadoActual == ESTADO_JUEGO) {
            if (!spawner.isHordaActiva()) {
                if (portalJefe != null && portalJefe.getHitbox().intersects(jugador.getBounds())) {
                    cambiarEstado(ESTADO_BOSS_FIGHT);
                    GestorSonidos.reproducir(GestorSonidos.MENU_OPEN);
                    return;
                }

                for (Trader t : traders) {
                    if (Math.abs(jugador.getXLogical() - t.getXLogico(cameraX)) < 100 &&
                            Math.abs(jugador.getY() - t.getY()) < 50) {
                        estadoActual = ESTADO_DIALOGO_TIENDA;
                        traderActivo = t;
                        t.setEstado(Trader.DIALOGUE);
                        opcionTiendaActiva = 0;
                        indiceDialogoTrader = 0;
                        pararMovimientoJugador();
                        return;
                    }
                }
            }

        } else if (estadoActual == ESTADO_TIENDA) {
            estadoActual = ESTADO_JUEGO;
            if (traderActivo != null && traderActivo.getEstadoActual() != Trader.APPROVAL) {
                traderActivo.setEstado(Trader.IDLE);
            }
            traderActivo = null;
        }
    }

    private void pararMovimientoJugador() {
        up = false;
        down = false;
        left = false;
        right = false;
        if (jugador != null)
            jugador.pararMovimiento();
    }

    public void toggleFullScreen() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame == null)
            return;
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        frame.dispose();
        enPantallaCompleta = !enPantallaCompleta;
        if (enPantallaCompleta) {
            frame.setUndecorated(true);
            if (gd.isFullScreenSupported())
                gd.setFullScreenWindow(frame);
            else
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            gd.setFullScreenWindow(null);
            frame.setUndecorated(false);
            frame.setSize(1280, 760);
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.NORMAL);
        }
        frame.setVisible(true);
        this.requestFocus();
    }

    private void actualizarIntro() {

        introAniTick++;
        if (introAniTick >= 5) {
            introAniTick = 0;
            introAniIndex++;

            if (introAniIndex >= 10)
                introAniIndex = 0;
        }

        if (faseIntro == 0) {
            jugador.moverAutomatico(false, false, false, true);
            estadoVillanoIntro = 0;
            estadoHermanoIntro = 0;
            if (jugador.getX() >= 200) {
                jugador.pararMovimiento();
                jugador.moverAutomatico(false, false, false, false);
                faseIntro = 1;
            }
        } else if (faseIntro == 1) {
            jugador.moverAutomatico(false, false, false, false);
            estadoVillanoIntro = 2;
            estadoHermanoIntro = 1;
            villanoMirandoIzq = true;

            if (villanoX > 900) {
                villanoX -= 4;
                hermanoX = villanoX + 120;
            } else {
                faseIntro = 2;
                mostrarDialogo = true;
                introAniIndex = 0;
            }
        } else if (faseIntro == 2) {
            jugador.moverAutomatico(false, false, false, false);
            estadoVillanoIntro = 0;
            estadoHermanoIntro = 0;
        } else if (faseIntro == 3) {
            estadoVillanoIntro = 2;
            estadoHermanoIntro = 1;
            villanoMirandoIzq = false;

            villanoX += 7;
            hermanoX += 7;

            if (villanoX > 1600) {

                estadoActual = ESTADO_JUEGO;
                up = false;
                down = false;
                left = false;
                right = false;
            }
        }
        jugador.actualizar();
    }

    private void actualizarCinematicaODialogo() {
        if (estadoActual == ESTADO_CINEMATICA_HORDA) {
            if (faseCinematicaHorda == 0) {

                int targetY = 540;
                boolean requiereHaciaAbajo = jugador.getY() < targetY - 4;
                boolean requiereHaciaArriba = jugador.getY() > targetY + 4;
                boolean requiereDerecha = jugador.getX() < cinematicTargetX;

                if (requiereDerecha || requiereHaciaAbajo || requiereHaciaArriba) {
                    jugador.moverAutomatico(requiereHaciaArriba, requiereHaciaAbajo, false, requiereDerecha);
                } else {
                    jugador.moverAutomatico(false, false, false, false);
                    faseCinematicaHorda = 1;
                }

            } else if (faseCinematicaHorda == 1) {

                int repulsorCamaraDerecha = cinematicTargetX - 300;
                if (cameraX < repulsorCamaraDerecha) {
                    cameraX += Math.max(3, (repulsorCamaraDerecha - cameraX) / 14);
                }

                if (Math.abs(repulsorCamaraDerecha - cameraX) < 15) {
                    faseCinematicaHorda = 2;
                }

            } else if (faseCinematicaHorda == 2) {

                boolean todosLlegaron = true;
                for (EnemigoBase e : enemigos) {
                    e.actualizarMarchaCinematica();
                    if (!e.isLlegoMetaCinematica()) {
                        todosLlegaron = false;
                    }
                }

                if (todosLlegaron) {
                    faseCinematicaHorda = 4;
                }

            } else if (faseCinematicaHorda == 4) {

                int h = spawner.getHordaActual();

                if (h == 0 || h == 5 || h == 8) {
                    estadoActual = ESTADO_DIALOGO_HORDA;
                    faseDialogoHorda = 0;
                } else {
                    jugador.setVelocidadCinematica(4.0f);
                    estadoActual = ESTADO_JUEGO;
                }
            }
        } else if (estadoActual == ESTADO_CINEMATICA_BOSS) {
            int targetX = 200;
            if (jugador.getX() < targetX) {
                jugador.moverAutomatico(false, false, false, true);
            } else {
                jugador.moverAutomatico(false, false, false, false);
                jugador.setX(targetX);
                estadoActual = ESTADO_DIALOGO_BOSS;
                faseDialogoHorda = 0;
            }
        } else {

            jugador.moverAutomatico(false, false, false, false);
        }

        jugador.actualizar();

        for (EnemigoBase e : enemigos) {
            if (faseCinematicaHorda != 2) {
                e.actualizar();
            } else {

                e.actualizar();
            }
        }
    }

    private void gestionarSonidosEnemigos() {
        if (estadoActual == ESTADO_MENU || estadoActual == ESTADO_PAUSA || consolaAbierta) {
            GestorSonidos.detener(GestorSonidos.RUN_ESQUELETO);
            GestorSonidos.detener(GestorSonidos.RUN_NIGHTBORNE);
            return;
        }

        boolean esqueletoCorre = false;
        boolean eliteCorre = false;
        boolean nightborneCorre = false;

        if (enemigos != null) {
            for (EnemigoBase e : enemigos) {
                if (!e.isMuerto()) {
                    if (e instanceof EnemigoEsqueleto && e.getEstadoActual() == EnemigoEsqueleto.WALK) {
                        esqueletoCorre = true;
                    } else if (e instanceof EnemigoEsqueletoElite && e.getEstadoActual() == 2) {
                        eliteCorre = true;
                    } else if (e instanceof EnemigoNightBorne && e.getEstadoActual() == EnemigoNightBorne.RUN) {
                        nightborneCorre = true;
                    }
                }
            }
        }

        if (estadoActual == ESTADO_INTRO && estadoVillanoIntro == 2) {
            nightborneCorre = true;
        }

        if (esqueletoCorre || eliteCorre)
            GestorSonidos.reproducirenLoop(GestorSonidos.RUN_ESQUELETO);
        else
            GestorSonidos.detener(GestorSonidos.RUN_ESQUELETO);

        if (nightborneCorre)
            GestorSonidos.reproducirenLoop(GestorSonidos.RUN_NIGHTBORNE);
        else
            GestorSonidos.detener(GestorSonidos.RUN_NIGHTBORNE);
    }

    private void actualizarJuego() {
        if (hitStopTicks > 0) {
            hitStopTicks--;
            return;
        }
        if (shakeTicks > 0)
            shakeTicks--;

        jugador.mover(up, down, left, right);
        if (jugador.isAtacando()) {
            colisiones.verificarGolpeContinuo(jugador, enemigos, textosDano);
        }

        if (!spawner.isHordaActiva() && estadoActual != ESTADO_BOSS_FIGHT) {
            int cameraTarget = jugador.getX() - PUNTO_SCROLL;

            if (portalJefe != null) {
                int maxCam = portalJefe.getHitbox().x - (ANCHO / 2);
                if (cameraTarget > maxCam) {
                    cameraTarget = maxCam;
                }
            }
            if (cameraTarget > cameraX) {
                int diff = cameraTarget - cameraX;
                if (diff <= 20) {

                    cameraX = cameraTarget;
                } else {

                    cameraX += Math.max(6, diff / 15);
                }
            }
        }
        if (cameraX < 0)
            cameraX = 0;

        if (estadoActual != ESTADO_BOSS_FIGHT && jugador.getX() < cameraX)
            jugador.setX(cameraX);

        if (spawner.isHordaActiva()) {
            int limiteIzqHorda = cameraX + 50;

            int limiteDerecho = cameraX + ANCHO - 50;

            if (jugador.getX() < limiteIzqHorda)
                jugador.setX(limiteIzqHorda);
            if (jugador.getX() > limiteDerecho)
                jugador.setX(limiteDerecho);
        } else if (estadoActual != ESTADO_BOSS_FIGHT && portalJefe != null) {

            int limiteDerechoPortal = cameraX + ANCHO - 50;
            if (jugador.getX() > limiteDerechoPortal) {
                jugador.setX(limiteDerechoPortal);
            }
        } else if (estadoActual == ESTADO_BOSS_FIGHT) {

            if (jugador.getX() < 0)
                jugador.setX(0);
            if (jugador.getX() > 1120)
                jugador.setX(1120);
        }

        spawner.actualizar(jugador, getWidth(), cameraX);
        jugador.actualizar();

        int h = spawner.getHordaActual();
        if (spawner.isHordaActiva() && ultimaHordaDialogada != h) {
            ultimaHordaDialogada = h;
            estadoActual = ESTADO_CINEMATICA_HORDA;
            faseCinematicaHorda = 1;
            faseDialogoHorda = 0;
            cinematicTargetX = jugador.getX();
            pararMovimientoJugador();

            spawner.setCameraArena(cameraX, getWidth());
        }

        if (barraJefeNivel != null) {
            barraJefeNivel.actualizar();
        }

        boolean enemigoMuertoEsteFrame = false;
        Iterator<EnemigoBase> it = enemigos.iterator();
        while (it.hasNext()) {
            EnemigoBase e = it.next();

            if (spawner.isHordaActiva() && e.isLlegoMetaCinematica()) {
                e.setLimitesCamara(cameraX + 50, cameraX + ANCHO - 50);
            } else {
                e.setLimitesCamara(Integer.MIN_VALUE, Integer.MAX_VALUE);
            }

            if (jugador.getVida() > 0 && !spawner.isHordaDurmiente() && !e.isSpawning()) {
                e.actualizarIA(jugador);
            }
            e.actualizar();

            e.actualizarInvulnerabilidad();

            if (spawner.isHordaActiva() && e.isLlegoMetaCinematica()) {
                e.clampearEnPantalla(cameraX + 50, cameraX + ANCHO - 50);
            }

            if (e.isMuerto()) {
                if (!e.isLootSoltado()) {
                    e.setLootSoltado(true);
                    GestorContadores.get().registrarEnemigoDerrotado();

                    if (e instanceof EnemigoJefeDemonio) {

                    } else {
                        int dropX = e.getHitbox().x + (e.getHitbox().width / 2);
                        int dropY = e.getHitbox().y;

                        int piesEnemigo = e.getHitbox().y + e.getHitbox().height;
                        int sueloBase = Math.max(445, Math.min(580, piesEnemigo));

                        if (Math.random() < 0.5) {
                            int cantidad = 2 + (int) (Math.random() * 3);
                            for (int i = 0; i < cantidad; i++) {
                                objetosSuelo.add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_MONEDA,
                                        sueloBase + (int) (Math.random() * 40 - 20)));
                            }
                        }
                        int randP = (int) (Math.random() * 100);
                        if (randP < 15)
                            objetosSuelo
                                    .add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_VIDA,
                                            sueloBase));
                        else if (randP < 30)
                            objetosSuelo
                                    .add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_FUERZA,
                                            sueloBase));
                        else if (randP < 45)
                            objetosSuelo.add(
                                    new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_VELOCIDAD,
                                            sueloBase));
                    }
                }

                if (e.isAnimacionMuerteTerminada()) {

                    if (e instanceof EnemigoJefeDemonio) {
                        cambiarEstado(ESTADO_VICTORIA);
                        juegoActivo = false;
                        it.remove();
                        continue;
                    }
                    jugador.ganarXP(e.getXpQueDa());

                    textosDano.add(new TextoDano(e.getX(), e.getY() - 40, "+" + e.getXpQueDa() + " XP",
                            new Color(160, 32, 240)));
                    it.remove();
                    enemigoMuertoEsteFrame = true;
                }
                continue;
            }

            if (!spawner.isHordaActiva() && e.getX() < cameraX - 200) {
                it.remove();
                continue;
            }

            colisiones.manejarColisionAtaqueEnemigo(jugador, e, textosDano);

            if (jugador.getEstado() == Jugador.Estado.MUERTO)
                juegoActivo = false;
        }

        if (enemigoMuertoEsteFrame) {

            spawner.notificarMuerteEnemigo((int) jugador.getX(), cameraX, ANCHO);
        }

        jugador.actualizarBuffs();

        gestorHabilidades.actualizar(jugador, proyectiles);

        colisiones.actualizarProyectilesYColisiones(jugador, proyectiles, enemigos, textosDano);

        colisiones.actualizarObjetosSueloYColisiones(jugador, objetosSuelo, textosDano);

        for (Trader t : traders) {
            t.actualizar(jugador, cameraX);
        }

        if (portalJefe != null) {
            portalJefe.actualizar();
        }

        Iterator<TextoDano> itTexto = textosDano.iterator();
        while (itTexto.hasNext()) {
            TextoDano td = itTexto.next();
            td.actualizar();
            if (!td.isActivo())
                itTexto.remove();
        }
    }

    private void agregarAlLog(String msg) {
        historialLogs.add(msg);
        if (historialLogs.size() > 10)
            historialLogs.remove(0);
    }

    private void aplicarSugerencia(String sugerencia) {
        String base = comandoAntesDeTab;
        int ultimoEspacio = base.lastIndexOf(" ");
        if (ultimoEspacio == -1) {

            textoConsola.setLength(0);
            textoConsola.append(sugerencia);
        } else {

            String prefijo = base.substring(0, ultimoEspacio + 1);
            textoConsola.setLength(0);
            textoConsola.append(prefijo).append(sugerencia);
        }
        cursorConsola = textoConsola.length();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // --- OPTIMIZACIÓN DE RENDIMIENTO PANTALLA COMPLETA ---
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // -----------------------------------------------------

        if (this.getKeyListeners().length == 0) {
            this.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyTyped(java.awt.event.KeyEvent e) {
                    if (consolaAbierta) {
                        char c = e.getKeyChar();
                        if (c >= 32 && c <= 126) {
                            textoConsola.insert(cursorConsola, c);
                            cursorConsola++;
                            sugerenciasActivas.clear();
                        }
                    }
                }

                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (consolaAbierta) {
                        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                            if (cursorConsola > 0) {
                                textoConsola.deleteCharAt(cursorConsola - 1);
                                cursorConsola--;
                                sugerenciasActivas.clear();
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                            if (cursorConsola < textoConsola.length()) {
                                textoConsola.deleteCharAt(cursorConsola);
                                sugerenciasActivas.clear();
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            String cmd = textoConsola.toString();
                            if (!cmd.isEmpty()) {
                                historialComandos.add(cmd);
                                indiceHistorial = historialComandos.size();
                                String resultado = GestorComandos.ejecutar(cmd, GamePanel.this);
                                agregarAlLog("> " + cmd);
                                if (!resultado.isEmpty())
                                    agregarAlLog(resultado);
                                textoConsola.setLength(0);
                                cursorConsola = 0;
                                sugerenciasActivas.clear();
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                            if (sugerenciasActivas.isEmpty()) {
                                comandoAntesDeTab = textoConsola.toString();
                                sugerenciasActivas = GestorComandos.getListaSugerencias(comandoAntesDeTab);
                                indiceSugerencia = 0;
                            } else {
                                indiceSugerencia = (indiceSugerencia + 1) % sugerenciasActivas.size();
                            }
                            if (!sugerenciasActivas.isEmpty()) {
                                aplicarSugerencia(sugerenciasActivas.get(indiceSugerencia));
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                            if (cursorConsola > 0)
                                cursorConsola--;
                        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            if (cursorConsola < textoConsola.length())
                                cursorConsola++;
                        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                            if (!historialComandos.isEmpty()) {
                                if (indiceHistorial == -1)
                                    indiceHistorial = historialComandos.size() - 1;
                                else if (indiceHistorial > 0)
                                    indiceHistorial--;
                                textoConsola.setLength(0);
                                textoConsola.append(historialComandos.get(indiceHistorial));
                                cursorConsola = textoConsola.length();
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            if (!historialComandos.isEmpty() && indiceHistorial != -1) {
                                if (indiceHistorial < historialComandos.size() - 1) {
                                    indiceHistorial++;
                                    textoConsola.setLength(0);
                                    textoConsola.append(historialComandos.get(indiceHistorial));
                                    cursorConsola = textoConsola.length();
                                } else {
                                    indiceHistorial = -1;
                                    textoConsola.setLength(0);
                                    cursorConsola = 0;
                                }
                            }
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            consolaAbierta = false;
                        }
                    }
                }
            });
        }

        double escalaX = (double) getWidth() / 1280;
        double escalaY = (double) getHeight() / ALTO;
        g2.scale(escalaX, escalaY);

        if (estadoActual == ESTADO_MENU) {
            dibujarMenu(g2);
        } else if (estadoActual == ESTADO_INTRO) {
            dibujarIntro(g2);
        } else if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT || estadoActual == ESTADO_PAUSA
                || estadoActual == ESTADO_DIALOGO_HORDA
                || estadoActual == ESTADO_CINEMATICA_HORDA || estadoActual == ESTADO_TIENDA
                || estadoActual == ESTADO_CINEMATICA_BOSS || estadoActual == ESTADO_DIALOGO_BOSS
                || estadoActual == ESTADO_DIALOGO_TIENDA || estadoActual == ESTADO_VICTORIA) {
            int offsetX = 0;
            int offsetY = 0;
            if (shakeTicks > 0) {

                double decay = (double) shakeTicks / maxShakeTicks;
                offsetX = (int) (Math.sin(shakeTicks * 1.5) * shakeIntensity * decay);
                offsetY = (int) (Math.cos(shakeTicks * 1.2) * shakeIntensity * decay);
                g2.translate(offsetX, offsetY);
            }

            dibujarJuego(g2, g);

            if (estadoActual == ESTADO_DIALOGO_HORDA) {
                dibujarDialogoHorda(g2);
            }

            if (estadoActual == ESTADO_DIALOGO_TIENDA) {
                dibujarDialogoTrader(g2);
            }

            if (estadoActual == ESTADO_DIALOGO_BOSS) {
                dibujarDialogoBoss(g2);
            }

            if (shakeTicks > 0) {
                g2.translate(-offsetX, -offsetY);
            }

            if (!juegoActivo && estadoActual != ESTADO_VICTORIA && estadoSiguiente != ESTADO_VICTORIA) {

                g2.setColor(new Color(50, 0, 0, 180));
                g2.fillRect(0, 0, ANCHO, ALTO);

                g2.setFont(new Font("Serif", Font.BOLD, 100));
                g2.setColor(Color.RED);
                String msgMuerte = "HAS MUERTO";
                g2.drawString(msgMuerte, (1280 - g2.getFontMetrics().stringWidth(msgMuerte)) / 2, 350);

                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g2.setFont(new Font("Arial", Font.BOLD, 30));
                    g2.setColor(Color.WHITE);
                    String msgEnter = "PRESIONA ENTER PARA VOLVER AL MENU";
                    g2.drawString(msgEnter, (1280 - g2.getFontMetrics().stringWidth(msgEnter)) / 2, 500);
                }
            }

            if (estadoActual == ESTADO_VICTORIA) {
                dibujarPantallaVictoria(g2);
            }
        }

        if (enTransicion) {
            Composite original = g2.getComposite();

            float alphaSeguro = Math.max(0f, Math.min(1f, alphaTransicion));

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaSeguro));
            g2.setColor(Color.BLACK);

            g2.fillRect(0, 0, ANCHO, ALTO);

            g2.setComposite(original);
        }

        g2.dispose();
    }

    private void dibujarDialogoHorda(Graphics2D g2) {

        int pW = 800;
        int pH = 150;
        int pX = (1280 - pW) / 2;
        int pY = 720 - pH - 30;

        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(pX, pY, pW, pH, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(pX, pY, pW, pH, 20, 20);

        String texto = "";
        String hablador = "";
        BufferedImage retrato = null;

        if (faseDialogoHorda == 0) {
            hablador = "Caballero";
            texto = "¡Quítense del camino, no tengo tiempo que perder!";
            retrato = GestorRecursos.faceCaballero;
        } else {
            int h = spawner.getHordaActual();
            if (h == 0) {
                hablador = "Slime Verde";
                texto = "¡No nos subestimes, te mataremos por tu insolencia!";
                retrato = GestorRecursos.faceSlime;
            } else if (h == 5) {
                hablador = "Esqueleto";
                texto = "¡Los vivos no son bienvenidos aquí!";
                retrato = GestorRecursos.faceEsqueleto;
            } else if (h == 8) {
                hablador = "Esqueleto Élite";
                texto = "¡Tu viaje termina ahora, mortal!";
                retrato = GestorRecursos.faceEsqueletoElite;
            }
        }

        if (retrato != null) {
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(pX + 20, pY + 15, 120, 120, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawRoundRect(pX + 20, pY + 15, 120, 120, 10, 10);

            g2.drawImage(retrato, pX + 22, pY + 17, 116, 116, null);
        }

        g2.setFont(new Font("Serif", Font.BOLD, 28));
        g2.setColor(Color.YELLOW);
        g2.drawString(hablador, pX + 160, pY + 45);

        g2.setFont(new Font("Serif", Font.PLAIN, 24));
        g2.setColor(Color.WHITE);
        g2.drawString(texto, pX + 160, pY + 95);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(Color.LIGHT_GRAY);
            String ind = "ENTER ->";
            g2.drawString(ind, pX + pW - g2.getFontMetrics().stringWidth(ind) - 20, pY + pH - 15);
        }
    }

    private void dibujarDialogoBoss(Graphics2D g2) {
        int pW = 800;
        int pH = 150;
        String hablador = "";
        String texto = "";
        java.awt.image.BufferedImage retrato = null;

        if (faseDialogoHorda == 0) {
            hablador = "Caballero";
            texto = "¡Prepárate bestia, este es tu final!";
            retrato = GestorRecursos.faceCaballero;
        } else if (faseDialogoHorda == 1) {
            hablador = "Rey Demonio";
            texto = "Ingenuo mortal, arderás en las llamas eternas...";
            retrato = GestorRecursos.faceBoss;
        }

        int pX = (1280 - pW) / 2;
        int pY = 720 - pH - 40;

        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(pX, pY, pW, pH, 20, 20);
        g2.setColor(new Color(150, 0, 0));
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.drawRoundRect(pX, pY, pW, pH, 20, 20);

        if (retrato != null) {
            g2.drawImage(retrato, pX + 20, pY + 15, 120, 120, null);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new java.awt.Font("Arial", Font.BOLD, 22));
        g2.drawString(hablador, pX + 160, pY + 45);

        g2.setFont(new java.awt.Font("Arial", Font.PLAIN, 18));
        g2.drawString(texto, pX + 160, pY + 95);

        if ((System.currentTimeMillis() / 400) % 2 == 0) {
            g2.setFont(new java.awt.Font("Arial", Font.BOLD, 14));
            g2.setColor(Color.YELLOW);
            g2.drawString("[ENTER] Continuar", pX + pW - 140, pY + pH - 15);
        }
    }

    private void dibujarDialogoTrader(Graphics2D g2) {

        int pW = 800;
        int pH = 150;
        int pX = (1280 - pW) / 2;
        int pY = 720 - pH - 30;

        g2.setColor(new Color(0, 0, 0, 225));
        g2.fillRoundRect(pX, pY, pW, pH, 20, 20);
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(pX, pY, pW, pH, 20, 20);

        if (GestorRecursos.faceNpc != null) {
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(pX + 20, pY + 15, 120, 120, 10, 10);
            g2.drawImage(GestorRecursos.faceNpc, pX + 22, pY + 17, 116, 116, null);
        }

        g2.setFont(new Font("Serif", Font.BOLD, 28));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("David", pX + 160, pY + 45);

        g2.setFont(new Font("Serif", Font.PLAIN, 24));
        g2.setColor(Color.WHITE);

        if (indiceDialogoTrader >= 0 && indiceDialogoTrader < dialogosTrader.length) {
            String msg = dialogosTrader[indiceDialogoTrader];
            g2.drawString(msg, pX + 160, pY + 95);
        }

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(Color.LIGHT_GRAY);
            String ind = "ENTER";
            g2.drawString(ind, pX + pW - g2.getFontMetrics().stringWidth(ind) - 20, pY + pH - 15);
        }
    }

    private void dibujarMenu(Graphics2D g2) {

        for (CapaFondo c : capasParallax)
            c.dibujar(g2, menuScrollX);

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, ANCHO, ALTO);

        g2.setColor(Color.WHITE);

        if (GestorRecursos.tituloImg != null) {

            double escalaTitulo = 0.6;

            int imgW = (int) (GestorRecursos.tituloImg.getWidth() * escalaTitulo);
            int imgH = (int) (GestorRecursos.tituloImg.getHeight() * escalaTitulo);

            int xTitulo = (1280 - imgW) / 2;
            int yTitulo = -55;

            g2.drawImage(GestorRecursos.tituloImg, xTitulo, yTitulo, imgW, imgH, null);

        } else {

            g2.setFont(fuenteTitulo);
            g2.setColor(Color.GRAY.darker());
            g2.drawString("BROTHER QUEST", 305, 155);
            g2.setColor(new Color(200, 200, 200));
            g2.drawString("BROTHER QUEST", 300, 150);
        }

        if (subEstadoMenu == 0) {
            int bW = 400;
            int espaciado = 55;
            int margenInferior = 80;
            int bX = (1280 - bW) / 2;
            int startY = 720 - margenInferior - (opcionesMenu.length * espaciado);

            g2.setFont(new Font("Arial", Font.BOLD, 28));
            for (int i = 0; i < opcionesMenu.length; i++) {
                int yPos = startY + (i * espaciado);
                boolean seleccionado = (i == seleccionMenu);

                if (seleccionado) {

                    g2.setColor(new Color(255, 255, 0, 30));
                    g2.fillRoundRect(bX, yPos - 30, bW, 40, 10, 10);

                    g2.setColor(Color.YELLOW);
                } else {
                    g2.setColor(Color.WHITE);
                }

                String txt = opcionesMenu[i];
                int txtW = g2.getFontMetrics().stringWidth(txt);

                if (seleccionado) {
                    g2.drawString("> ", bX + (bW - txtW) / 2 - 30, yPos);
                }
                g2.drawString(txt, bX + (bW - txtW) / 2, yPos);
            }
        } else {

            dibujado.dibujarMenuPausa(g2, seleccionPausa, 1);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("F11: Pantalla Completa", 1100, 700);
    }

    private void dibujarIntro(Graphics2D g2) {

        for (CapaFondo c : capasParallax)
            c.dibujar(g2, 0);

        g2.setColor(new Color(0, 0, 0, 100));

        g2.fillOval(jugador.getX() - 11, jugador.getY(), 60, 20);

        jugador.dibujar(g2, 0);

        if (faseIntro >= 1) {
            int frameVillano = introAniIndex % (estadoVillanoIntro == 0 ? 4 : 6);
            int frameHermano = introAniIndex % (estadoHermanoIntro == 0 ? 5 : 6);

            BufferedImage imgVillano = null;
            BufferedImage imgHermano = null;

            if (GestorRecursos.animacionesNightBorne != null
                    && estadoVillanoIntro >= 0
                    && estadoVillanoIntro < GestorRecursos.animacionesNightBorne.length
                    && GestorRecursos.animacionesNightBorne[estadoVillanoIntro] != null
                    && frameVillano >= 0
                    && frameVillano < GestorRecursos.animacionesNightBorne[estadoVillanoIntro].length) {
                imgVillano = GestorRecursos.animacionesNightBorne[estadoVillanoIntro][frameVillano];
            }

            if (GestorRecursos.animacionesHermano != null
                    && estadoHermanoIntro >= 0
                    && estadoHermanoIntro < GestorRecursos.animacionesHermano.length
                    && GestorRecursos.animacionesHermano[estadoHermanoIntro] != null
                    && frameHermano >= 0
                    && frameHermano < GestorRecursos.animacionesHermano[estadoHermanoIntro].length) {
                imgHermano = GestorRecursos.animacionesHermano[estadoHermanoIntro][frameHermano];
            }

            int anchoV = 300, altoV = 300;
            int vDrawX = villanoX - 120;
            int vDrawY = villanoY - 135;

            int anchoH = 240, altoH = 240;
            int hDrawX = hermanoX;
            int hDrawY = villanoY - 100;

            g2.setColor(new Color(0, 0, 0, 100));

            if (imgHermano != null) {
                g2.fillOval(hDrawX + 90, hDrawY + 170, 60, 20);
            }

            if (imgVillano != null) {
                g2.fillOval(vDrawX + 95, vDrawY + 222, 115, 30);
            }

            if (imgHermano != null) {
                if (villanoMirandoIzq)
                    g2.drawImage(imgHermano, hDrawX + anchoH, hDrawY, -anchoH, altoH, null);
                else
                    g2.drawImage(imgHermano, hDrawX, hDrawY, anchoH, altoH, null);
            }

            if (imgVillano != null) {
                if (villanoMirandoIzq)
                    g2.drawImage(imgVillano, vDrawX + anchoV, vDrawY, -anchoV, altoV, null);
                else
                    g2.drawImage(imgVillano, vDrawX, vDrawY, anchoV, altoV, null);
            }
        }

        if (mostrarDialogo) {
            int boxX = 200, boxY = 550, boxW = 880, boxH = 150;
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.RED);
            g2.drawString("NIGHTBORNE", boxX + 30, boxY + 40);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            g2.drawString(textoDialogoLinea1, boxX + 30, boxY + 80);
            g2.drawString(textoDialogoLinea2, boxX + 30, boxY + 110);

            if ((System.currentTimeMillis() / 400) % 2 == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString("PRESIONA ENTER >", boxX + boxW - 180, boxY + boxH - 20);
            }
        }
    }

    private void dibujarJuego(Graphics2D g2, Graphics g) {
        dibujado.dibujarJuego(
                g2,
                g,
                estadoActual == ESTADO_VICTORIA ? (int) victoriaCamX : cameraX,
                capasParallax,
                jugador,
                enemigos,
                objetosSuelo,
                textosDano,
                proyectiles,
                barraJefeNivel,
                traders);

        if (portalJefe != null && estadoActual == ESTADO_JUEGO
                && portalJefe.getHitbox().intersects(jugador.getBounds())) {
            portalJefe.dibujarInteraccionNpc(g2, cameraX);
        }

        for (Trader t : traders) {

            if (estadoActual == ESTADO_JUEGO && !spawner.isHordaActiva()
                    && Math.abs(jugador.getXLogical() - t.getXLogico(cameraX)) < 100
                    && Math.abs(jugador.getY() - t.getY()) < 50) {

                dibujado.dibujarIndicadorG(g2, (int) t.getXLogico(cameraX) - cameraX + 30, t.hitbox.y - 15);
            }
        }

        if (consolaAbierta) {
            dibujado.dibujarConsola(g2, textoConsola.toString(), ultimoResultado);
        }

        if (estadoActual == ESTADO_PAUSA) {
            dibujado.dibujarMenuPausa(g2, seleccionPausa, subEstadoPausa);
        } else if (estadoActual == ESTADO_TIENDA) {
            dibujarTienda(g2);
        }

        if (debugActivado) {
            int limA = getLimiteArribaGlobal();
            int limB = getLimiteAbajoGlobal();
            g2.setColor(new java.awt.Color(255, 0, 0, 180));
            g2.setStroke(new java.awt.BasicStroke(2f));

            g2.drawLine(0, limA, 1280, limA);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            g2.drawString("limA=" + limA, 10, limA - 4);

            g2.setColor(new java.awt.Color(255, 100, 0, 180));
            g2.drawLine(0, limB, 1280, limB);
            g2.drawString("limB=" + limB, 10, limB + 14);

            g2.setColor(java.awt.Color.CYAN);
            g2.drawString("jugadorY=" + jugador.getY() + "  cameraX=" + cameraX + "  estado=" + estadoActual, 10, 20);
        }
    }

    private void dibujarTienda(Graphics2D g2) {

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, 1280, 720);

        int menuW = 750;
        int menuH = 480;
        int menuX = (1280 - menuW) / 2;
        int menuY = (720 - menuH) / 2;

        g2.setColor(new Color(20, 20, 20, 240));
        g2.fillRoundRect(menuX, menuY, menuW, menuH, 20, 20);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(menuX, menuY, menuW, menuH, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 45));
        g2.setColor(Color.ORANGE);
        String titulo = "TIENDA DE DAVID";
        int tX = menuX + (menuW - g2.getFontMetrics().stringWidth(titulo)) / 2;
        g2.drawString(titulo, tX, menuY + 60);

        g2.setColor(new Color(255, 165, 0, 100));
        g2.drawLine(menuX + 50, menuY + 90, menuX + menuW - 50, menuY + 90);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        String txtOro = "" + jugador.getOro();
        int oroX = menuX + menuW - g2.getFontMetrics().stringWidth(txtOro) - 50;
        g2.drawString(txtOro, oroX, menuY + 125);
        if (GestorRecursos.monedaImg != null) {
            g2.drawImage(GestorRecursos.monedaImg, oroX - 25, menuY + 105, 20, 20, null);
        }

        int py = menuY + 180;
        for (int i = 0; i < itemsTienda.length; i++) {
            boolean seleccionado = (i == opcionTiendaActiva);

            BufferedImage icon = null;
            if (i == 0)
                icon = GestorRecursos.logoVidaImg;
            else if (i == 1)
                icon = GestorRecursos.logoDanoImg;
            else if (i == 2)
                icon = GestorRecursos.pocCuracionImg;
            else if (i == 3)
                icon = GestorRecursos.pocFuerzaImg;
            else if (i == 4)
                icon = GestorRecursos.pocVelocidadImg;

            if (seleccionado) {

                g2.setColor(new Color(255, 255, 0, 30));
                g2.fillRoundRect(menuX + 40, py - 30, menuW - 80, 40, 10, 10);

                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("Arial", Font.BOLD, 24));
                g2.drawString("> ", menuX + 50, py);
                if (icon != null)
                    g2.drawImage(icon, menuX + 75, py - 24, 30, 30, null);
                g2.drawString(itemsTienda[i].toUpperCase(), menuX + 115, py);
            } else {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.PLAIN, 24));
                if (icon != null)
                    g2.drawImage(icon, menuX + 75, py - 24, 30, 30, null);
                g2.drawString(itemsTienda[i].toUpperCase(), menuX + 115, py);
            }

            if (jugador.getOro() >= costosTienda[i])
                g2.setColor(new Color(0, 255, 100));
            else
                g2.setColor(new Color(255, 80, 80));

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            String precioText = "" + costosTienda[i];
            int pX = menuX + menuW - g2.getFontMetrics().stringWidth(precioText) - 50;
            g2.drawString(precioText, pX, py);
            if (GestorRecursos.monedaImg != null) {
                g2.drawImage(GestorRecursos.monedaImg, pX - 25, py - 20, 20, 20, null);
            }

            py += 48;
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(Color.GRAY);
        String msg = "Usa [W/S] para navegar, [ENTER] para comprar y [ESC] para salir";
        int anchoTxt = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (1280 - anchoTxt) / 2, menuY + menuH - 20);
    }

    @SuppressWarnings("unused")
    private void dibujarHUD(Graphics g) {
        dibujado.dibujarHUD(g, jugador, barraJefeNivel);
    }

    @SuppressWarnings("unused")
    private void dibujarSlot(Graphics2D g2, int x, int y, int size, String tecla, int cant, Color col, float prog) {

    }

    public void cambiarEstado(int nuevoEstado) {
        if (!enTransicion) {
            estadoSiguiente = nuevoEstado;
            enTransicion = true;
            oscureciendo = true;
            alphaTransicion = 0f;
        }
    }

    private void actualizarTransicion() {
        if (!enTransicion)
            return;

        if (oscureciendo) {
            alphaTransicion += velTransicion;
            if (alphaTransicion >= 1.0f) {
                alphaTransicion = 1.0f;
                estadoActual = estadoSiguiente;

                if (estadoActual == ESTADO_MENU) {
                    reiniciarJuego();
                    GestorSonidos.detener(GestorSonidos.AMBIENTE_BOSQUE);
                    GestorSonidos.detener(GestorSonidos.RUN_SONIDO);
                } else if (estadoActual == ESTADO_INTRO) {
                    faseIntro = 0;
                    jugador.setX(-50);
                    jugador.setY(550);
                } else if (estadoActual == ESTADO_JUEGO) {
                    jugador.setX(100);
                    jugador.setY(490);
                    jugador.pararMovimiento();

                    GestorSonidos.reproducirenLoop(GestorSonidos.AMBIENTE_BOSQUE);
                } else if (estadoActual == ESTADO_BOSS_FIGHT) {
                    estadoActual = ESTADO_CINEMATICA_BOSS;

                    cameraX = 0;
                    int limiteArriba = getLimiteArribaGlobal();
                    jugador.setX(-100);
                    jugador.setY(limiteArriba + 30);
                    enemigos.clear();
                    proyectiles.clear();
                    objetosSuelo.clear();

                    jefeFinal = new EnemigoJefeDemonio(900, 300);

                    int bossHPBase = Math.max(2500, jugador.getDano() * 40);
                    int bossDanoBase = Math.max(44, (int) (jugador.getVidaMax() * 0.25f));

                    jefeFinal.setVidaMax(bossHPBase);
                    jefeFinal.setVida(bossHPBase);
                    jefeFinal.setDano(bossDanoBase);

                    if (jefeFinal instanceof EnemigoJefeDemonio) {
                        ((EnemigoJefeDemonio) jefeFinal).setActivo(false);
                    }
                    enemigos.add(jefeFinal);

                    jugador.setVelocidadCinematica(4.0f);
                    GestorSonidos.reproducir(GestorSonidos.BOSS_INTRO);
                }

                oscureciendo = false;
            }
        } else {
            alphaTransicion -= velTransicion;
            if (alphaTransicion <= 0.0f) {
                alphaTransicion = 0.0f;
                enTransicion = false;
            }
        }
    }

    private void reiniciarJuego() {
        initGame();
        juegoActivo = true;
        proyectiles.clear();
        objetosSuelo.clear();
        textosDano.clear();
        GestorContadores.reiniciar();

        cameraX = 0;

        if (estadoActual == ESTADO_JUEGO) {
            GestorSonidos.reproducirenLoop(GestorSonidos.AMBIENTE_BOSQUE);
        }
    }

    public List<Proyectil> getProyectiles() {
        return proyectiles;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public List<EnemigoBase> getEnemigos() {
        return enemigos;
    }

    public boolean isConsolaAbierta() {
        return consolaAbierta;
    }

    public void setConsolaAbierta(boolean b) {
        this.consolaAbierta = b;
    }

    public StringBuilder getTextoConsola() {
        return textoConsola;
    }

    public Portal getPortalJefe() {
        return portalJefe;
    }

    public void setPortalJefe(Portal p) {
        this.portalJefe = p;
    }

    public Spawner getSpawner() {
        return spawner;
    }

    public int getLimiteArribaGlobal() {
        if (estadoActual == ESTADO_BOSS_FIGHT || estadoActual == ESTADO_CINEMATICA_BOSS
                || estadoActual == ESTADO_DIALOGO_BOSS) {
            return 450;
        }
        return 470;
    }

    public int getLimiteAbajoGlobal() {
        if (estadoActual == ESTADO_BOSS_FIGHT || estadoActual == ESTADO_CINEMATICA_BOSS
                || estadoActual == ESTADO_DIALOGO_BOSS) {
            return 555;
        }
        return 610;
    }

    public void setCameraX(int x) {
        if (x < 0)
            x = 0;
        this.cameraX = x;
    }

    public int getCursorConsola() {
        return cursorConsola;
    }

    public boolean isFullScreen() {
        return enPantallaCompleta;
    }

    public void setInputs(boolean u, boolean d, boolean l, boolean r) {
        this.up = u;
        this.down = d;
        this.left = l;
        this.right = r;
    }

    public List<String> getHistorialLogs() {
        return historialLogs;
    }

    public List<String> getSugerenciasActivas() {
        return sugerenciasActivas;
    }

    public int getIndiceSugerencia() {
        return indiceSugerencia;
    }

    public List<Trader> getTraders() {
        return traders;
    }

    public GestorHabilidades getGestorHabilidades() {
        return gestorHabilidades;
    }

    public boolean isEnTransicion() {
        return enTransicion;
    }

    private void comprarItemTienda() {
        if (traderActivo == null)
            return;
        int costo = costosTienda[opcionTiendaActiva];
        if (jugador.getOro() >= costo) {
            jugador.setOro(jugador.getOro() - costo);
            switch (opcionTiendaActiva) {
                case 0:
                    jugador.setVidaMax((int) (jugador.getVidaMax() * 1.20f));
                    jugador.setVida(jugador.getVidaMax());
                    break;
                case 1:
                    jugador.setDano((int) (jugador.getDano() * 1.20f));
                    break;
                case 2:
                    jugador.setPocionesVida(jugador.getPocionesVida() + 1);
                    break;
                case 3:
                    jugador.setPocionesFuerza(jugador.getPocionesFuerza() + 1);
                    break;
                case 4:
                    jugador.setPocionesVelocidad(jugador.getPocionesVelocidad() + 1);
                    break;
            }
            GestorSonidos.reproducir(GestorSonidos.RECOGER_MONEDA);
            traderActivo.setEstado(Trader.APPROVAL);
        } else {
            GestorSonidos.reproducir(GestorSonidos.ERROR);
        }
    }

    private void dibujarPantallaVictoria(Graphics2D g2) {

        g2.setColor(new Color(10, 8, 2, 220));
        g2.fillRect(0, 0, ANCHO, ALTO);

        g2.setColor(new Color(200, 160, 30, 160));
        g2.fillRect(0, 0, ANCHO, 6);
        g2.fillRect(0, ALTO - 6, ANCHO, 6);

        Font fuenteVictoria = new Font("Serif", Font.BOLD, 90);
        g2.setFont(fuenteVictoria);
        String titulo = "VICTORIA";
        int tW = g2.getFontMetrics().stringWidth(titulo);

        g2.setColor(new Color(180, 130, 0, 120));
        g2.drawString(titulo, (ANCHO - tW) / 2 + 4, 124);

        g2.setColor(new Color(255, 215, 60));
        g2.drawString(titulo, (ANCHO - tW) / 2, 120);

        g2.setColor(new Color(200, 160, 30, 180));
        g2.fillRect(ANCHO / 2 - 300, 135, 600, 2);

        Font fuenteCreditos = new Font("Serif", Font.ITALIC, 20);
        g2.setFont(fuenteCreditos);
        g2.setColor(new Color(200, 190, 150));
        String[] creditos = {
                "BROTHER QUEST - CRISTIAN ORTIZ",
                "Dise\u00f1o  \u2022  Arte  \u2022  Programaci\u00f3n",
        };
        int cy = 165;
        for (String linea : creditos) {
            int lW = g2.getFontMetrics().stringWidth(linea);
            g2.drawString(linea, (ANCHO - lW) / 2, cy);
            cy += 24;
        }

        Font fuenteContinua = new Font("Serif", Font.BOLD | Font.ITALIC, 42);
        g2.setFont(fuenteContinua);

        long t = System.currentTimeMillis();
        float alpha = 0.65f + 0.35f * (float) Math.sin(t / 600.0);
        g2.setColor(new Color(1f, 0.9f, 0.4f, alpha));
        String continua = "CONTINUAR\u00c1...";
        int cW = g2.getFontMetrics().stringWidth(continua);
        g2.drawString(continua, (ANCHO - cW) / 2, 240);

        g2.setColor(new Color(200, 160, 30, 100));
        g2.fillRect(ANCHO / 2 - 400, 260, 800, 1);

        dibujarSpritesVictoria(g2);

        GestorContadores gc = GestorContadores.get();
        Font fuenteTitStat = new Font("Arial", Font.BOLD, 18);
        Font fuenteStat = new Font("Arial", Font.PLAIN, 17);

        g2.setFont(fuenteTitStat);
        g2.setColor(new Color(255, 215, 60));
        String tituloStats = "ESTAD\u00cdSTICAS DE PARTIDA";
        int tsW = g2.getFontMetrics().stringWidth(tituloStats);
        g2.drawString(tituloStats, (ANCHO - tsW) / 2, 290);

        Object[][] stats = {
                { "Enemigos derrotados", gc.getEnemigosDerrotados() },
                { "Da\u00f1o total infligido", gc.getDanioInfligido() },
                { "Da\u00f1o total recibido", gc.getDanioRecibido() },
                { "Bloqueos exitosos", gc.getBloqueosExitosos() },
                { "Monedas ganadas", gc.getMonedasGanadas() },
        };

        int sy = 320;
        int colIzq = ANCHO / 2 - 280;
        int colDer = ANCHO / 2 + 280;

        for (Object[] fila : stats) {
            String etiqueta = (String) fila[0];
            String valor = String.valueOf(fila[1]);

            g2.setFont(fuenteStat);
            g2.setColor(new Color(210, 200, 170));
            g2.drawString(etiqueta, colIzq, sy);

            g2.setColor(Color.WHITE);
            int vW = g2.getFontMetrics().stringWidth(valor);
            g2.drawString(valor, colDer - vW, sy);

            g2.setColor(new Color(120, 110, 80, 160));
            int etQ = colIzq + g2.getFontMetrics().stringWidth(etiqueta) + 6;
            g2.drawLine(etQ, sy - 3, colDer - vW - 6, sy - 3);

            sy += 30;
        }

        g2.setColor(new Color(200, 160, 30, 160));
        g2.fillRect(ANCHO / 2 - 280, sy, 560, 1);
        sy += 19;

        Font fuentePuntos = new Font("Arial", Font.BOLD, 22);
        g2.setFont(fuentePuntos);
        g2.setColor(new Color(255, 230, 80));
        String puntuacionLabel = "PUNTOS TOTALES";
        String puntuacionValor = String.valueOf(gc.getPuntuacion()) + " pts";
        g2.drawString(puntuacionLabel, colIzq, sy);
        int pvW = g2.getFontMetrics().stringWidth(puntuacionValor);
        g2.drawString(puntuacionValor, colDer - pvW, sy);

        if ((System.currentTimeMillis() / 600) % 2 == 0) {
            Font fuenteEnter = new Font("Arial", Font.BOLD, 24);
            g2.setFont(fuenteEnter);
            g2.setColor(Color.WHITE);
            String enter = "Presiona  ENTER  para volver al men\u00fa";
            int eW = g2.getFontMetrics().stringWidth(enter);
            g2.drawString(enter, (ANCHO - eW) / 2, ALTO - 40);
        }
    }

    private static final int MAX_SPRITES_VICTORIA = 7;
    private static final int INTERVALO_SPAWN_VICTORIA = 180;

    private void actualizarSpritesVictoria() {

        victoriaCamX += 0.3f;

        timerSpawnVictoria--;
        if (timerSpawnVictoria <= 0 && spritesVictoria.size() < MAX_SPRITES_VICTORIA) {
            timerSpawnVictoria = INTERVALO_SPAWN_VICTORIA;
            int tipo = (int) (Math.random() * 3);
            float spd = 0.9f + (float) (Math.random() * 0.8f);
            float yB = 530 + (float) (Math.random() * 60);
            spritesVictoria.add(new float[] { -120, yB, tipo, 0, 0, 0, spd });
        }

        java.util.Iterator<float[]> it = spritesVictoria.iterator();
        while (it.hasNext()) {
            float[] s = it.next();
            s[0] += s[6];
            s[5] += 0.10f;

            s[4]++;
            int aniSpd = (s[2] < 3) ? 7 : 6;
            int maxFrames = (s[2] < 3) ? 4 : 10;
            if (s[4] >= aniSpd) {
                s[4] = 0;
                s[3] = (s[3] + 1) % maxFrames;
            }
            if (s[0] > ANCHO + 150)
                it.remove();
        }
    }

    private void dibujarSpritesVictoria(Graphics2D g2) {
        for (float[] s : spritesVictoria) {
            int tipo = (int) s[2];
            int aniIndex = (int) s[3];

            float jumpY = (tipo < 3)
                    ? (float) Math.abs(Math.sin(s[5])) * -22f
                    : (float) Math.sin(s[5] * 0.5f) * -3f;

            java.awt.image.BufferedImage img = null;
            if (tipo == 0 && GestorRecursos.animacionesSlime != null) {
                img = GestorRecursos.animacionesSlime[1][aniIndex % 4];
            } else if (tipo == 1 && GestorRecursos.animacionesSlimeAzul != null) {
                img = GestorRecursos.animacionesSlimeAzul[1][aniIndex % 4];
            } else if (tipo == 2 && GestorRecursos.animacionesSlimeRojo != null) {
                img = GestorRecursos.animacionesSlimeRojo[1][aniIndex % 4];
            } else if (tipo == 3 && GestorRecursos.animacionesEsqBlanco != null) {
                img = GestorRecursos.animacionesEsqBlanco[1][aniIndex % 10];
            } else if (tipo == 4 && GestorRecursos.animacionesEsqOro != null) {
                img = GestorRecursos.animacionesEsqOro[1][aniIndex % 10];
            }
            if (img == null)
                continue;

            int drawW = (tipo < 3) ? 72 : 110;
            int drawH = (tipo < 3) ? 72 : 78;
            int drawX = (int) s[0];
            int drawY = (int) (s[1] + jumpY) - drawH;

            java.awt.Composite orig = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, 0.28f));
            g2.drawImage(img, drawX, drawY, drawW, drawH, null);
            g2.setComposite(orig);
        }
    }
}
=======
package com.rpg.juego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel {
    private static final int ANCHO = 1280;
    private static final int ALTO = 720;

    // --- Estados del juego ---
    public static final int ESTADO_MENU = 0;
    public static final int ESTADO_INTRO = 1;
    public static final int ESTADO_JUEGO = 2;

    public int estadoActual = ESTADO_MENU;

    private boolean enPantallaCompleta = true;
    private int cameraX = 0;
    private int menuScrollX = 0;
    private final int PUNTO_SCROLL = ANCHO / 2;

    private Timer timer;
    private Jugador jugador;
    private List<CapaFondo> capasParallax;

    // Lista polimorfica para la gestion de entidades enemigas en pantalla
    private List<EnemigoBase> enemigos;
    private Spawner spawner;
    private BarraJefe barraJefeNivel;

    // --- Variables para la secuencia de introduccion ---
    private int faseIntro = 0;
    private int villanoX = 1400;
    private int villanoY = 480;
    private int hermanoX = 1500;
    private String textoDialogoLinea1 = "¡JAJAJA! ¡Eres tan debil, que no puedes proteger a tu hermano!";
    private String textoDialogoLinea2 = "¡Ahora sera convertido en el monstruo mas poderoso!";
    private boolean mostrarDialogo = false;

    // --- Variables Transiciones ---
    private boolean enTransicion = false;
    private float alphaTransicion = 0f;
    private int estadoSiguiente = -1;
    private boolean oscureciendo = true; // true = yendo a negro, false = aclarando
    private float velTransicion = 0.03f; // Velocidad del fundido (ajusta a tu gusto)

    // --- Variables de animacion de introduccion ---
    private int introAniTick = 0;
    private int introAniIndex = 0;
    private boolean villanoMirandoIzq = true;
    private int estadoVillanoIntro = 2; // 2 = RUN
    private int estadoHermanoIntro = 1; // 1 = WALK

    private boolean up, down, left, right;
    private boolean juegoActivo = true;
    private List<TextoDano> textosDano = new ArrayList<>();
    private List<Object> golpeadosEnEsteAtaque = new ArrayList<>();
    private ArrayList<ObjetoRecogible> objetosSuelo = new ArrayList<>();
    private List<Proyectil> proyectiles = new ArrayList<>();
    private int cargaHabilidad = 300;
    private final int MAX_CARGA = 300; // Capacidad maxima dividida en 3 barras
    private boolean lanzandoPoder = false;
    private boolean dispararProyectilAhora = false;

    private Font fuenteTitulo;

    public static boolean debugActivado = false;

    public GamePanel() {
        this.setDoubleBuffered(true);
        setBackground(Color.BLACK);
        setFocusable(true);

        try { fuenteTitulo = new Font("Serif", Font.BOLD, 80); } catch (Exception e) {}

        GestorRecursos.cargarRecursos();
        initGame();
        initKeyBindings();

        timer = new Timer(16, e -> {
            if (estadoActual == ESTADO_MENU) {
                menuScrollX += 2;
            } else if (estadoActual == ESTADO_INTRO) {
                actualizarIntro();
            } else if (estadoActual == ESTADO_JUEGO && juegoActivo) {
                actualizarJuego();
            }
            // Invocacion del pipeline matematico de transicion de estados
            actualizarTransicion();
            repaint();
        });
        timer.start();
    }

    private void initGame() {
        jugador = new Jugador();
        capasParallax = new ArrayList<>();
        enemigos = new ArrayList<>();
        spawner = new Spawner(enemigos);
        cargarCapas();

        EnemigoMushroom prueba = new EnemigoMushroom(1000, 500);

        prueba.establecerNivel(1);
        prueba.setDano(1);
        prueba.setVida(200);
        prueba.setVidaMax(200);
        enemigos.add(prueba);

        EnemigoEsqueleto esq1 = new EnemigoEsqueleto(1500, 500, "blanco");
        esq1.establecerNivel(1);
        esq1.setDano(1);
        esq1.setVida(100);
        esq1.setVidaMax(100);
        enemigos.add(esq1);

        EnemigoEsqueleto esq2 = new EnemigoEsqueleto(2000, 500, "dorado");
        esq2.establecerNivel(1);
        esq2.setDano(1);
        esq2.setVida(100);
        esq2.setVidaMax(100);
        enemigos.add(esq2);

        EnemigoEsqueletoElite esq3 = new EnemigoEsqueletoElite(2500, 500);
        esq3.establecerNivel(1);
        esq3.setDano(1);
        esq3.setVida(100);
        esq3.setVidaMax(100);
        enemigos.add(esq3);

        EnemigoNightBorne night = new EnemigoNightBorne(3000, 500);
        night.establecerNivel(1);
        night.setDano(1);
        night.setVida(100);
        night.setVidaMax(100);
        enemigos.add(night);

    }

    private void cargarCapas() {
        if (!GestorRecursos.capasFondo.isEmpty()) {
            double escala = 0.33334;
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(6), 0.0, escala, 0));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(5), 0.1, escala, 0));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(4), 0.2, escala, 5));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(3), 0.5, escala, 10));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(2), 0.7, escala, 290));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(1), 0.85, escala, 385));
            capasParallax.add(new CapaFondo(GestorRecursos.capasFondo.get(0), 0.85, escala, 600));
        }
    }

    private void initKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, false), "fullscreen");
        am.put("fullscreen", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { toggleFullScreen(); }
        });

// Tecla para omitir secuencia de introduccion (F2)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, false), "skip_intro");
        am.put("skip_intro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((estadoActual == ESTADO_MENU || estadoActual == ESTADO_INTRO) && !enTransicion) {
                    cambiarEstado(ESTADO_JUEGO); // Transicion suave al saltar intro
                }
            }
        });

        // Tecla para alternar modo depuracion (F3)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, false), "toggle_debug");
        am.put("toggle_debug", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                debugActivado = !debugActivado; // Alterna visualizacion de hitboxes
            }
        });

        // Modificador de estado general (ENTER)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter_action");
        am.put("enter_action", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoActual == ESTADO_MENU && !enTransicion) {
                    cambiarEstado(ESTADO_INTRO);
                } else if (estadoActual == ESTADO_INTRO && mostrarDialogo) {
                    mostrarDialogo = false;
                    faseIntro = 3;
                } else if (estadoActual == ESTADO_JUEGO && !juegoActivo && !enTransicion) {
                    // NUEVO: Si estamos muertos, activamos transicion al menu
                    cambiarEstado(ESTADO_MENU);
                }
            }
        });

        registrarTecla(im, am, KeyEvent.VK_W, "w", true);
        registrarTecla(im, am, KeyEvent.VK_S, "s", true);
        registrarTecla(im, am, KeyEvent.VK_A, "a", true);
        registrarTecla(im, am, KeyEvent.VK_D, "d", true);
        registrarTecla(im, am, KeyEvent.VK_W, "!w", false);
        registrarTecla(im, am, KeyEvent.VK_S, "!s", false);
        registrarTecla(im, am, KeyEvent.VK_A, "!a", false);
        registrarTecla(im, am, KeyEvent.VK_D, "!d", false);

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "saltar");
        am.put("saltar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.saltar(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0, false), "atacar");
        am.put("atacar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (estadoActual == ESTADO_JUEGO && !jugador.isAtacando() && !jugador.isDefendiendo()) jugador.setAtacando(true);
            }
        });

        // Accion de evasion o rodar (R)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "rodar");
        am.put("rodar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoActual == ESTADO_JUEGO) {
                    jugador.rodar();
                }
            }
        });

        // Teclas para navegacion (Q/E) y uso (F) del inventario de pociones
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0, false), "pocion_ant");
        am.put("pocion_ant", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.cambiarPocion(-1); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, false), "pocion_sig");
        am.put("pocion_sig", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.cambiarPocion(1); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "usar_pocion");
        am.put("usar_pocion", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.usarPocionSeleccionada(); }
        });

        // Asignacion de habilidades especiales (1, 2, 3)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false), "hab_1");
        am.put("hab_1", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                // Validacion de estado del juego, enfriamiento e interrupcion de ataques
                if (estadoActual == ESTADO_JUEGO && jugador.habilidad1Lista() && !jugador.isAtacando()) {

                    // Metodo de consumo y validacion de recursos de habilidad
                    if (jugador.usarHabilidad(1)) {
                        jugador.usarCooldownHabilidad1(); // Inicia tiempo de enfriamiento
                        System.out.println("Lanzando habilidad 1 (Gasto 1 barra)");
                        // Logica para instanciar proyectil manejada en la actualizacion principal
                    } else {
                        System.out.println("No hay suficiente energia para la habilidad 1");
                    }
                }
            }
        });

        // Activacion de escudo defensivo (K)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, false), "escudo");
        am.put("escudo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (estadoActual == ESTADO_JUEGO && !jugador.isEscudoRoto()) jugador.setDefendiendo(true);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, true), "soltar_escudo");
        am.put("soltar_escudo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.setDefendiendo(false); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, true), "soltar_escudo");
        am.put("soltar_escudo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (estadoActual == ESTADO_JUEGO) jugador.setDefendiendo(false); }
        });
    }

    private void registrarTecla(InputMap im, ActionMap am, int key, String id, boolean pressed){
        im.put(KeyStroke.getKeyStroke(key, 0, !pressed), id);
        am.put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estadoActual != ESTADO_JUEGO) return;
                switch (key) {
                    case KeyEvent.VK_W: up = pressed; break;
                    case KeyEvent.VK_S: down = pressed; break;
                    case KeyEvent.VK_A: left = pressed; break;
                    case KeyEvent.VK_D: right = pressed; break;
                }
            }
        });
    }

    private void toggleFullScreen() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame == null) return;
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        frame.dispose();
        enPantallaCompleta = !enPantallaCompleta;
        if (enPantallaCompleta) {
            frame.setUndecorated(true);
            if (gd.isFullScreenSupported()) gd.setFullScreenWindow(frame);
            else frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            gd.setFullScreenWindow(null);
            frame.setUndecorated(false);
            frame.setSize(1280, 760);
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.NORMAL);
        }
        frame.setVisible(true);
        this.requestFocus();
    }

    private void actualizarIntro() {
        // 1. Gestion de velocidad y duracion de frames en la introduccion
        introAniTick++;
        if (introAniTick >= 5) {
            introAniTick = 0;
            introAniIndex++;
            // Modulo de seguridad para evitar desbordamiento
            if (introAniIndex >= 10) introAniIndex = 0;
        }

        // 2. Logica de desplazamiento y estados de las entidades
        if (faseIntro == 0) {
            jugador.moverAutomatico(false, false, false, true);
            estadoVillanoIntro = 0; // Estado IDLE (Jefe)
            estadoHermanoIntro = 0; // Estado IDLE (Hermano)
            if (jugador.getX() >= 200) {
                jugador.pararMovimiento();
                jugador.moverAutomatico(false, false, false, false);
                faseIntro = 1;
            }
        } else if (faseIntro == 1) {
            jugador.moverAutomatico(false, false, false, false);
            estadoVillanoIntro = 2; // Estado RUN (Jefe)
            estadoHermanoIntro = 1; // Estado WALK (Hermano)
            villanoMirandoIzq = true;

            if (villanoX > 900) {
                villanoX -= 4;
                hermanoX = villanoX + 120; // Sincronizacion de posicionamiento
            } else {
                faseIntro = 2;
                mostrarDialogo = true;
                introAniIndex = 0; // Reinicio de indice para el ciclo IDLE
            }
        } else if (faseIntro == 2) {
            jugador.moverAutomatico(false, false, false, false);
            estadoVillanoIntro = 0; // Estado IDLE (Jefe)
            estadoHermanoIntro = 0; // Estado IDLE (Hermano)
        } else if (faseIntro == 3) {
            estadoVillanoIntro = 2; // Estado RUN (Jefe)
            estadoHermanoIntro = 1; // Estado WALK (Hermano)
            villanoMirandoIzq = false; // Cambio de orientacion

            villanoX += 7; // Incremento de velocidad de salida
            hermanoX += 7; // Desplazamiento paralelo

            if (villanoX > 1600) {
                estadoActual = ESTADO_JUEGO;
                up = false; down = false; left = false; right = false;
            }
        }
        jugador.actualizar();
    }

    private void actualizarJuego() {
        jugador.mover(up, down, left, right);
        if (jugador.isAtacando()) {
            if (jugador.getAniIndex() == 0) golpeadosEnEsteAtaque.clear();
            verificarGolpeContinuo();
        }

        // --- Logica de camara ---
        if (jugador.getX() - cameraX > PUNTO_SCROLL) {
            // Desplazamiento anclado a la posicion del jugador
            cameraX = jugador.getX() - PUNTO_SCROLL;
        }

        // Restriccion de retroceso fuera de los limites de renderizado
        if (jugador.getX() < cameraX) jugador.setX(cameraX);

        // --- Actualizacion general de entidades ---
        spawner.actualizar(jugador, getWidth());
        jugador.actualizar();

        if (barraJefeNivel != null) {
            barraJefeNivel.actualizar();
        }

        // --- Bucle principal de procesamiento de enemigos ---
        Iterator<EnemigoBase> it = enemigos.iterator();
        while (it.hasNext()) {
            EnemigoBase e = it.next();

            // Evaluacion condicional del comportamiento de persecucion
            if (jugador.getVida() > 0) e.actualizarIA(jugador);
            e.actualizar();

            // 1. Validacion del estado de mortalidad y generacion de recompensas
            if (e.isMuerto()) {
                if (!e.isLootSoltado()) {
                    e.setLootSoltado(true);

                    int dropX = e.getHitbox().x + (e.getHitbox().width / 2);
                    int dropY = e.getHitbox().y;

                    int piesEnemigo = e.getHitbox().y + e.getHitbox().height;
                    int sueloBase = Math.max(445, Math.min(580, piesEnemigo));

                    if (Math.random() < 0.5) {
                        int cantidad = 2 + (int) (Math.random() * 3);
                        for (int i = 0; i < cantidad; i++) {
                            objetosSuelo.add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_MONEDA, sueloBase + (int)(Math.random()*40-20)));
                        }
                    }
                    int randP = (int) (Math.random() * 100);
                    if (randP < 15) objetosSuelo.add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_VIDA, sueloBase));
                    else if (randP < 30) objetosSuelo.add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_FUERZA, sueloBase));
                    else if (randP < 45) objetosSuelo.add(new ObjetoRecogible(dropX, dropY, ObjetoRecogible.TIPO_POCION_VELOCIDAD, sueloBase));
                }

                // Sincronizacion de eliminacion de entidad con el fin de su animacion respectiva
                boolean animMuerteTerminada = false;
                if (e instanceof EnemigoSlime && e.getAniIndex() >= 3) animMuerteTerminada = true;
                else if (e instanceof EnemigoEsqueletoElite && e.getAniIndex() >= 12) animMuerteTerminada = true;
                else if (e instanceof EnemigoNightBorne && e.getAniIndex() >= 21) animMuerteTerminada = true;

                if (animMuerteTerminada) {
                    jugador.ganarXP(e.getXpQueDa());
                    // Instanciacion de indicador flotante de experiencia
                    textosDano.add(new TextoDano(e.getX(), e.getY() - 40, "+" + e.getXpQueDa() + " XP", new Color(160, 32, 240)));
                    it.remove();
                }
                continue;
            }

            // Descarga de memoria para entidades fuera del margen operativo
            if (e.getX() < cameraX - 200) { it.remove(); continue; }

            // 2. Calculo de colisiones y procesamiento de dano al jugador
            if (e instanceof EnemigoEsqueletoElite || e instanceof EnemigoNightBorne || e instanceof EnemigoEsqueleto || e instanceof EnemigoMushroom) {
                Rectangle arma = e.getAttackBox();

                if (arma != null && arma.intersects(jugador.getBounds()) && jugador.getVida() > 0) {

                    if (e instanceof EnemigoEsqueletoElite) {
                        EnemigoEsqueletoElite esq = (EnemigoEsqueletoElite) e;
                        if (!esq.isGolpeRegistrado()) {
                            esq.setGolpeRegistrado(true);
                            procesarDanoAJugador(e);
                        }
                    } else if (e instanceof EnemigoNightBorne) {
                        EnemigoNightBorne nb = (EnemigoNightBorne) e;
                        // Ajuste preciso del frame de impacto
                        if (nb.isAtacando() && nb.getAniIndex() == 10 && !nb.isGolpeRegistrado()) {
                            procesarDanoAJugador(e);
                            nb.setGolpeRegistrado(true);
                        }
                    } else if (e instanceof EnemigoEsqueleto) {
                        // Comportamiento asignado al tipo de entidad basica
                        EnemigoEsqueleto esqNormal = (EnemigoEsqueleto) e;
                        if (esqNormal.isAtacando() && !esqNormal.isGolpeRegistrado()) {
                            procesarDanoAJugador(e);
                            esqNormal.setGolpeRegistrado(true);
                        }
                    } else if (e instanceof EnemigoMushroom) {
                        // NUEVO: Comportamiento para el Hongo
                        EnemigoMushroom hongo = (EnemigoMushroom) e;
                        if (hongo.isAtacando() && !hongo.isGolpeRegistrado()) {
                            procesarDanoAJugador(e);
                            hongo.setGolpeRegistrado(true);
                        }
                    }
                }
            } else {
                // Logica de contacto fisico para entidades que carecen de ataque a distancia/armas
                if (jugador.getBounds().intersects(e.getBounds()) && e.puedeAtacar()) {
                    boolean esquivado = (jugador.getZ() > 30);
                    if (!esquivado) {
                        procesarDanoAJugador(e);
                        e.reiniciarCooldown();
                    }
                }
            }

            if (jugador.getEstado() == Jugador.Estado.MUERTO) juegoActivo = false;
        }

        jugador.actualizarBuffs();

        // --- Logica de disparo y generacion de proyectiles (Habilidad 1) ---
        if (jugador.isLanzandoPoder()) {
            if (jugador.getAniIndex() == 3 && !dispararProyectilAhora) {
                dispararProyectilAhora = true;
                int startX = jugador.getX() + (jugador.isMirandoDerecha() ? 50 : -20);
                int startY = jugador.getY() - 66;
                proyectiles.add(new Proyectil(startX, startY, jugador.isMirandoDerecha(), jugador.getDano() * 2));
            }
        } else {
            dispararProyectilAhora = false;
        }

        // --- Calculo de trayectoria de proyectiles ---
        Iterator<Proyectil> itProyectil = proyectiles.iterator();
        while (itProyectil.hasNext()) {
            Proyectil p = itProyectil.next();
            p.actualizar();

            if (!p.isActivo()) {
                itProyectil.remove();
                continue;
            }

            for (EnemigoBase e : enemigos) {
                if (!e.isMuerto() && p.getHitbox().intersects(e.getBounds())) {
                    e.recibirDano(p.getDano());
                    textosDano.add(new TextoDano(e.getX(), e.getY() - 20, p.getDano(), Color.CYAN));
                    p.setActivo(false);
                    break;
                }
            }
        }

        // --- Administracion de objetos recolectables ---
        Iterator<ObjetoRecogible> itObj = objetosSuelo.iterator();
        while (itObj.hasNext()) {
            ObjetoRecogible obj = itObj.next();
            obj.actualizar();
            obj.setMostrarTag(Math.sqrt(Math.pow(jugador.getX()-obj.getX(),2) + Math.pow(jugador.getY()-obj.getY(),2)) < 150);
            if (jugador.getBounds().intersects(obj.getHitbox())) {
                if (obj.getTipo() == ObjetoRecogible.TIPO_MONEDA) textosDano.add(new TextoDano(jugador.getX(), jugador.getY()-30, "+15 Oro", new Color(255, 215, 0)));
                jugador.recogerObjeto(obj.getTipo());
                itObj.remove();
            }
        }

        // --- Limpieza de indicadores flotantes caducados ---
        Iterator<TextoDano> itTexto = textosDano.iterator();
        while (itTexto.hasNext()) {
            TextoDano td = itTexto.next();
            td.actualizar();
            if (!td.isActivo()) itTexto.remove();
        }
    }

    private void procesarDanoAJugador(EnemigoBase e) {
        // Implementacion de frames de invulnerabilidad
        if (jugador.isRodando()) {
            // Confirmacion visual de evasion exitosa
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "ESQUIVADO", Color.YELLOW));
            return;
        }

        // 1. Calculo de centros para direccionar vector de colision
        int centroJugador = jugador.getBounds().x + (jugador.getBounds().width / 2);
        int centroEnemigo = e.getBounds().x + (e.getBounds().width / 2);

        // 2. Aplicacion de fuerza de retroceso (Knockback)
        int direccionEmpuje = (centroJugador >= centroEnemigo) ? 1 : -1;
        int fuerzaEmpuje = 20;

        if (jugador.isDefendiendo()) {
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "BLOQUEADO", Color.CYAN));
            jugador.ganarCarga(10);
            jugador.setX(jugador.getX() + (direccionEmpuje * fuerzaEmpuje));
        } else {
            jugador.recibirGolpe(e.getDano());
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, e.getDano(), Color.RED));
            jugador.setX(jugador.getX() + (direccionEmpuje * (fuerzaEmpuje / 2)));
        }
    }

    private void verificarGolpeContinuo() {
        if (jugador.getAniIndex() != 2) return;

        int anchoAtaque = 80;
        int xAtaque = jugador.isMirandoDerecha() ? (int)jugador.getX() + 40 : (int)jugador.getX() - anchoAtaque + 20;

        Rectangle areaAtaque = new Rectangle(xAtaque, (int)jugador.getY() - 40, anchoAtaque, 80);

        for (EnemigoBase e : enemigos) {
            // Verificacion estricta de colision para ataque cuerpo a cuerpo
            if (!e.isMuerto() && areaAtaque.intersects(e.getHitbox()) && !golpeadosEnEsteAtaque.contains(e)) {

                // 1. Procesamiento matematico del dano base
                int danoActual = jugador.getDano();
                e.recibirDano(danoActual);

                // 2. Generacion de notificacion visual del impacto
                textosDano.add(new TextoDano(e.getHitbox().x, e.getHitbox().y, danoActual, Color.YELLOW));

                // 3. Incremento de medidor de carga ofensiva
                jugador.ganarCarga(15);

                // 4. Aplicacion de retroceso estandar al oponente
                if (!e.isAtacando()) {
                    if (jugador.isMirandoDerecha()) {
                        e.setX(e.getX() + 30);
                    } else {
                        e.setX(e.getX() - 30);
                    }
                }

                // 5. Insercion en lista temporal para prevenir calculo duplicado por el mismo ataque
                golpeadosEnEsteAtaque.add(e);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        double escalaX = (double) getWidth() / 1280;
        double escalaY = (double) getHeight() / 720;
        g2.scale(escalaX, escalaY);

        if (estadoActual == ESTADO_MENU) {
            dibujarMenu(g2);
        } else if (estadoActual == ESTADO_INTRO) {
            dibujarIntro(g2);
        } else if (estadoActual == ESTADO_JUEGO) {
            // Llamamos a dibujarJuego
            dibujarJuego(g2, g);

            // --- NUEVO: PANTALLA DE GAME OVER ---
            if (!juegoActivo) {
                // Filtro rojo oscuro semitransparente
                g2.setColor(new Color(50, 0, 0, 180));
                g2.fillRect(0, 0, 1280, 720);

                // Texto principal
                g2.setFont(new Font("Serif", Font.BOLD, 100));
                g2.setColor(Color.RED);
                String msgMuerte = "HAS MUERTO";
                g2.drawString(msgMuerte, (1280 - g2.getFontMetrics().stringWidth(msgMuerte)) / 2, 350);

                // Texto parpadeante para continuar
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g2.setFont(new Font("Arial", Font.BOLD, 30));
                    g2.setColor(Color.WHITE);
                    String msgEnter = "PRESIONA ENTER PARA VOLVER AL MENU";
                    g2.drawString(msgEnter, (1280 - g2.getFontMetrics().stringWidth(msgEnter)) / 2, 500);
                }
            }
        }

        // --- SISTEMA GLOBAL DE RENDERIZADO DE TRANSICIONES ---
        // Se ejecuta POSTERIOR a cualquier estado para superponerse visualmente
        if (enTransicion) {
            Composite original = g2.getComposite();
            // Clamp matematico de seguridad para evitar excepciones de canal Alpha (0.0f a 1.0f)
            float alphaSeguro = Math.max(0f, Math.min(1f, alphaTransicion));

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaSeguro));
            g2.setColor(Color.BLACK);
            // Uso de resolucion nativa (1280x720) debido a la escala aplicada previamente
            g2.fillRect(0, 0, 1280, 720);

            g2.setComposite(original); // Restauracion del pipeline grafico
        }

        g2.dispose();
    }

    private void dibujarMenu(Graphics2D g2) {
        // 1. Renderizado de capas de paralaje para el fondo dinamico
        for (CapaFondo c : capasParallax) c.dibujar(g2, menuScrollX);

        // 2. Filtro de oscurecimiento global para mejorar el contraste de la UI
        g2.setColor(new Color(0,0,0,100));
        g2.fillRect(0,0, 1280, 720);

        g2.setColor(Color.WHITE);

        // 3. Renderizado condicional del asset principal del titulo
        if (GestorRecursos.tituloImg != null) {

            // --- CONTROL DE ESCALA DEL TITULO ---
            // Modifica este valor: 1.0 es tamaño original, 0.5 es la mitad, etc.
            double escalaTitulo = 0.6;

            // Calculo de las nuevas dimensiones basadas en la escala
            int imgW = (int) (GestorRecursos.tituloImg.getWidth() * escalaTitulo);
            int imgH = (int) (GestorRecursos.tituloImg.getHeight() * escalaTitulo);

            // Resolucion de coordenadas espaciales para centrado absoluto
            int xTitulo = (1280 - imgW) / 2;
            int yTitulo = -20; // Margen superior ajustado para el nuevo tamaño

            // Dibujado del asset aplicando el redimensionamiento (Width, Height)
            g2.drawImage(GestorRecursos.tituloImg, xTitulo, yTitulo, imgW, imgH, null);

        } else {
            // Mecanismo de contingencia (Fallback) usando tipografia renderizada
            g2.setFont(fuenteTitulo);
            g2.setColor(Color.GRAY.darker());
            g2.drawString("BROTHER QUEST", 305, 205);
            g2.setColor(new Color(200, 200, 200));
            g2.drawString("BROTHER QUEST", 300, 200);
        }

        // 4. Modulo de intermitencia visual (Blinking effect) para indicador de accion
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            g2.setColor(Color.WHITE);
            String msg = "PRESIONA ENTER PARA EMPEZAR";
            g2.drawString(msg, (1280 - g2.getFontMetrics().stringWidth(msg))/2, 500);
        }

        // 5. Indicadores auxiliares de control de ventana
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("F11: Pantalla Completa", 1100, 700);
    }

    private void dibujarIntro(Graphics2D g2) {
        // 1. Capas de fondo
        for (CapaFondo c : capasParallax) c.dibujar(g2, 0);

        // --- NUEVO: Sombra del Jugador ---
        // Dibujamos la sombra antes que al jugador para que quede debajo de sus pies
        g2.setColor(new Color(0, 0, 0, 100));
        // Ajustamos la posición basándonos en el X e Y del jugador
        // Nota: Los valores +25 y +135 son aproximados para centrar la sombra en sus pies
        g2.fillOval(jugador.getX() - 11, jugador.getY(), 60, 20);

        // 2. Dibujar al jugador
        jugador.dibujar(g2, 0);

        // 3. Renderizado de Villano y Hermano
        if (faseIntro >= 1) {
            int frameVillano = introAniIndex % (estadoVillanoIntro == 0 ? 4 : 6);
            int frameHermano = introAniIndex % (estadoHermanoIntro == 0 ? 5 : 6);

            BufferedImage imgVillano = GestorRecursos.animacionesNightBorne[estadoVillanoIntro][frameVillano];
            BufferedImage imgHermano = GestorRecursos.animacionesHermano[estadoHermanoIntro][frameHermano];

            int anchoV = 300, altoV = 300;
            int vDrawX = villanoX - 120;
            int vDrawY = villanoY - 135;

            int anchoH = 240, altoH = 240;
            int hDrawX = hermanoX;
            int hDrawY = villanoY - 100;

            // Sombras de las otras entidades
            g2.setColor(new Color(0, 0, 0, 100));

            if (imgHermano != null) {
                g2.fillOval(hDrawX + 90, hDrawY + 170, 60, 20);
            }

            if (imgVillano != null) {
                g2.fillOval(vDrawX + 95, vDrawY + 222, 115, 30);
            }

            // --- Renderizado de Sprites (A y B) ---
            // (Se mantiene tu lógica de drawImage con el flip horizontal...)
            if (imgHermano != null) {
                if (villanoMirandoIzq) g2.drawImage(imgHermano, hDrawX + anchoH, hDrawY, -anchoH, altoH, null);
                else g2.drawImage(imgHermano, hDrawX, hDrawY, anchoH, altoH, null);
            }

            if (imgVillano != null) {
                if (villanoMirandoIzq) g2.drawImage(imgVillano, vDrawX + anchoV, vDrawY, -anchoV, altoV, null);
                else g2.drawImage(imgVillano, vDrawX, vDrawY, anchoV, altoV, null);
            }
        }

        // 3. Modulo visual de texto interactivo
        if (mostrarDialogo) {
            int boxX = 200, boxY = 550, boxW = 880, boxH = 150;
            g2.setColor(new Color(0, 0, 0, 200)); g2.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
            g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(4)); g2.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);
            g2.setFont(new Font("Arial", Font.BOLD, 24)); g2.setColor(Color.RED); g2.drawString("NIGHTBORNE", boxX + 30, boxY + 40);
            g2.setFont(new Font("Arial", Font.PLAIN, 20)); g2.setColor(Color.WHITE);
            g2.drawString(textoDialogoLinea1, boxX + 30, boxY + 80);
            g2.drawString(textoDialogoLinea2, boxX + 30, boxY + 110);
            if ((System.currentTimeMillis() / 400) % 2 == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 14)); g2.drawString("PRESIONA ENTER >", boxX + boxW - 180, boxY + boxH - 20);
            }
        }
    }

    // --- Metodo principal de renderizado del nivel ---
    private void dibujarJuego(Graphics2D g2, Graphics g) {
        for (CapaFondo c : capasParallax) c.dibujar(g2, cameraX);

        List<EnemigoBase> enemigosDetras = new ArrayList<>();
        List<EnemigoBase> enemigosDelante = new ArrayList<>();
        double piesJugador = jugador.getBounds().getMaxY();

        jugador.dibujarSombra(g2, cameraX);

        for (EnemigoBase e : enemigos) {
            e.dibujarSombra(g2, cameraX);
        }

        for (ObjetoRecogible obj : objetosSuelo) {
            obj.dibujarSombra(g2, cameraX);
        }

        for (ObjetoRecogible obj : objetosSuelo) obj.dibujar(g, cameraX);

        // 1. Organizacion de matriz por profundidad (Eje Y) para efecto seudo-3D
        for (EnemigoBase e : enemigos) {
            if (e.getHitbox().getMaxY() < piesJugador) enemigosDetras.add(e);
            else enemigosDelante.add(e);
        }

        // 2. Ejecucion secuencial de dibujos basada en el plano de profundidad
        for (EnemigoBase e : enemigosDetras) e.dibujar(g2, cameraX);
        jugador.dibujar(g2, cameraX);
        for (EnemigoBase e : enemigosDelante) e.dibujar(g2, cameraX);

        // 3. Renderizado final de elementos UI (Sobre todas las entidades)
        for (EnemigoBase e : enemigos) e.dibujarHUD(g2, cameraX);
        for (TextoDano td : textosDano) td.dibujar(g, cameraX);
        for (Proyectil p : proyectiles) {
            p.dibujar((Graphics2D) g, cameraX);
        }
        dibujarHUD(g);
    }

    // --- REEMPLAZA ESTE METODO EN GAMEPANEL.JAVA ---
    private void dibujarHUD(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int xBase = 20, yBase = 20;

        // Aumentamos un poco la altura del fondo negro para que quepan las nuevas barras
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(xBase - 10, yBase - 10, 260, 110, 15, 15); // Altura ajustada a 110

        g2.setColor(Color.BLACK);
        g2.fillRect(xBase, yBase, 60, 60);
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(xBase, yBase, 60, 60);
        BufferedImage img = jugador.getSpriteActual();
        if (img != null)
            g2.drawImage(img, xBase + 30 - img.getWidth(), yBase + 25 - img.getHeight(), img.getWidth() * 2, img.getHeight() * 2, null);

        // --- 1. SISTEMA DE BARRAS PRINCIPALES (VIDA, XP, STAMINA) ---
        int barX = xBase + 70;
        int barY = yBase + 10;
        int barW = 150;
        int alturaBarraGrande = 15;

        // A) BARRA DE VIDA (Roja - Arriba)
        g2.setColor(new Color(100, 0, 0));
        g2.fillRect(barX, barY, barW, alturaBarraGrande);
        g2.setColor(new Color(50, 205, 50));
        g2.fillRect(barX, barY, (int) (barW * ((double) jugador.getVida() / jugador.getVidaMax())), alturaBarraGrande);
        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barW, alturaBarraGrande);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        String tV = jugador.getVida() + " / " + jugador.getVidaMax();
        // Centrado de texto matematico
        g2.drawString(tV, barX + barW / 2 - g2.getFontMetrics().stringWidth(tV) / 2, barY + 12);

        // B) BARRA DE EXPERIENCIA (Morada - Medio) - AHORA CON TEXTO Y MAS GRANDE
        int xpY = barY + 20; // 15 de altura + 5 de margen
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(barX, xpY, barW, alturaBarraGrande);
        g2.setColor(new Color(180, 50, 255));
        // Calculo proporcional de progreso de nivel
        g2.fillRect(barX, xpY, (int) (barW * ((double) jugador.getXp() / jugador.getXpParaSiguienteNivel())), alturaBarraGrande);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawRect(barX, xpY, barW, alturaBarraGrande);

        // Texto de experiencia centrado
        g2.setColor(Color.WHITE);
        String tXP = jugador.getXp() + " / " + jugador.getXpParaSiguienteNivel();
        g2.drawString(tXP, barX + barW / 2 - g2.getFontMetrics().stringWidth(tXP) / 2, xpY + 12);

        // C) BARRA DE STAMINA (Amarilla - Abajo) - MANTIENE SU TAMAÑO PEQUEÑO
        int stamY = xpY + 20; // Bajamos otros 20 pixeles
        int alturaBarraPeque = 10;
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(barX, stamY, barW, alturaBarraPeque);
        g2.setColor(jugador.isEscudoRoto() ? Color.GRAY : new Color(255, 215, 0));
        g2.fillRect(barX, stamY, (int) (barW * (jugador.getStamina() / jugador.getMaxStamina())), alturaBarraPeque);
        g2.setColor(Color.BLACK);
        g2.drawRect(barX, stamY, barW, alturaBarraPeque);

        // --- INFORMACION INFERIOR (Oro y Nivel) ---
        // Calculo de nueva posicion Y: Altura stamina (10) + Margen solicitado (15)
        int textY = stamY + 25;

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(255, 215, 0));

        // Integracion del icono de moneda con ajuste de coordenadas
        if (GestorRecursos.monedaImg != null) {
            // Se resta 13 al Y para centrar la imagen respecto a la linea base del texto
            g2.drawImage(GestorRecursos.monedaImg, barX, textY - 13, 16, 16, null);
            g2.drawString(" " + jugador.getOro(), barX + 16, textY);
        } else {
            g2.drawString("Oro: " + jugador.getOro(), barX, textY);
        }

        g2.setColor(Color.LIGHT_GRAY);
        // Desplazamiento horizontal de 10px adicionales a la derecha (Total +90 desde barX)
        g2.drawString("Nivel: " + jugador.getNivel(), barX + 90, textY);


        // --- 2. Interfaz grafica del inventario rapido (SIN CAMBIOS) ---
        int slotX = 20;
        // Ajustamos un poco la Y del slot para que no quede pegado al fondo negro agrandado
        int slotY = 145;
        int slotSize = 50;

        int pocionSeleccionada = jugador.getPocionSeleccionada();
        int cantidad = 0;
        Color colorPocion = Color.WHITE;
        String nombrePocion = "";
        long ultimoUso = 0;

        // Recuperacion de metadatos del objeto basado en el puntero de inventario
        if (pocionSeleccionada == 0) {
            cantidad = jugador.getPocionesVida();
            colorPocion = Color.RED;
            nombrePocion = "Vida";
            ultimoUso = jugador.getUltimoUsoVida();
        } else if (pocionSeleccionada == 1) {
            cantidad = jugador.getPocionesFuerza();
            colorPocion = new Color(180, 50, 255); // Identificador visual morado
            nombrePocion = "Fuerza";
            ultimoUso = jugador.getUltimoUsoFuerza();
        } else if (pocionSeleccionada == 2) {
            cantidad = jugador.getPocionesVelocidad();
            colorPocion = Color.CYAN;
            nombrePocion = "Velocidad";
            ultimoUso = jugador.getUltimoUsoVelocidad();
        }

        // Formula de calculo para opacidad del tiempo de enfriamiento (Rango normalizado 0.0 - 1.0)
        long tiempoActual = System.currentTimeMillis();
        float progresoCooldown = 0f;
        final long COOLDOWN = 10000;
        if (tiempoActual - ultimoUso < COOLDOWN) {
            progresoCooldown = 1.0f - ((float)(tiempoActual - ultimoUso) / COOLDOWN);
        }

        // Punteros de navegacion sobre el marco UI
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("< Q", slotX, slotY - 5);
        g2.drawString("E >", slotX + slotSize - 22, slotY - 5);

        // Metodo de dibujo dedicado al contenedor del item
        dibujarSlot(g2, slotX, slotY, slotSize, "F", cantidad, colorPocion, progresoCooldown);

        // Identificador textual inferior
        g2.setColor(Color.WHITE);
        g2.drawString(nombrePocion, slotX + (slotSize / 2) - (g2.getFontMetrics().stringWidth(nombrePocion) / 2), slotY + slotSize + 15);

        // --- 3. Panel indicador de reservas de habilidad especial (SIN CAMBIOS) ---
        int habX = 20;
        int habY = 660; // Posicion absoluta basada en resolucion nativa
        int anchoBarraHab = 50;
        int altoBarraHab = 12;

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("PODER (1, 2, 3)", habX, habY - 5);

        int cargaActual = jugador.getCargaHabilidad();

        for (int i = 0; i < 3; i++) {
            int posX = habX + (i * (anchoBarraHab + 5));

            // Marco del modulo
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRect(posX, habY, anchoBarraHab, altoBarraHab);

            // Evaluacion fraccionada del valor de carga por slot
            int cargaDeEstaBarra = Math.max(0, Math.min(100, cargaActual - (i * 100)));
            int anchoRelleno = (int) (anchoBarraHab * (cargaDeEstaBarra / 100f));

            // Gradiente o distincion de color si el slot ha alcanzado su capacidad plena
            if (cargaDeEstaBarra == 100) g2.setColor(new Color(0, 200, 255));
            else g2.setColor(new Color(0, 100, 250));

            g2.fillRect(posX, habY, anchoRelleno, altoBarraHab);

            // Contorno de alta visibilidad para slots disponibles
            g2.setColor(cargaDeEstaBarra == 100 ? Color.WHITE : Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(posX, habY, anchoBarraHab, altoBarraHab);

            // Vinculacion a comandos numericos
            g2.setColor(Color.YELLOW);
            g2.drawString(String.valueOf(i + 1), posX + anchoBarraHab / 2 - 4, habY + altoBarraHab + 15);

            // Anclaje de capa visual del enemigo principal
            if (barraJefeNivel != null) {
                barraJefeNivel.dibujar((Graphics2D) g);
            }
        }
    }

    private void dibujarSlot(Graphics2D g2, int x, int y, int size, String tecla, int cant, Color col, float prog) {
        g2.setColor(new Color(20,20,20,220)); g2.fillRoundRect(x, y, size, size, 10, 10);
        g2.setColor(cant>0 && prog<=0 ? new Color(255,255,255,120) : new Color(100,100,100,50));
        g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x, y, size, size, 10, 10);
        if (cant>0) { g2.setColor(col); g2.fillRoundRect(x+size/4+2, y+size/2-2, size/2-4, size/2-4, 4, 4); g2.fillRect(x+size/2-3, y+size/4+2, 6, size/4); }
        else { g2.setColor(new Color(255,255,255,20)); g2.fillRoundRect(x+size/4+2, y+size/2-2, size/2-4, size/2-4, 4, 4); }
        if (prog>0) { g2.setColor(new Color(0,0,0,180)); g2.fillRect(x, y+(size-(int)(size*prog)), size, (int)(size*prog)); g2.setFont(new Font("Arial",Font.BOLD,10)); g2.setColor(Color.WHITE); g2.drawString(String.format("%.1fs", prog*10), x+2, y+size-5); }
        if (cant>0) { g2.setFont(new Font("Arial",Font.BOLD,12)); g2.setColor(Color.WHITE); String c="x"+cant; g2.drawString(c, x+size-g2.getFontMetrics().stringWidth(c)-4, y+size-5); }
        g2.setFont(new Font("Monospaced", Font.BOLD, 11)); g2.setColor(Color.ORANGE); g2.drawString(tecla, x+5, y+13);
    }

    // Metodo de interface para solicitar un cambio de estado con transicion cinematica
    public void cambiarEstado(int nuevoEstado) {
        if (!enTransicion) {
            estadoSiguiente = nuevoEstado;
            enTransicion = true;
            oscureciendo = true;
            alphaTransicion = 0f;
        }
    }

    // Metodo interno para calculo de deltas de opacidad (Alpha Blending)
    private void actualizarTransicion() {
        if (!enTransicion) return;

        if (oscureciendo) {
            alphaTransicion += velTransicion;
            if (alphaTransicion >= 1.0f) {
                alphaTransicion = 1.0f;
                estadoActual = estadoSiguiente; // Transmutacion logica de estado

                // Preparacion de coordenadas y logica pre-estado
                if (estadoActual == ESTADO_MENU) {
                    reiniciarJuego(); // Partida nueva, estadisticas reiniciadas
                } else if (estadoActual == ESTADO_INTRO) {
                    faseIntro = 0;
                    jugador.setX(-50);
                    jugador.setY(550);
                } else if (estadoActual == ESTADO_JUEGO) {
                    jugador.setX(100);
                    jugador.setY(490);
                    jugador.pararMovimiento();
                }

                oscureciendo = false; // Inversion de curva de opacidad
            }
        } else {
            alphaTransicion -= velTransicion;
            if (alphaTransicion <= 0.0f) {
                alphaTransicion = 0.0f;
                enTransicion = false; // Fin de proceso renderizado
            }
        }
    }

    private void reiniciarJuego() {
        initGame(); // Re-instancia al jugador, enemigos y mapa
        juegoActivo = true;
        proyectiles.clear();
        objetosSuelo.clear();
        textosDano.clear();
        golpeadosEnEsteAtaque.clear();
        cameraX = 0;
    }

    public void setInputs(boolean u, boolean d, boolean l, boolean r) {
        this.up = u; this.down = d; this.left = l; this.right = r;
    }
    public void ganarCarga(int cantidad) {
        cargaHabilidad += cantidad;
        if (cargaHabilidad > MAX_CARGA) cargaHabilidad = MAX_CARGA;
    }

    public int getCargaHabilidad() { return cargaHabilidad; }

    // Funcion booleana que evalua si existen recursos suficientes para efectuar la llamada
    public boolean usarHabilidad(int costoBarras) {
        int costo = costoBarras * 100;
        if (cargaHabilidad >= costo) {
            cargaHabilidad -= costo;
            lanzandoPoder = true; // Activa el booleano para el ciclo de animacion
            return true; // Retorna exito en la gestion de consumo
        }
        return false; // Retorna falla por escasez de recurso
    }

    // Funciones de encapsulamiento
    public boolean isLanzandoPoder() { return lanzandoPoder; }
    public void setLanzandoPoder(boolean b) { this.lanzandoPoder = b; }
}
>>>>>>> da25f6dd6bf3c69498f22ffaa92c786d38130149
