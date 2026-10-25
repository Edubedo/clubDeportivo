package com.example.clubdeportivo.ui.registro

import com.example.clubdeportivo.data.Catalogos

/** Un apartado de los términos o del aviso de privacidad: título y uno o más párrafos. */
data class SeccionLegal(val titulo: String, val parrafos: List<String>)

/**
 * Texto legal que se muestra al crear una cuenta. Las cifras (cancelación, inasistencias, límites de reserva) salen de
 * [Catalogos], así el texto no se desfasa de lo que la app realmente aplica. Cada vez que cambie el contenido hay que
 * subir [VERSION]: esa es la versión que queda guardada junto al perfil de quien la aceptó.
 *
 * Es un texto base pensado para un club deportivo: antes de publicar la app conviene que lo revise quien asesore
 * legalmente al club (razón social, domicilio, correo de contacto y datos del responsable de privacidad).
 */
object TerminosTexto {
    const val VERSION = "1.0"
    const val ULTIMA_ACTUALIZACION = "24 de octubre de 2026"

    val terminos: List<SeccionLegal> = listOf(
        SeccionLegal(
            "1. Aceptación de los términos",
            listOf(
                "Al crear una cuenta en la app de Athletic Club (el \"Club\") declaras que leíste y aceptas estos Términos y condiciones y el Aviso de privacidad. Si no estás de acuerdo, no crees la cuenta ni uses la app."
            )
        ),
        SeccionLegal(
            "2. Tu cuenta",
            listOf(
                "Solo pueden registrarse las personas a las que el Club entregó un código de miembro. El código es personal e intransferible.",
                "Eres responsable de la veracidad de tus datos, de mantener en secreto tu contraseña y de la actividad que ocurra con tu cuenta. Avisa al Club de inmediato si sospechas un uso no autorizado.",
                "El Club puede suspender o cancelar una cuenta que incumpla estos términos o el reglamento de las instalaciones."
            )
        ),
        SeccionLegal(
            "3. Membresías y pagos",
            listOf(
                "Las membresías individuales y los paquetes familiares se pagan cada mes; la visita se paga una sola vez por día. Los precios vigentes los fija el administrador del Club y se muestran en la sección Membresías.",
                "Un cambio de precio se aplica a nuevas altas y renovaciones; quien ya tiene membresía conserva el precio que pagó hasta que renueve.",
                "Si la membresía vence o se suspende (por falta de pago, incumplimiento del reglamento u otro motivo que se te indicará), no podrás reservar hasta regularizarla en recepción."
            )
        ),
        SeccionLegal(
            "4. Plan familiar",
            listOf(
                "En un paquete familiar, el titular puede dar de alta a las personas de su familia hasta el máximo de integrantes del paquete y compartirles su enlace de acceso. Cada integrante crea su propia cuenta y acepta estos términos por separado.",
                "El titular es responsable de dar de alta únicamente a familiares que tengan derecho al paquete y de que el pago del paquete se mantenga al corriente; si la membresía se suspende o vence, el acceso de todos sus integrantes se suspende."
            )
        ),
        SeccionLegal(
            "5. Reservaciones",
            listOf(
                "Puedes reservar con hasta ${Catalogos.ANTICIPACION_MAXIMA_DIAS} días de anticipación, un máximo de ${Catalogos.MAX_HORAS_POR_RESERVA} horas por reserva y hasta ${Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO} reservaciones activas. Las áreas abren de ${Catalogos.HORA_APERTURA_AREAS} a ${Catalogos.HORA_CIERRE_AREAS}.",
                "Las solicitudes de miembros y visitantes quedan en revisión hasta que el encargado del área las apruebe o rechace. Un torneo o el mantenimiento pueden bloquear un área.",
                "Cancelar con menos de ${Catalogos.CANCELACION_SIN_PENALIZACION_HORAS} horas de anticipación cuenta como cancelación tardía. Si acumulas ${Catalogos.INASISTENCIAS_PARA_BLOQUEO} inasistencias en ${Catalogos.VENTANA_INASISTENCIAS_DIAS} días, no podrás reservar durante ${Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS} días."
            )
        ),
        SeccionLegal(
            "6. Uso de las instalaciones",
            listOf(
                "Te comprometes a respetar el reglamento del Club, al personal y a las demás personas, y a cuidar las instalaciones y el material. Los daños causados por mal uso pueden cobrarse a quien los provoque.",
                "El uso de las instalaciones implica riesgos propios de la actividad deportiva. Declaras estar en condiciones de salud adecuadas para practicarla; el Club no es responsable de lesiones derivadas de tu estado de salud previo o del incumplimiento de las indicaciones del personal."
            )
        ),
        SeccionLegal(
            "7. Uso correcto de la app",
            listOf(
                "No debes intentar acceder a cuentas o datos de otras personas, alterar el funcionamiento de la app ni usarla con fines distintos a gestionar tu membresía y tus reservaciones.",
                "Hacemos lo posible por mantener la app disponible, pero puede haber interrupciones por mantenimiento o por causas ajenas al Club."
            )
        ),
        SeccionLegal(
            "8. Cambios a estos términos",
            listOf(
                "El Club puede actualizar estos términos. Cuando el cambio sea importante te lo avisaremos en la app y, si es necesario, te pediremos aceptarlos de nuevo. Seguir usando la app después de un cambio implica que lo aceptas."
            )
        )
    )

    val privacidad: List<SeccionLegal> = listOf(
        SeccionLegal(
            "Quién es el responsable",
            listOf("Athletic Club es el responsable del tratamiento de tus datos personales conforme a este Aviso de privacidad.")
        ),
        SeccionLegal(
            "Qué datos recopilamos",
            listOf(
                "Datos de identificación y contacto: nombre, correo electrónico y, si los das, teléfono y foto de perfil.",
                "Datos de tu membresía y uso del Club: código de miembro, plan, vigencia, pagos, reservaciones, asistencias y torneos en los que participas.",
                "Datos de tu cuenta: la contraseña se guarda cifrada por el servicio de autenticación; el Club nunca la ve."
            )
        ),
        SeccionLegal(
            "Para qué los usamos",
            listOf(
                "Crear y administrar tu cuenta, gestionar tu membresía y tus cobros, procesar tus reservaciones, avisarte de cambios importantes (vencimientos, aprobaciones, torneos) y mantener la seguridad de las instalaciones.",
                "No vendemos tus datos ni los usamos para publicidad de terceros."
            )
        ),
        SeccionLegal(
            "Con quién se comparten",
            listOf(
                "Solo el personal autorizado del Club ve tus datos, según su función. En un paquete familiar, el titular ve el nombre, parentesco y estado de registro de las personas que dio de alta.",
                "Usamos servicios de Google Firebase para autenticación y almacenamiento de datos; tratan la información por cuenta del Club."
            )
        ),
        SeccionLegal(
            "Tus derechos",
            listOf(
                "Puedes acceder a tus datos, corregirlos, pedir que se eliminen u oponerte a su uso (derechos ARCO) escribiendo o acudiendo a recepción del Club. Algunos datos se conservan el tiempo que exijan las obligaciones fiscales y contables."
            )
        )
    )
}
