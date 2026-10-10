# Frontend: Reservas de Espacios Académicos

Aplicación web en React 19 con TypeScript y Vite. Consume la API REST del backend para iniciar sesión, consultar espacios, solicitar reservas, aprobarlas o rechazarlas, y administrar usuarios.

## Requisitos

- Node.js 20 o superior
- Backend en ejecución (por defecto en `http://localhost:8080`)

## Configuración

La URL del backend se define con la variable `VITE_API_URL`. Si no se define, se usa `http://localhost:8080`.

Para cambiarla, crea un archivo `.env.local` dentro de `frontend/`:

```
VITE_API_URL=http://localhost:8080
```

El backend acepta por defecto el origen `http://localhost:5173`. Si cambias el puerto del frontend, ajusta `CORS_ORIGIN` en el backend.

## Scripts

```bash
npm install        # instalar dependencias
npm run dev        # servidor de desarrollo en http://localhost:5173
npm run build      # comprobación de tipos y build de producción en dist/
npm run preview    # servir el build de producción localmente
npm test           # pruebas con Vitest y React Testing Library
```

## Pantallas y roles

| Ruta | Pantalla | Roles |
|---|---|---|
| `/login` | Inicio de sesión | Público |
| `/reservas` | Solicitar reservas, ver el listado, cancelar y consultar el historial | Todos |
| `/espacios` | Listado de espacios activos. El administrador puede crear y desactivar espacios. | Todos |
| `/aprobaciones` | Solicitudes pendientes: aprobar o rechazar con motivo | Aprobador, administrador |
| `/usuarios` | Crear usuarios, cambiar roles y desactivar usuarios | Administrador |

El menú solo muestra las secciones permitidas para el rol del usuario. Las rutas protegidas redirigen al login si no hay sesión, o al inicio si el rol no tiene acceso. La seguridad real está en el backend.

Una solicitud propia no muestra los botones de aprobar o rechazar, porque nadie puede decidir sobre su propia reserva.

## Validación en el formulario

Antes de enviar una reserva, el formulario comprueba:

- Fecha, hora de inicio y hora de fin informadas.
- Horas múltiplos de 30 minutos.
- Hora de fin posterior a la de inicio.
- Duración máxima de cuatro horas.
- Inicio no anterior al momento actual.

Esta validación es orientativa. El backend valida de nuevo y su respuesta es la que prevalece.

## Estructura

```
src/
  api/          Cliente HTTP (cliente.ts) y tipos compartidos (tipos.ts)
  auth/         Contexto de sesión, rutas protegidas y almacenamiento de la sesión
  componentes/  Layout con menú por rol
  paginas/      Una página por sección: login, reservas, espacios, aprobaciones, usuarios
  reservas/     Validación de solicitudes (validaciones.ts)
  etiquetas.ts  Textos en español para roles, tipos, estados y formato de fechas
  test/         Configuración de pruebas
```

La sesión (token, identificador, nombre, correo y rol) se guarda en `localStorage` con la clave `reservas.sesion`. Al cerrar sesión se borra.

## Pruebas

```bash
npm test
```

- Validación de solicitudes de reserva.
- Redirecciones de las rutas protegidas.
- Menú según el rol.

## Limitaciones conocidas

- Las pruebas cubren la lógica de validación y el menú por rol, pero no todos los formularios ni las acciones de las tablas.
- No hay internacionalización: los textos están fijados en español.
