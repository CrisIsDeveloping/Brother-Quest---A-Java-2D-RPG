<<<<<<< HEAD
package com.rpg.juego;

import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;

public class Spawner {
    private List<EnemigoBase> enemigos;
    private Random random = new Random();

    private int hordaActual = 0;
    private boolean hordaActiva = false;
    private boolean hordaSpawneada = false; // Si los enemigos de esta horda ya fueron colocados
    
    // Cola para hordas orgánicas (escala por muertes)
    private Queue<List<String>> oleadasPendientes = new LinkedList<>();
    private int playerFinalX = 0;
    private int cameraXArena = 0;
    private int anchoPantallaArena = 1280;

    /** Actualizado cada vez que muere un enemigo con el cameraX real del frame */
    public void setCameraArena(int cx, int ancho) {
        this.cameraXArena = cx;
        this.anchoPantallaArena = ancho;
    }
    public int getCameraXArena() { return cameraXArena; }
    public int getAnchoPantallaArena() { return anchoPantallaArena; }

    // --- Puntos fijos del mundo donde se bloquea la cámara ---
    // El jugador camina hasta aquí, la cámara se congela y aparecen los enemigos a su derecha.
    // Separación de ~1600px entre hordas (aproximadamente 1 pantalla de ancho).
    private static final int[] TRIGGER_XS = {
        1600, 3200, 4800, 6400, 8000, 9600, 11200, 12800, 14400, 16000
    };

    public Spawner(List<EnemigoBase> enemigos) {
        this.enemigos = enemigos;
    }

    public void actualizar(Jugador jugador, int anchoPantalla, int cameraX) {
        // Si hay horda activa y el jugador mató a todos, liberar y pasar a la siguiente
        if (hordaActiva && enemigos.isEmpty() && oleadasPendientes.isEmpty()) {
            hordaActiva = false;
            hordaSpawneada = false;
            hordaActual++;
            
            // Spawn del mercader cada 2 hordas
            if (hordaActual > 0 && hordaActual % 2 == 0 && hordaActual < 10) {
                GamePanel.getInstancia().limpiarTradersTemporales();
                int spawnX = (int)jugador.getX() + 250; // Siempre cerca de ti, sin depender del trigger
                GamePanel.getInstancia().getTraders().add(new Trader(spawnX, 470));
            }
            
            // Si superamos la última horda (la 10ma), aparece el portal hacia el Jefe
            // Si superamos la última horda (la 10ma), la cámara frenará y veremos el final fijo
            if (hordaActual == 10) {
                 GamePanel.getInstancia().limpiarTradersTemporales();
            }
        }

        if (hordaActual >= TRIGGER_XS.length) return;
        int trigger = TRIGGER_XS[hordaActual];

        // LOCK: cuando el jugador llega al punto de trigger, la cámara se bloquea y spawnean los bichos
        if (!hordaSpawneada && jugador.getX() >= trigger) {
            hordaActiva = true;
            hordaSpawneada = true;
            
            // Y desaparece el Trader Temporal si el jugador lo dejó atrás
            GamePanel.getInstancia().limpiarTradersTemporales();
            
            int proyeccionJugadorX = (int)jugador.getX();
            colocarEnemigos(hordaActual, proyeccionJugadorX, true);
        }
    }

    // Coloca los enemigos a la derecha basados en la posición final del jugador
    private void colocarEnemigos(int numero, int playerFinalX, boolean cinematica) {
        this.playerFinalX = playerFinalX;
        oleadasPendientes.clear();

        switch (numero) {
            case 0: // Horda 1
                oleadasPendientes.add(List.of("SLIME:VERDE", "SLIME:VERDE"));
                break;
            case 1: // Horda 2: 2 Slime Verde -> 1 Azul -> 2 Azules -> 1 Rojo
                oleadasPendientes.add(List.of("SLIME:VERDE", "SLIME:VERDE"));
                oleadasPendientes.add(List.of("SLIME:AZUL"));
                oleadasPendientes.add(List.of("SLIME:AZUL", "SLIME:AZUL"));
                oleadasPendientes.add(List.of("SLIME:ROJO"));
                break;
            case 2: // Horda 3: Azul -> Rojo -> Verde + Azul -> Rojo
                oleadasPendientes.add(List.of("SLIME:AZUL"));
                oleadasPendientes.add(List.of("SLIME:ROJO"));
                oleadasPendientes.add(List.of("SLIME:VERDE", "SLIME:AZUL"));
                oleadasPendientes.add(List.of("SLIME:ROJO"));
                break;
            case 3: // Horda 4: Negro -> Rojo + Azul (atrás) -> Verde
                oleadasPendientes.add(List.of("SLIME:NEGRO"));
                oleadasPendientes.add(List.of("SLIME:ROJO", "BACK:SLIME:AZUL"));
                oleadasPendientes.add(List.of("SLIME:VERDE"));
                break;
            case 4: // Horda 5
                oleadasPendientes.add(List.of("ESQ:BLANCO"));
                oleadasPendientes.add(List.of("SLIME:VERDE", "SLIME:VERDE"));
                oleadasPendientes.add(List.of("BACK:SLIME:ROJO"));
                break;
            case 5: // Horda 6: Esq Blanco -> Dorado -> Negro -> 2 Azules
                oleadasPendientes.add(List.of("ESQ:BLANCO"));
                oleadasPendientes.add(List.of("ESQ:DORADO"));
                oleadasPendientes.add(List.of("SLIME:NEGRO"));
                oleadasPendientes.add(List.of("SLIME:AZUL", "BACK:SLIME:AZUL"));
                break;
            case 6: // Horda 7
                oleadasPendientes.add(List.of("ESQ:DORADO"));
                oleadasPendientes.add(List.of("BACK:SLIME:NEGRO"));
                oleadasPendientes.add(List.of("SLIME:ROJO", "SLIME:AZUL"));
                break;
            case 7: // Horda 8
                oleadasPendientes.add(List.of("ESQ:DORADO"));
                oleadasPendientes.add(List.of("ESQ:BLANCO", "BACK:ESQ:BLANCO"));
                oleadasPendientes.add(List.of("ESQ:DORADO"));
                break;
            case 8: // Horda 9
                oleadasPendientes.add(List.of("ESQ:ELITE"));
                oleadasPendientes.add(List.of("ESQ:BLANCO", "BACK:ESQ:BLANCO"));
                break;
            case 9: // Horda 10
                oleadasPendientes.add(List.of("ESQ:ELITE"));
                oleadasPendientes.add(List.of("BACK:ESQ:DORADO", "ESQ:DORADO"));
                oleadasPendientes.add(List.of("ESQ:DORADO"));
                break;
            default:
                oleadasPendientes.add(List.of("ESQ:ELITE"));
                break;
        }

        desplegarSiguienteOleada(cinematica);
    }

    public void notificarMuerteEnemigo(int jugadorX, int cameraX, int anchoPantalla) {
        if (!oleadasPendientes.isEmpty() && enemigos.size() <= 1) {
            this.playerFinalX = jugadorX;
            // Guardamos el cameraX ACTUAL (ya fijo/locked) para el spawn de refuerzos
            this.cameraXArena = cameraX;
            this.anchoPantallaArena = anchoPantalla;
            desplegarSiguienteOleada(false);
        }
    }

    private int contadorOleadasRefuerzo = 0; // Alterna dirección de entrada

    private void desplegarSiguienteOleada(boolean cinematica) {
        if (!oleadasPendientes.isEmpty()) {
            List<String> wave = oleadasPendientes.poll();
            
            if (!cinematica) {
                // Para refuerzos: alternamos la dirección pero respetamos el prefijo BACK: que tiene prioridad
                List<String> waveFinal = new java.util.ArrayList<>();
                boolean hayBACK = wave.stream().anyMatch(s -> s.startsWith("BACK:"));
                
                if (!hayBACK) {
                    // Si no tiene BACK explícito, alternamos: par=derecha, impar=izquierda
                    for (String def : wave) {
                        if (contadorOleadasRefuerzo % 2 == 1) {
                            waveFinal.add("BACK:" + def);
                        } else {
                            waveFinal.add(def);
                        }
                    }
                    contadorOleadasRefuerzo++;
                } else {
                    waveFinal.addAll(wave); // Si usa BACK: explícito, se respeta
                    contadorOleadasRefuerzo++;
                }
                
                spawnAleatorio(playerFinalX, waveFinal, hordaActual + 1, false);
            } else {
                contadorOleadasRefuerzo = 0; // Reset al inicio de una horda nueva
                spawnAleatorio(playerFinalX, wave, hordaActual + 1, true);
            }
        }
    }

    private void invocarSlime(String tipo, int x, int y, int nivel, boolean esRefuerzo) {
        EnemigoSlime s = new EnemigoSlime(x, y, tipo);
        s.establecerNivel(nivel);
        if (!esRefuerzo) s.spawnFlashTicks = 0; // Sin parpadeo para los de cinemática

        int vidaBase = 0, danoBase = 0;
        switch (tipo) {
            case "VERDE": vidaBase = 60;  danoBase = 8; break; // HP 80 -> 60, DMG 10 -> 8
            case "AZUL":  vidaBase = 90; danoBase = 8; break; // HP 140 -> 90, DMG 12 -> 8
            case "ROJO":  vidaBase = 70; danoBase = 10; break; // HP 100 -> 70, DMG 18 -> 10
            case "NEGRO": vidaBase = 180; danoBase = 15; break; // HP 250 -> 180, DMG 20 -> 15
        }

        int vidaFinal = vidaBase;
        int danoFinal = danoBase;
        for (int i = 1; i < nivel; i++) {
            vidaFinal += Math.max(1, (int)(vidaFinal * 0.38f));
            danoFinal += Math.max(1, (int)(danoFinal * 0.38f));
        }

        if (nivel >= 5) {
            s.convertirEnTanque();
            vidaFinal = (int)(vidaFinal * 2.5);
            danoFinal += 15;
        }

        s.setVidaMax(vidaFinal);
        s.setVida(vidaFinal);
        s.setDano(danoFinal);
        enemigos.add(s);
    }

    private void invocarEsqueleto(String tipo, int x, int y, int nivel, boolean esRefuerzo) {
        EnemigoBase e;
        int vidaBase = 0, danoBase = 0;

        if (tipo.equals("ELITE")) {
            e = new EnemigoEsqueletoElite(x, y);
            vidaBase = 1500; danoBase = 45;
        } else {
            e = new EnemigoEsqueleto(x, y, tipo);
            if (tipo.equals("DORADO")) { vidaBase = 400; danoBase = 35; }
            else { vidaBase = 200; danoBase = 20; }
        }

        if (!esRefuerzo) e.spawnFlashTicks = 0; // Sin parpadeo para los de cinemática

        e.establecerNivel(nivel);
        int vidaFinal = vidaBase;
        int danoFinal = danoBase;
        for (int i = 1; i < nivel; i++) {
            vidaFinal += Math.max(1, (int)(vidaFinal * 0.38f));
            danoFinal += Math.max(1, (int)(danoFinal * 0.38f));
        }

        e.setVidaMax(vidaFinal);
        e.setVida(vidaFinal);
        e.setDano(danoFinal);
        enemigos.add(e);
    }

    /**
     * Sistema de spawn aleatorio restringido:
     * Dispersa a los enemigos en el carril sin una rejilla fija, 
     * alineando sus pies con el suelo del camino (470-610).
     */
    private void spawnAleatorio(int playerFinalX, List<String> definicionHorda, int nivel, boolean cinematica) {
        int avanceOffscreenRight = 0;
        int avanceOffscreenLeft = 0;

        for (String def : definicionHorda) {
            String defLimpia = def;
            boolean porAtras = false;
            
            if (def.startsWith("BACK:")) {
                porAtras = true;
                defLimpia = def.substring(5);
            }
            
            // spawnX: durante cinemática, viene de off-screen. En refuerzo, nace dentro de la pantalla.
            int spawnX;
            if (cinematica) {
                if (porAtras) {
                    spawnX = playerFinalX - 900 - avanceOffscreenLeft;
                    avanceOffscreenLeft += 45;
                } else {
                    spawnX = playerFinalX + 900 + avanceOffscreenRight;
                    avanceOffscreenRight += 45;
                }
            } else {
                // Refuerzo: spawn céntrico para garantizar visibilidad al nacer (no en bordes)
                if (porAtras) {
                    // Lado izquierdo: entre 200 y 400 píxeles de la cámara
                    spawnX = cameraXArena + 200 + random.nextInt(200);
                } else {
                    // Lado derecho: entre 500 y 700 píxeles del borde derecho (zona central-derecha)
                    spawnX = cameraXArena + anchoPantallaArena - 500 - random.nextInt(200);
                }
            }

            int posXMeta = 0;
            int piesY = 0;
            boolean posicionValida = false;
            int intentos = 0;

            while (!posicionValida && intentos < 60) {
                if (porAtras) {
                    posXMeta = playerFinalX - 250 - random.nextInt(200);
                } else {
                    posXMeta = playerFinalX + 550 + random.nextInt(200);
                }
                
                // Para refuerzos directos usamos el propio spawnX como punto de chequeo
                int checkX = cinematica ? posXMeta : spawnX;
                piesY = 500 + random.nextInt(81); 
                posicionValida = true;
                
                // Siempre chequeamos separación (cinematic y refuerzos)
                for (EnemigoBase e : enemigos) {
                    double refX = cinematica ? (e.isLlegoMetaCinematica() ? e.getX() : e.cinematicTargetX) : e.getX();
                    double dist = Math.hypot(refX - checkX, (e.getHitbox() != null ? e.getHitbox().getMaxY() : e.getY()) - piesY);
                    if (dist < 75) {
                        posicionValida = false;
                        break;
                    }
                }
                intentos++;
            }

            String[] partes = defLimpia.split(":");
            String categoria = partes[0];
            String tipoSub = partes[1];

            int numEnemigosAntes = enemigos.size();

            if (categoria.equals("SLIME")) {
                invocarSlime(tipoSub, spawnX, piesY - 42, nivel, !cinematica);
            } else if (categoria.equals("ESQ")) {
                int h = tipoSub.equals("DORADO") ? 110 : 95;
                invocarEsqueleto(tipoSub, spawnX, piesY - h, nivel, !cinematica);
            }

            // Si estamos en cinemática pura de horda, inician formaciones ordenadas
            if (cinematica && enemigos.size() > numEnemigosAntes) {
                for (int i = numEnemigosAntes; i < enemigos.size(); i++) {
                    enemigos.get(i).iniciarMarchaCinematica(posXMeta);
                }
            } else if (!cinematica && enemigos.size() > numEnemigosAntes) {
                // Refuerzo: marcar como ya en combate (llegoMetaCinematica=true)
                // Así el sistema de jaula los continúa y no escapan
                for (int i = numEnemigosAntes; i < enemigos.size(); i++) {
                    enemigos.get(i).llegoMetaCinematica = true;
                }
            }
        }
    }

    public boolean isHordaActiva() {
        return hordaActiva;
    }

    public boolean isHordaDurmiente() {
        return hordaSpawneada && !hordaActiva;
    }

    public boolean isColaVacia() {
        return oleadasPendientes.isEmpty();
    }

    public int getHordaActual() {
        return hordaActual;
    }
    
    public void saltarHordasHasta(int xDestino) {
        GamePanel.getInstancia().getEnemigos().clear();
        hordaActiva = false;
        hordaSpawneada = false;
        
        for (int i = 0; i < TRIGGER_XS.length; i++) {
            if (xDestino >= TRIGGER_XS[i]) {
                hordaActual = i + 1;
            } else {
                break;
            }
        }
    }
}
=======
package com.rpg.juego;

import java.util.List;
import java.util.Random;

public class Spawner {
    private List<EnemigoBase> enemigos;
    private Random random = new Random();

    private int hordaActual = 0;
    private boolean hordaActiva = false;

    private int ultimoXDondeAparecioEncuentro = -200;
    private final int DISTANCIA_ENTRE_HORDAS = 320; // Constante de umbral de distancia para ritmo de generacion

    public Spawner(List<EnemigoBase> enemigos) {
        this.enemigos = enemigos;
    }

    public void actualizar(Jugador jugador, int anchoPantalla) {
        // --- Mecanismo de recoleccion de entidades obsoletas ---
        // Eliminacion por proximidad para optimizacion de memoria y prevencion de bloqueos logicos
        // Permite liberar la condicion de horda activa si el jugador avanza ignorando enemigos
        enemigos.removeIf(e -> e.getX() < jugador.getX() - 1200);

        if (hordaActiva && enemigos.isEmpty()) {
            hordaActiva = false;
            hordaActual++;
            ultimoXDondeAparecioEncuentro = (int) jugador.getX();
        }

        if (!hordaActiva && jugador.getX() > ultimoXDondeAparecioEncuentro + DISTANCIA_ENTRE_HORDAS) {
            int xBase = (int) jugador.getX() + anchoPantalla + 50;
            lanzarHorda(hordaActual, xBase);
            hordaActiva = true;
        }
    }

    private void lanzarHorda(int numeroHorda, int xBase) {
        // --- Modulos de instanciacion con aplicacion de offsets espaciales (Prevencion de overlapping) ---
        switch (numeroHorda) {
            case 0: // Oleada 1: Fase de introduccion basica
                invocarSlime("VERDE", xBase, 1);
                invocarSlime("VERDE", xBase + 150, 1);
                break;
            case 1: // Oleada 2: Despliegue escalonado
                invocarSlime("VERDE", xBase, 2);
                invocarSlime("AZUL", xBase + 140, 2);
                invocarSlime("VERDE", xBase + 280, 2);
                break;
            case 2: // Oleada 3: Incremento de hostilidad y dano
                invocarSlime("AZUL", xBase, 3);
                invocarSlime("ROJO", xBase + 160, 3);
                invocarSlime("AZUL", xBase + 320, 3);
                break;
            case 3: // Oleada 4: Introduccion de variante de alta resistencia
                invocarSlime("NEGRO", xBase, 4);
                invocarSlime("ROJO", xBase + 180, 4);
                invocarSlime("ROJO", xBase + 300, 4);
                break;
            case 4: // Oleada 5: Despliegue de infanteria rapida (Esqueletos)
                invocarEsqueleto("BLANCO", xBase, 5);
                invocarEsqueleto("BLANCO", xBase + 150, 5);
                invocarSlime("VERDE", xBase + 350, 5); // Variante pesada en retaguardia
                break;
            case 5: // Oleada 6: Composicion hibrida de amenaza media
                invocarEsqueleto("BLANCO", xBase, 6);
                invocarSlime("AZUL", xBase + 180, 6); // Variante pesada central
                invocarEsqueleto("BLANCO", xBase + 360, 6);
                break;
            case 6: // Oleada 7: Introduccion de infanteria de elite
                invocarEsqueleto("DORADO", xBase, 7);
                invocarSlime("NEGRO", xBase + 200, 7); // Variante pesada de alta vitalidad
                invocarSlime("ROJO", xBase + 380, 7); // Variante pesada ofensiva
                break;
            case 7: // Oleada 8: Escuadron tactico coordinado
                invocarEsqueleto("DORADO", xBase, 8);
                invocarEsqueleto("DORADO", xBase + 180, 8);
                invocarEsqueleto("BLANCO", xBase + 360, 8);
                break;
            case 8: // Oleada 9: Formacion defensiva con sub-comandante
                invocarEsqueleto("BLANCO", xBase, 9); // Escolta frontal
                invocarEsqueleto("ELITE", xBase + 180, 9); // Sub-comandante en centroide
                invocarEsqueleto("BLANCO", xBase + 400, 9); // Escolta de retaguardia
                break;
            case 9: // Oleada 10: Bloqueo de alta resistencia pre-jefe
                invocarEsqueleto("DORADO", xBase, 10);
                invocarEsqueleto("ELITE", xBase + 200, 10);
                invocarEsqueleto("DORADO", xBase + 420, 10);
                break;
            default: // Generacion procedimental infinita para contingencias post-nivel
                invocarEsqueleto("DORADO", xBase, 11);
                invocarSlime("NEGRO", xBase + 180, 11);
                invocarEsqueleto("ELITE", xBase + 380, 11);
                break;
        }
    }

    private void invocarSlime(String tipo, int x, int nivel) {
        int y = 450 + random.nextInt(80);
        EnemigoSlime s = new EnemigoSlime(x, y, tipo);

        s.establecerNivel(nivel);

        int vidaBase = 0;
        int danoBase = 0;

        switch (tipo) {
            case "VERDE": vidaBase = 80;  danoBase = 10; break;
            case "AZUL":  vidaBase = 140; danoBase = 15; break;
            case "ROJO":  vidaBase = 100; danoBase = 25; break;
            case "NEGRO": vidaBase = 250; danoBase = 20; break;
        }

        int vidaFinal = vidaBase + ((nivel - 1) * 40);
        int danoFinal = danoBase + ((nivel - 1) * 5);

        if (nivel >= 5) {
            s.convertirEnTanque();
            vidaFinal = (int)(vidaFinal * 2.5);
            danoFinal += 15;
        }

        s.setVidaMax(vidaFinal);
        s.setVida(vidaFinal);
        s.setDano(danoFinal);

        enemigos.add(s);
    }

    private void invocarEsqueleto(String tipo, int x, int nivel) {
        int y = 430 + random.nextInt(50);

        EnemigoBase e;
        int vidaBase = 0;
        int danoBase = 0;

        if (tipo.equals("ELITE")) {
            e = new EnemigoEsqueletoElite(x, y);
            vidaBase = 1500;
            danoBase = 45;
        } else {
            e = new EnemigoEsqueleto(x, y, tipo);
            if (tipo.equals("DORADO")) {
                vidaBase = 400;
                danoBase = 35;
            } else {
                vidaBase = 200;
                danoBase = 20;
            }
        }

        e.establecerNivel(nivel);

        int vidaFinal = vidaBase + ((nivel - 1) * 60);
        int danoFinal = danoBase + ((nivel - 1) * 8);

        e.setVidaMax(vidaFinal);
        e.setVida(vidaFinal);
        e.setDano(danoFinal);

        enemigos.add(e);
    }
}
>>>>>>> da25f6dd6bf3c69498f22ffaa92c786d38130149
