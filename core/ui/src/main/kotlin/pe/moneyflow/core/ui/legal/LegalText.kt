package pe.moneyflow.core.ui.legal

/**
 * Centralized user-facing legal copy (Spanish). This is a **starting template** for the Peruvian
 * market and should be reviewed by qualified legal counsel before release — it is not a substitute
 * for professional advice on trademark use or Ley N.° 29733 (protección de datos personales).
 */
object LegalText {

    /** Trademark / non-affiliation notice, shown wherever bank or wallet brand names appear. */
    const val NON_AFFILIATION: String =
        "Todas las marcas, logotipos y nombres de bancos y billeteras (como BCP, BBVA, Interbank, " +
            "Scotiabank, Plin, Yape) son propiedad exclusiva de sus respectivos titulares " +
            "registrados. Esta aplicación es una herramienta independiente de control de gastos y " +
            "no tiene afiliación oficial, respaldo ni asociación comercial con dichas " +
            "instituciones financieras."

    /** Short version for compact footers. */
    const val NON_AFFILIATION_SHORT: String =
        "Marcas y nombres de bancos/billeteras son de sus respectivos titulares. MoneyFlow es una " +
            "herramienta independiente, sin afiliación ni respaldo de dichas instituciones."

    /** In-app privacy policy draft. Placeholders in [] must be completed before publishing. */
    const val PRIVACY_POLICY: String = """
MoneyFlow es una herramienta independiente para registrar tus gastos. Cuidamos tu privacidad y la
seguridad de tu información personal, de acuerdo con la Ley N.° 29733, Ley de Protección de Datos
Personales, y su reglamento, supervisados por la Autoridad Nacional de Protección de Datos
Personales (ANPD).

1. Qué datos guardamos
Solo la información que tú registras dentro de la app (movimientos, montos, categorías, cuentas y
métodos de pago). Estos datos se almacenan localmente en tu dispositivo.

2. Dónde se guardan
Tu información permanece en tu dispositivo. La app no envía tus movimientos a servidores nuestros ni
de terceros. Si creas una copia de seguridad, el archivo se guarda donde tú elijas y bajo tu
control.

3. Datos bancarios y credenciales
MoneyFlow nunca te pedirá ni almacenará contraseñas, PIN, tokens ni claves de tus bancos o
billeteras. No accedemos a tus cuentas bancarias.

4. Enlaces a apps y bancos
Cuando abres la app o la banca web de una entidad desde MoneyFlow, sales de nuestra aplicación y se
abre la app oficial o tu navegador externo. No leemos, interceptamos ni registramos lo que haces
después de salir de MoneyFlow.

5. Tus derechos
Puedes acceder, rectificar, actualizar o eliminar tus datos en cualquier momento desde la app.
Conforme a la Ley N.° 29733, tienes derecho de acceso, rectificación, cancelación y oposición.

6. Contacto
Para consultas sobre privacidad, escríbenos a [correo de contacto].

Última actualización: [fecha].
"""
}
