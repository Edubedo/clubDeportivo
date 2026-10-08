// Mismos requisitos que util/ReglasContrasena.kt de la app: si cambian allá, cambian aquí.
const LONGITUD_MINIMA = 8;

/** Mensaje del primer requisito que falta, o null si la contraseña cumple todos. */
function primerError(contrasena) {
  if (typeof contrasena !== 'string' || contrasena.length === 0) return 'Escribe una contraseña.';
  if (contrasena.length < LONGITUD_MINIMA) return `La contraseña debe tener al menos ${LONGITUD_MINIMA} caracteres.`;
  if (/\s/u.test(contrasena)) return 'La contraseña no puede tener espacios.';
  if (!/\p{Lu}/u.test(contrasena)) return 'La contraseña debe incluir al menos una letra mayúscula.';
  if (!/\p{Ll}/u.test(contrasena)) return 'La contraseña debe incluir al menos una letra minúscula.';
  if (!/\p{N}/u.test(contrasena)) return 'La contraseña debe incluir al menos un número.';
  if (!/[^\p{L}\p{N}\s]/u.test(contrasena)) return 'La contraseña debe incluir al menos un carácter especial (! @ # $ % & *).';
  return null;
}

module.exports = { primerError };
