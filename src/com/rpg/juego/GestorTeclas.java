package com.rpg.juego;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;



/**
 * GestorTeclas: Responsable de registrar y gestionar todos los InputBindings
 * del juego. Se separa de GamePanel para mantener la responsabilidad única
 * (principio de diseño OOP: separación de responsabilidades).
 */
public class GestorTeclas {

    private final GamePanel panel;
    private final InputMap im;
    private final ActionMap am;

    public GestorTeclas(GamePanel panel, InputMap im, ActionMap am) {
        this.panel = panel;
        this.im = im;
        this.am = am;
        configurarTeclas();
    }

    public void configurarTeclas() {
        registrarTeclasGlobales();
        registrarTeclasMenusYNavegacion();
        registrarTeclasInteraccion();
        registrarTeclasDireccion();
        registrarTeclasCombate();
        registrarTeclasHabilidades();
    }

    
    //  TECLAS GLOBALES (independientes del estado)
    
    private void registrarTeclasGlobales() {
        // F11 → Pantalla completa
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, false), "fullscreen");
        am.put("fullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.toggleFullScreen();
            }
        });

        // F2 → Saltar intro
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, false), "skip_intro");
        am.put("skip_intro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.isConsolaAbierta() || panel.estadoActual == GamePanel.ESTADO_PAUSA)
                    return;
                if ((panel.estadoActual == GamePanel.ESTADO_MENU || panel.estadoActual == GamePanel.ESTADO_INTRO)
                        && !panel.isEnTransicion()) {
                    panel.cambiarEstado(GamePanel.ESTADO_JUEGO);
                }
            }
        });

        // F3 → Activar/desactivar Debug (hitboxes)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0, false), "toggle_debug");
        am.put("toggle_debug", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GamePanel.debugActivado = !GamePanel.debugActivado;
            }
        });

        // F4 → Abrir/cerrar Consola de comandos
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0, false), "toggle_console");
        am.put("toggle_console", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.toggleConsola();
            }
        });
    }

    
    //  MENÚS Y NAVEGACIÓN
    
    private void registrarTeclasMenusYNavegacion() {
        // ENTER → Acción de aceptar en menús
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter_action");
        am.put("enter_action", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.accionEnter();
            }
        });

        // ESCAPE → Volver atrás / pausar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "escape");
        am.put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.accionEscape();
            }
        });

        // W / ARRIBA → Navegar arriba en menús
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "acc_up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "acc_up");
        am.put("acc_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.isConsolaAbierta()) return;
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) {
                    panel.setUp(true);
                } else if (panel.estadoActual == GamePanel.ESTADO_MENU
                        || panel.estadoActual == GamePanel.ESTADO_PAUSA
                        || panel.estadoActual == GamePanel.ESTADO_TIENDA) {
                    panel.navegarMenu(-1);
                } else if (panel.isEnOpciones()) {
                    panel.ajustarOpcion(false);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "stop_up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "stop_up");
        am.put("stop_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setUp(false);
                if (panel.getJugador() != null) panel.getJugador().pararMovimiento();
            }
        });

        // S / ABAJO → Navegar abajo en menús
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "acc_down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "acc_down");
        am.put("acc_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.isConsolaAbierta()) return;
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) {
                    panel.setDown(true);
                } else if (panel.estadoActual == GamePanel.ESTADO_MENU
                        || panel.estadoActual == GamePanel.ESTADO_PAUSA
                        || panel.estadoActual == GamePanel.ESTADO_TIENDA) {
                    panel.navegarMenu(1);
                } else if (panel.isEnOpciones()) {
                    panel.ajustarOpcion(true);
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "stop_down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "stop_down");
        am.put("stop_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setDown(false);
                if (panel.getJugador() != null) panel.getJugador().pararMovimiento();
            }
        });

        // Q → Poción anterior
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0, false), "prev_potion");
        am.put("prev_potion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) {
                    panel.getJugador().cambiarPocion(-1);
                    GestorSonidos.reproducir(GestorSonidos.SWITCH);
                } else if (panel.isEnOpciones()) {
                    panel.ajustarOpcion(false);
                }
            }
        });

        // E → Poción siguiente
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, false), "next_potion");
        am.put("next_potion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) {
                    panel.getJugador().cambiarPocion(1);
                    GestorSonidos.reproducir(GestorSonidos.SWITCH);
                } else if (panel.isEnOpciones()) {
                    panel.ajustarOpcion(true);
                }
            }
        });
    }

    
    //  INTERACCIÓN CON EL MUNDO
    
    private void registrarTeclasInteraccion() {
        // G → Interactuar (mercader, ítems)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0, false), "interact");
        am.put("interact", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.accionInteractuar();
            }
        });
    }

    
    //  MOVIMIENTO HORIZONTAL (A / D)
    
    private void registrarTeclasDireccion() {
        // A / IZQUIERDA
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "acc_left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "acc_left");
        am.put("acc_left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.isConsolaAbierta()) return;
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) panel.setLeft(true);
                else if (panel.isEnOpciones()) panel.ajustarOpcion(false);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "stop_left");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "stop_left");
        am.put("stop_left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setLeft(false);
                if (panel.getJugador() != null) panel.getJugador().pararMovimiento();
            }
        });

        // D / DERECHA
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "acc_right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "acc_right");
        am.put("acc_right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.isConsolaAbierta()) return;
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) panel.setRight(true);
                else if (panel.isEnOpciones()) panel.ajustarOpcion(true);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "stop_right");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "stop_right");
        am.put("stop_right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.setRight(false);
                if (panel.getJugador() != null) panel.getJugador().pararMovimiento();
            }
        });
    }

    
    //  COMBATE BÁSICO
    
    private void registrarTeclasCombate() {
        // ESPACIO → Saltar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "jump");
        am.put("jump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta())
                    panel.getJugador().saltar();
            }
        });

        // J → Atacar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0, false), "attack");
        am.put("attack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta()
                        && !panel.getJugador().isAtacando())
                    panel.getJugador().setAtacando(true);
            }
        });

        // R → Rodar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "roll");
        am.put("roll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta())
                    panel.getJugador().rodar();
            }
        });

        // F → Usar poción
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "potion");
        am.put("potion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta())
                    panel.getJugador().usarPocionSeleccionada();
            }
        });

        // K (mantener) → Guardia/Bloqueo
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, false), "guard");
        am.put("guard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta())
                    panel.getJugador().setDefendiendo(true);
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0, true), "stop_guard");
        am.put("stop_guard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT)
                    panel.getJugador().setDefendiendo(false);
            }
        });
    }

    
    //  HABILIDAD MÁGICA ÚNICA (H)
    
    private void registrarTeclasHabilidades() {
        // H → Lanzar Habilidad
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0, false), "skill");
        am.put("skill", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((panel.estadoActual == GamePanel.ESTADO_JUEGO
                        || panel.estadoActual == GamePanel.ESTADO_BOSS_FIGHT) && !panel.isConsolaAbierta()) {
                    panel.getGestorHabilidades().intentarLanzarHabilidad(panel.getJugador());
                }
            }
        });
    }
}
