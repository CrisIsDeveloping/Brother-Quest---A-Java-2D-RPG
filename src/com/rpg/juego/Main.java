<<<<<<< HEAD
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
=======
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
>>>>>>> da25f6dd6bf3c69498f22ffaa92c786d38130149
}