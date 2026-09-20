package app.orionmd.management.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.orionmd.management.data.entity.AppSettingsEntity
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.CustomerEntity
import app.orionmd.management.data.entity.GoodsEntity
import app.orionmd.management.data.entity.GoodsTransactionEntity
import app.orionmd.management.data.entity.PaymentMethodEntity
import app.orionmd.management.data.entity.RateTypeEntity
import app.orionmd.management.data.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<CustomerEntity?>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ServiceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(service: ServiceEntity): Long

    @Update
    suspend fun update(service: ServiceEntity)

    @Delete
    suspend fun delete(service: ServiceEntity)

    @Query("SELECT COUNT(*) FROM services")
    suspend fun count(): Int
}

@Dao
interface RateTypeDao {
    @Query("SELECT * FROM rate_types ORDER BY id ASC")
    fun observeAll(): Flow<List<RateTypeEntity>>

    @Query("SELECT * FROM rate_types ORDER BY id ASC")
    suspend fun getAll(): List<RateTypeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rateType: RateTypeEntity): Long

    @Update
    suspend fun update(rateType: RateTypeEntity)

    @Delete
    suspend fun delete(rateType: RateTypeEntity)

    @Query("SELECT COUNT(*) FROM rate_types")
    suspend fun count(): Int
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY id ASC")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods ORDER BY id ASC")
    suspend fun getAll(): List<PaymentMethodEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(paymentMethod: PaymentMethodEntity): Long

    @Update
    suspend fun update(paymentMethod: PaymentMethodEntity)

    @Delete
    suspend fun delete(paymentMethod: PaymentMethodEntity)

    @Query("SELECT COUNT(*) FROM payment_methods")
    suspend fun count(): Int
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY date_epoch_day ASC, start_minute ASC")
    fun observeAll(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE date_epoch_day = :epochDay ORDER BY start_minute ASC")
    fun observeForDate(epochDay: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day ASC, start_minute ASC")
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day ASC, start_minute ASC")
    suspend fun getForRange(startEpochDay: Long, endEpochDay: Long): List<BookingEntity>

    @Query("SELECT DISTINCT date_epoch_day FROM bookings WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay")
    fun observeBookedDatesInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<Long>>

    /**
     * Other timed bookings on the same date UNDER THE SAME SERVICE - conflict checks are scoped
     * per-service so, e.g., a "Phone" booking and a "Phone2" booking (a different device) are
     * allowed to overlap in time.
     */
    @Query("SELECT * FROM bookings WHERE date_epoch_day = :epochDay AND service_id = :serviceId AND id != :excludeId AND time_not_applicable = 0")
    suspend fun getOtherTimedBookingsOnDate(epochDay: Long, serviceId: Long, excludeId: Long): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE customer_id = :customerId ORDER BY date_epoch_day DESC")
    fun observeForCustomer(customerId: Long): Flow<List<BookingEntity>>

    /** Timed (has a real start/end) bookings for one customer under one service - the basis for Phone Time totals. */
    @Query(
        "SELECT * FROM bookings WHERE customer_id = :customerId AND service_id = :serviceId " +
        "AND time_not_applicable = 0 AND start_minute IS NOT NULL AND end_minute IS NOT NULL " +
        "ORDER BY date_epoch_day ASC"
    )
    suspend fun getTimedBookingsForCustomerAndService(customerId: Long, serviceId: Long): List<BookingEntity>

    @Query("SELECT * FROM bookings ORDER BY date_epoch_day DESC")
    suspend fun getAll(): List<BookingEntity>

    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Update
    suspend fun update(booking: BookingEntity)

    @Delete
    suspend fun delete(booking: BookingEntity)
}

@Dao
interface GoodsDao {
    @Query("SELECT * FROM goods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<GoodsEntity>

    @Query("SELECT * FROM goods WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GoodsEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(goods: GoodsEntity): Long

    @Update
    suspend fun update(goods: GoodsEntity)

    @Delete
    suspend fun delete(goods: GoodsEntity)
}

@Dao
interface GoodsTransactionDao {
    @Query("SELECT * FROM goods_transactions ORDER BY date_epoch_day DESC, created_at DESC")
    fun observeAll(): Flow<List<GoodsTransactionEntity>>

    @Query("SELECT * FROM goods_transactions WHERE goods_id = :goodsId ORDER BY date_epoch_day DESC, created_at DESC")
    fun observeForGoods(goodsId: Long): Flow<List<GoodsTransactionEntity>>

    @Query(
        "SELECT * FROM goods_transactions WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay " +
        "ORDER BY date_epoch_day DESC, created_at DESC"
    )
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<GoodsTransactionEntity>>

    @Query(
        "SELECT * FROM goods_transactions WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay " +
        "ORDER BY date_epoch_day DESC, created_at DESC"
    )
    suspend fun getForRange(startEpochDay: Long, endEpochDay: Long): List<GoodsTransactionEntity>

    @Insert
    suspend fun insert(transaction: GoodsTransactionEntity): Long
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}
