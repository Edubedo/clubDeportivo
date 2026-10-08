# Funciones de servidor

Lo que la app no puede hacer sola porque exige permisos de administrador de Firebase.

## `cambiarContrasenaPersonal`
Un administrador cambia la contraseña de una persona del personal **sin enviarle correo**. Comprueba en el servidor que quien
llama es administrador activo, que la cuenta es de personal (nunca de un miembro) y que la contraseña cumple los mismos
requisitos que la app; después cierra las sesiones abiertas de esa persona y deja `contrasenaCambiadaEn/Por` en su perfil.

### Publicarla (una sola vez, y cada vez que cambie `index.js`)
Requiere el plan **Blaze** del proyecto (tiene capa gratuita: este uso no genera costo en la práctica).

```
npm install -g firebase-tools        # si no lo tienes
firebase login
cd functions && npm install && cd ..
firebase deploy --only functions
```

### Probarla sin tocar tu proyecto
```
cd functions && npm install && npm test
```
(usa los emuladores de Firebase; requiere Java 11+).
