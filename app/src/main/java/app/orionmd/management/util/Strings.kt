package app.orionmd.management.util

import androidx.compose.runtime.staticCompositionLocalOf
import app.orionmd.management.data.entity.PaymentStatus
import java.util.Locale

/**
 * All user-facing app chrome (labels, buttons, messages) in one place, so Settings > Language can
 * switch the whole app between English and Spanish instantly, independent of the device's system
 * language (this app targets API 29, which predates Android's per-app language API). Data the
 * owner typed in themselves - customer names, service/rate/payment-method names, notes - is never
 * auto-translated, only the surrounding app UI.
 */
interface AppStrings {
    val locale: Locale

    // Tabs
    val tabBooking: String
    val tabSchedule: String
    val tabRevenue: String
    val tabAnalytics: String
    val tabDatabase: String
    val tabSettings: String

    // Common
    val cancel: String
    val save: String
    val ok: String
    val delete: String
    val add: String
    val addNewPrefix: String
    val notApplicable: String
    val noneDash: String
    val previous: String
    val next: String
    val saveAsPdf: String
    val pdfSavedMessage: String
    val pdfFailedPrefix: String
    val weekdayShortLabels: List<String>
    fun statusLabel(status: PaymentStatus): String
    fun bookingsSummary(count: Int, unpaidCount: Int): String

    // Booking tab
    val bookingScreenTitle: String
    val previousMonth: String
    val nextMonth: String
    val noBookingsYet: String
    val addBookingDesc: String
    val timeConflictMessage: String

    // Booking form dialog
    val newBookingTitle: String
    val editBookingTitle: String
    val customerLabel: String
    val serviceLabel: String
    val notApplicableCheckboxLabel: String
    val timeStartLabel: String
    val timeEndLabel: String
    val rateLabel: String
    val paymentMethodLabel: String
    val paymentAmountLabel: String
    val dollarsChipLabel: String
    val booksChipLabel: String
    val mackerelsChipLabel: String
    val paymentStatusLabel: String
    val amountPaidLabel: String
    val notesLabel: String
    val errorPickCustomer: String
    val errorPickService: String
    val errorPickRate: String
    val errorPickPaymentMethod: String
    val errorInvalidAmount: String
    val errorSetStartEnd: String
    val errorEndAfterStart: String

    // Schedule tab
    val scheduleScreenTitle: String
    val dailyLabel: String
    val weekLabel: String
    val monthLabel: String
    val noBookingsScheduled: String
    val noBookingsThisDay: String

    // Revenue tab
    val revenueScreenTitle: String
    val dateLabel: String
    val rangeLabel: String
    val fromLabel: String
    val toLabel: String
    val bottomLineTitle: String
    val paidLabel: String
    val unpaidLabel: String
    val entriesLabel: String
    val noRevenueEntries: String
    val goodsSection: String
    val goodsRevenueLabel: String
    val goodsCostLabel: String
    val goodsProfitLabel: String
    val goodsSoldPrefix: String
    val goodsLabel: String
    val quantityAbbrev: String

    // Analytics tab
    val analyticsScreenTitle: String
    val clientsLabel: String
    val breakdownsLabel: String
    val noAnalyticsData: String
    val preferredPaymentPrefix: String
    val servicesPrefix: String
    val dimensionService: String
    val dimensionPaymentMethod: String
    val dimensionClient: String

    // Analytics tab — Inventory view
    val inventoryLabel: String
    val lowStockAlertsTitle: String
    val noLowStockItems: String
    fun lowStockRow(name: String, quantity: Int, threshold: Int): String
    val topSellingGoodsTitle: String
    val noGoodsSalesYet: String
    fun unitsSoldSuffix(count: Int): String
    val inventoryValuationTitle: String
    val totalUnitsInStockLabel: String
    val valueAtCostLabel: String
    val valueAtSaleLabel: String
    val potentialProfitLabel: String
    val profitMarginTitle: String
    val overallMarginLabel: String

    // Client detail dialog (Phone Time)
    val phoneTimeTitle: String
    val allTimeLabel: String
    val customLabel: String
    val contractLabel: String
    val lifetimePhoneTime: String
    val sinceClientAdded: String
    val sincePrefix: String
    val resetCounterButton: String
    val resetCounterConfirmTitle: String
    fun resetCounterConfirmText(name: String): String
    val contractPriceLabel: String
    val hoursGivenLabel: String
    val timeUsedLabel: String
    val timeRemainingLabel: String
    val overByPrefix: String
    val contractStartedPrefix: String
    val editContractButton: String
    val clearContractButton: String
    val noContractSetUp: String
    val setUpContractButton: String
    val clearContractConfirmTitle: String
    fun clearContractConfirmText(name: String): String
    val contractSetupTitle: String
    val contractPriceFieldLabel: String
    val contractHoursFieldLabel: String
    val contractStartDatePrefix: String
    val errorInvalidPrice: String
    val errorInvalidHours: String

    // Contract section (booking form + Analytics client card)
    val contractSectionTitle: String
    val contractStatusPrefix: String
    fun contractGivenSummary(price: String, hours: String): String
    fun remainingSuffix(time: String): String
    fun overSuffix(time: String): String
    fun contractCardLine(used: String, remainingOrOver: String): String

    // Database tab
    val customersSection: String
    val addCustomerLabel: String
    val noGoodsYet: String
    val addGoodsLabel: String
    val goodsNameLabel: String
    val goodsCostPriceLabel: String
    val goodsSalePriceLabel: String
    val goodsInitialQuantityLabel: String
    val errorEnterGoodsName: String
    val errorInvalidQuantity: String
    val lowStockLabel: String
    val updatePricesLabel: String
    val restockLabel: String
    val sellLabel: String
    val quantityLabel: String
    val restockedMessage: String
    val soldMessage: String
    val insufficientStockMessage: String

    // Settings
    val settingsScreenTitle: String
    val servicesSection: String
    val ratesSection: String
    val paymentMethodsSection: String
    val addServiceLabel: String
    val addRateTypeLabel: String
    val addPaymentMethodLabel: String
    val booksConversionTitle: String
    val oneBookEquals: String
    val mackerelsEqualOneBook: String
    val themeSection: String
    val themeDay: String
    val themeNight: String
    val themeMaterialGray: String
    val showLess: String
    val showFullHistory: String
    val languageSection: String
    val languageEnglish: String
    val languageSpanish: String
    val appLockSection: String
    val changeLockDescription: String
    val changeLockButton: String
    val backupSection: String
    val backupDescription: String
    val exportLabel: String
    val importLabel: String
    val backupSavedMessage: String
    val backupFailedPrefix: String
    val backupRestoredMessage: String
    val restoreFailedPrefix: String
    val changelogTitlePrefix: String
    val buyMeACoffee: String
    val copyrightPrefix: String
    val confirmCurrentPrefix: String
    val chooseNewLockType: String
    val enterNewPrefix: String
    val confirmNewPrefix: String
    val incorrectTryAgain: String
    val didntMatchTryAgain: String
    val changeLockFailedPrefix: String

    // Lock screens
    val appDisplayName: String
    val setUpYourLockTitle: String
    val chooseHowToUnlockSubtitle: String
    val chooseDifferentLockType: String
    val enterYourNewPrefix: String
    val confirmYourNewPrefix: String
    fun enterToUnlockSubtitle(typeLabel: String): String
    val usePin: String
    val usePattern: String
    val usePassword: String
    val continueLabel: String
    val connectAtLeastFourDots: String
    val passwordLabel: String
    val toggleVisibilityDesc: String
    fun credentialTypeLabel(type: String): String

    // Common inputs
    val addNewTitlePrefix: String
    val tapToSelect: String
    val setLabel: String
}

private fun statusLabelEn(status: PaymentStatus) = when (status) {
    PaymentStatus.PAID -> "Paid"
    PaymentStatus.PARTIAL -> "Partial"
    PaymentStatus.UNPAID -> "Unpaid"
}

private fun statusLabelEs(status: PaymentStatus) = when (status) {
    PaymentStatus.PAID -> "Pagado"
    PaymentStatus.PARTIAL -> "Parcial"
    PaymentStatus.UNPAID -> "Sin pagar"
}

object EnglishStrings : AppStrings {
    override val locale = Locale.US

    override val tabBooking = "Booking"
    override val tabSchedule = "Schedule"
    override val tabRevenue = "Revenue"
    override val tabAnalytics = "Analytics"
    override val tabDatabase = "Database"
    override val tabSettings = "Settings"

    override val cancel = "Cancel"
    override val save = "Save"
    override val ok = "OK"
    override val delete = "Delete"
    override val add = "Add"
    override val addNewPrefix = "+ Add new..."
    override val notApplicable = "Not Applicable"
    override val noneDash = "—"
    override val previous = "Previous"
    override val next = "Next"
    override val saveAsPdf = "Save as PDF"
    override val pdfSavedMessage = "PDF saved"
    override val pdfFailedPrefix = "PDF export failed: "
    override val weekdayShortLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    override fun statusLabel(status: PaymentStatus) = statusLabelEn(status)
    override fun bookingsSummary(count: Int, unpaidCount: Int): String {
        val noun = if (count == 1) "booking" else "bookings"
        return "$count $noun • $unpaidCount not fully paid"
    }

    override val bookingScreenTitle = "Booking"
    override val previousMonth = "Previous month"
    override val nextMonth = "Next month"
    override val noBookingsYet = "No bookings yet"
    override val addBookingDesc = "Add booking"
    override val timeConflictMessage = "Time conflict - please fix"

    override val newBookingTitle = "New Booking"
    override val editBookingTitle = "Edit Booking"
    override val customerLabel = "Customer"
    override val serviceLabel = "Service"
    override val notApplicableCheckboxLabel = "Not Applicable (no time slot needed)"
    override val timeStartLabel = "Time Start"
    override val timeEndLabel = "Time End"
    override val rateLabel = "Rate"
    override val paymentMethodLabel = "Payment Method"
    override val paymentAmountLabel = "Payment Amount"
    override val dollarsChipLabel = "$"
    override val booksChipLabel = "Books"
    override val mackerelsChipLabel = "Mackerels"
    override val paymentStatusLabel = "Payment Status"
    override val amountPaidLabel = "Amount Paid So Far"
    override val notesLabel = "Notes (optional)"
    override val errorPickCustomer = "Pick or add a customer"
    override val errorPickService = "Pick or add a service"
    override val errorPickRate = "Pick or add a rate"
    override val errorPickPaymentMethod = "Pick or add a payment method"
    override val errorInvalidAmount = "Enter a valid payment amount"
    override val errorSetStartEnd = "Set a start and end time, or mark Not Applicable"
    override val errorEndAfterStart = "End time must be after start time"

    override val scheduleScreenTitle = "Schedule"
    override val dailyLabel = "Daily"
    override val weekLabel = "Week"
    override val monthLabel = "Month"
    override val noBookingsScheduled = "No bookings scheduled"
    override val noBookingsThisDay = "No bookings"

    override val revenueScreenTitle = "Revenue"
    override val dateLabel = "Date"
    override val rangeLabel = "Range"
    override val fromLabel = "From"
    override val toLabel = "To"
    override val bottomLineTitle = "Bottom Line"
    override val paidLabel = "Paid"
    override val unpaidLabel = "Unpaid"
    override val entriesLabel = "Entries"
    override val noRevenueEntries = "No revenue entries for this period"
    override val goodsSection = "Goods"
    override val goodsRevenueLabel = "Goods Revenue"
    override val goodsCostLabel = "Cost of Goods"
    override val goodsProfitLabel = "Goods Profit"
    override val goodsSoldPrefix = "Sold: "
    override val goodsLabel = "Goods"
    override val quantityAbbrev = "Qty: "

    override val analyticsScreenTitle = "Analytics"
    override val clientsLabel = "Clients"
    override val breakdownsLabel = "Breakdowns"
    override val noAnalyticsData = "No data yet — analytics will fill in once you have bookings"
    override val preferredPaymentPrefix = "Preferred payment: "
    override val servicesPrefix = "Services: "
    override val dimensionService = "Service"
    override val dimensionPaymentMethod = "Payment Method"
    override val dimensionClient = "Client"

    override val inventoryLabel = "Inventory"
    override val lowStockAlertsTitle = "Low Stock Alerts"
    override val noLowStockItems = "All stocked up — nothing at or below its reorder threshold"
    override fun lowStockRow(name: String, quantity: Int, threshold: Int) = "$name — $quantity left (reorder at $threshold)"
    override val topSellingGoodsTitle = "Top-Selling Goods"
    override val noGoodsSalesYet = "No goods sold yet"
    override fun unitsSoldSuffix(count: Int) = "$count sold"
    override val inventoryValuationTitle = "Inventory Valuation"
    override val totalUnitsInStockLabel = "Total units in stock"
    override val valueAtCostLabel = "Value at cost"
    override val valueAtSaleLabel = "Value at sale price"
    override val potentialProfitLabel = "Potential profit if all sold"
    override val profitMarginTitle = "Profit Margin"
    override val overallMarginLabel = "Overall margin"

    override val phoneTimeTitle = "Phone Time"
    override val allTimeLabel = "All Time"
    override val customLabel = "Custom"
    override val contractLabel = "Contract"
    override val lifetimePhoneTime = "Lifetime phone time"
    override val sinceClientAdded = "Since this client was added"
    override val sincePrefix = "Since "
    override val resetCounterButton = "Reset Counter"
    override val resetCounterConfirmTitle = "Reset Counter?"
    override fun resetCounterConfirmText(name: String) =
        "This zeroes out the Custom phone-time counter for $name starting today. Past bookings aren't deleted."
    override val contractPriceLabel = "Contract price"
    override val hoursGivenLabel = "Hours given"
    override val timeUsedLabel = "Time used"
    override val timeRemainingLabel = "Time remaining"
    override val overByPrefix = "Over by "
    override val contractStartedPrefix = "Contract started "
    override val editContractButton = "Edit Contract"
    override val clearContractButton = "Clear Contract"
    override val noContractSetUp = "No contract set up"
    override val setUpContractButton = "Set Up Contract"
    override val clearContractConfirmTitle = "Clear Contract?"
    override fun clearContractConfirmText(name: String) =
        "This removes $name's contract details. Time already logged is not affected."
    override val contractSetupTitle = "Set Up Contract"
    override val contractPriceFieldLabel = "Contract price ($)"
    override val contractHoursFieldLabel = "Hours given"
    override val contractStartDatePrefix = "Start date: "
    override val errorInvalidPrice = "Enter a valid price"
    override val errorInvalidHours = "Enter valid hours"

    override val contractSectionTitle = "Phone Time Contract"
    override val contractStatusPrefix = "Contract: "
    override fun contractGivenSummary(price: String, hours: String) = "$price · ${hours} given"
    override fun remainingSuffix(time: String) = "$time remaining"
    override fun overSuffix(time: String) = "$time over"
    override fun contractCardLine(used: String, remainingOrOver: String) = "$used used · $remainingOrOver"

    override val customersSection = "Customers"
    override val addCustomerLabel = "Add customer"
    override val noGoodsYet = "No goods added yet"
    override val addGoodsLabel = "Add goods"
    override val goodsNameLabel = "Item name"
    override val goodsCostPriceLabel = "Cost price ($)"
    override val goodsSalePriceLabel = "Sale price ($)"
    override val goodsInitialQuantityLabel = "Starting quantity"
    override val errorEnterGoodsName = "Enter an item name"
    override val errorInvalidQuantity = "Enter a valid quantity"
    override val lowStockLabel = "Low stock"
    override val updatePricesLabel = "Update prices"
    override val restockLabel = "Restock (inflow)"
    override val sellLabel = "Sell (outflow)"
    override val quantityLabel = "Quantity"
    override val restockedMessage = "Restocked"
    override val soldMessage = "Sale logged"
    override val insufficientStockMessage = "Not enough stock on hand"

    override val settingsScreenTitle = "Settings"
    override val servicesSection = "Services"
    override val ratesSection = "Rates"
    override val paymentMethodsSection = "Payment Methods"
    override val addServiceLabel = "Add service"
    override val addRateTypeLabel = "Add rate type"
    override val addPaymentMethodLabel = "Add payment method"
    override val booksConversionTitle = "Books Conversion Rate"
    override val oneBookEquals = "1 Book =  $"
    override val mackerelsEqualOneBook = "Mackerels = 1 Book"
    override val themeSection = "Theme"
    override val themeDay = "Day"
    override val themeNight = "Night"
    override val themeMaterialGray = "Material Gray"
    override val showLess = "Show less"
    override val showFullHistory = "Show full history"
    override val languageSection = "Language"
    override val languageEnglish = "English"
    override val languageSpanish = "Español"
    override val appLockSection = "App Lock"
    override val changeLockDescription = "Change your PIN, pattern, or password."
    override val changeLockButton = "Change Lock"
    override val backupSection = "Backup"
    override val backupDescription = "Export or restore all client and booking data (encrypted)."
    override val exportLabel = "Export"
    override val importLabel = "Import"
    override val backupSavedMessage = "Backup saved"
    override val backupFailedPrefix = "Backup failed: "
    override val backupRestoredMessage = "Backup restored - unlock again to continue"
    override val restoreFailedPrefix = "Restore failed: "
    override val changelogTitlePrefix = "Changelog — v"
    override val buyMeACoffee = "Buy Me a Coffee"
    override val copyrightPrefix = "© "
    override val confirmCurrentPrefix = "Confirm your current "
    override val chooseNewLockType = "Choose a new lock type"
    override val enterNewPrefix = "Enter your new "
    override val confirmNewPrefix = "Confirm your new "
    override val incorrectTryAgain = "Incorrect - try again"
    override val didntMatchTryAgain = "Didn't match - try again"
    override val changeLockFailedPrefix = "Couldn't change lock (your old one still works): "

    override val appDisplayName = "Management"
    override val setUpYourLockTitle = "Set Up Your Lock"
    override val chooseHowToUnlockSubtitle =
        "Choose how you'll unlock Management. All of your booking, client, and inventory data is encrypted with this."
    override val chooseDifferentLockType = "Choose a different lock type"
    override val enterYourNewPrefix = "Enter your new "
    override val confirmYourNewPrefix = "Confirm your "
    override fun enterToUnlockSubtitle(typeLabel: String) = "Enter your $typeLabel to unlock"
    override val usePin = "Use a PIN (4-8 digits)"
    override val usePattern = "Use a Pattern"
    override val usePassword = "Use a Password"
    override val continueLabel = "Continue"
    override val connectAtLeastFourDots = "Connect at least 4 dots"
    override val passwordLabel = "Password"
    override val toggleVisibilityDesc = "Toggle visibility"
    override fun credentialTypeLabel(type: String) = when (type) {
        "PIN" -> "PIN"
        "PATTERN" -> "pattern"
        "PASSWORD" -> "password"
        else -> type.lowercase()
    }

    override val addNewTitlePrefix = "Add new "
    override val tapToSelect = "Tap to select"
    override val setLabel = "Set"
}

object SpanishStrings : AppStrings {
    override val locale: Locale = Locale("es", "ES")

    override val tabBooking = "Reservas"
    override val tabSchedule = "Agenda"
    override val tabRevenue = "Ingresos"
    override val tabAnalytics = "Análisis"
    override val tabDatabase = "Base de Datos"
    override val tabSettings = "Ajustes"

    override val cancel = "Cancelar"
    override val save = "Guardar"
    override val ok = "Aceptar"
    override val delete = "Eliminar"
    override val add = "Añadir"
    override val addNewPrefix = "+ Añadir nuevo..."
    override val notApplicable = "No aplica"
    override val noneDash = "—"
    override val previous = "Anterior"
    override val next = "Siguiente"
    override val saveAsPdf = "Guardar como PDF"
    override val pdfSavedMessage = "PDF guardado"
    override val pdfFailedPrefix = "Error al exportar el PDF: "
    override val weekdayShortLabels = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    override fun statusLabel(status: PaymentStatus) = statusLabelEs(status)
    override fun bookingsSummary(count: Int, unpaidCount: Int): String {
        val noun = if (count == 1) "reserva" else "reservas"
        return "$count $noun • $unpaidCount sin pagar por completo"
    }

    override val bookingScreenTitle = "Reservas"
    override val previousMonth = "Mes anterior"
    override val nextMonth = "Mes siguiente"
    override val noBookingsYet = "Aún no hay reservas"
    override val addBookingDesc = "Añadir reserva"
    override val timeConflictMessage = "Conflicto de horario - corrígelo"

    override val newBookingTitle = "Nueva Reserva"
    override val editBookingTitle = "Editar Reserva"
    override val customerLabel = "Cliente"
    override val serviceLabel = "Servicio"
    override val notApplicableCheckboxLabel = "No aplica (no necesita horario)"
    override val timeStartLabel = "Hora de Inicio"
    override val timeEndLabel = "Hora de Fin"
    override val rateLabel = "Tarifa"
    override val paymentMethodLabel = "Método de Pago"
    override val paymentAmountLabel = "Monto del Pago"
    override val dollarsChipLabel = "$"
    override val booksChipLabel = "Libros"
    override val mackerelsChipLabel = "Macarelas"
    override val paymentStatusLabel = "Estado del Pago"
    override val amountPaidLabel = "Monto Pagado Hasta Ahora"
    override val notesLabel = "Notas (opcional)"
    override val errorPickCustomer = "Elige o añade un cliente"
    override val errorPickService = "Elige o añade un servicio"
    override val errorPickRate = "Elige o añade una tarifa"
    override val errorPickPaymentMethod = "Elige o añade un método de pago"
    override val errorInvalidAmount = "Ingresa un monto de pago válido"
    override val errorSetStartEnd = "Establece hora de inicio y fin, o marca No aplica"
    override val errorEndAfterStart = "La hora de fin debe ser posterior a la de inicio"

    override val scheduleScreenTitle = "Agenda"
    override val dailyLabel = "Diario"
    override val weekLabel = "Semana"
    override val monthLabel = "Mes"
    override val noBookingsScheduled = "No hay reservas programadas"
    override val noBookingsThisDay = "Sin reservas"

    override val revenueScreenTitle = "Ingresos"
    override val dateLabel = "Fecha"
    override val rangeLabel = "Rango"
    override val fromLabel = "Desde"
    override val toLabel = "Hasta"
    override val bottomLineTitle = "Balance Final"
    override val paidLabel = "Pagado"
    override val unpaidLabel = "Sin pagar"
    override val entriesLabel = "Entradas"
    override val noRevenueEntries = "No hay ingresos registrados en este período"
    override val goodsSection = "Mercancía"
    override val goodsRevenueLabel = "Ingresos por Mercancía"
    override val goodsCostLabel = "Costo de Mercancía"
    override val goodsProfitLabel = "Ganancia de Mercancía"
    override val goodsSoldPrefix = "Vendido: "
    override val goodsLabel = "Mercancía"
    override val quantityAbbrev = "Cant: "

    override val analyticsScreenTitle = "Análisis"
    override val clientsLabel = "Clientes"
    override val breakdownsLabel = "Desgloses"
    override val noAnalyticsData = "Aún no hay datos — el análisis se completará con tus reservas"
    override val preferredPaymentPrefix = "Pago preferido: "
    override val servicesPrefix = "Servicios: "
    override val dimensionService = "Servicio"
    override val dimensionPaymentMethod = "Método de Pago"
    override val dimensionClient = "Cliente"

    override val inventoryLabel = "Inventario"
    override val lowStockAlertsTitle = "Alertas de Stock Bajo"
    override val noLowStockItems = "Todo abastecido — nada está en o por debajo de su umbral de reposición"
    override fun lowStockRow(name: String, quantity: Int, threshold: Int) = "$name — quedan $quantity (reponer en $threshold)"
    override val topSellingGoodsTitle = "Mercancía Más Vendida"
    override val noGoodsSalesYet = "Aún no se ha vendido mercancía"
    override fun unitsSoldSuffix(count: Int) = "$count vendidos"
    override val inventoryValuationTitle = "Valoración de Inventario"
    override val totalUnitsInStockLabel = "Total de unidades en stock"
    override val valueAtCostLabel = "Valor al costo"
    override val valueAtSaleLabel = "Valor al precio de venta"
    override val potentialProfitLabel = "Ganancia potencial si se vende todo"
    override val profitMarginTitle = "Margen de Ganancia"
    override val overallMarginLabel = "Margen general"

    override val phoneTimeTitle = "Tiempo de Llamadas"
    override val allTimeLabel = "Todo el Tiempo"
    override val customLabel = "Personalizado"
    override val contractLabel = "Contrato"
    override val lifetimePhoneTime = "Tiempo total de llamadas"
    override val sinceClientAdded = "Desde que se añadió este cliente"
    override val sincePrefix = "Desde "
    override val resetCounterButton = "Reiniciar Contador"
    override val resetCounterConfirmTitle = "¿Reiniciar contador?"
    override fun resetCounterConfirmText(name: String) =
        "Esto pone en cero el contador personalizado de $name a partir de hoy. Las reservas pasadas no se eliminan."
    override val contractPriceLabel = "Precio del contrato"
    override val hoursGivenLabel = "Horas otorgadas"
    override val timeUsedLabel = "Tiempo usado"
    override val timeRemainingLabel = "Tiempo restante"
    override val overByPrefix = "Excedido por "
    override val contractStartedPrefix = "Contrato iniciado el "
    override val editContractButton = "Editar Contrato"
    override val clearContractButton = "Eliminar Contrato"
    override val noContractSetUp = "No hay contrato configurado"
    override val setUpContractButton = "Configurar Contrato"
    override val clearContractConfirmTitle = "¿Eliminar contrato?"
    override fun clearContractConfirmText(name: String) =
        "Esto elimina los datos del contrato de $name. El tiempo ya registrado no se ve afectado."
    override val contractSetupTitle = "Configurar Contrato"
    override val contractPriceFieldLabel = "Precio del contrato ($)"
    override val contractHoursFieldLabel = "Horas otorgadas"
    override val contractStartDatePrefix = "Fecha de inicio: "
    override val errorInvalidPrice = "Ingresa un precio válido"
    override val errorInvalidHours = "Ingresa horas válidas"

    override val contractSectionTitle = "Contrato de Tiempo de Llamadas"
    override val contractStatusPrefix = "Contrato: "
    override fun contractGivenSummary(price: String, hours: String) = "$price · $hours otorgadas"
    override fun remainingSuffix(time: String) = "$time restante"
    override fun overSuffix(time: String) = "$time excedido"
    override fun contractCardLine(used: String, remainingOrOver: String) = "$used usado · $remainingOrOver"

    override val customersSection = "Clientes"
    override val addCustomerLabel = "Añadir cliente"
    override val noGoodsYet = "Aún no hay mercancía añadida"
    override val addGoodsLabel = "Añadir mercancía"
    override val goodsNameLabel = "Nombre del artículo"
    override val goodsCostPriceLabel = "Precio de costo ($)"
    override val goodsSalePriceLabel = "Precio de venta ($)"
    override val goodsInitialQuantityLabel = "Cantidad inicial"
    override val errorEnterGoodsName = "Ingresa un nombre de artículo"
    override val errorInvalidQuantity = "Ingresa una cantidad válida"
    override val lowStockLabel = "Poco inventario"
    override val updatePricesLabel = "Actualizar precios"
    override val restockLabel = "Reabastecer (entrada)"
    override val sellLabel = "Vender (salida)"
    override val quantityLabel = "Cantidad"
    override val restockedMessage = "Reabastecido"
    override val soldMessage = "Venta registrada"
    override val insufficientStockMessage = "No hay suficiente inventario"

    override val settingsScreenTitle = "Ajustes"
    override val servicesSection = "Servicios"
    override val ratesSection = "Tarifas"
    override val paymentMethodsSection = "Métodos de Pago"
    override val addServiceLabel = "Añadir servicio"
    override val addRateTypeLabel = "Añadir tipo de tarifa"
    override val addPaymentMethodLabel = "Añadir método de pago"
    override val booksConversionTitle = "Tasa de Conversión de Libros"
    override val oneBookEquals = "1 Libro =  $"
    override val mackerelsEqualOneBook = "Macarelas = 1 Libro"
    override val themeSection = "Tema"
    override val themeDay = "Día"
    override val themeNight = "Noche"
    override val themeMaterialGray = "Gris Material"
    override val showLess = "Mostrar menos"
    override val showFullHistory = "Mostrar historial completo"
    override val languageSection = "Idioma"
    override val languageEnglish = "English"
    override val languageSpanish = "Español"
    override val appLockSection = "Bloqueo de la App"
    override val changeLockDescription = "Cambia tu PIN, patrón o contraseña."
    override val changeLockButton = "Cambiar Bloqueo"
    override val backupSection = "Copia de Seguridad"
    override val backupDescription = "Exporta o restaura todos los datos de clientes y reservas (encriptados)."
    override val exportLabel = "Exportar"
    override val importLabel = "Importar"
    override val backupSavedMessage = "Copia de seguridad guardada"
    override val backupFailedPrefix = "Error en la copia de seguridad: "
    override val backupRestoredMessage = "Copia restaurada - vuelve a desbloquear para continuar"
    override val restoreFailedPrefix = "Error al restaurar: "
    override val changelogTitlePrefix = "Historial de Cambios — v"
    override val buyMeACoffee = "Invítame un Café"
    override val copyrightPrefix = "© "
    override val confirmCurrentPrefix = "Confirma tu "
    override val chooseNewLockType = "Elige un nuevo tipo de bloqueo"
    override val enterNewPrefix = "Ingresa tu nuevo "
    override val confirmNewPrefix = "Confirma tu nuevo "
    override val incorrectTryAgain = "Incorrecto - inténtalo de nuevo"
    override val didntMatchTryAgain = "No coincide - inténtalo de nuevo"
    override val changeLockFailedPrefix = "No se pudo cambiar el bloqueo (el anterior sigue funcionando): "

    override val appDisplayName = "Management"
    override val setUpYourLockTitle = "Configura tu Bloqueo"
    override val chooseHowToUnlockSubtitle =
        "Elige cómo desbloquearás Management. Todos tus datos de reservas, clientes e inventario se encriptan con esto."
    override val chooseDifferentLockType = "Elegir un tipo de bloqueo diferente"
    override val enterYourNewPrefix = "Ingresa tu nuevo "
    override val confirmYourNewPrefix = "Confirma tu "
    override fun enterToUnlockSubtitle(typeLabel: String) = "Ingresa tu $typeLabel para desbloquear"
    override val usePin = "Usar un PIN (4-8 dígitos)"
    override val usePattern = "Usar un Patrón"
    override val usePassword = "Usar una Contraseña"
    override val continueLabel = "Continuar"
    override val connectAtLeastFourDots = "Conecta al menos 4 puntos"
    override val passwordLabel = "Contraseña"
    override val toggleVisibilityDesc = "Mostrar u ocultar"
    override fun credentialTypeLabel(type: String) = when (type) {
        "PIN" -> "PIN"
        "PATTERN" -> "patrón"
        "PASSWORD" -> "contraseña"
        else -> type.lowercase()
    }

    override val addNewTitlePrefix = "Añadir nuevo "
    override val tapToSelect = "Toca para seleccionar"
    override val setLabel = "Establecer"
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
