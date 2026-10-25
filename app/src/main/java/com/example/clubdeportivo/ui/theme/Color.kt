package com.example.clubdeportivo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta del club en dos apariencias. Cada color se pide por su nombre de siempre (Marca, TextoPrincipal, Superficie...)
 * y devuelve el tono claro u oscuro según el tema activo ([LocalModoOscuro], lo fija [ClubDeportivoTheme]); por eso se
 * leen dentro de funciones @Composable. Los que no cambian de apariencia (marca sobre fondo azul marino fijo, la pantalla
 * de login, que ya es oscura) siguen siendo constantes.
 */
val LocalModoOscuro = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
internal fun tono(claro: Long, oscuro: Long): Color = Color(if (LocalModoOscuro.current) oscuro else claro)

// --- Colores fijos (iguales en claro y oscuro) ---
val DeepNavy = Color(0xFF021024)
val ActionBlue = Color(0xFF0A3A82)
val ActionOrange = Color(0xFFF96015)
val ActionOrangeForeground = Color(0xFFC2410C)
val SlateAccent = Color(0xFF5483B3)
val SoftBlueSelection = Color(0xFFC1E8FF)
val LoginBackground = Color(0xFF061B35)
val LoginFieldSurface = Color(0xFF172F4D)
val LoginMutedText = Color(0xFFA8C2DF)
val LoginAccentShape = Color(0xFF123B66)
val CourtBlueBright = Color(0xFF2563EB)
val CourtGreen = Color(0xFF16A34A)
val SuccessOnDark = Color(0xFF86EFAC)
val AlertOnDark = Color(0xFFFCA5A5)
val AttentionAmber = Color(0xFFF59E0B)
val RevenueHighBadge = Color(0xFF10B981)
val RevenueMidBadge = Color(0xFFF59E0B)
val RevenueLowBadge = Color(0xFFEF4444)
/** Azul marino de marca, siempre el mismo: para tarjetas con fondo azul fijo y texto blanco encima. */
val NavyFijo = Color(0xFF052659)

// --- Colores que cambian con el tema ---
val PrimaryNavy: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF052659, 0xFF8DB6F5)
val Background: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFF8FAFC, 0xFF0B1220)
val Surface: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFFFFFF, 0xFF121C2E)
val BorderSubdued: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFE2E8F0, 0xFF263449)
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF475569, 0xFFA9B8CC)
val SuccessForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF15803D, 0xFF6EE7A0)
val SuccessContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDCFCE7, 0xFF0F2E1E)
val AlertRed: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDC2626, 0xFFF87171)
val AlertRedForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF991B1B, 0xFFFCA5A5)
val AlertRedContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEE2E2, 0xFF3A1519)
val StatusRedForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFB91C1C, 0xFFFCA5A5)
val StatusRedBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFECACA, 0xFF6B2A30)
val AmberForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF92400E, 0xFFFBBF24)
val AmberContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEF3C7, 0xFF33260A)
val StatusAmberForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFB45309, 0xFFFBBF24)
val StatusAmberBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFDE68A, 0xFF5C4410)
val MembershipPro: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF10B981, 0xFF34D399)
val MembershipProForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF047857, 0xFF6EE7B7)
val PaymentTransfer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF7C3AED, 0xFFA78BFA)
val PaymentWallet: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF06B6D4, 0xFF22D3EE)
val ActivityClassesContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDBEAFE, 0xFF14263F)
val ActivityClassesForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF1D4ED8, 0xFF8DB6F5)
val MetricBlueContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFEFF6FF, 0xFF14263F)
val MetricBlueForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF2563EB, 0xFF8DB6F5)
val MetricGreenContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDCFCE7, 0xFF0F2E1E)
val MetricGreenForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF16A34A, 0xFF6EE7A0)
val MetricAmberContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEF3C7, 0xFF33260A)
val MetricAmberForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFD97706, 0xFFFBBF24)
val MetricRedContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEE2E2, 0xFF3A1519)
val MetricRedForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDC2626, 0xFFFCA5A5)
val MetricPurpleContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFF3E8FF, 0xFF2A1B45)
val MetricPurpleForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF7C3AED, 0xFFC4A1F7)
val MetricOrangeContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFFEDD5, 0xFF3A200E)
val MetricOrangeForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFC2410C, 0xFFFDA66B)
val MetricVioletForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF9333EA, 0xFFC4A1F7)
val MetricGoldContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEF9C3, 0xFF332B0A)
val MetricGoldForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFCA8A04, 0xFFF2CC4B)
val RoleAdminContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFF3E8FF, 0xFF2A1B45)
val RoleAdminBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFE9D5FF, 0xFF4A3175)
val RoleAdminForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF6B21A8, 0xFFD3B8FA)
val RoleManagerContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFCE7F3, 0xFF3A1730)
val RoleManagerBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFBCFE8, 0xFF63284F)
val RoleManagerForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF9D174D, 0xFFF5A3CE)
val RoleMemberContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFFEDD5, 0xFF3A200E)
val RoleMemberBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFED7AA, 0xFF633A17)
val RoleMemberForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFC2410C, 0xFFFDA66B)
val PackageBasicContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFEFF6FF, 0xFF14263F)
val PackageBasicBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDBEAFE, 0xFF24416B)
val PackageBasicForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF2563EB, 0xFF8DB6F5)
val PackageIntermediateContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDCFCE7, 0xFF0F2E1E)
val PackageIntermediateBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFBBF7D0, 0xFF1E5236)
val PackageIntermediateForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF16A34A, 0xFF6EE7A0)
val PackagePremiumContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEF3C7, 0xFF33260A)
val PackagePremiumBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFDE68A, 0xFF5C4410)
val PackagePremiumForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFD97706, 0xFFFBBF24)
val PackageFamilyContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFF3E8FF, 0xFF2A1B45)
val PackageFamilyBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFE9D5FF, 0xFF4A3175)
val PackageFamilyForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF9333EA, 0xFFC4A1F7)
val AvailabilityContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFDCFCE7, 0xFF0F2E1E)
val AvailabilityForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF15803D, 0xFF6EE7A0)
val AvailabilityBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFBBF7D0, 0xFF1E5236)
val RevenueHighContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFECFDF5, 0xFF0F2E1E)
val RevenueHighBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFA7F3D0, 0xFF1E5236)
val RevenueHighForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF065F46, 0xFF6EE7B7)
val RevenueMidContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFFFBEB, 0xFF33260A)
val RevenueMidBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFDE68A, 0xFF5C4410)
val RevenueMidForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF92400E, 0xFFFBBF24)
val RevenueLowContainer: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFEF2F2, 0xFF3A1519)
val RevenueLowBorder: Color
    @Composable @ReadOnlyComposable get() = tono(0xFFFECACA, 0xFF6B2A30)
val RevenueLowForeground: Color
    @Composable @ReadOnlyComposable get() = tono(0xFF991B1B, 0xFFFCA5A5)

// --- Alias de colores que dependen de otros ---
val CourtBlue: Color @Composable @ReadOnlyComposable get() = PrimaryNavy
val MembershipBasic = SlateAccent
val MembershipPremium = AttentionAmber
val MembershipBasicForeground: Color @Composable @ReadOnlyComposable get() = MetricBlueForeground
val MembershipPremiumForeground: Color @Composable @ReadOnlyComposable get() = AmberForeground
val PaymentCash = CourtGreen
val PaymentCard = CourtBlueBright
val ActivityClasses = CourtBlueBright
val ActivityTournaments = AttentionAmber
val ActivityMaintenance = SlateAccent
val UnavailableContainer: Color @Composable @ReadOnlyComposable get() = AlertRedContainer
val UnavailableForeground: Color @Composable @ReadOnlyComposable get() = StatusRedForeground
val UnavailableBorder: Color @Composable @ReadOnlyComposable get() = StatusRedBorder
val MaintenanceContainer: Color @Composable @ReadOnlyComposable get() = AmberContainer
val MaintenanceForeground: Color @Composable @ReadOnlyComposable get() = StatusAmberForeground
val MaintenanceBorder: Color @Composable @ReadOnlyComposable get() = StatusAmberBorder

// --- Nombres que usa la interfaz ---
val Marca: Color @Composable @ReadOnlyComposable get() = PrimaryNavy
/** Azul de acción (cifras y acentos): el azul profundo de siempre en claro y azul claro en oscuro, donde el profundo no se lee. */
val AzulAccion: Color @Composable @ReadOnlyComposable get() = tono(0xFF0A3A82, 0xFF8DB6F5)
val MarcaOscura: Color @Composable @ReadOnlyComposable get() = tono(0xFF021024, 0xFFCFE0FA)
val MarcaSuave: Color @Composable @ReadOnlyComposable get() = tono(0xFFC1E8FF, 0xFF1B3358)
/** Texto o ícono sobre un relleno de [Marca]: blanco en claro, azul muy oscuro en oscuro (donde Marca es azul claro). */
val SobreMarca: Color @Composable @ReadOnlyComposable get() = tono(0xFFFFFFFF, 0xFF06142B)
val Trigo: Color @Composable @ReadOnlyComposable get() = MarcaSuave
val Perla: Color @Composable @ReadOnlyComposable get() = Background
val Acero = SlateAccent
val Turquesa: Color @Composable @ReadOnlyComposable get() = PaymentWallet
val Caledula = AttentionAmber
val CaledulaSuave: Color @Composable @ReadOnlyComposable get() = AmberContainer
val Citrino: Color @Composable @ReadOnlyComposable get() = AmberForeground
val FondoApp: Color @Composable @ReadOnlyComposable get() = Background
val Superficie: Color @Composable @ReadOnlyComposable get() = Surface
val Borde: Color @Composable @ReadOnlyComposable get() = BorderSubdued
val BordeCampo: Color @Composable @ReadOnlyComposable get() = tono(0xFF5483B3, 0xFF4A6A94)
val TextoPrincipal: Color @Composable @ReadOnlyComposable get() = tono(0xFF021024, 0xFFE8EEF7)
val TextoSecundario: Color @Composable @ReadOnlyComposable get() = TextSecondary
val TextoTenue: Color @Composable @ReadOnlyComposable get() = tono(0xFF475569, 0xFF8393AA)
val Exito: Color @Composable @ReadOnlyComposable get() = SuccessForeground
val ExitoSuave: Color @Composable @ReadOnlyComposable get() = SuccessContainer
val Alerta: Color @Composable @ReadOnlyComposable get() = AmberForeground
val AlertaSuave: Color @Composable @ReadOnlyComposable get() = AmberContainer
val Peligro: Color @Composable @ReadOnlyComposable get() = AlertRedForeground
val PeligroSuave: Color @Composable @ReadOnlyComposable get() = AlertRedContainer
