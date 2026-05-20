package com.rpg.juego;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Dibujado {

    // --- El método que dibuja todo el nivel mientras jugamos ---
    public void dibujarJuego(
            Graphics2D g2,
            Graphics g,
            int cameraX,
            List<CapaFondo> capasParallax,
            Jugador jugador,
            List<EnemigoBase> enemigos,
            List<ObjetoRecogible> objetosSuelo,
            List<TextoDano> textosDano,
            List<Proyectil> proyectiles,
            BarraJefe barraJefeNivel,
            List<Trader> traders) {
        if (GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_BOSS_FIGHT ||
                GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_CINEMATICA_BOSS ||
                GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_DIALOGO_BOSS) {
            if (GestorRecursos.bossBackgroundImg != null) {
                g2.drawImage(GestorRecursos.bossBackgroundImg, 0, 0, 1280, 720, null);
            }
        } else {
            // Victoria y todo lo demás → fondo de bosque con parallax
            for (CapaFondo c : capasParallax)
                c.dibujar(g2, cameraX);
        }

        List<EnemigoBase> enemigosDetras = new ArrayList<>();
        List<EnemigoBase> enemigosDelante = new ArrayList<>();

        // En pantalla de victoria solo dibujamos el fondo (los sprites decorativos
        // los gestiona GamePanel.dibujarSpritesVictoria)
        boolean esVictoria = (GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_VICTORIA);
        if (esVictoria)
            return; // Nada más: fondo ya dibujado arriba

        double piesJugador = jugador.getBounds().getMaxY();

        jugador.dibujarSombra(g2, cameraX);

        for (EnemigoBase e : enemigos) {
            e.dibujarSombra(g2, cameraX);
        }

        if (traders != null) {
            for (Trader t : traders)
                t.dibujarSombra(g2, cameraX);
        }

        Portal portal = GamePanel.getInstancia().getPortalJefe();

        for (ObjetoRecogible obj : objetosSuelo) {
            obj.dibujarSombra(g2, cameraX);
        }

        for (ObjetoRecogible obj : objetosSuelo)
            obj.dibujar(g, cameraX);

        // 1. Separamos enemigos y proyectiles por profundidad (Eje Z e Y)
        List<Proyectil> proyDetras = new ArrayList<>();
        List<Proyectil> proyDelante = new ArrayList<>();

        for (EnemigoBase e : enemigos) {
            if (e.getHitbox().getMaxY() < piesJugador)
                enemigosDetras.add(e);
            else
                enemigosDelante.add(e);
        }

        for (Proyectil p : proyectiles) {
            // El usuario pidió: si z > 35 (mitad del cuerpo), dibujar detrás.
            if (p.getZ() > 35)
                proyDetras.add(p);
            else
                proyDelante.add(p);
        }

        List<Trader> tradersDetras = new ArrayList<>();
        List<Trader> tradersDelante = new ArrayList<>();

        if (traders != null) {
            for (Trader t : traders) {
                // t.y es la base en el caso del trader originalmente, pero usemos la hitbox max
                // Y si la tiene
                double traderPies = t.getBounds() != null ? t.getBounds().getMaxY() : t.getY();
                if (traderPies < piesJugador)
                    tradersDetras.add(t);
                else
                    tradersDelante.add(t);
            }
        }

        boolean portalDetras = false;
        if (portal != null && portal.getMaxY() < piesJugador) {
            portalDetras = true;
        }

        // 2. Pintamos en orden de capas (Pintor)
        if (portal != null && portalDetras && GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_JUEGO) {
            portal.dibujar(g2, cameraX);
        }
        for (Proyectil p : proyDetras)
            p.dibujar(g2, cameraX);
        for (EnemigoBase e : enemigosDetras)
            e.dibujar(g2, cameraX);
        for (Trader t : tradersDetras)
            t.dibujar(g2, cameraX);

        jugador.dibujar(g2, cameraX);

        for (Trader t : tradersDelante)
            t.dibujar(g2, cameraX);
        for (EnemigoBase e : enemigosDelante)
            e.dibujar(g2, cameraX);
        if (portal != null && !portalDetras && GamePanel.getInstancia().estadoActual == GamePanel.ESTADO_JUEGO) {
            portal.dibujar(g2, cameraX);
        }
        for (Proyectil p : proyDelante)
            p.dibujar(g2, cameraX);

        // 3. Dibujamos la UI y números por encima
        for (EnemigoBase e : enemigos)
            e.dibujarHUD(g2, cameraX);
        for (TextoDano td : textosDano)
            td.actualizar(); // Asegurar actualización
        for (TextoDano td : textosDano)
            td.dibujar(g, cameraX);

        dibujarHUD(g, jugador, barraJefeNivel);

        if (barraJefeNivel != null) {
            barraJefeNivel.dibujar(g2, jugador);
        }
    }

    public void dibujarConsola(Graphics2D g2, String texto, String resultado) {
        GamePanel gp = GamePanel.getInstancia();
        int ancho = 1280;
        int altoBarra = 40;
        int yBarra = 720 - altoBarra;
        Font fontConsola = new Font("Monospaced", Font.BOLD, 18);
        g2.setFont(fontConsola);
        FontMetrics fm = g2.getFontMetrics();

        // 1. Dibujar HISTORIAL (Chat Log) - Ahora "completamente largo"
        List<String> logs = gp.getHistorialLogs();
        if (!logs.isEmpty()) {
            int margenHorizontal = 10;
            int anchoLog = ancho - (margenHorizontal * 2);
            int logH = logs.size() * 25 + 10;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(margenHorizontal, yBarra - 40 - logH, anchoLog, logH);

            for (int i = 0; i < logs.size(); i++) {
                String log = logs.get(i);
                if (log.startsWith(">"))
                    g2.setColor(Color.YELLOW);
                else if (log.startsWith("Error"))
                    g2.setColor(Color.RED);
                else
                    g2.setColor(Color.WHITE);

                g2.drawString(log, margenHorizontal + 10, yBarra - 40 - logH + 25 + (i * 25));
            }
        }

        // 2. Dibujar Barara de Entrada
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, yBarra, ancho, altoBarra);
        g2.setColor(new Color(0, 255, 0, 150));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, yBarra, ancho, yBarra);

        String prompt = "> ";
        g2.setColor(Color.GREEN);
        g2.drawString(prompt + texto, 10, yBarra + 25);

        // Cursor
        int cursorIdx = gp.getCursorConsola();
        int xCursor = 10 + fm.stringWidth(prompt + texto.substring(0, Math.min(cursorIdx, texto.length())));
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            g2.setColor(Color.WHITE);
            g2.fillRect(xCursor, yBarra + 8, 2, 20);
        }

        // 3. Dibujar CUADRO DE SUGERENCIAS (TAB)
        List<String> sugs = gp.getSugerenciasActivas();
        if (!sugs.isEmpty()) {
            int sugW = 200;
            for (String s : sugs)
                sugW = Math.max(sugW, fm.stringWidth(s) + 40);

            int sugH = sugs.size() * 25 + 10;
            int sugX = 10;
            int sugY = yBarra - 10 - sugH;

            // Fondo sugerencias
            g2.setColor(new Color(30, 30, 30, 230));
            g2.fillRect(sugX, sugY, sugW, sugH);
            g2.setColor(Color.GRAY);
            g2.drawRect(sugX, sugY, sugW, sugH);

            int indice = gp.getIndiceSugerencia();
            for (int i = 0; i < sugs.size(); i++) {
                if (i == indice) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRect(sugX + 2, sugY + 5 + (i * 25), sugW - 4, 25);
                    g2.setColor(Color.YELLOW);
                } else {
                    g2.setColor(Color.LIGHT_GRAY);
                }
                g2.drawString(sugs.get(i), sugX + 10, sugY + 25 + (i * 25));
            }
        }
    }

    public void dibujarMenuPausa(Graphics2D g2, int seleccion, int subEstado) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, 1280, 720);

        if (subEstado == 2) {
            dibujarPantallaControles(g2);
            return;
        }
        // Cuadro central del menú
        int menuW = 600;
        int menuH = 500;
        int menuX = (1280 - menuW) / 2;
        int menuY = (720 - menuH) / 2;

        // Sombra y fondo del cuadro
        g2.setColor(new Color(20, 20, 20, 240));
        g2.fillRoundRect(menuX, menuY, menuW, menuH, 20, 20);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(menuX, menuY, menuW, menuH, 20, 20);

        String titulo = (subEstado == 0) ? "PAUSA" : "OPCIONES";
        String[] opciones = (subEstado == 0)
                ? new String[] { "CONTINUAR", "OPCIONES", "CONTROLES", "VOLVER AL MENU", "CERRAR EL JUEGO" }
                : new String[] { "SONIDO / SFX", "MUSICA", "PANTALLA COMPLETA", "VOLVER" };

        // Dibujar Título del menú
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.setColor(Color.ORANGE);
        drawCenteredString(g2, titulo, 1280 / 2, menuY + 80);

        // Línea decorativa
        g2.setColor(new Color(255, 165, 0, 100));
        g2.drawLine(menuX + 50, menuY + 110, menuX + menuW - 50, menuY + 110);

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        for (int i = 0; i < opciones.length; i++) {
            int yPos = menuY + 175 + i * 52;

            if (i == seleccion) {
                // Efecto de brillo detrás de la selección
                g2.setColor(new Color(255, 255, 0, 30));
                g2.fillRoundRect(menuX + 50, yPos - 30, menuW - 100, 40, 10, 10);

                g2.setColor(Color.YELLOW);
                g2.drawString("> ", menuX + 80, yPos);
            } else {
                g2.setColor(Color.WHITE);
            }

            String textoOpcion = opciones[i];

            if (subEstado == 1) {
                if (i == 0) {
                    float vol = GestorSonidos.getVolumenMaestroSFX();
                    int barra = (int) ((vol + 20) / 2.6);
                    textoOpcion += " [" + "|".repeat(Math.max(0, barra)) + ".".repeat(Math.max(0, 10 - barra)) + "]";
                } else if (i == 1) {
                    textoOpcion += " : " + (GestorSonidos.isMusicaActivada() ? "SI" : "NO");
                } else if (i == 2) {
                    boolean fs = GamePanel.getInstancia().isFullScreen();
                    textoOpcion += " : " + (fs ? "ON" : "OFF");
                }
            }

            g2.drawString(textoOpcion, menuX + 120, yPos);
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(Color.GRAY);
        drawCenteredString(g2, "Usa [Flechas] para elegir y [ENTER] para confirmar", 1280 / 2, menuY + menuH - 25);
    }

    private void dibujarPantallaControles(Graphics2D g2) {
        int panW = 760;
        int panH = 560;
        int panX = (1280 - panW) / 2;
        int panY = (720 - panH) / 2;

        g2.setColor(new Color(20, 20, 20, 240));
        g2.fillRoundRect(panX, panY, panW, panH, 20, 20);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(panX, panY, panW, panH, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 38));
        g2.setColor(Color.ORANGE);
        drawCenteredString(g2, "CONTROLES", 1280 / 2, panY + 60);

        g2.setColor(new Color(255, 165, 0, 80));
        g2.drawLine(panX + 40, panY + 80, panX + panW - 40, panY + 80);

        int colIzqX = panX + 55;
        int colDerX = panX + panW / 2 + 20;
        int filaY = panY + 110;
        int paso = 38;

        String[][] controles = {
                { "W / A / S / D", "Moverse" },
                { "ESPACIO", "Saltar" },
                { "J", "Atacar" },
                { "K", "Bloquear" },
                { "R", "Rodar (Evasion)" },
                { "H", "Habilidad Magica" },
                { "F", "Consumir pocion" },
                { "E / Q", "Cambiar de pocion" },
                { "Rueda del raton", "Cambiar de pocion" },
                { "G", "Interactuar" },
                { "ESCAPE", "Pausar / Cerrar menu" },
                { "F4", "Consola de comandos" },
        };

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int mitad = (controles.length + 1) / 2;

        for (int i = 0; i < controles.length; i++) {
            int col = (i < mitad) ? 0 : 1;
            int fila = (i < mitad) ? i : i - mitad;
            int baseX = (col == 0) ? colIzqX : colDerX;
            int baseY = filaY + fila * paso;

            if (fila % 2 == 0) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(baseX - 8, baseY - 20, panW / 2 - 60, paso - 4, 6, 6);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            String tecla = controles[i][0];
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(tecla) + 14;
            g2.setColor(new Color(60, 60, 60, 230));
            g2.fillRoundRect(baseX, baseY - 16, tw, 22, 6, 6);
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(baseX, baseY - 16, tw, 22, 6, 6);
            g2.setColor(new Color(220, 220, 220));
            g2.drawString(tecla, baseX + 7, baseY - 1);

            g2.setColor(new Color(120, 120, 120));
            g2.drawString("->", baseX + tw + 5, baseY - 1);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(Color.WHITE);
            g2.drawString(controles[i][1], baseX + tw + 28, baseY - 1);
        }

        int btnW = 300;
        int btnX = 1280 / 2;
        int btnY = panY + panH - 40;

        g2.setColor(new Color(255, 255, 0, 30));
        g2.fillRoundRect(btnX - btnW / 2, btnY - 30, btnW, 40, 10, 10);

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(Color.YELLOW);
        drawCenteredString(g2, "> ACEPTAR <", btnX, btnY);
    }

    private void drawCenteredString(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int xPos = x - (metrics.stringWidth(text) / 2);
        g.drawString(text, xPos, y);
    }

    public void dibujarHUD(Graphics g, Jugador jugador, BarraJefe barraJefeNivel) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int xBase = 20, yBase = 20;

        // Cuadro negro de fondo para las barras de arriba a la izquierda
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(xBase - 10, yBase - 10, 260, 110, 15, 15);

        // Cuadrito con la cara del jugador
        g2.setColor(Color.BLACK);
        g2.fillRect(xBase, yBase, 60, 60);
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(xBase, yBase, 60, 60);
        BufferedImage img = jugador.getSpriteActual();
        if (img != null)
            g2.drawImage(img, xBase + 30 - img.getWidth(), yBase + 25 - img.getHeight(), img.getWidth() * 2,
                    img.getHeight() * 2, null);

        // --- 1. LAS BARRAS DE VIDA, MAGIA Y ESTAMINA ---
        int barX = xBase + 70;
        int barY = yBase + 10;
        int barW = 150;
        int alturaBarraGrande = 15;

        // A) BARRA DE VIDA (Roja)
        g2.setColor(new Color(100, 0, 0));
        g2.fillRect(barX, barY, barW, alturaBarraGrande);
        g2.setColor(new Color(50, 205, 50));
        g2.fillRect(barX, barY, (int) (barW * ((double) jugador.getVida() / jugador.getVidaMax())), alturaBarraGrande);
        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barW, alturaBarraGrande);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        String tV = jugador.getVida() + " / " + jugador.getVidaMax();
        g2.drawString(tV, barX + barW / 2 - g2.getFontMetrics().stringWidth(tV) / 2, barY + 12);

        // B) BARRA DE EXPERIENCIA (Morada)
        int xpY = barY + 20;
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(barX, xpY, barW, alturaBarraGrande);
        g2.setColor(new Color(180, 50, 255));
        g2.fillRect(barX, xpY, (int) (barW * ((double) jugador.getXp() / jugador.getXpParaSiguienteNivel())),
                alturaBarraGrande);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawRect(barX, xpY, barW, alturaBarraGrande);

        g2.setColor(Color.WHITE);
        String tXP = jugador.getXp() + " / " + jugador.getXpParaSiguienteNivel();
        g2.drawString(tXP, barX + barW / 2 - g2.getFontMetrics().stringWidth(tXP) / 2, xpY + 12);

        // C) BARRA DE ESTAMINA (Amarilla)
        int stamY = xpY + 20;
        int alturaBarraPeque = 10;
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(barX, stamY, barW, alturaBarraPeque);
        g2.setColor(jugador.isEscudoRoto() ? Color.GRAY : new Color(255, 215, 0));
        g2.fillRect(barX, stamY, (int) (barW * (jugador.getStamina() / jugador.getMaxStamina())), alturaBarraPeque);
        g2.setColor(Color.BLACK);
        g2.drawRect(barX, stamY, barW, alturaBarraPeque);

        // --- Oro y Nivel ---
        int textY = stamY + 25;

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(255, 215, 0));

        // Dibujamos la monedita al lado de los números si cargó bien
        if (GestorRecursos.monedaImg != null) {
            g2.drawImage(GestorRecursos.monedaImg, barX, textY - 13, 16, 16, null);
            g2.drawString(" " + jugador.getOro(), barX + 16, textY);
        } else {
            g2.drawString("Oro: " + jugador.getOro(), barX, textY);
        }

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Nivel: " + jugador.getNivel(), barX + 90, textY);

        // --- 2. POCIONES ---
        int slotX = 20;
        int slotY = 145;
        int slotSize = 50;

        int pocionSeleccionada = jugador.getPocionSeleccionada();
        int cantidad = 0;
        BufferedImage imgPocion = null;
        String nombrePocion = "";
        long ultimoUso = 0;

        if (pocionSeleccionada == 0) {
            cantidad = jugador.getPocionesVida();
            imgPocion = GestorRecursos.pocCuracionImg;
            nombrePocion = "Vida";
            ultimoUso = jugador.getUltimoUsoVida();
        } else if (pocionSeleccionada == 1) {
            cantidad = jugador.getPocionesFuerza();
            imgPocion = GestorRecursos.pocFuerzaImg;
            nombrePocion = "Fuerza";
            ultimoUso = jugador.getUltimoUsoFuerza();
        } else if (pocionSeleccionada == 2) {
            cantidad = jugador.getPocionesVelocidad();
            imgPocion = GestorRecursos.pocVelocidadImg;
            nombrePocion = "Velocidad";
            ultimoUso = jugador.getUltimoUsoVelocidad();
        }

        long tiempoActual = System.currentTimeMillis();
        float progresoCooldown = 0f;
        final long COOLDOWN = 5000; // Sincronizado con Jugador.java (5 segundos)
        if (tiempoActual - ultimoUso < COOLDOWN) {
            progresoCooldown = 1.0f - ((float) (tiempoActual - ultimoUso) / COOLDOWN);
        }

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("< Q", slotX, slotY - 5);
        g2.drawString("E >", slotX + slotSize - 22, slotY - 5);

        dibujarSlot(g2, slotX, slotY, slotSize, "F", cantidad, imgPocion, progresoCooldown);

        g2.setColor(Color.WHITE);
        g2.drawString(
                nombrePocion,
                slotX + (slotSize / 2) - (g2.getFontMetrics().stringWidth(nombrePocion) / 2),
                slotY + slotSize + 15);

        // --- 3. PODER MÁGICO ---
        int habX = 20;
        int habY = 640;
        int habSize = 50;

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("HABILIDAD (H)", habX, habY - 5);

        long ultimoUsoHab = jugador.getUltimoUsoHabilidad();
        long cooldownHab = jugador.getCooldownHabilidad();
        float progresoHabCooldown = 0f;
        long tiempoActualM = System.currentTimeMillis();

        if (tiempoActualM - ultimoUsoHab < cooldownHab) {
            progresoHabCooldown = 1.0f - ((float) (tiempoActualM - ultimoUsoHab) / cooldownHab);
        }

        // Fondo del Slot
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRoundRect(habX, habY, habSize, habSize, 10, 10);

        // Icono de la habilidad
        if (GestorRecursos.habilidad1Img != null) {
            // Dibujamos la imagen de 64x64 centrada/escalada dentro del slot
            g2.drawImage(GestorRecursos.habilidad1Img, habX + 5, habY + 5, habSize - 10, habSize - 10, null);
        } else {
            // Placeholder temporal en caso de que la imagen aún no se coloque en la carpeta
            g2.setColor(new Color(0, 150, 255));
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(habX + 12, habY + 38, habX + 38, habY + 12);
            g2.setColor(new Color(100, 200, 255));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(habX + 16, habY + 38, habX + 38, habY + 16);
        }

        // Borde
        g2.setColor(progresoHabCooldown <= 0 ? new Color(255, 255, 255, 120) : new Color(100, 100, 100, 50));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(habX, habY, habSize, habSize, 10, 10);

        // Sombra / Filtro de Cooldown
        if (progresoHabCooldown > 0) {
            g2.setColor(new Color(0, 0, 0, 180));
            int altoSombra = (int) (habSize * progresoHabCooldown);
            g2.fillRect(habX, habY + (habSize - altoSombra), habSize, altoSombra);
        }

        // Nota: Dibujado de barra de jefe sacada del bucle erróneo de habilidades (ya
        // no hay bucle)
        if (barraJefeNivel != null) {
            barraJefeNivel.dibujar((Graphics2D) g, jugador);
        }
    }

    private void dibujarSlot(Graphics2D g2, int x, int y, int size, String tecla, int cant, BufferedImage icon,
            float prog) {
        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRoundRect(x, y, size, size, 10, 10);
        g2.setColor(cant > 0 && prog <= 0 ? new Color(255, 255, 255, 120) : new Color(100, 100, 100, 50));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, size, size, 10, 10);
        if (cant > 0) {
            if (icon != null) {
                g2.drawImage(icon, x + 8, y + 8, size - 16, size - 16, null);
            } else {
                g2.setColor(Color.RED);
                g2.fillRoundRect(x + size / 4 + 2, y + size / 2 - 2, size / 2 - 4, size / 2 - 4, 4, 4);
                g2.fillRect(x + size / 2 - 3, y + size / 4 + 2, 6, size / 4);
            }
        } else {
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRoundRect(x + size / 4 + 2, y + size / 2 - 2, size / 2 - 4, size / 2 - 4, 4, 4);
        }
        if (prog > 0) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(x, y + (size - (int) (size * prog)), size, (int) (size * prog));
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("%.1fs", prog * 5), x + 2, y + size - 5); // Multiplicado por 5 segundos de
                                                                                  // cooldown
        }
        if (cant > 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            String c = "x" + cant;
            g2.drawString(c, x + size - g2.getFontMetrics().stringWidth(c) - 4, y + size - 5);
        }
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(Color.ORANGE);
        g2.drawString(tecla, x + 5, y + 13);
    }

    public void dibujarIndicadorG(Graphics2D g2, int x, int y) {
        // Efecto de flotación senoidal
        int floatY = (int) (Math.sin(System.currentTimeMillis() / 200.0) * 3); // Flotación más sutil
        int size = 25; // Más pequeño
        int pX = x - (size / 2);
        int pY = y - 35 + floatY; // Más cerca del objeto

        // Fondo del cuadrito
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(pX, pY, size, size, 6, 6);

        // Borde dorado
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(pX, pY, size, size, 6, 6);

        // Letra G
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("G", pX + 7, pY + 18);
    }
}
