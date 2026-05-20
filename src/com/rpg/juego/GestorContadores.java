package com.rpg.juego;

/**
 * GestorContadores — Singleton que acumula estadísticas de partida.
 * Se registran durante el juego y se muestran al terminar.
 *
 * Puntuación final (Exp. de leyenda):
 *   +100 pts por enemigo derrotado
 *   + 1 pt por cada 5 puntos de daño infligido
 *   +250 pts por cada bloqueo exitoso
 *   + 5 pts por cada moneda ganada
 *   -  5 pts por cada punto de daño recibido (penalización)
 */
public class GestorContadores {

    private static GestorContadores instancia;

    private int enemigosDerrotados  = 0;
    private int danioInfligido      = 0;
    private int danioRecibido       = 0;
    private int bloqueosExitosos    = 0;
    private int monedasGanadas      = 0;

    
    public static GestorContadores get() {
        if (instancia == null) instancia = new GestorContadores();
        return instancia;
    }

    /** Reinicia todos los contadores al empezar una nueva partida. */
    public static void reiniciar() {
        instancia = new GestorContadores();
    }

    
    public void registrarEnemigoDerrotado()         { enemigosDerrotados++; }
    public void registrarDanioInfligido(int d)       { danioInfligido      += Math.max(0, d); }
    public void registrarDanioRecibido(int d)        { danioRecibido       += Math.max(0, d); }
    public void registrarBloqueo()                   { bloqueosExitosos++; }
    public void registrarMoneda(int cantidad)        { monedasGanadas      += Math.max(0, cantidad); }

    
    public int getEnemigosDerrotados()  { return enemigosDerrotados; }
    public int getDanioInfligido()      { return danioInfligido; }
    public int getDanioRecibido()       { return danioRecibido; }
    public int getBloqueosExitosos()    { return bloqueosExitosos; }
    public int getMonedasGanadas()      { return monedasGanadas; }

    /** Calcula la puntuación final de la partida. */
    public int getPuntuacion() {
        int pts = 0;
        pts += enemigosDerrotados * 100;
        pts += danioInfligido     / 5;
        pts += bloqueosExitosos   * 250;
        pts += monedasGanadas     *  5;
        pts -= danioRecibido      *  5;
        return Math.max(0, pts);
    }
}
