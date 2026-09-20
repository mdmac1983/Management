package app.orionmd.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.orionmd.management.data.dao.AppSettingsDao
import app.orionmd.management.data.dao.BookingDao
import app.orionmd.management.data.dao.CustomerDao
import app.orionmd.management.data.dao.GoodsDao
import app.orionmd.management.data.dao.GoodsTransactionDao
import app.orionmd.management.data.dao.PaymentMethodDao
import app.orionmd.management.data.dao.RateTypeDao
import app.orionmd.management.data.dao.ServiceDao
import app.orionmd.management.data.entity.AppSettingsEntity
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.CustomerEntity
import app.orionmd.management.data.entity.GoodsEntity
import app.orionmd.management.data.entity.GoodsTransactionEntity
import app.orionmd.management.data.entity.GoodsTransactionType
import app.orionmd.management.data.entity.PaymentMethodEntity
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.data.entity.PaymentUnit
import app.orionmd.management.data.entity.RateTypeEntity
import app.orionmd.management.data.entity.ServiceEntity
import app.orionmd.management.data.entity.ThemeMode
import net.sqlcipher.database.SupportFactory

class Converters {
    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = PaymentStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentUnit(value: PaymentUnit): String = value.name

    @TypeConverter
    fun toPaymentUnit(value: String): PaymentUnit = PaymentUnit.valueOf(value)

    @TypeConverter
    fun fromGoodsTransactionType(value: GoodsTransactionType): String = value.name

    @TypeConverter
    fun toGoodsTransactionType(value: String): GoodsTransactionType = GoodsTransactionType.valueOf(value)

    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)
}

@Database(
    entities = [
        CustomerEntity::class,
        ServiceEntity::class,
        RateTypeEntity::class,
        PaymentMethodEntity::class,
        BookingEntity::class,
        AppSettingsEntity::class,
        GoodsEntity::class,
        GoodsTransactionEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun serviceDao(): ServiceDao
    abstract fun rateTypeDao(): RateTypeDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun bookingDao(): BookingDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun goodsDao(): GoodsDao
    abstract fun goodsTransactionDao(): GoodsTransactionDao

    companion object {
        private const val DB_NAME = "rentals_encrypted.db"

        /** v1 -> v2: adds Phone Time tracking fields to customers, and fixes the seeded "Phone"
         *  service to actually capture a time slot (needed for phone-time hours to accumulate -
         *  it was wrongly seeded as timeless in v1). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN has_contract INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customers ADD COLUMN contract_price REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE customers ADD COLUMN contract_hours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE customers ADD COLUMN contract_start_epoch_day INTEGER")
                db.execSQL("ALTER TABLE customers ADD COLUMN counter_reset_epoch_day INTEGER")
                db.execSQL("UPDATE services SET requiresTimeSlot = 1 WHERE name = 'Phone'")
            }
        }

        /** v2 -> v3: adds the in-app display-language setting (Settings > Language). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
            }
        }

        /**
         * v3 -> v4: one-time cleanup for a v1.3 concurrency bug where adding a brand-new customer
         * from the booking form's "+ Add new..." could fire two concurrent "find or create" calls
         * for the same name, creating two customer rows with identical names. This merges any
         * customers that already ended up with a duplicate name: bookings pointing at the extra
         * row(s) are re-pointed at one surviving row (preferring one with an active contract, then
         * the one with more booking history, then the oldest), and the extra row(s) are removed.
         * This is a ONE-TIME cleanup, not an ongoing uniqueness rule - two genuinely different
         * clients who happen to share a first name can still both be added going forward.
         */
        private data class DuplicateCustomerCandidate(val id: Long, val hasContract: Boolean, val bookingCount: Int)

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val duplicateNames = mutableListOf<String>()
                db.query("SELECT name FROM customers GROUP BY name HAVING COUNT(*) > 1").use { cursor ->
                    while (cursor.moveToNext()) duplicateNames.add(cursor.getString(0))
                }
                for (name in duplicateNames) {
                    val candidates = mutableListOf<DuplicateCustomerCandidate>()
                    db.query("SELECT id, has_contract FROM customers WHERE name = ?", arrayOf<Any?>(name)).use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(0)
                            val hasContract = cursor.getInt(1) != 0
                            var bookingCount = 0
                            db.query("SELECT COUNT(*) FROM bookings WHERE customer_id = ?", arrayOf<Any?>(id)).use { bc ->
                                if (bc.moveToFirst()) bookingCount = bc.getInt(0)
                            }
                            candidates.add(DuplicateCustomerCandidate(id, hasContract, bookingCount))
                        }
                    }
                    if (candidates.size <= 1) continue
                    val keeper = candidates
                        .sortedWith(
                            compareByDescending<DuplicateCustomerCandidate> { it.hasContract }
                                .thenByDescending { it.bookingCount }
                                .thenBy { it.id }
                        )
                        .first()
                    candidates.filter { it.id != keeper.id }.forEach { dup ->
                        db.execSQL("UPDATE bookings SET customer_id = ? WHERE customer_id = ?", arrayOf<Any?>(keeper.id, dup.id))
                        db.execSQL("DELETE FROM customers WHERE id = ?", arrayOf<Any?>(dup.id))
                    }
                }
            }
        }

        /**
         * v4 -> v5 (the "Management" rewrite): adds Goods (inventory) tracking with an
         * inflow/outflow ledger, a Mackerels payment-unit conversion rate, and the Day/Night/
         * Material Gray theme setting.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goods (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "cost_price REAL NOT NULL DEFAULT 0.0, " +
                        "sale_price REAL NOT NULL DEFAULT 0.0, " +
                        "quantity_on_hand INTEGER NOT NULL DEFAULT 0, " +
                        "low_stock_threshold INTEGER NOT NULL DEFAULT 3)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goods_transactions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "goods_id INTEGER NOT NULL, " +
                        "goods_name TEXT NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "quantity INTEGER NOT NULL, " +
                        "unit_sale_price REAL NOT NULL DEFAULT 0.0, " +
                        "unit_cost REAL NOT NULL DEFAULT 0.0, " +
                        "date_epoch_day INTEGER NOT NULL, " +
                        "notes TEXT NOT NULL DEFAULT '', " +
                        "created_at INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE app_settings ADD COLUMN mackerel_to_book_rate REAL NOT NULL DEFAULT 4.0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN theme_mode TEXT NOT NULL DEFAULT 'DAY'")
            }
        }

        /** Opens (or creates) the SQLCipher-encrypted database using [rawKey] as the passphrase. */
        fun open(context: Context, rawKey: ByteArray): AppDatabase {
            val factory = SupportFactory(rawKey)
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }

        fun databaseFile(context: Context) = context.applicationContext.getDatabasePath(DB_NAME)

        /** Populates first-run defaults. Safe to call every launch - each insert is IGNORE-on-conflict. */
        suspend fun seedDefaultsIfNeeded(db: AppDatabase) {
            if (db.serviceDao().count() == 0) {
                db.serviceDao().insert(ServiceEntity(name = "Phone", requiresTimeSlot = true, isDefault = true))
            }
            if (db.rateTypeDao().count() == 0) {
                db.rateTypeDao().insert(RateTypeEntity(name = "Hourly"))
                db.rateTypeDao().insert(RateTypeEntity(name = "Session"))
                db.rateTypeDao().insert(RateTypeEntity(name = "Other"))
            }
            if (db.paymentMethodDao().count() == 0) {
                listOf("Books", "Cash App", "Zelle", "Chime", "PayPal", "Other").forEach {
                    db.paymentMethodDao().insert(PaymentMethodEntity(name = it))
                }
            }
            if (db.appSettingsDao().get() == null) {
                db.appSettingsDao().upsert(AppSettingsEntity(id = 1, bookToDollarRate = 8.0))
            }
        }
    }
}
