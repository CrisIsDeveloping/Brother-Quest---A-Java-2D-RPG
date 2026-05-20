package com.rpg.juego;

import java.util.List;

public class GestorHabilidades {

    private boolean dispararProyectilAhora = false;

    public GestorHabilidades() {
    }

    public void actualizar(Jugador jugador, List<Proyectil> proyectiles) {
        // --- Lógica de magia (Habilidad 1) ---
        if (jugador.isLanzandoPoder()) {
            if (jugador.getAniIndex() == 3 && !dispararProyectilAhora) {
                dispararProyectilAhora = true;
                int startX = jugador.getX() + (jugador.isMirandoDerecha() ? 50 : -20);
                int startY = jugador.getY() - 66;

                // Creamos la bolita de magia
                // Calculamos un objetivo delante del jugador basado en su dirección
                float targetX = jugador.isMirandoDerecha() ? startX + 100 : startX - 100;
                float targetY = startY;

                Proyectil nuevaMagia = new Proyectil(
                    startX, 
                    startY, 
                    targetX,
                    targetY,
                    jugador.getDano(), 
                    Proyectil.Emisor.JUGADOR, 
                    Proyectil.Tipo.ONDA_MAGICA,
                    null,
                    1.0f
                );

                proyectiles.add(nuevaMagia);
            }
        } else {
            dispararProyectilAhora = false;
        }
    }

    public boolean intentarLanzarHabilidad(Jugador jugador) {
        if (!jugador.isAtacando()) {
            if (jugador.usarHabilidad()) {
                GestorSonidos.reproducir(GestorSonidos.HABILIDAD);
                System.out.println("Lanzando habilidad (Enfriamiento 7s)");
                return true;
            } else {
                System.out.println("Habilidad en recarga");
            }
        }
        return false;
    }
}
