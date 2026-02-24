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