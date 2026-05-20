package com.rpg.juego;

import java.awt.Rectangle;

public class CajaColision extends Rectangle {
    private int offsetX;
    private int offsetY;

    public CajaColision(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.offsetX = 0;
        this.offsetY = 0;
    }

    public CajaColision(int x, int y, int width, int height, int offsetX, int offsetY) {
        super(x + offsetX, y + offsetY, width, height);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    // Actualiza tomando en cuenta el eje Z (saltos, etc)
    public void actualizar(float x, float y, double z) {
        this.x = (int) x + offsetX;
        this.y = (int) (y - z) + offsetY;
    }
    
    // Actualiza en base a un plano 2D directo
    public void actualizar(float x, float y) {
        this.x = (int) x + offsetX;
        this.y = (int) y + offsetY;
    }

    public int getOffsetX() { return offsetX; }
    public void setOffsetX(int offsetX) { this.offsetX = offsetX; }
    public int getOffsetY() { return offsetY; }
    public void setOffsetY(int offsetY) { this.offsetY = offsetY; }
}
