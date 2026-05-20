<<<<<<< HEAD
package com.rpg.juego;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.awt.image.BufferedImage;
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
    
    // Iconos de items y pociones
    public static BufferedImage pocCuracionImg;
    public static BufferedImage pocVelocidadImg;
    public static BufferedImage pocFuerzaImg;
    public static BufferedImage logoVidaImg;
    public static BufferedImage logoDanoImg;
    public static BufferedImage tituloImg;
    public static BufferedImage habilidad1Img;

    public static BufferedImage faceSlime;
    public static BufferedImage faceCaballero;
    public static BufferedImage faceBoss;
    public static BufferedImage faceEsqueleto;
    public static BufferedImage faceEsqueletoElite;
    public static BufferedImage faceNpc;

    public static BufferedImage[] bgMenu;
    public static BufferedImage bossBackgroundImg;
    public static BufferedImage portalImg;
    
    // animacionesDemon[estado][frame]:  0=idle(6), 1=cleave(15), 2=take_hit(5), 3=death(22)
    public static BufferedImage[][] animacionesDemon;

    // Imágenes del Trader
    public static BufferedImage[] traderIdle;
    public static BufferedImage[] traderIdle2;
    public static BufferedImage[] traderIdle3;
    public static BufferedImage[] traderDialogue;
    public static BufferedImage[] traderApproval;

    public static BufferedImage[] animacionOnda = new BufferedImage[4];

    public static void cargarRecursos() {
        System.out.println("--- CARGANDO IMÁGENES DEL JUEGO ---");
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

            // Recortamos las imágenes de los esqueletos según su color
            animacionesEsqBlanco = cargarCarpetaEsqueleto("blanco");
            animacionesEsqOro    = cargarCarpetaEsqueleto("oro");

            animacionesMushroom  = cargarCarpetaMushroom();

        } catch (Exception e) {
            System.err.println("No se pudieron cargar los enemigos");
        }

        try {
            traderIdle = cargarTiraTrader("/trader/Idle.png", 5);
            traderIdle2 = cargarTiraTrader("/trader/Idle_2.png", 5);
            traderIdle3 = cargarTiraTrader("/trader/Idle_3.png", 15);
            traderDialogue = cargarTiraTrader("/trader/Dialogue.png", 5);
            traderApproval = cargarTiraTrader("/trader/Approval.png", 5);
        } catch (Exception e) {
            System.err.println("No se pudo cargar animaciones trader");
        }

        try {
            InputStream is1 = abrirInputStream("/dialogs/faceslime.png");
            if (is1 != null) faceSlime = ImageIO.read(is1);
            
            InputStream is2 = abrirInputStream("/dialogs/facecaballero.png");
            if (is2 != null) faceCaballero = ImageIO.read(is2);

            InputStream isBoss = abrirInputStream("/dialogs/bossface.png");
            if (isBoss == null) isBoss = abrirInputStream("/bossface.png");
            if (isBoss != null) faceBoss = ImageIO.read(isBoss);

            InputStream is3 = abrirInputStream("/dialogs/faceesqueleto.png");
            if (is3 != null) faceEsqueleto = ImageIO.read(is3);

            InputStream is4 = abrirInputStream("/dialogs/faceesqueletoelite.png");
            if (is4 != null) faceEsqueletoElite = ImageIO.read(is4);

            InputStream is5 = abrirInputStream("/dialogs/facenpc.png");
            if (is5 == null) is5 = abrirInputStream("/facenpc.png");
            if (is5 != null) faceNpc = ImageIO.read(is5);
        } catch (Exception e) {
            System.err.println("Error cargando faces de dialogos: " + e.getMessage());
        }
        
        cargarAssetsBossFase1();

        System.out.println("--- TODO LISTO ---");
        
        cargarIconosItems();
    }

    private static void cargarAssetsBossFase1() {
        try {
            InputStream isBackground = abrirInputStream("/bossbackground.png");
            if (isBackground != null) bossBackgroundImg = ImageIO.read(isBackground);

            InputStream isPortal = abrirInputStream("/portal.png");
            if (isPortal != null) portalImg = ImageIO.read(isPortal);

            // Carga completa de animaciones del demonio
            // 0=idle(6), 1=walk(12), 2=cleave(15), 3=take_hit(5), 4=death(22)
            String[][] demonDirs = {
                { "01_demon_idle",    "demon_idle_",     "6"  },
                { "02_demon_walk",    "demon_walk_",     "12" },
                { "03_demon_cleave",  "demon_cleave_",   "15" },
                { "04_demon_take_hit","demon_take_hit_", "5"  },
                { "05_demon_death",   "demon_death_",    "22" }
            };
            animacionesDemon = new BufferedImage[demonDirs.length][];
            for (int estado = 0; estado < demonDirs.length; estado++) {
                int cantFrames = Integer.parseInt(demonDirs[estado][2]);
                animacionesDemon[estado] = new BufferedImage[cantFrames];
                for (int i = 1; i <= cantFrames; i++) {
                    String ruta = "/demon/" + demonDirs[estado][0] + "/" + demonDirs[estado][1] + i + ".png";
                    InputStream isFrame = abrirInputStream(ruta);
                    if (isFrame != null) {
                        animacionesDemon[estado][i - 1] = ImageIO.read(isFrame);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando assets fase boss: " + e.getMessage());
        }
    }

    private static void cargarIconosItems() {
        try {
            InputStream is;
            is = abrirInputStream("/poc_curacion.png");
            if (is != null) pocCuracionImg = ImageIO.read(is);
            
            is = abrirInputStream("/poc_velocidad.png");
            if (is != null) pocVelocidadImg = ImageIO.read(is);
            
            is = abrirInputStream("/poc_fuerza.png");
            if (is != null) pocFuerzaImg = ImageIO.read(is);
            
            is = abrirInputStream("/logo_mejoravida.png");
            if (is != null) logoVidaImg = ImageIO.read(is);
            
            is = abrirInputStream("/logo_mejoradano.png");
            if (is != null) logoDanoImg = ImageIO.read(is);
            
            is = abrirInputStream("/habilidad1.png");
            if (is != null) habilidad1Img = ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("Error cargando iconos de items: " + e.getMessage());
        }
    }

    private static InputStream abrirInputStream(String rutaRecurso) {
        // 1) Intento por classpath (lo que ya hacías)
        InputStream is = GestorRecursos.class.getResourceAsStream(rutaRecurso);
        if (is != null) return is;

        // 2) Fallback por filesystem: ./res/...
        String sinSlash = rutaRecurso.startsWith("/") ? rutaRecurso.substring(1) : rutaRecurso;
        File archivo = new File("res", sinSlash);
        try {
            if (archivo.exists()) return new FileInputStream(archivo);
        } catch (Exception e) {
            // Ignoramos y dejamos que devuelva null
        }
        return null;
    }

    private static void cargarCapaFondo(String ruta) {
        try {
            InputStream is = abrirInputStream(ruta);
            if (is != null) {
                capasFondo.add(ImageIO.read(is));
                System.out.println("Fondo listo: " + ruta);
            } else {
                System.err.println("Falta esta imagen: " + ruta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cargarJugador() {
        try {
            BufferedImage sheet = ImageIO.read(abrirInputStream("/caballero.png"));
            animacionesJugador = new BufferedImage[10][10];

            for (int j = 0; j < 10; j++) {
                for (int i = 0; i < 10; i++) {
                    if (i * 64 < sheet.getWidth() && j * 64 < sheet.getHeight()) {
                        animacionesJugador[j][i] = sheet.getSubimage(i * 64, j * 64, 64, 64);
                    }
                }
            }

            InputStream isMagic = abrirInputStream("/corte_magico.png");
            if (isMagic != null) {
                BufferedImage magicSheet = ImageIO.read(isMagic);
                for (int i = 0; i < 10; i++) {
                    if (i * 64 < magicSheet.getWidth()) {
                        animacionesJugador[9][i] = magicSheet.getSubimage(i * 64, 0, 64, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error recortando al jugador");
        }
    }

    private static void cargarOndaMagica() {
        try {
            InputStream is = abrirInputStream("/onda_proyectil.png");
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < 4; i++) {
                    animacionOnda[i] = sheet.getSubimage(i * 64, 0, 64, 64);
                }
                System.out.println("Poder mágico listo.");
            }
        } catch (Exception e) {}
    }

    private static BufferedImage[][] cargarSpriteSlime(String ruta) {
        try {
            InputStream is = abrirInputStream(ruta);
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

    // Recorta las imágenes largas donde vienen todas las fotos del esqueleto en fila
    private static BufferedImage[] extraerFilaDeImagen(String ruta, int cantidadFrames) {
        BufferedImage[] fila = new BufferedImage[13];
        try {
            var is = abrirInputStream(ruta);
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < cantidadFrames; i++) {
                    if (i * 96 < sheet.getWidth()) {
                        fila[i] = sheet.getSubimage(i * 96, 0, 96, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Falta la imagen: " + ruta);
        }
        return fila;
    }

    public static BufferedImage[] cargarTiraSprite1D(String ruta, int cantidadFrames) {
        BufferedImage img = null;
        try {
            InputStream is = abrirInputStream(ruta);
            if (is != null) img = ImageIO.read(is);
        } catch (Exception e) { }

        if (img == null) return new BufferedImage[cantidadFrames];

        int imgAncho = img.getWidth();
        int imgAlto = img.getHeight();
        int frameWidth = imgAncho / cantidadFrames;

        BufferedImage[] frames = new BufferedImage[cantidadFrames];
        for (int i = 0; i < cantidadFrames; i++) {
            frames[i] = img.getSubimage(i * frameWidth, 0, frameWidth, imgAlto);
        }
        return frames;
    }

    private static BufferedImage[] cargarTiraTrader(String ruta, int cantidadFrames) {
        BufferedImage img = null;
        try {
            InputStream is = abrirInputStream(ruta);
            if (is != null) img = ImageIO.read(is);
        } catch (Exception e) { }

        if (img == null) return new BufferedImage[cantidadFrames];

        // Forzamos 128x128 según indicación del usuario para el mercader
        int frameW = 128;
        int frameH = 128;
        BufferedImage[] frames = new BufferedImage[cantidadFrames];
        for (int i = 0; i < cantidadFrames; i++) {
            if (i * frameW < img.getWidth()) {
                frames[i] = img.getSubimage(i * frameW, 0, frameW, frameH);
            }
        }
        return frames;
    }

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
            var is = abrirInputStream(ruta);
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int i = 0; i < cantidadFrames; i++) {
                    if (i * 80 < sheet.getWidth()) {
                        fila[i] = sheet.getSubimage(i * 80, 0, 80, 64);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error con el hongo: " + ruta);
        }
        return fila;
    }

    private static void cargarMoneda() {
        try {
            monedaImg = ImageIO.read(abrirInputStream("/coin.png"));
        } catch (Exception e) {
            System.err.println("Error con la monedita de oro.");
        }
    }

    private static void cargarTitulo() {
        try {
            tituloImg = ImageIO.read(abrirInputStream("/title.png"));
            System.out.println("Logo listo.");
        } catch (Exception e) {
            System.err.println("Error con el logo del titulo.");
        }
    }

    private static void cargarVillanoIntro() {
        try {
            InputStream is = abrirInputStream("/nightborne.png");
            if (is != null) is.close(); // La carga real la hace `cargarAnimacionesNightBorne`
        } catch (Exception e) {}
    }

    private static void cargarAnimacionesNightBorne() {
        try {
            InputStream is = abrirInputStream("/nightborne.png");
            // Si no existe el asset en el classpath, evitamos NPE en tiempo de ejecución.
            if (is == null) {
                animacionesNightBorne = new BufferedImage[6][22];
                System.err.println("Falta la imagen: /nightborne.png (no está en el classpath)");
                return;
            }
            BufferedImage sheet = ImageIO.read(is);

            animacionesNightBorne = new BufferedImage[6][22];
            int[] framesPorFila = {4, 9, 6, 12, 5, 22};

            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < framesPorFila[j]; i++) {
                    animacionesNightBorne[j][i] = sheet.getSubimage(i * 80, j * 80, 80, 80);
                }
            }
        } catch (Exception e) {
            animacionesNightBorne = new BufferedImage[6][22];
            System.err.println("Error cargando /nightborne.png: " + e.getMessage());
        }
    }

    private static void cargarHermanoIntro() {
        try {
            InputStream is = abrirInputStream("/brother.png");
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                for (int j = 0; j < 10; j++) {
                    for (int i = 0; i < 10; i++) {
                        if (i * 64 < sheet.getWidth() && j * 64 < sheet.getHeight()) {
                            animacionesHermano[j][i] = sheet.getSubimage(i * 64, j * 64, 64, 64);
                        }
                    }
                }
                System.out.println("El hermano cargo bien.");
            }
        } catch (Exception e) {
            System.err.println("Error buscando al hermano en brother.png");
        }
    }
}
=======
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
>>>>>>> da25f6dd6bf3c69498f22ffaa92c786d38130149
