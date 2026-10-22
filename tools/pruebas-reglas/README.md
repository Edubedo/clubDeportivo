# Pruebas de las reglas de seguridad

Comprueban `firestore.rules` contra el emulador de Firestore (no tocan tu proyecto real). Cubren sobre todo el
registro de miembros con código, el acceso por rol, los avisos, las publicaciones y los precios.

Requisitos: Node 18+ y JDK 21+ (el emulador de Firestore requiere Java 21 o superior).

```
cd tools/pruebas-reglas
npm install
npm test
```

Corre estas pruebas **cada vez que cambies `firestore.rules`** y antes de publicarlas en la consola de Firebase.
