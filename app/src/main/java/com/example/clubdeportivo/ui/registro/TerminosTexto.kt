package com.example.clubdeportivo.ui.registro

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

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
    val ULTIMA_ACTUALIZACION: String get() = Textos.get(R.string.manual_ultima_actualizacion)

    val terminos: List<SeccionLegal> = listOf(
        SeccionLegal(
            Textos.get(R.string.n_1_acceptance_of_the_terms),
            listOf(
                Textos.get(R.string.by_creating_an_account_in_the)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_2_your_account),
            listOf(
                Textos.get(R.string.only_people_to_whom_the_club),
                Textos.get(R.string.you_are_responsible_for_the_accuracy),
                Textos.get(R.string.the_club_may_suspend_or_cancel)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_3_memberships_and_payments),
            listOf(
                Textos.get(R.string.individual_memberships_and_family_pack_2),
                Textos.get(R.string.a_price_change_applies_to_new),
                Textos.get(R.string.if_the_membership_expires_or_is)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_4_family_plan),
            listOf(
                Textos.get(R.string.in_a_family_package_the_holder),
                Textos.get(R.string.the_holder_is_responsible_for_adding)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_5_bookings),
            listOf(
                Textos.get(R.string.you_can_book_up_to_days, Catalogos.ANTICIPACION_MAXIMA_DIAS, Catalogos.MAX_HORAS_POR_RESERVA, Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO, Catalogos.HORA_APERTURA_AREAS, Catalogos.HORA_CIERRE_AREAS),
                Textos.get(R.string.requests_from_members_and_visitors_rem),
                Textos.get(R.string.cancelling_less_than_hours_in_advance, Catalogos.CANCELACION_SIN_PENALIZACION_HORAS, Catalogos.INASISTENCIAS_PARA_BLOQUEO, Catalogos.VENTANA_INASISTENCIAS_DIAS, Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_6_use_of_the_facilities),
            listOf(
                Textos.get(R.string.you_agree_to_respect_the_club),
                Textos.get(R.string.using_the_facilities_involves_the_risk)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_7_proper_use_of_the_app),
            listOf(
                Textos.get(R.string.you_must_not_attempt_to_access),
                Textos.get(R.string.we_do_our_best_to_keep)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.n_8_changes_to_these_terms),
            listOf(
                Textos.get(R.string.the_club_may_update_these_terms)
            )
        )
    )

    val privacidad: List<SeccionLegal> = listOf(
        SeccionLegal(
            Textos.get(R.string.who_is_responsible),
            listOf(Textos.get(R.string.athletic_club_is_responsible_for_the))
        ),
        SeccionLegal(
            Textos.get(R.string.what_data_we_collect),
            listOf(
                Textos.get(R.string.identification_and_contact_data_name_e),
                Textos.get(R.string.data_about_your_membership_and_use),
                Textos.get(R.string.account_data_the_password_is_stored)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.what_we_use_it_for),
            listOf(
                Textos.get(R.string.to_create_and_manage_your_account),
                Textos.get(R.string.we_do_not_sell_your_data)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.who_it_is_shared_with),
            listOf(
                Textos.get(R.string.only_authorized_club_staff_see_your),
                Textos.get(R.string.we_use_google_firebase_services_for)
            )
        ),
        SeccionLegal(
            Textos.get(R.string.your_rights),
            listOf(
                Textos.get(R.string.you_can_access_your_data_correct)
            )
        )
    )
}
