package com.rpg.juego;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Activamos la aceleración por hardware (OpenGL y D3D) para evitar lag en pantalla completa
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "true");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Lane Runner RPG");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Quitamos los bordes de la ventana de Windows
            frame.setUndecorated(true);

            // Ponemos el juego en pantalla completa segun la resolución del monitor
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(screenSize);

            // Agregamos el panel principal del juego a la ventana
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);

            // Centramos y mostramos la ventana
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Pedimos el foco para que el teclado funcione correctamente en el juego
            gamePanel.requestFocusInWindow();
        });
    }
}