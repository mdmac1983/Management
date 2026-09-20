package app.orionmd.management.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A client the owner has booked rentals/services for. */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",

    // --- Phone Time tracking (Analytics > client detail) ---
    /** Whether this client has an active phone-time contract. */
    @ColumnInfo(name = "has_contract", defaultValue = "0") val hasContract: Boolean = false,
    @ColumnInfo(name = "contract_price", defaultValue = "0.0") val contractPrice: Double = 0.0,
    /** Total hours granted under the contract (e.g. 10.5). */
    @ColumnInfo(name = "contract_hours", defaultValue = "0.0") val contractHours: Double = 0.0,
    /** Usage is measured from this date forward. Null until a contract is set up. */
    @ColumnInfo(name = "contract_start_epoch_day") val contractStartEpochDay: Long? = null,
    /** The "User Defined" counter counts Phone-time bookings from this date forward; null = since the client was added. */
    @ColumnInfo(name = "counter_reset_epoch_day") val customCounterResetEpochDay: Long? = null
)

/** A service the owner offers (e.g. "Phone", "DJ Booth", "Speaker Rental"). */
@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** If false, this service never needs a time slot - bookings default to "Not Applicable". */
    val requiresTimeSlot: Boolean = true,
    val isDefault: Boolean = false
)

/** An owner-editable rate structure (Hourly / Session / Other, plus any custom ones added). */
@Entity(tableName = "rate_types")
data class RateTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** An owner-editable payment method (Books / Cash App / Zelle / Chime / PayPal / Other, + custom). */
@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

enum class PaymentStatus { PAID, PARTIAL, UNPAID }
/** MACKERELS converts to BOOKS via [AppSettingsEntity.mackerelToBookRate], then BOOKS to dollars as usual. */
enum class PaymentUnit { DOLLARS, BOOKS, MACKERELS }

/** A trackable store item (goods/inventory), separate from time-based [ServiceEntity] rentals. */
@Entity(tableName = "goods")
data class GoodsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "cost_price", defaultValue = "0.0") val costPrice: Double = 0.0,
    @ColumnInfo(name = "sale_price", defaultValue = "0.0") val salePrice: Double = 0.0,
    @ColumnInfo(name = "quantity_on_hand", defaultValue = "0") val quantityOnHand: Int = 0,
    /** A restock-alert threshold; quantityOnHand at/below this is surfaced as low stock in Analytics. */
    @ColumnInfo(name = "low_stock_threshold", defaultValue = "3") val lowStockThreshold: Int = 3
)

enum class GoodsTransactionType { IN, OUT }

/**
 * One inflow (restock) or outflow (sale) of a [GoodsEntity], the basis for Goods revenue/profit
 * and the Database tab's per-item history. Cost/price are captured at transaction time so
 * changing an item's current cost/sale price later doesn't rewrite past history.
 */
@Entity(tableName = "goods_transactions")
data class GoodsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goods_id") val goodsId: Long,
    @ColumnInfo(name = "goods_name") val goodsName: String,
    val type: GoodsTransactionType,
    val quantity: Int,
    /** Sale price per unit at the time of an OUT transaction (0 for IN). */
    @ColumnInfo(name = "unit_sale_price") val unitSalePrice: Double = 0.0,
    /** Cost basis per unit at the time of the transaction (used for profit-margin math on OUT). */
    @ColumnInfo(name = "unit_cost") val unitCost: Double = 0.0,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/** One scheduled rental/booking entry, shown on both the Booking calendar and the Schedule tab. */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,

    @ColumnInfo(name = "customer_id") val customerId: Long,
    @ColumnInfo(name = "customer_name") val customerName: String,

    @ColumnInfo(name = "service_id") val serviceId: Long,
    @ColumnInfo(name = "service_name") val serviceName: String,

    /** Minutes since midnight, local time. Null when [timeNotApplicable] is true. */
    @ColumnInfo(name = "start_minute") val startMinute: Int?,
    @ColumnInfo(name = "end_minute") val endMinute: Int?,
    @ColumnInfo(name = "time_not_applicable") val timeNotApplicable: Boolean = false,

    @ColumnInfo(name = "rate_type") val rateType: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,

    @ColumnInfo(name = "payment_amount") val paymentAmount: Double,
    @ColumnInfo(name = "payment_unit") val paymentUnit: PaymentUnit = PaymentUnit.DOLLARS,

    @ColumnInfo(name = "payment_status") val paymentStatus: PaymentStatus,
    /** Only meaningful when [paymentStatus] == PARTIAL - how much of [paymentAmount] has been paid. */
    @ColumnInfo(name = "amount_paid") val amountPaid: Double = 0.0,

    val notes: String = "",

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/** "day" (light), "night" (dark), or "gray" (Material Gray) - Settings > Theme. */
enum class ThemeMode { DAY, NIGHT, GRAY }

/** Single-row table of app-wide editable settings (Books conversion rate, language, etc). */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "book_to_dollar_rate") val bookToDollarRate: Double = 8.0,
    /** "en" or "es" - the in-app display language, independent of the device's system language. */
    @ColumnInfo(name = "language", defaultValue = "en") val language: String = "en",
    /** How many Mackerels equal 1 Book (e.g. 4.0 = "4 Mackerels = 1 Book"). */
    @ColumnInfo(name = "mackerel_to_book_rate", defaultValue = "4.0") val mackerelToBookRate: Double = 4.0,
    @ColumnInfo(name = "theme_mode", defaultValue = "DAY") val themeMode: ThemeMode = ThemeMode.DAY
)
