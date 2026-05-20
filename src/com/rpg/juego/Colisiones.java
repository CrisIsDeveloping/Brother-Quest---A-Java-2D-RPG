package com.rpg.juego;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;

public class Colisiones {

    public void manejarColisionAtaqueEnemigo(Jugador jugador, EnemigoBase e, List<TextoDano> textosDano) {
        // 1) Enemigos con "arma" (box de ataque)
        if (e instanceof EnemigoEsqueletoElite
                || e instanceof EnemigoNightBorne
                || e instanceof EnemigoEsqueleto
                || e instanceof EnemigoMushroom
                || e instanceof EnemigoJefeDemonio) {

            Rectangle arma = e.getAttackBox();
            if (arma != null && arma.intersects(jugador.getBounds()) && jugador.getVida() > 0) {
                if (e instanceof EnemigoJefeDemonio) {
                    EnemigoJefeDemonio boss = (EnemigoJefeDemonio) e;
                    if (!boss.isGolpeRegistradoBoss()) {
                        boss.setGolpeRegistradoBoss(true);
                        procesarDanoAJugador(jugador, e, textosDano);
                    }
                } else if (e instanceof EnemigoEsqueletoElite) {
                    EnemigoEsqueletoElite esq = (EnemigoEsqueletoElite) e;
                    if (!esq.isGolpeRegistrado()) {
                        esq.setGolpeRegistrado(true);
                        procesarDanoAJugador(jugador, e, textosDano);
                    }
                } else if (e instanceof EnemigoNightBorne) {
                    EnemigoNightBorne nb = (EnemigoNightBorne) e;
                    if (nb.isAtacando() && nb.getAniIndex() == 10 && !nb.isGolpeRegistrado()) {
                        procesarDanoAJugador(jugador, e, textosDano);
                        nb.setGolpeRegistrado(true);
                    }
                } else if (e instanceof EnemigoEsqueleto) {
                    EnemigoEsqueleto esqNormal = (EnemigoEsqueleto) e;
                    if (esqNormal.isAtacando() && !esqNormal.isGolpeRegistrado()) {
                        procesarDanoAJugador(jugador, e, textosDano);
                        esqNormal.setGolpeRegistrado(true);
                    }
                } else if (e instanceof EnemigoMushroom) {
                    EnemigoMushroom hongo = (EnemigoMushroom) e;
                    if (hongo.isAtacando() && !hongo.isGolpeRegistrado()) {
                        procesarDanoAJugador(jugador, e, textosDano);
                        hongo.setGolpeRegistrado(true);
                    }
                }
            }
            if (!(e instanceof EnemigoJefeDemonio)) {
                return;
            }
        }

        // 2) Enemigos tipo "cuerpo a cuerpo" sin arma box (slime) y colisión corporal del Boss
        if (e instanceof EnemigoJefeDemonio) {
            EnemigoJefeDemonio jefe = (EnemigoJefeDemonio) e;
            if (jugador.getBounds().intersects(e.getBounds())) {
                if (jugador.isRodando()) {
                    if (!jugador.isEsquivadoBossEsteRoll()) {
                        jugador.setEsquivadoBossEsteRoll(true);
                        textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "ESQUIVADO", Color.YELLOW));
                    }
                } else if (jefe.puedeHacerDanoCuerpo()) {
                    procesarDanoAJugador(jugador, e, textosDano);
                    jefe.reiniciarCooldownCuerpo();
                }
            }
            return;
        }

        if (jugador.getBounds().intersects(e.getBounds()) && e.puedeAtacar()) {
            boolean esquivado = (jugador.getZ() > 30);
            if (!esquivado) {
                procesarDanoAJugador(jugador, e, textosDano);
                e.reiniciarCooldown();
            }
        }
    }

    private void procesarDanoAJugador(Jugador jugador, EnemigoBase e, List<TextoDano> textosDano) {
        // Si estamos rodando (como en Dark Souls), no nos hacen daño
        if (jugador.isRodando()) {
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "ESQUIVADO", Color.YELLOW));
            return;
        }

        // 1. Buscamos el centro para saber de qué lado nos pegaron y poder empujarnos
        int centroJugador = jugador.getBounds().x + (jugador.getBounds().width / 2);
        int centroEnemigo = e.getBounds().x + (e.getBounds().width / 2);

        // 2. Calculamos hacia dónde nos empujan por el golpe
        int direccionEmpuje = (centroJugador >= centroEnemigo) ? 1 : -1;
        int fuerzaEmpuje = 12; // Reducido un 40% (antes 20)

        // Si tenemos el escudo arriba mitigamos el daño
        if (jugador.isDefendiendo()) {
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "BLOQUEADO", Color.CYAN));
            GestorSonidos.reproducir(GestorSonidos.ESCUDO);
            jugador.setX(jugador.getX() + (direccionEmpuje * fuerzaEmpuje));
            GestorContadores.get().registrarBloqueo(); // Bloqueo exitoso
        } else {
            jugador.recibirGolpe(e.getDano());
            GestorContadores.get().registrarDanioRecibido(e.getDano()); // Daño recibido
            textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, e.getDano(), Color.RED));
            jugador.setX(jugador.getX() + (direccionEmpuje * (fuerzaEmpuje / 2)));
            // Suave sacudida al ser golpeado por enemigos
            GamePanel.getInstancia().iniciarCameraShake(8, 5);
            GamePanel.getInstancia().iniciarHitStop(3);
        }
    }

    public void verificarGolpeContinuo(
            Jugador jugador,
            List<EnemigoBase> enemigos,
            List<TextoDano> textosDano
    ) {
        if (jugador.getAniIndex() != 2) return;

        int anchoAtaque = 80;
        int xAtaque = jugador.isMirandoDerecha()
                ? (int) jugador.getX() + 40
                : (int) jugador.getX() - anchoAtaque + 20;

        Rectangle areaAtaque = new Rectangle(xAtaque, (int) jugador.getY() - 40, anchoAtaque, 80);

        for (EnemigoBase e : enemigos) {
            // Revisamos si le dimos con la espada, si no está spawneando y si no le dimos en este golpe
            if (!e.isMuerto() && !e.isSpawning() && areaAtaque.intersects(e.getHitbox()) && !jugador.enemigosGolpeados.contains(e)) {
                int danoActual = jugador.getDano();
                
                if (e.isVulnerablePorDelay()) {
                    danoActual = (int)(danoActual * 1.5f);
                    textosDano.add(new TextoDano(e.getHitbox().x, e.getHitbox().y - 20, "¡CRÍTICO!", Color.MAGENTA));
                }

                e.recibirDano(danoActual);
                GestorContadores.get().registrarDanioInfligido(danoActual); // Daño infligido

                // Sacamos los numeritos amarillos de daño flotante
                textosDano.add(new TextoDano(e.getHitbox().x, e.getHitbox().y, danoActual, Color.YELLOW));

                // Sensación de impacto ligero cuando atacamos
                GamePanel.getInstancia().iniciarCameraShake(5, 3);
                GamePanel.getInstancia().iniciarHitStop(2);

                // Lo empujamos para atrás por la fuerza del golpe
                // El Boss no recibe knockback (es un ser colosal)
                if (!(e instanceof EnemigoJefeDemonio)) {
                    int fuerzaKnockback = 22;

                    // Si está atacando, tiene "Poise" (resistencia) y solo retrocede un poco
                    if (e.isAtacando()) {
                        fuerzaKnockback = 10;
                    }

                    // El Esqueleto Élite y el Jefe tienen aún más resistencia base
                    if (e instanceof EnemigoEsqueletoElite || e instanceof EnemigoNightBorne) {
                        fuerzaKnockback /= 2;
                    }

                    if (jugador.isMirandoDerecha()) {
                        e.setX(e.getX() + fuerzaKnockback);
                    } else {
                        e.setX(e.getX() - fuerzaKnockback);
                    }
                }

                // Guardamos en esta lista para no bajarle vida dos veces por el mismo click
                jugador.enemigosGolpeados.add(e);
            }
        }
    }

    public void actualizarProyectilesYColisiones(
            Jugador jugador,
            List<Proyectil> proyectiles,
            List<EnemigoBase> enemigos,
            List<TextoDano> textosDano
    ) {
        Iterator<Proyectil> itProyectil = proyectiles.iterator();
        while (itProyectil.hasNext()) {
            Proyectil p = itProyectil.next();
            p.actualizar();

            if (!p.isActivo()) {
                itProyectil.remove();
                continue;
            }

            if (p.getEmisor() == Proyectil.Emisor.JUGADOR) {
                for (EnemigoBase e : enemigos) {
                    if (!e.isMuerto() && !e.isSpawning() && p.getHitbox().intersects(e.getBounds()) && !p.enemigosGolpeados.contains(e)) {
                        e.recibirDano(p.getDano());
                        GestorContadores.get().registrarDanioInfligido(p.getDano()); // Daño infligido por magia
                        textosDano.add(new TextoDano(e.getX(), e.getY() - 20, p.getDano(), Color.CYAN));
                        p.enemigosGolpeados.add(e);
                        
                        // Si es la onda mágica (Habilidad 1), NO DEBE DESAPARECER para seguir atravesando enemigos.
                        // Solo los demás desaparecen al chocar.
                        if (p.getTipo() != Proyectil.Tipo.ONDA_MAGICA) {
                            p.setActivo(false); 
                            break;
                        }
                    }
                }
            } else if (p.getEmisor() == Proyectil.Emisor.ENEMIGO) {
                // Verificación de altura (Eje Z): Solo golpea si está en la mitad inferior (pies)
                // y el proyectil no es una mancha en el suelo (Splash)
                boolean alturaCoincide = p.getZ() < 35 && !p.isSplash();

                if (jugador.getVida() > 0 && p.getHitbox().intersects(jugador.getBounds()) && alturaCoincide) {
                    if (jugador.isRodando()) {
                        textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "ESQUIVADO", Color.YELLOW));
                    } else if (jugador.isDefendiendo()) {
                        textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, "BLOQUEADO", Color.CYAN));
                        GestorSonidos.reproducir(GestorSonidos.ESCUDO);
                        int dir = (jugador.getX() >= p.getHitbox().x) ? 1 : -1;
                        jugador.setX(jugador.getX() + (dir * 9)); // Reducido retroceso de 15 a 9
                        GestorContadores.get().registrarBloqueo(); // Bloqueo exitoso
                    } else {
                        jugador.recibirGolpe(p.getDano());
                        GestorContadores.get().registrarDanioRecibido(p.getDano()); // Daño recibido
                        textosDano.add(new TextoDano(jugador.getX(), jugador.getY() - 20, p.getDano(), Color.RED));
                        // Sin retroceso físico — el proyectil solo aplica daño
                        GamePanel.getInstancia().iniciarCameraShake(6, 4);
                        GamePanel.getInstancia().iniciarHitStop(2);
                    }
                    p.setActivo(false);
                }
            }
        }
    }

    public void actualizarObjetosSueloYColisiones(
            Jugador jugador,
            List<ObjetoRecogible> objetosSuelo,
            List<TextoDano> textosDano
    ) {
        Iterator<ObjetoRecogible> itObj = objetosSuelo.iterator();
        while (itObj.hasNext()) {
            ObjetoRecogible obj = itObj.next();
            obj.actualizar();

            if (jugador.getBounds().intersects(obj.getHitbox()) && obj.isEnSuelo()) {
                if (obj.getTipo() == ObjetoRecogible.TIPO_MONEDA) {
                    GestorSonidos.reproducir(GestorSonidos.RECOGER_MONEDA);
                    textosDano.add(new TextoDano(
                            jugador.getX(),
                            jugador.getY() - 30,
                            "+15 Oro",
                            new Color(255, 215, 0)
                    ));
                    GestorContadores.get().registrarMoneda(15); // Moneda recogida
                } else {
                    GestorSonidos.reproducir(GestorSonidos.RECOGER_ITEM);
                }
                jugador.recogerObjeto(obj.getTipo());
                itObj.remove();
            }
        }
    }
}

