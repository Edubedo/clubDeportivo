# Pruebas de las reglas de seguridad

Comprueban `firestore.rules` contra el emulador de Firestore (no tocan tu proyecto real). Cubren sobre todo el
registro de miembros con código, el acceso por rol, los avisos y los precios.

Requisitos: Node 18+ y Java 11+ (el emulador de Firestore es Java).

```
cd tools/pruebas-reglas
npm install
npm test
```

Corre estas pruebas **cada vez que cambies `firestore.rules`** y antes de publicarlas en la consola de Firebase.
