# Orden de implementación

Seis bloques. El orden es por **dependencia**, no por tamaño: la navegación primero porque todo
cuelga de ella, el héroe último porque espera un dato que aún no existe.

Cada bloque es un PR. No los mezcles: el bloque 1 toca casi todos los archivos de rutas y
revisar eso junto con cambios de contenido hace la revisión imposible.

---

## Bloque 1 · Navegación

**Archivos**: `app/MoneyFlowApp.kt`, `app/MoreScreen.kt` (eliminar),
nuevos `app/money/MoneyScreen.kt` + `MoneyViewModel.kt`, `app/settings/SettingsScreen.kt`,
`feature/budgets/BudgetsNavigation.kt`

- [ ] `TopLevelDestination`: quitar `BUDGETS`, renombrar `MORE` → `MONEY` con `MoneyRoute`
- [ ] `BudgetsRoute` pasa a destino apilado, `onBack` deja de ser nullable
- [ ] `onOpenBudgets` usa `navigate()` en vez de `navigateToTopLevel()`
- [ ] `MoneyScreen`: cinco filas con cifra viva
- [ ] `SettingsScreen`: siete pantallas en dos grupos
- [ ] Eliminar `MoreScreen.kt`

**Terminado cuando**: las cuatro pestañas navegan, ningún destino quedó huérfano, y el badge
de vencidos sigue en Inicio.

---

## Bloque 2 · Buscador fijo en Movimientos

**Archivos**: `feature/transactions/TransactionsScreen.kt`

- [ ] Campo de búsqueda fuera del `LazyColumn`, en cabecera fija
- [ ] Chips de categoría en la misma cabecera
- [ ] Chip activo relleno, resto contorno

**Terminado cuando**: se puede hacer scroll de toda la lista sin perder de vista qué filtro
está aplicado. Sin cambios en el ViewModel.

---

## Bloque 3 · Análisis accionable

**Archivos**: `feature/analytics/AnalyticsScreen.kt`, `AnalyticsViewModel.kt`,
`feature/insights/` (absorber), `core/domain/model/BudgetProgress`

- [ ] Tarjeta de peor sobregiro al abrir, con las dos salidas
- [ ] "Ajustar el límite" abre el editor con monto cargado
- [ ] "Ver esos gastos" abre Movimientos filtrado
- [ ] Variante de gasto fijo: otro tono, otro mensaje, sin presión de días
- [ ] Absorber `GetInsightsUseCase`; retirar Sugerencias como destino
- [ ] Unificar el total del mes entre dona y héroe

**Terminado cuando**: la tarea "averigua si te pasaste en alguna categoría y sube ese límite"
se resuelve en dos toques desde Análisis.

**Depende de**: bloque 1 (rutas nuevas).

---

## Bloque 4 · Sheet de pago

**Archivos**: nuevo `feature/upcoming/PaySheet.kt`, `UpcomingScreen.kt`, `UpcomingViewModel.kt`

- [ ] Sheet con monto, vencimiento y método sugerido
- [ ] Método con app: abrir vía `launchPaymentApp()`, marcar pagado al volver, ofrecer deshacer
- [ ] Método sin app: marcar pagado, con una línea que explique por qué no hay app
- [ ] "Ya pagué por fuera": marca pagado **y persiste el método elegido**
- [ ] Separar comprometido de gastado en el total del mes

**Terminado cuando**: pagar Netflix desde Próximos abre la app correcta y vuelve con el pago
registrado. Autocontenido, el launcher ya existe.

---

## Bloque 5 · Onboarding y atajos

**Archivos**: `app/OnboardingScreen.kt`, `core/datastore/`,
`feature/dashboard/DashboardScreen.kt`, `DashboardSections.kt`, `DashboardViewModel.kt`

- [ ] Cinco pasos, una decisión por paso
- [ ] Pasos 2, 3 y 4 saltables, cada salto con su degradación definida
- [ ] Persistir presupuesto, métodos y atajos
- [ ] Fila "De un toque" en el dashboard, con deshacer
- [ ] Quitar el `StatTile` "Movimientos"
- [ ] Racha de siete días
- [ ] Reordenar tarjetas: atajos → pagos → presupuestos → hoy

**Terminado cuando**: registrar el almuerzo de hoy cuesta **un** toque por la ruta de atajo,
y saltar el paso 2 no rompe el dashboard.

**Depende de**: los atajos consumen lo que el onboarding recoge, así que van juntos.

---

## Bloque 6 · Héroe — BLOQUEADO

**No empezar.** Espera las sesiones con 3–5 usuarios.

La pregunta: ¿dos números (lo que queda / de cuánto) o las siete cifras actuales?
`HeroBalanceCard.kt` argumenta que una cifra sin referencia es dato, no información.
El prototipo prueba lo contrario. Nadie tiene razón todavía.

Lo que decide: en la tarea "averigua cuánto te queda para el resto del mes", ¿cuánto tarda el
usuario en dar la respuesta correcta, y menciona la proyección por su cuenta? Si nadie la usa
para decidir, sobra en el héroe. Si la buscan al no encontrarla, el código actual tenía razón.

---

## Reglas transversales

Aplican a todos los bloques:

- **Nada de texto secundario en opacidad.** `onSurfaceVariant`, no `.copy(alpha = 0.7f)`.
- **Todo lo destructivo o automático ofrece deshacer.**
- **Un botón deshabilitado no se ve tocable.**
- **Área táctil mínima 48 dp.**
- **No añadir transiciones nuevas** — `NavTransitions.kt` ya define las tres que existen.

## Si algo no cuadra

La especificación da la razón de cada decisión, no solo el cambio. Si al implementar aparece
una restricción que invalida la razón, **para y pregunta** en vez de implementar a medias.
Una decisión de diseño con su premisa rota es una decisión distinta.
