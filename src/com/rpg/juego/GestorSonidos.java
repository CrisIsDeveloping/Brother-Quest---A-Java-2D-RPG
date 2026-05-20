package com.rpg.juego;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GestorSonidos {

    private static class SoundPool {
        Clip[] clips;
        int nextClip = 0;
        float gain = 0;

        public SoundPool(AudioFormat format, byte[] data, int count, float currentGain) {
            this.clips = new Clip[count];
            this.gain = currentGain;
            for (int i = 0; i < count; i++) {
                try {
                    clips[i] = AudioSystem.getClip();
                    clips[i].open(format, data, 0, data.length);
                    aplicarVolumenClip(clips[i], gain);
                } catch (Exception e) {
                    System.err.println("Error al pre-cargar clip [" + i + "]: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public void reproducir() {
            Clip c = clips[nextClip];
            c.stop();
            c.setFramePosition(0);
            c.start();
            nextClip = (nextClip + 1) % clips.length;
        }

        public void reproducirEnBucle() {
            Clip c = clips[0];
            if (!c.isRunning()) {
                c.stop();
                c.setFramePosition(0);
                // Aseguramos que los puntos de loop cubran todo el archivo
                c.setLoopPoints(0, -1);
                c.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }

        public void detener() {
            for (Clip c : clips) {
                c.stop();
                c.setFramePosition(0);
            }
        }

        public boolean estaReproduciendo() {
            for (Clip c : clips) {
                if (c.isRunning())
                    return true;
            }
            return false;
        }
    }

    private static Map<String, SoundPool> sonidos = new HashMap<>();
    private static float volumenMaestroSFX = 0.0f; // 0.0f es el máximo (normal), valores negativos reducen
    private static boolean musicaActivada = true;

    public enum TipoMusica {
        NINGUNA, MENU, JUEGO, COMBATE, TIENDA, BOSS, VICTORIA
    };

    private static TipoMusica musicaActual = TipoMusica.NINGUNA;

    private static float alphaJuego = 0.0f;
    private static float alphaCombate = 0.0f;
    private static float alphaMenu = 1.0f; // Empieza en el menú
    private static float alphaTienda = 0.0f;
    private static float alphaBoss = 0.0f;
    private static float alphaVictoria = 0.0f;

    public static boolean isMusicaActivada() {
        return musicaActivada;
    }

    public static void toggleMusica() {
        musicaActivada = !musicaActivada;
    }

    public static void setMusicaAmbiental(TipoMusica tipo) {
        musicaActual = tipo;
    }

    private static long ultimaActualizacionFades = System.currentTimeMillis();

    public static void actualizarFundidoMusica() {
        long ahora = System.currentTimeMillis();
        float deltaSecs = (ahora - ultimaActualizacionFades) / 1000.0f;
        ultimaActualizacionFades = ahora;

        // Evitamos que un pico de lag avance el fundido de golpe
        if (deltaSecs > 0.05f) {
            deltaSecs = 0.016f;
        }

        boolean juegoOn = musicaActivada && musicaActual == TipoMusica.JUEGO;
        boolean combateOn = musicaActivada && musicaActual == TipoMusica.COMBATE;
        boolean menuOn = musicaActivada && musicaActual == TipoMusica.MENU;
        boolean tiendaOn = musicaActivada && musicaActual == TipoMusica.TIENDA;
        boolean bossOn = musicaActivada && musicaActual == TipoMusica.BOSS;
        boolean victoriaOn = musicaActivada && musicaActual == TipoMusica.VICTORIA;

        // Velocidad de fundido (1.5 significa ~0.66 segundos para desvanecerse)
        float fadeSpeed = 1.5f;
        float dAlpha = fadeSpeed * deltaSecs;

        if (juegoOn && alphaJuego < 1.0f)
            alphaJuego = Math.min(1.0f, alphaJuego + dAlpha);
        else if (!juegoOn && alphaJuego > 0.0f)
            alphaJuego = Math.max(0.0f, alphaJuego - dAlpha);

        if (combateOn && alphaCombate < 1.0f)
            alphaCombate = Math.min(1.0f, alphaCombate + dAlpha);
        else if (!combateOn && alphaCombate > 0.0f)
            alphaCombate = Math.max(0.0f, alphaCombate - dAlpha);

        if (menuOn && alphaMenu < 1.0f)
            alphaMenu = Math.min(1.0f, alphaMenu + dAlpha);
        else if (!menuOn && alphaMenu > 0.0f)
            alphaMenu = Math.max(0.0f, alphaMenu - dAlpha);

        if (tiendaOn && alphaTienda < 1.0f)
            alphaTienda = Math.min(1.0f, alphaTienda + dAlpha);
        else if (!tiendaOn && alphaTienda > 0.0f)
            alphaTienda = Math.max(0.0f, alphaTienda - dAlpha);

        if (bossOn && alphaBoss < 1.0f)
            alphaBoss = Math.min(1.0f, alphaBoss + dAlpha);
        else if (!bossOn && alphaBoss > 0.0f)
            alphaBoss = Math.max(0.0f, alphaBoss - dAlpha);

        if (victoriaOn && alphaVictoria < 1.0f)
            alphaVictoria = Math.min(1.0f, alphaVictoria + dAlpha);
        else if (!victoriaOn && alphaVictoria > 0.0f)
            alphaVictoria = Math.max(0.0f, alphaVictoria - dAlpha);

        aplicarFadeA(MUSIC, alphaJuego);
        aplicarFadeA(MUSIC2, alphaCombate);
        aplicarFadeA(MUSICA_MENU, alphaMenu);
        aplicarFadeA(MUSICA_TIENDA, alphaTienda);
        aplicarFadeA(MUSICA_BOSS, alphaBoss);
        aplicarFadeA(MUSICA_FINAL, alphaVictoria);
    }

    private static void aplicarFadeA(String clave, float alpha) {
        SoundPool pool = sonidos.get(clave);
        if (pool == null || pool.clips.length == 0)
            return;
        Clip c = pool.clips[0];
        if (c != null && c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl control = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            if (control != null) {
                // Siempre mantenemos el clip reproduciendose para evitar los delays de Java
                // Sound Clip.loop() o cortes internos
                if (!c.isRunning()) {
                    c.loop(Clip.LOOP_CONTINUOUSLY);
                }

                if (alpha <= 0.001f) {
                    control.setValue(control.getMinimum()); // Silencio total en hardware
                } else {
                    // Conversión exacta a Decibeles con protección contra caídas severas a
                    // -Infinity
                    float volAdd = 20.0f * (float) Math.log10(alpha);
                    if (volAdd < -45.0f)
                        volAdd = -45.0f; // -45dB es inaudible en mezcla, previene un salto a -80dB

                    float valorFinal = pool.gain + volAdd;
                    valorFinal = Math.max(control.getMinimum(), Math.min(control.getMaximum(), valorFinal));
                    control.setValue(valorFinal);
                }
            }
        }
    }

    public static final String RUN_SONIDO = "run";
    public static final String ESCUDO = "shield";
    public static final String RODAR = "roll";
    public static final String ESPADA_1 = "swordswipe01";
    public static final String ESPADA_2 = "swordswipe02";
    public static final String ESPADA_3 = "swordswipe03";
    public static final String HABILIDAD = "habilidad";

    public static final String MUERTE_ESQUELETO = "skeletondie";
    public static final String SLIME_SALTO_1 = "slimejump1";
    public static final String SLIME_SALTO_2 = "slimejump2";
    public static final String HURT_1 = "hurt1";
    public static final String HURT_2 = "hurt2";
    public static final String HURT_3 = "hurt3";
    public static final String HURT_4 = "hurt4";
    public static final String SALTO_1 = "jump1";
    public static final String SALTO_2 = "jump2";
    public static final String MUSH_SALTO = "mushroomjump";
    public static final String AMBIENTE_BOSQUE = "forest";
    public static final String RECOGER_MONEDA = "pickup_coin";
    public static final String RECOGER_ITEM = "pickup_item";
    public static final String CARGA_ATAQUE = "charge1";
    public static final String SWING_ESPADA = "swing";
    public static final String RUN_NIGHTBORNE = "runnightborne";
    public static final String RUN_ESQUELETO = "runesqueleto";
    public static final String RUN_ELITE = "runelite";
    public static final String SELECT = "select";
    public static final String SWITCH = "switch";
    public static final String MENU_OPEN = "menu";
    public static final String ERROR = "error";
    public static final String MUSIC = "music";
    public static final String MUSIC2 = "music2";
    public static final String MUSICA_MENU = "menumusic";
    public static final String MUSICA_TIENDA = "storemusic";
    public static final String MUSICA_BOSS = "bossmusic";
    public static final String MUSICA_FINAL = "musicfinal"; // Pantalla de victoria
    public static final String NEXT = "next";

    public static final String BOSS_INTRO = "bossintro";
    public static final String BOSS_LAUGHT = "bosslaught";
    public static final String BOSS_ATTACK = "bossattack";
    public static final String BOSS_DEATH = "bossdeath";
    public static final String BOSS_ANGRY = "bossangry";

    public static void inicializar() {
        System.out.println("Cargando sonidos (Pool de Clips Optimizado)...");

        cargar("music.wav", MUSIC, 1, -4.0f);
        cargar("music2.wav", MUSIC2, 1, -4.0f);
        cargar("menumusic.wav", MUSICA_MENU, 1, -4.0f);
        cargar("storemusic.wav", MUSICA_TIENDA, 1, -4.0f);
        cargar("bossmusic.wav", MUSICA_BOSS, 1, -4.0f);
        cargar("musicfinal.wav", MUSICA_FINAL, 1, -4.0f);
        cargar("next.wav", NEXT, 3, 0.0f);

        cargar("bossintro.wav", BOSS_INTRO, 1, 0.0f);
        cargar("bosslaught.wav", BOSS_LAUGHT, 1, 0.0f);
        cargar("bossattack.wav", BOSS_ATTACK, 2, +4.0f);
        cargar("bossdeath.wav", BOSS_DEATH, 1, +4.0f);
        cargar("bossangry.wav", BOSS_ANGRY, 1, +4.0f);

        cargar("run.wav", RUN_SONIDO, 1, +4.0f);
        cargar("habilidad1.wav", HABILIDAD, 1, 0.0f);
        cargar("shield.wav", ESCUDO, 3, +4.0f);
        cargar("roll.wav", RODAR, 2, -2.0f);

        cargar("skeletondie.wav", MUERTE_ESQUELETO, 2, 0.0f);
        cargar("swordswipe01.wav", ESPADA_1, 2, 0.0f);
        cargar("swordswipe02.wav", ESPADA_2, 2, 0.0f);
        cargar("swordswipe03.wav", ESPADA_3, 2, 0.0f);
        cargar("slimejump1.wav", SLIME_SALTO_1, 2, 0.0f);
        cargar("slimejump2.wav", SLIME_SALTO_2, 2, 0.0f);
        cargar("hurt1.wav", HURT_1, 2, +5.0f);
        cargar("hurt2.wav", HURT_2, 2, +5.0f);
        cargar("hurt3.wav", HURT_3, 2, +5.0f);
        cargar("hurt4.wav", HURT_4, 2, +5.0f);
        cargar("jump1.wav", SALTO_1, 2, +5.0f);
        cargar("jump2.wav", SALTO_2, 2, +5.0f);
        cargar("mushroomjump.wav", MUSH_SALTO, 2, 0.0f);
        cargar("forest.wav", AMBIENTE_BOSQUE, 1, +5.0f);
        cargar("pickup_coin.wav", RECOGER_MONEDA, 5, 0.0f);
        cargar("pickup_item.wav", RECOGER_ITEM, 3, 0.0f);
        cargar("charge1.wav", CARGA_ATAQUE, 2, 0.0f);
        cargar("swing.wav", SWING_ESPADA, 2, 0.0f);
        cargar("runnightborne.wav", RUN_NIGHTBORNE, 1, +4.0f);
        cargar("runesqueleto.wav", RUN_ESQUELETO, 1, +4.0f);

        // Fallback: si no hay runelite.wav, usamos runesqueleto.wav
        if (new java.io.File("res/sounds/runelite.wav").exists()) {
            cargar("runelite.wav", RUN_ELITE, 1, +5.0f);
        } else {
            cargar("runesqueleto.wav", RUN_ELITE, 1, +5.0f);
        }

        cargar("select.wav", SELECT, 3, 0.0f);
        cargar("switch.wav", SWITCH, 3, 0.0f);
        cargar("menu.wav", MENU_OPEN, 2, 0.0f);
        cargar("error.wav", ERROR, 2, 0.0f);

        System.out.println("Sonidos listos en memoria.");
    }

    private static void cargar(String rutaArchivo, String nombreClave, int poolSize, float gain) {
        try {
            File archivo = new File("res/sounds/" + rutaArchivo);
            if (!archivo.exists()) {
                System.err.println("No se encontro el archivo: " + rutaArchivo + " (Buscado en: "
                        + archivo.getAbsolutePath() + ")");
                return;
            }

            try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(archivo)) {
                AudioFormat baseFormat = sourceStream.getFormat();

                // Forzamos la conversión a PCM_SIGNED para máxima compatibilidad
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false);

                try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                    byte[] data = convertedStream.readAllBytes();
                    sonidos.put(nombreClave, new SoundPool(targetFormat, data, poolSize, gain));
                }
            }
        } catch (Exception e) {
            System.err.println("Excepción crítica cargando " + rutaArchivo + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void reproducir(String nombreClave) {
        SoundPool pool = sonidos.get(nombreClave);
        if (pool != null)
            pool.reproducir();
    }

    public static void reproducirenLoop(String nombreClave) {
        SoundPool pool = sonidos.get(nombreClave);
        if (pool != null)
            pool.reproducirEnBucle();
    }

    public static void iniciarMusicasFondo() {
        // Pre-ajustar volúmenes antes de reproducir para evitar "glitches" o picos de
        // volumen al inicio
        aplicarFadeA(MUSIC, alphaJuego);
        aplicarFadeA(MUSIC2, alphaCombate);
        aplicarFadeA(MUSICA_MENU, alphaMenu);
        aplicarFadeA(MUSICA_TIENDA, alphaTienda);

        // MUSIC 1 - Con punto de loop especial de 32s si es necesario
        SoundPool pool1 = sonidos.get(MUSIC);
        if (pool1 != null && pool1.clips.length > 0) {
            Clip c1 = pool1.clips[0];
            if (!c1.isRunning()) {
                c1.setFramePosition(0);
                try {
                    int endFrame = (int) (c1.getFormat().getFrameRate() * 32.0f);
                    if (endFrame > 0 && endFrame < c1.getFrameLength()) {
                        c1.setLoopPoints(0, endFrame);
                    } else {
                        c1.setLoopPoints(0, -1);
                    }
                } catch (Exception e) {
                    c1.setLoopPoints(0, -1);
                }
                c1.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }

        // MUSIC 2 - Bucle estándar
        SoundPool pool2 = sonidos.get(MUSIC2);
        if (pool2 != null && pool2.clips.length > 0) {
            Clip c2 = pool2.clips[0];
            if (!c2.isRunning()) {
                c2.setFramePosition(0);
                c2.setLoopPoints(0, -1);
                c2.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }

        // MUSICA FINAL - Victoria (empieza silenciosa, el fade la sube cuando toque)
        SoundPool poolFinal = sonidos.get(MUSICA_FINAL);
        if (poolFinal != null && poolFinal.clips.length > 0) {
            Clip cf = poolFinal.clips[0];
            if (!cf.isRunning()) {
                cf.setFramePosition(0);
                cf.setLoopPoints(0, -1);
                cf.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
    }

    private static void aplicarVolumenClip(Clip clip, float gain) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float valorFinal = gain + volumenMaestroSFX;
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), valorFinal)));
        }
    }

    public static void setVolumenMaestroSFX(float nuevoVolumen) {
        volumenMaestroSFX = nuevoVolumen;
        for (SoundPool pool : sonidos.values()) {
            for (Clip c : pool.clips) {
                aplicarVolumenClip(c, pool.gain);
            }
        }
    }

    public static float getVolumenMaestroSFX() {
        return volumenMaestroSFX;
    }

    public static void detener(String nombreClave) {
        SoundPool pool = sonidos.get(nombreClave);
        if (pool != null)
            pool.detener();
    }

    public static boolean estaReproduciendo(String nombreClave) {
        SoundPool pool = sonidos.get(nombreClave);
        return pool != null && pool.estaReproduciendo();
    }

    public static void reproducirEspadaAleatoria() {
        int r = (int) (Math.random() * 3);
        if (r == 0)
            reproducir(ESPADA_1);
        else if (r == 1)
            reproducir(ESPADA_2);
        else
            reproducir(ESPADA_3);
    }

    public static void reproducirHurtAleatorio() {
        int r = (int) (Math.random() * 4);
        if (r == 0)
            reproducir(HURT_1);
        else if (r == 1)
            reproducir(HURT_2);
        else if (r == 2)
            reproducir(HURT_3);
        else
            reproducir(HURT_4);
    }

    public static void reproducirSaltoAleatorio() {
        int r = (int) (Math.random() * 2);
        if (r == 0)
            reproducir(SALTO_1);
        else
            reproducir(SALTO_2);
    }
}
