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
    public int estadoPrePausa = ESTADO_JUEGO;
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
                    estadoActual = estadoPrePausa;
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
        } else if (estadoActual == ESTADO_JUEGO || estadoActual == ESTADO_BOSS_FIGHT) {
            GestorSonidos.reproducir(GestorSonidos.MENU_OPEN);
            estadoPrePausa = estadoActual;
            estadoActual = ESTADO_PAUSA;
            subEstadoPausa = 0;
            pararMovimientoJugador();
        } else if (estadoActual == ESTADO_PAUSA) {
            if (subEstadoPausa == 1) {
                subEstadoPausa = 0;
                seleccionPausa = 1;
            } else {
                estadoActual = estadoPrePausa;
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
