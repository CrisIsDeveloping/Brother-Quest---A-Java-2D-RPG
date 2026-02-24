package com.rpg.juego;

import java.awt.*;

public abstract class Entidad {
    // Encapsulamiento 'protected' para permitir herencia y mutabilidad desde subclases (Jugador, EnemigoBase)
    protected float x, y; // Uso de punto flotante para interpolacion suave de coordenadas espaciales
    protected float velocidad;
    protected int vida;
    protected int vidaMax; // Limite maximo de salud
    protected int dano;
    protected Rectangle hitbox;

    public Entidad() {
        // Inicializacion de Bounding Box por defecto (Sobrescribible via Polimorfismo en las subclases)
        this.hitbox = new Rectangle(0, 0, 40, 40);

        // Optimizacion del ciclo de vida: La asignacion de salud maxima se delega al constructor
        // de las subclases para evitar reseteos de memoria prematuros e inconsistencias de estado.
    }

    public abstract void actualizar();

    // --- Capa de Acceso de Datos (Getters) ---
    public int getX() { return (int)x; } // Casteo explicito a entero para compatibilidad con el motor 2D
    public int getY() { return (int)y; }
    public int getVida() { return vida; }
    public int getVidaMax() { return vidaMax; }
    public int getDano() { return dano; }

    public Rectangle getBounds() {
        // Optimizacion de memoria: Se retorna la referencia al objeto existente (Heap)
        // en lugar de instanciar uno nuevo por cada calculo de resolucion de colision.
        return hitbox;
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    // --- Logica Transaccional de Atributos ---
    public void curar(int cantidad){
        vida += cantidad;

        // Validacion del limite superior (Ceiling) previniendo desbordamiento de salud
        if(vida > vidaMax) {
            vida = vidaMax;
        }
    }

    // Metodo base de sombreado. Se puede sobrescribir (Override) en las subclases si
    // la entidad tiene fisicas complejas (como saltos o vuelos).
    public void dibujarSombra(Graphics g, int cameraX) {
        if (estaVivo()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 100));

            // Usamos la hitbox para que la sombra siempre mida el ancho de la entidad
            int sombraX = hitbox.x - cameraX;
            int sombraY = hitbox.y + hitbox.height - 10; // Posicionada en la base (los pies)
            g2.fillOval(sombraX, sombraY, hitbox.width, 20);
        }
    }

    public void recibirDano(int cantidad) {
        this.vida -= cantidad;
        // Validacion del limite inferior (Floor)
        if (this.vida < 0) this.vida = 0;
    }
}