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