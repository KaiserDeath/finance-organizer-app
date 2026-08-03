# Handoff: MoneyFlow — Propuesta C

> **Documento histórico — no es la lista de trabajo pendiente.** Los seis bloques están
> implementados y la auditoría del 2026-08-01 está cerrada. Se conserva porque explica *por qué* se
> decidió cada cosa, que sigue siendo la razón para leerlo.
>
> Dos avisos, porque el texto de abajo quedó desactualizado y sus instrucciones ya no aplican:
>
> - **El bloque 6 (héroe) ya no está bloqueado.** Las sesiones con usuarios ocurrieron y mataron la
>   variación mes a mes; el KDoc de `HeroBalanceCard` registra el resultado. La advertencia de
>   "qué NO tocar" que sigue vigente en este archivo describe un estado que ya pasó.
> - **Parte del prototipo fue rechazada al probarlo** — el héroe de dos números y el bloque
>   "Cifras del cierre". No las reconstruyas desde aquí.
>
> Lo que sigue abierto: [`docs/open-items.md`](../open-items.md).
> Lo que se decidió a contracorriente del prototipo, y por qué:
> [`docs/design-decisions.md`](../design-decisions.md).

## Resumen

Rediseño de MoneyFlow (app Android de gastos personales, Kotlin + Jetpack Compose, multi-módulo)
a partir de una auditoría de nueve hallazgos. Este paquete contiene el prototipo navegable,
la especificación de desarrollo, y las instrucciones para aplicarlo sobre el repo existente.

El trabajo cubre: reducción de la navegación de doce destinos a seis, buscador fijo en
Movimientos, una pantalla de Análisis accionable, un flujo de pago nuevo, y un onboarding
de cinco pasos.

**Este NO es un rediseño desde cero.** El repo ya existe y ya resolvió parte de los hallazgos.
La tarea es aplicar cambios quirúrgicos sobre archivos concretos, no reescribir pantallas.

---

## ⚠️ Antes de empezar: qué NO tocar

**`feature/dashboard/HeroBalanceCard.kt` está bloqueado.** El diseño propone un héroe de dos
números; el código actual muestra siete cifras y documenta por qué. Ninguna de las dos posturas
está validada. La decisión depende de sesiones con usuarios que aún no ocurrieron.

No implementes el bloque 6 (héroe). Los bloques 1–5 son independientes de esa decisión.

Si al leer el código te parece obvio cuál es mejor: ese es exactamente el sesgo que las sesiones
existen para corregir. Déjalo.

---

## Sobre los archivos de diseño

Los HTML de este bundle son **referencias de diseño**, no código a portar. Son prototipos que
muestran comportamiento e intención visual, escritos en HTML porque es el medio más rápido para
probarlos con usuarios.

El destino es el repo Kotlin/Compose existente. Todo debe implementarse con:

- Los componentes de `core/designsystem/` (`GlassCard`, `pressScale`, etc.)
- El tema Material 3 ya configurado (`MaterialTheme.colorScheme`)
- Los patrones de navegación ya establecidos (rutas `@Serializable`, `NavGraphBuilder`
  extensions por feature, `NavigationSuiteScaffold`)
- Los `UseCase` y repositorios de `core/domain/`

**Los hex del prototipo son orientativos.** Mapean al tema actual; no los hardcodees.
La única regla de color que sí es normativa: ningún texto secundario en opacidad
(nada de `.copy(alpha = 0.7f)`) — usar `onSurfaceVariant` del tema, que da 7.4:1.

## Fidelidad

**Alta (hifi)** en layout, jerarquía, copy y comportamiento: el prototipo tiene los textos
finales en español y los flujos completos, incluyendo estados vacíos, deshacer y toasts.

**Media** en pixel-perfect: espaciados y radios del prototipo son la intención, pero deben
expresarse en la escala del design system del repo, no copiarse en dp exactos.

---

## Documentos incluidos

| Archivo | Qué es |
|---|---|
| `spec/Especificación de desarrollo — Propuesta C.dc.html` | **La fuente de verdad.** Once secciones, pantalla por pantalla, con archivos Kotlin y el razonamiento de cada decisión. Ábrelo en un navegador. |
| `prototipo/MoneyFlow Prototipo C (standalone).html` | Prototipo navegable, funciona offline. Ábrelo en un navegador y recorre los cinco flujos. |
| ~~`TAREAS.md`~~ | Los seis bloques en orden de dependencia. **Eliminado**: los seis están hechos, y una lista terminada con las casillas sin marcar se lee como pendiente. Lo que queda abierto vive en [`docs/open-items.md`](../open-items.md). |

**Lee la especificación antes de escribir código.** Cada sección explica *por qué* se decidió
algo. Si al implementar aparece una restricción técnica que invalida la razón, la decisión se
vuelve a abrir — no se implementa a medias. Anótalo y pregunta.

---

## Mapa: pantalla → archivos del repo

| Pantalla | Archivos que toca | Sección de la spec |
|---|---|---|
| Navegación global | `app/MoneyFlowApp.kt`, `app/MoreScreen.kt` (se elimina), `feature/budgets/BudgetsNavigation.kt` | 2 |
| Inicio | `feature/dashboard/DashboardScreen.kt`, `DashboardSections.kt`, `DashboardViewModel.kt` | 3 |
| Registrar gasto | `feature/addedit/AddEditScreen.kt`, `AddEditViewModel.kt`, `core/ui/paymentmethod/PaymentMethodSelector.kt` | 4 |
| Movimientos | `feature/transactions/TransactionsScreen.kt` | 5 |
| Análisis | `feature/analytics/AnalyticsScreen.kt`, `AnalyticsViewModel.kt`, `feature/insights/` (se absorbe) | 6 |
| Tu dinero | nuevo `app/money/MoneyScreen.kt` + `MoneyViewModel.kt` | 7 |
| Ajustes | nuevo `app/settings/SettingsScreen.kt` | 7 |
| Pagar | nuevo `feature/upcoming/PaySheet.kt`, `UpcomingScreen.kt`, `UpcomingViewModel.kt` | 8 |
| Onboarding | `app/OnboardingScreen.kt`, `core/datastore/` | 9 |

---

## Detalle por pantalla

### Navegación (sección 2)

De cinco pestañas a cuatro: **Inicio · Movimientos · Análisis · Tu dinero**.

En `TopLevelDestination` (dentro de `MoneyFlowApp.kt`):

- Eliminar la entrada `BUDGETS`. `BudgetsRoute` pasa a destino apilado con `onBack` obligatorio
  (hoy es nullable en `BudgetsNavigation.kt`).
- Renombrar `MORE` → `MONEY`: ruta `MoneyRoute`, etiqueta "Tu dinero",
  ícono `Icons.Rounded.AccountBalanceWallet`.
- El badge de vencidos se queda en `DASHBOARD`, sin cambios.
- `onOpenBudgets` en el dashboard pasa de `navigateToTopLevel()` a `navigate()` normal —
  y con eso gana su flecha de retorno.

El cajón `MoreScreen.kt` se parte en dos: `MoneyScreen` (destinos de plata) y `SettingsScreen`
(configuración). Las rutas destino existentes no cambian de nombre ni de firma.

### Inicio (sección 3)

- **Quitar** el `StatTile` "Movimientos" de `DashboardSections.kt`. Saber que van 47 movimientos
  no habilita ninguna decisión.
- **Añadir** fila de atajos "De un toque": chips con descripción + monto habitual. Un toque
  guarda el gasto y muestra un toast con deshacer. Fuente: lo que el usuario eligió en el
  onboarding; si lo saltó, las cuatro combinaciones descripción+categoría+método más frecuentes
  de los últimos 30 días.
- **Añadir** racha: siete puntos, uno por día, contra el permitido diario variable.
- **Orden de las tarjetas**: atajos → nudge de pagos → presupuestos en riesgo → movimientos de hoy.
- `DashboardUiState` necesita `shortcuts` y `streak`. `pace` y `topBudgets` ya existen y se conservan.

### Registrar gasto (sección 4)

Auto-focus en monto y detalles plegados ya están en código. Falta:

- Teclado numérico propio abierto de entrada.
- Sugerencia de categoría desde la descripción (el prototipo trae la tabla de patrones:
  "almuerzo|cena|mercado" → Comida, "pasaje|taxi|uber" → Transporte, etc.).
- Selector de método filtrado a los métodos activos del usuario.

**Tres reglas del selector** — los tres defectos que aparecieron probando:

1. Si el método por defecto ya no existe o quedó fuera de la selección del usuario, cae al
   primer método válido. Nunca a `null` ni a un id huérfano.
2. Solo métodos activos. Nada de chips que se pintan y no seleccionan.
3. Un botón deshabilitado no se ve tocable: sin sombra, sin ripple, opacidad en el contenedor
   y no en el texto.

### Movimientos (sección 5)

Sacar el campo de búsqueda del `LazyColumn` y ponerlo en una cabecera fija sobre él, junto con
los chips de categoría. El chip activo es el único relleno; los demás, contorno.

`TransactionsViewModel` no cambia — query y filtro ya viven ahí. Es solo layout.

### Análisis (sección 6)

Abre con la categoría en la que el usuario se pasó, cuánto, y dos salidas:

- **Ajustar el límite** → editor de ese presupuesto con el monto cargado, un toque.
- **Ver esos gastos** → Movimientos filtrado por esa categoría.

Si el peor sobregiro es un **gasto fijo**, la tarjeta cambia de tono: un alquiler no se recorta,
así que el mensaje es "tu límite está corto" y desaparece la presión de los días que faltan.
`BudgetProgress` necesita saber si la categoría es de gasto fijo.

`AnalyticsViewModel` absorbe `GetInsightsUseCase` y `GetBudgetsProgressUseCase`.
`feature/insights/` se retira como destino; su tarjeta se reutiliza.

**Además**: el centro de la dona y el héroe dan totales distintos del mismo mes. Regla —
el total del mes es *lo gastado*, sin pendientes. Si la dona incluye comprometidos, lo dice
en su etiqueta.

### Tu dinero y Ajustes (sección 7)

**Tu dinero**: cinco filas, cada una con su cifra viva (no un chevron mudo).

| Fila | Cifra en la fila |
|---|---|
| Presupuestos | cuántos en riesgo |
| Próximos pagos | total pendiente + cuántos vencidos |
| Cuentas | saldo |
| Ahorros | saldo |
| Métodos de pago | cuántos configurados |

`MoneyViewModel` combina presupuestos, próximos, cuentas, ahorros y métodos solo para esas cifras.

**Ajustes**: dos grupos. *La app* (Categorías, Moneda, Apariencia) y *Tus datos* (Recurrentes,
Copia de seguridad, Seguridad, Legal).

### Pagar (sección 8) — flujo nuevo

Hoy "Pagar" marca como pagado y nada más. Pasa a abrir un sheet con monto, fecha de
vencimiento y método sugerido.

| Método | Acción principal | Qué ve el usuario |
|---|---|---|
| Con app (Yape, BCP) | Abre la app; al volver marca pagado y ofrece deshacer | Ícono de teléfono en el chip; toast al volver |
| Sin app (efectivo, tarjetas, Plin) | Marca pagado directamente | Una línea que explica por qué no hay app que abrir — no un botón muerto |
| Ya pagado por fuera | Marca pagado con el método elegido **y lo recuerda** | Acción secundaria, siempre disponible |

`launchPaymentApp()` ya existe en `core/ui/util/AppLauncher.kt`, con fallback a Play Store y a
web banking. Solo faltaba llamarla desde aquí. El `deepLinkPackage` sale de
`core/ui/preset/FinancePresets.kt`.

Que "ya pagué por fuera" recuerde el método es lo que hace que la sugerencia mejore con el uso.
No es opcional.

`UpcomingViewModel` ya tiene `methodsById` y `methodFor()`. Falta persistir el método al
marcar pagado.

### Onboarding (sección 9)

Cinco pasos, una decisión cada uno: promesa → presupuesto mensual → métodos de pago →
compras diarias → resumen. Los pasos 2, 3 y 4 se pueden saltar.

Cada paso paga una pantalla concreta:

- El presupuesto es el denominador del héroe. **Si se salta, el héroe degrada con gracia — no se rompe.**
- Los métodos elegidos son los únicos que aparecen luego al registrar y al pagar.
  Si se salta, se activan los seis.
- Los atajos llenan "De un toque". Si se salta, la fila queda vacía hasta tener 30 días de historial.

Persistencia en `core/datastore`. El salto al primer gasto ya está resuelto por
`startInAddTransaction` en `MoneyFlowApp.kt`.

---

## Interacciones y movimiento

El repo ya define las transiciones en `NavTransitions.kt` y las respeta: fade-through entre
pestañas hermanas, slide direccional al entrar en profundidad, nada en destinos que son sheet.
**No añadas transiciones nuevas** — usa las que hay.

- Sheets: entrada 260 ms, `cubic-bezier(.2, 0, 0, 1)` (en Compose, el equivalente del tema).
- Toast de deshacer: 4 s, con acción. Todo lo destructivo o automático ofrece deshacer —
  guardar por atajo, editar monto, borrar, marcar pagado, cambiar límite.
- Háptica en el FAB ya existe (`HapticFeedbackType.LongPress`); replicarla en los atajos.

## Accesibilidad

- Área táctil mínima 48 dp. Los botones primarios del prototipo son de 58 px de alto.
- Texto secundario: `onSurfaceVariant`, nunca alpha sobre `onSurface`.
- Los badges de conteo ya llevan `contentDescription` en `MoneyFlowApp.kt` — mantener el patrón
  en las cifras de fila de Tu dinero.

## Datos de prueba

El prototipo usa julio 2026, día 29, moneda PEN, presupuesto mensual S/ 2 300, límite diario
S/ 90, y 23 movimientos sembrados. Sirve para reproducir cualquier pantalla del prototipo.
No es data de producción.

## Qué queda fuera

Deliberadamente no cubierto, para que no se cuele por omisión:

- Contenido de Cuentas y Ahorros — aparecen en el mapa de navegación; sus pantallas no se tocan.
- Selector de mes en la barra superior — el sitio está reservado, la interacción no está diseñada.
- Multi-moneda, widget y notificaciones — sin cambios.
- Tablet y foldable — `NavigationSuiteScaffold` ya adapta la barra a rail, pero el contenido de
  las pantallas nuevas está diseñado a una columna.
