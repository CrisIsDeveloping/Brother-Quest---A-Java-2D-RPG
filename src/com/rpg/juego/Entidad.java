package com.rpg.juego;

import java.awt.*;

public abstract class Entidad {
    // Usamos 'protected' para que el Jugador y los Enemigos puedan usar estas variables
    protected float x, y; // Usamos float para que el movimiento se vea más suave
    protected float velocidad;
    protected int vida;
    protected int vidaMax; // Vida total que puede tener
    protected int dano;
    protected CajaColision hitbox;

    public Entidad() {
        // Le damos una hitbox por defecto, pero cada enemigo luego le pone la suya
        this.hitbox = new CajaColision(0, 0, 40, 40);

        // La vida y el daño se los ponemos directamente en la clase de cada enemigo o jugador
    }

    public abstract void actualizar();

    // --- Getters para poder ver los datos desde otras clases ---
    public int getX() { return (int)x; } // Lo pasamos a entero porque Java dibuja usando enteros
    public int getY() { return (int)y; }
    public int getVida() { return vida; }
    public int getVidaMax() { return vidaMax; }
    public int getDano() { return dano; }

    public CajaColision getBounds() {
        // Devolvemos la hitbox para poder revisar choques
        return hitbox;
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    // --- Funciones para la vida ---
    public void curar(int cantidad){
        vida += cantidad;

        // Revisamos que no se cure más de su vida máxima
        if(vida > vidaMax) {
            vida = vidaMax;
        }
    }

    // Método para dibujar la sombra. Los enemigos que vuelan o saltan pueden cambiar esto
    public void dibujarSombra(Graphics g, int cameraX) {
        if (estaVivo()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 100));

            // Hacemos que la sombra mida lo mismo que la hitbox del enemigo
            int sombraX = hitbox.x - cameraX;
            int sombraY = hitbox.y + hitbox.height - 10; // La ponemos en los pies
            g2.fillOval(sombraX, sombraY, hitbox.width, 20);
        }
    }

    public void recibirDano(int cantidad) {
        this.vida -= cantidad;
        // Evitamos que la vida baje a números negativos
        if (this.vida < 0) this.vida = 0;
    }
}