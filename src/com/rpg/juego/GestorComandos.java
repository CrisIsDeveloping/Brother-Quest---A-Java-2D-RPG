package com.rpg.juego;

import java.util.*;

public class GestorComandos {

    public static String ejecutar(String input, GamePanel gp) {
        if (input == null || input.isEmpty()) return "";
        
        String[] partes = input.trim().split("\\s+");
        String comando = partes[0].toLowerCase();

        try {
            switch (comando) {
                case "/help":
                case "/?":
                    return "Comandos: /summon <tipo> <color> <hp> <dmg> [pos], /set <vida|dmg|xp> <v>, /clear, /gold, /god";
                case "/summon":
                    return procesarSummon(partes, gp);
                case "/tp":
                    return procesarTp(partes, gp);
                case "/set":
                    return procesarSet(partes, gp);
                case "/invincible":
                case "/god":
                    gp.getJugador().setInvencible(!gp.getJugador().isInvencible());
                    return "Modo invencible: " + (gp.getJugador().isInvencible() ? "ON" : "OFF");
                case "/clear":
                    Jugador j = gp.getJugador();
                    int eliminados = 0;
                    Iterator<EnemigoBase> it = gp.getEnemigos().iterator();
                    while (it.hasNext()) {
                        EnemigoBase e = it.next();
                        double dist = Math.hypot(e.getX() - j.getX(), e.getY() - j.getY());
                        if (dist <= 500) {
                            it.remove();
                            eliminados++;
                        }
                    }
                    return "Eliminados " + eliminados + " enemigos en un radio de 500px.";
                case "/gold":
                case "/oro":
                    if (partes.length > 1) {
                        int amount = Integer.parseInt(partes[1]);
                        gp.getJugador().setOro(gp.getJugador().getOro() + amount);
                        return "Oro añadido: " + amount;
                    }
                    gp.getJugador().setOro(gp.getJugador().getOro() + 1000);
                    return "Oro añadido: 1000";
                default:
                    return "Comando desconocido: " + comando + ". Usa /help";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static List<String> getListaSugerencias(String input) {
        List<String> sugerencias = new ArrayList<>();
        if (input == null) return sugerencias;
        
        String inputTrimeado = input;
        String[] partes = inputTrimeado.split("\\s+", -1); // El -1 es clave para atrapar el espacio final
        String ultimaPalabra = partes[partes.length - 1].toLowerCase();

        // 1. Sugerir comandos base
        if (partes.length == 1) {
            String[] comandos = {"/summon", "/set", "/invincible", "/god", "/clear", "/gold", "/oro", "/help"};
            for (String s : comandos) {
                if (s.startsWith(ultimaPalabra)) sugerencias.add(s);
            }
        } 
        // 2. Sugerencias contextuales para /summon
        else if (partes[0].equalsIgnoreCase("/summon")) {
            if (partes.length == 2) { // Entidad
                String[] entidades = {"slime", "esqueleto", "elite", "mushroom", "nightborne"};
                for (String e : entidades) {
                    if (e.startsWith(ultimaPalabra)) sugerencias.add(e);
                }
            } else if (partes.length == 3) { // Color/Tipo
                String entidad = partes[1].toLowerCase();
                String[] tipos = {};
                if (entidad.equals("slime")) tipos = new String[]{"verde", "azul", "rojo", "negro"};
                else if (entidad.equals("esqueleto")) tipos = new String[]{"blanco", "dorado"};
                
                if (tipos.length > 0) {
                    for (String t : tipos) {
                        if (t.startsWith(ultimaPalabra)) sugerencias.add(t);
                    }
                } else {
                    sugerencias.addAll(Arrays.asList("100", "200", "500", "1000")); // Sugerencia de vida si no hay tipo
                }
            } else if (partes.length == 4) { // Vida (si ya hay tipo)
                sugerencias.addAll(Arrays.asList("100", "200", "500", "1500"));
            } else if (partes.length == 5) { // Daño
                sugerencias.addAll(Arrays.asList("10", "25", "50", "100"));
            } else if (partes.length == 6) { // Posición
                String[] pos = {"front", "near", "here"};
                for (String p : pos) {
                    if (p.startsWith(ultimaPalabra)) sugerencias.add(p);
                }
            }
        }
        // 3. Sugerencias contextuales para /set
        else if (partes[0].equalsIgnoreCase("/set")) {
            if (partes.length == 2) {
                String[] stats = {"vida", "hp", "dano", "damage", "xp"};
                for (String s : stats) {
                    if (s.startsWith(ultimaPalabra)) sugerencias.add(s);
                }
            } else if (partes.length == 3) {
                sugerencias.addAll(Arrays.asList("10", "50", "100", "1000"));
            }
        }

        return sugerencias;
    }

    private static String procesarSummon(String[] partes, GamePanel gp) {
        if (partes.length < 2) return "Sintaxis: /summon <entidad> <color> <hp> <dmg> [pos]";
        
        String entidad = partes[1].toLowerCase();
        Jugador j = gp.getJugador();
        
        // Valores por defecto
        String tipoCustom = (partes.length > 2) ? partes[2].toUpperCase() : "VERDE";
        int vidaCustom = (partes.length > 3) ? Integer.parseInt(partes[3]) : -1;
        int danoCustom = (partes.length > 4) ? Integer.parseInt(partes[4]) : -1;
        String pos = (partes.length > 5) ? partes[5].toLowerCase() : "front";

        int spawnX = j.getX() + (j.isMirandoDerecha() ? 400 : -400);
        int spawnY = j.getY();
        
        if (pos.equals("near") || pos.equals("here")) {
            spawnX = j.getX();
            spawnY = j.getY();
        }

        EnemigoBase nuevo = null;
        switch (entidad) {
            case "slime":
                nuevo = new EnemigoSlime(spawnX, spawnY, tipoCustom);
                break;
            case "esqueleto":
                nuevo = new EnemigoEsqueleto(spawnX, spawnY, tipoCustom.equals("VERDE") ? "blanco" : tipoCustom.toLowerCase());
                break;
            case "elite":
                nuevo = new EnemigoEsqueletoElite(spawnX, spawnY);
                break;
            case "mushroom":
                nuevo = new EnemigoMushroom(spawnX, spawnY);
                break;
            case "nightborne":
                nuevo = new EnemigoNightBorne(spawnX, spawnY);
                break;
        }

        if (nuevo != null) {
            nuevo.establecerNivel(j.getNivel());
            if (vidaCustom != -1) { nuevo.setVidaMax(vidaCustom); nuevo.setVida(vidaCustom); }
            if (danoCustom != -1) nuevo.setDano(danoCustom);
            
            gp.getEnemigos().add(nuevo);
            
            String msg = "Invocado " + entidad;
            // Solo añadir el tipo si la entidad lo soporta visualmente
            if (!entidad.equals("mushroom") && !entidad.equals("nightborne")) {
                msg += " " + tipoCustom;
            }
            msg += " (Vida: " + (vidaCustom == -1 ? "Base" : vidaCustom) + ")";
            return msg;
        }

        return "Entidad no reconocida: " + entidad;
    }
    private static String procesarSet(String[] partes, GamePanel gp) {
        if (partes.length < 3) return "Uso: /set <vida|damage|xp> <valor>";
        
        String stat = partes[1].toLowerCase();
        int valor = Integer.parseInt(partes[2]);
        Jugador j = gp.getJugador();

        switch (stat) {
            case "vida":
            case "hp":
                j.setVidaMax(valor);
                j.setVida(valor);
                return "Vida establecida en " + valor;
            case "damage":
            case "dano":
                j.setDano(valor);
                return "Daño establecido en " + valor;
            case "xp":
                j.ganarXP(valor);
                return "XP añadida: " + valor;
            default:
                return "Stat desconocido: " + stat;
        }
    }
    private static String procesarTp(String[] partes, GamePanel gp) {
        if (partes.length < 2) return "Uso: /tp <X_coords>";
        try {
            int x = Integer.parseInt(partes[1]);
            gp.getJugador().setX(x);
            gp.getSpawner().saltarHordasHasta(x);
            gp.setCameraX(x - 400);
            return "Teletransportado a X=" + x;
        } catch (NumberFormatException e) {
            return "Error: la coordenada debe ser numérica.";
        }
    }
}
