package com.rpg.juego;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GestorRecursos {

    public static List<BufferedImage> capasFondo = new ArrayList<>();

    public static BufferedImage[][] animacionesJugador = new BufferedImage[10][10];
    public static BufferedImage[][] animacionesHermano = new BufferedImage[10][10];

    public static BufferedImage[][] animacionesSlime;
    public static BufferedImage[][] animacionesSlimeAzul;
    public static BufferedImage[][] animacionesSlimeRojo;
    public static BufferedImage[][] animacionesSlimeNegro;

    public static BufferedImage[][] animacionesEsqBlanco;
    public static BufferedImage[][] animacionesEsqOro;

    public static BufferedImage[][] animacionesMushroom;
    public static BufferedImage[][] animacionesNightBorne;

    public static BufferedImage monedaImg;
    public static BufferedImage tituloImg;

    public static BufferedImage[] animacionOnda = new BufferedImage[4];

    public static void cargarRecursos() {
        System.out.println("--- INICIALIZANDO PIPELINE DE CARGA DE ASSETS ---");
        capasFondo.clear();

        cargarCapaFondo("/layer_1.png");
        cargarCapaFondo("/layer_2.png");
        cargarCapaFondo("/layer_3.png");
        cargarCapaFondo("/layer_4.png");
        cargarCapaFondo("/layer_5.png");
        cargarCapaFondo("/layer_6.png");
        cargarCapaFondo("/layer_7.png");

        cargarJugador();
        cargarMoneda();
        cargarTitulo();
        cargarOndaMagica();

        cargarVillanoIntro();
        cargarHermanoIntro();
        cargarAnimacionesNightBorne();

        try {
            animacionesSlime      = cargarSpriteSlime("/slime_verde.png");
            animacionesSlimeAzul  = cargarSpriteSlime("/slime_azul.png");
            animacionesSlimeRojo  = cargarSpriteSlime("/slime_rojo.png");
            animacionesSlimeNegro = cargarSpriteSlime("/slime_negro.png");

            // Instanciacion de matrices de sprites para entidades Esqueleto
            animacionesEsqBlanco = cargarCarpetaEsqueleto("blanco");
            animacionesEsqOro    = cargarCarpetaEsqueleto("oro");

            // Carga de animaciones del Mushroom
            animacionesMushroom  = cargarCarpetaMushroom();

        } catch (Exception e) {
            System.err.println("Excepcion I/O en el subsistema de carga de entidades enemigas");
        }
        System.out.println("--- CARGA FINALIZADA ---");
    }

    private static void cargarCapaFondo(String ruta) {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream(ruta);
            if (is != null) {
                capasFondo.add(ImageIO.read(is));
                System.out.println("Fondo procesado: " + ruta);
            } else {
                System.err.println("Recurso no localizado: " + ruta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cargarJugador() {
        try {
            BufferedImage sheet = ImageIO.read(GestorRecursos.class.getResourceAsStream("/caballero.png"));
            animacionesJugador = new BufferedImage[10][10];

            for (int j = 0; j < 10; j++) {
                for (int i = 0; i < 10; i++) {
                    if (i * 64 < sheet.getWidth() && j * 64 < sheet.getHeight()) {
                        animacionesJugador[j][i] = sheet.getSubimage(i * 64, j * 64, 64, 64);
                    }
                }
            }

            InputStream isMagic = GestorRecursos.class.getResourceAsStream("/corte_magico.png");
            if (isMagic != null) {
                BufferedImage magicSheet = ImageIO.read(isMagic);
                for (int i = 0; i < 10; i++) {
                    if (i * 64 < magicSheet.getWidth()) {
                        animacionesJugador[9][i] = magicSheet.getSubimage(i * 64, 0, 64, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando spritesheet de la entidad Jugador");
        }
    }

    private static void cargarOndaMagica() {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream("/onda_proyectil.png");
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < 4; i++) {
                    animacionOnda[i] = sheet.getSubimage(i * 64, 0, 64, 64);
                }
                System.out.println("Onda procesada: 4 frames en buffer.");
            }
        } catch (Exception e) {}
    }

    private static BufferedImage[][] cargarSpriteSlime(String ruta) {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream(ruta);
            if (is == null) return null;
            BufferedImage sheet = ImageIO.read(is);

            int w = sheet.getWidth() / 4;
            int h = sheet.getHeight() / 3;

            BufferedImage[][] temp = new BufferedImage[3][4];
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 4; i++) {
                    temp[j][i] = sheet.getSubimage(i * w, j * h, w, h);
                }
            }
            return temp;
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage[][] cargarCarpetaEsqueleto(String color) {
        BufferedImage[][] temp = new BufferedImage[6][13];
        String rutaBase = "/esq_" + color + "/esq_" + color + "_";

        temp[0] = extraerFilaDeImagen(rutaBase + "idle.png", 8);
        temp[1] = extraerFilaDeImagen(rutaBase + "walk.png", 10);
        temp[2] = extraerFilaDeImagen(rutaBase + "attack1.png", 10);
        temp[3] = extraerFilaDeImagen(rutaBase + "attack2.png", 9);
        temp[4] = extraerFilaDeImagen(rutaBase + "hurt.png", 5);
        temp[5] = extraerFilaDeImagen(rutaBase + "die.png", 13);

        return temp;
    }

    // --- CORREGIDO: RESTAURADO EL ANCHO A 96 PARA QUE COINCIDA CON LA HITBOX ---
    private static BufferedImage[] extraerFilaDeImagen(String ruta, int cantidadFrames) {
        BufferedImage[] fila = new BufferedImage[13];
        try {
            var is = GestorRecursos.class.getResourceAsStream(ruta);
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < cantidadFrames; i++) {
                    // Ahora recorta a 96 de ancho por 64 de alto
                    if (i * 96 < sheet.getWidth()) {
                        fila[i] = sheet.getSubimage(i * 96, 0, 96, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando: " + ruta);
        }
        return fila;
    }

    // --- METODO EXCLUSIVO PARA EL MUSHROOM ---
    private static BufferedImage[][] cargarCarpetaMushroom() {
        BufferedImage[][] temp = new BufferedImage[6][18];
        String rutaBase = "/mush/mush_";

        temp[0] = extraerFilaMushroom(rutaBase + "idle.png", 7);
        temp[1] = extraerFilaMushroom(rutaBase + "run.png", 8);
        temp[2] = extraerFilaMushroom(rutaBase + "attack.png", 10);
        temp[3] = extraerFilaMushroom(rutaBase + "damage.png", 5);
        temp[4] = extraerFilaMushroom(rutaBase + "die.png", 15);
        temp[5] = extraerFilaMushroom(rutaBase + "stun.png", 18);

        return temp;
    }

    private static BufferedImage[] extraerFilaMushroom(String ruta, int cantidadFrames) {
        BufferedImage[] fila = new BufferedImage[18];
        try {
            var is = GestorRecursos.class.getResourceAsStream(ruta);
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < cantidadFrames; i++) {
                    if (i * 80 < sheet.getWidth()) {
                        fila[i] = sheet.getSubimage(i * 80, 0, 80, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando hongo: " + ruta);
        }
        return fila;
    }

    private static void cargarMoneda() {
        try {
            monedaImg = ImageIO.read(GestorRecursos.class.getResourceAsStream("/coin.png"));
        } catch (Exception e) {
            System.err.println("Error procesando asset de moneda.");
        }
    }

    private static void cargarTitulo() {
        try {
            tituloImg = ImageIO.read(GestorRecursos.class.getResourceAsStream("/title.png"));
            System.out.println("Asset del titulo procesado con exito.");
        } catch (Exception e) {
            System.err.println("Error procesando asset del titulo.");
        }
    }

    private static void cargarVillanoIntro() {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream("/nightborne.png");
        } catch (Exception e) {}
    }

    private static void cargarAnimacionesNightBorne() {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream("/nightborne.png");
            if (is == null) return;
            BufferedImage sheet = ImageIO.read(is);

            animacionesNightBorne = new BufferedImage[6][22];
            int[] framesPorFila = {4, 9, 6, 12, 5, 22};

            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < framesPorFila[j]; i++) {
                    animacionesNightBorne[j][i] = sheet.getSubimage(i * 80, j * 80, 80, 80);
                }
            }
        } catch (Exception e) {}
    }

    private static void cargarHermanoIntro() {
        try {
            InputStream is = GestorRecursos.class.getResourceAsStream("/brother.png");
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int j = 0; j < 10; j++) {
                    for (int i = 0; i < 10; i++) {
                        if (i * 64 < sheet.getWidth() && j * 64 < sheet.getHeight()) {
                            animacionesHermano[j][i] = sheet.getSubimage(i * 64, j * 64, 64, 64);
                        }
                    }
                }
                System.out.println("Spritesheet del modulo Hermano procesado con exito.");
            }
        } catch (Exception e) {
            System.err.println("Error de lectura I/O en brother.png");
        }
    }
}