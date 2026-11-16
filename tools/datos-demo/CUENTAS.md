# Cuentas de prueba (emulador local + Firebase real)

Las 8 cuentas principales (personal + Mariana y Diego) existen también en el Firebase real (creadas el 2026-11-16 con `sembrar-cuentas-reales.mjs`, sin datos demo). Los miembros de relleno y los datos demo solo existen en el emulador.
Se recrean con `node tools/datos-demo/sembrar-demo.mjs`. Inicio de sesión: correo + contraseña.

| Rol | Nombre | Usuario (correo) | Contraseña | Notas |
|---|---|---|---|---|
| Administrador | Laura Mendoza Ortega | `admin@athleticclub.test` | `Admin#2026Club` |  |
| Supervisor | Carlos Rivera Beltrán | `supervisor@athleticclub.test` | `Super#2026Club` |  |
| Encargado de área | Jorge Salazar Peña | `encargado.futbol@athleticclub.test` | `Futbol#2026Club` | Área: Fútbol |
| Encargado de área | Ana Lucía Duarte | `encargado.basquetbol@athleticclub.test` | `Basquet#2026Club` | Área: Básquetbol |
| Encargado de área | Miguel Ángel Cota | `encargado.tenis@athleticclub.test` | `Tenis#2026Club` | Área: Tenis |
| Encargado de área | Daniela Robles Urías | `encargado.natacion@athleticclub.test` | `Natacion#2026Club` | Área: Natación |
| Miembro (socio) | Mariana López Ruiz | `mariana.lopez@athleticclub.test` | `Socio#2026Mariana` | Código CLB-7K3M9Q · plan NORMAL · vence 2026-11-17 |
| Miembro (socio) | Diego Hernández Soto | `diego.hernandez@athleticclub.test` | `Socio#2026Diego` | Código CLB-X4RT82 · plan DELUXE · vence 2026-12-05 |

Miembros de relleno (también pueden entrar): correo `<id>@athleticclub.test` con contraseña `Relleno#2026Club`:

- `soc-01@athleticclub.test` — Valeria Acosta Pérez (NORMAL, ACTIVA, vence 2026-11-27)
- `soc-02@athleticclub.test` — Roberto Medina Cruz (DELUXE, ACTIVA, vence 2026-11-20)
- `soc-03@athleticclub.test` — Sofía Ibarra Lara (NINO, ACTIVA, vence 2026-12-11)
- `soc-04@athleticclub.test` — Fernando Quiroz Vega (NORMAL, ACTIVA, vence 2026-11-16)
- `soc-05@athleticclub.test` — Camila Beltrán Ríos (NORMAL, ACTIVA, vence 2026-12-03)
- `soc-06@athleticclub.test` — Andrés Camacho Luna (DELUXE, SUSPENDIDA, vence 2026-11-24)
- `soc-07@athleticclub.test` — Paola Escalante Gil (NORMAL, ACTIVA, vence 2026-11-09)
- `soc-08@athleticclub.test` — Héctor Valenzuela Mora (NORMAL, ACTIVA, vence 2026-11-22)
- `soc-09@athleticclub.test` — Isabela Fierro Nava (NINO, ACTIVA, vence 2026-12-07)
- `soc-10@athleticclub.test` — Ricardo Sandoval Ayala (DELUXE, ACTIVA, vence 2026-11-30)

## Scripts (con los emuladores corriendo)

- `node tools/datos-demo/sembrar-demo.mjs` — borra y vuelve a crear todos los datos y estas cuentas.
- `node tools/datos-demo/probar-sistema.mjs` — permisos por rol y notificaciones de renovación.
- `node tools/datos-demo/probar-membresias.mjs` — cambio de plan (subir/bajar), renovaciones, correos y notificaciones.
- `node tools/datos-demo/probar-reservas-torneos.mjs` — reservas, aprobaciones, torneos, cupo y concurrencia.

Los correos en el emulador quedan como `SIMULADO` en la colección `correos_enviados` (no se manda nada real).
