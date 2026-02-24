package com.rpg.juego;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Lane Runner RPG");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 1. Desactivacion de decoraciones del sistema operativo (Configuracion Borderless)
            frame.setUndecorated(true);

            // 2. Sincronizacion del viewport con la resolucion nativa del hardware (Fullscreen mode)
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(screenSize);

            // 3. Acoplamiento del canvas principal (GamePanel) al contenedor Root
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);

            // Centrado relativo y visualizacion en la cola de eventos de UI
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Solicitud de foco para delegacion de inputs del teclado al panel
            gamePanel.requestFocusInWindow();
        });
    }
}