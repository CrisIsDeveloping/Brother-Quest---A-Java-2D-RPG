package com.rpg.juego;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;

public class Portal {
    private int x, y;
    private int aniTick = 0;
    private int aniIndex = 0;
    private int maxFrames = 8;
    private Rectangle hitbox;

    public Portal(int x, int y) {
        this.x = x;
        this.y = y + 30; // subido 20px desde el +50 anterior
        
        // Ajustamos la hitbox para que envuelva al portal visualmente
        this.hitbox = new Rectangle(this.x - 64, this.y - 64, 128, 128);
    }

    public void actualizar() {
        aniTick++;
        if (aniTick >= 10) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= maxFrames) {
                aniIndex = 0;
            }
        }
    }

    public void dibujar(Graphics2D g, int cameraX) {
        int w = 192; // 64 * 3
        int h = 192;
        int px = x - cameraX - w / 2;
        int py = y - h / 2;
        
        // Efecto flotar "hacia arriba desde la base"
        int floatY = -(int) (Math.abs(Math.sin(System.currentTimeMillis() / 400.0)) * 20);

        // Sombras con la mitad de grosor y hechas grisaceas y con doble estrechez
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x - cameraX - 30, y + (h / 2) - 20, 60, 12);
        
        if (GestorRecursos.portalImg != null) {
            g.drawImage(
                GestorRecursos.portalImg.getSubimage(aniIndex * 64, 0, 64, 64), 
                px, py + floatY, 
                w, h, null
            );
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(px, py + floatY, w, h);
        }
        
        if (GamePanel.debugActivado) {
            // Hitbox
            g.setColor(Color.MAGENTA);
            g.drawRect(hitbox.x - cameraX, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    public Rectangle getBounds() {
        return hitbox;
    }
    
    public Rectangle getHitbox() { return hitbox; }
    
    // Método implementado para obtener getMaxY y jugar con el Z-Order visual
    public double getMaxY() {
        return hitbox.getMaxY();
    }
    
    public void dibujarInteraccionNpc(Graphics2D g2, int cameraX) {
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String text = "[G] Entrar al Portal";
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int floatY = -(int) (Math.abs(Math.sin(System.currentTimeMillis() / 400.0)) * 20);

        int drawX = x - cameraX - tw / 2;
        int drawY = y + floatY - (192 / 2) - 10;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(drawX - 5, drawY - 15, tw + 10, 20, 5, 5);
        g2.setColor(Color.GREEN);
        g2.drawString(text, drawX, drawY);
    }
}
