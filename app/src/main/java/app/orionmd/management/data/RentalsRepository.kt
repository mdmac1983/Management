package app.orionmd.management.data

import androidx.room.withTransaction
import app.orionmd.management.data.entity.AppSettingsEntity
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.CustomerEntity
import app.orionmd.management.data.entity.GoodsEntity
import app.orionmd.management.data.entity.GoodsTransactionEntity
import app.orionmd.management.data.entity.GoodsTransactionType
import app.orionmd.management.data.entity.PaymentMethodEntity
import app.orionmd.management.data.entity.RateTypeEntity
import app.orionmd.management.data.entity.ServiceEntity
import app.orionmd.management.data.entity.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Result of attempting to save a booking. */
sealed class SaveBookingResult {
    data class Success(val id: Long) : SaveBookingResult()
    object TimeConflict : SaveBookingResult()
}

/** Result of attempting to sell goods out of inventory. */
sealed class SellGoodsResult {
    data class Success(val id: Long) : SellGoodsResult()
    object InsufficientStock : SellGoodsResult()
}

class RentalsRepository(private val db: AppDatabase) {

    // ---- Customers ----
    fun observeCustomers(): Flow<List<CustomerEntity>> = db.customerDao().observeAll()
    suspend fun getCustomers(): List<CustomerEntity> = db.customerDao().getAll()
    /**
     * Finds a customer by name, or creates one if none exists. Wrapped in a transaction because
     * this is called from more than one place for the same "add a new customer" user action (the
     * booking form's own contract-lookup, plus the "+ Add new..." dropdown's onAddNew callback) -
     * without the transaction, two concurrent calls for a brand-new name can both see "not found"
     * and both insert, creating two rows with the same name (the duplicate-name bug fixed in v1.5).
     * The transaction serializes concurrent calls on this database so the second one always sees
     * the first one's insert.
     */
    suspend fun getOrCreateCustomer(name: String): CustomerEntity = db.withTransaction {
        val trimmed = name.trim()
        db.customerDao().findByName(trimmed)?.let { return@withTransaction it }
        val id = db.customerDao().insert(CustomerEntity(name = trimmed))
        CustomerEntity(id = id, name = trimmed)
    }
    suspend fun getCustomerById(id: Long): CustomerEntity? = db.customerDao().getById(id)
    fun observeCustomerById(id: Long): Flow<CustomerEntity?> = db.customerDao().observeById(id)
    suspend fun deleteCustomer(customer: CustomerEntity) = db.customerDao().delete(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().update(customer)

    // ---- Phone Time tracking ----
    /**
     * Total minutes of timed bookings for [customerId] under the default ("Phone") service,
     * optionally restricted to bookings on/after [sinceEpochDay]. Used for all three Phone Time
     * views: All Time (sinceEpochDay = null), User Defined (since the counter's last reset), and
     * Contract (since the contract's start date).
     */
    suspend fun getPhoneTimeMinutes(customerId: Long, sinceEpochDay: Long? = null): Long {
        val defaultService = db.serviceDao().getAll().firstOrNull { it.isDefault } ?: return 0L
        val bookings = db.bookingDao().getTimedBookingsForCustomerAndService(customerId, defaultService.id)
        return bookings
            .asSequence()
            .filter { sinceEpochDay == null || it.dateEpochDay >= sinceEpochDay }
            .sumOf { (it.endMinute!! - it.startMinute!!).toLong() }
    }

    /** Sets up (or replaces) a customer's phone-time contract. */
    suspend fun setContract(customer: CustomerEntity, price: Double, hours: Double, startEpochDay: Long) {
        db.customerDao().update(
            customer.copy(
                hasContract = true,
                contractPrice = price,
                contractHours = hours,
                contractStartEpochDay = startEpochDay
            )
        )
    }

    /** Removes a customer's contract, reverting the client detail view to "no contract set up". */
    suspend fun clearContract(customer: CustomerEntity) {
        db.customerDao().update(
            customer.copy(hasContract = false, contractPrice = 0.0, contractHours = 0.0, contractStartEpochDay = null)
        )
    }

    /** Zeroes out the "User Defined" counter as of [resetEpochDay] (defaults to today). */
    suspend fun resetPhoneTimeCounter(customer: CustomerEntity, resetEpochDay: Long) {
        db.customerDao().update(customer.copy(customCounterResetEpochDay = resetEpochDay))
    }

    // ---- Services ----
    fun observeServices(): Flow<List<ServiceEntity>> = db.serviceDao().observeAll()
    suspend fun addService(name: String, requiresTimeSlot: Boolean) =
        db.serviceDao().insert(ServiceEntity(name = name.trim(), requiresTimeSlot = requiresTimeSlot))
    suspend fun updateService(service: ServiceEntity) = db.serviceDao().update(service)
    suspend fun deleteService(service: ServiceEntity) = db.serviceDao().delete(service)

    // ---- Rate types ----
    fun observeRateTypes(): Flow<List<RateTypeEntity>> = db.rateTypeDao().observeAll()
    suspend fun addRateType(name: String) = db.rateTypeDao().insert(RateTypeEntity(name = name.trim()))
    suspend fun updateRateType(rateType: RateTypeEntity) = db.rateTypeDao().update(rateType)
    suspend fun deleteRateType(rateType: RateTypeEntity) = db.rateTypeDao().delete(rateType)

    // ---- Payment methods ----
    fun observePaymentMethods(): Flow<List<PaymentMethodEntity>> = db.paymentMethodDao().observeAll()
    suspend fun addPaymentMethod(name: String) = db.paymentMethodDao().insert(PaymentMethodEntity(name = name.trim()))
    suspend fun updatePaymentMethod(pm: PaymentMethodEntity) = db.paymentMethodDao().update(pm)
    suspend fun deletePaymentMethod(pm: PaymentMethodEntity) = db.paymentMethodDao().delete(pm)

    // ---- Bookings ----
    fun observeAllBookings(): Flow<List<BookingEntity>> = db.bookingDao().observeAll()
    fun observeBookingsForDate(epochDay: Long): Flow<List<BookingEntity>> = db.bookingDao().observeForDate(epochDay)
    fun observeBookingsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<BookingEntity>> =
        db.bookingDao().observeForRange(startEpochDay, endEpochDay)
    fun observeBookedDatesInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<Long>> =
        db.bookingDao().observeBookedDatesInRange(startEpochDay, endEpochDay)
    fun observeBookingsForCustomer(customerId: Long): Flow<List<BookingEntity>> =
        db.bookingDao().observeForCustomer(customerId)
    suspend fun getAllBookings(): List<BookingEntity> = db.bookingDao().getAll()
    suspend fun deleteBooking(booking: BookingEntity) = db.bookingDao().delete(booking)

    /** Inserts or updates a booking after checking for a time-slot overlap on the same date. */
    suspend fun saveBooking(booking: BookingEntity): SaveBookingResult {
        if (!booking.timeNotApplicable && booking.startMinute != null && booking.endMinute != null) {
            val others = db.bookingDao().getOtherTimedBookingsOnDate(booking.dateEpochDay, booking.serviceId, booking.id)
            val overlaps = others.any { existing ->
                val existingStart = existing.startMinute ?: return@any false
                val existingEnd = existing.endMinute ?: return@any false
                booking.startMinute < existingEnd && existingStart < booking.endMinute
            }
            if (overlaps) return SaveBookingResult.TimeConflict
        }
        val id = if (booking.id == 0L) db.bookingDao().insert(booking) else {
            db.bookingDao().update(booking)
            booking.id
        }
        return SaveBookingResult.Success(id)
    }

    // ---- Goods (inventory) ----
    fun observeGoods(): Flow<List<GoodsEntity>> = db.goodsDao().observeAll()
    suspend fun getGoods(): List<GoodsEntity> = db.goodsDao().getAll()
    suspend fun addGoods(name: String, costPrice: Double, salePrice: Double, initialQuantity: Int, lowStockThreshold: Int = 3): Long =
        db.goodsDao().insert(
            GoodsEntity(
                name = name.trim(),
                costPrice = costPrice,
                salePrice = salePrice,
                quantityOnHand = initialQuantity,
                lowStockThreshold = lowStockThreshold
            )
        )
    suspend fun updateGoods(goods: GoodsEntity) = db.goodsDao().update(goods)
    suspend fun deleteGoods(goods: GoodsEntity) = db.goodsDao().delete(goods)

    fun observeGoodsTransactions(): Flow<List<GoodsTransactionEntity>> = db.goodsTransactionDao().observeAll()
    fun observeGoodsTransactionsForGoods(goodsId: Long): Flow<List<GoodsTransactionEntity>> =
        db.goodsTransactionDao().observeForGoods(goodsId)
    fun observeGoodsTransactionsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<GoodsTransactionEntity>> =
        db.goodsTransactionDao().observeForRange(startEpochDay, endEpochDay)

    /** Logs an inflow (restock) and increments quantity on hand. Optionally updates the running cost basis. */
    suspend fun restockGoods(goods: GoodsEntity, quantity: Int, unitCost: Double, dateEpochDay: Long, notes: String = ""): Long =
        db.withTransaction {
            db.goodsDao().update(goods.copy(quantityOnHand = goods.quantityOnHand + quantity, costPrice = unitCost))
            db.goodsTransactionDao().insert(
                GoodsTransactionEntity(
                    goodsId = goods.id,
                    goodsName = goods.name,
                    type = GoodsTransactionType.IN,
                    quantity = quantity,
                    unitCost = unitCost,
                    dateEpochDay = dateEpochDay,
                    notes = notes
                )
            )
        }

    /** Logs an outflow (sale) and decrements quantity on hand, refusing to sell below zero on hand. */
    suspend fun sellGoods(goods: GoodsEntity, quantity: Int, unitSalePrice: Double, dateEpochDay: Long, notes: String = ""): SellGoodsResult =
        db.withTransaction {
            if (quantity > goods.quantityOnHand) return@withTransaction SellGoodsResult.InsufficientStock
            db.goodsDao().update(goods.copy(quantityOnHand = goods.quantityOnHand - quantity))
            val id = db.goodsTransactionDao().insert(
                GoodsTransactionEntity(
                    goodsId = goods.id,
                    goodsName = goods.name,
                    type = GoodsTransactionType.OUT,
                    quantity = quantity,
                    unitSalePrice = unitSalePrice,
                    unitCost = goods.costPrice,
                    dateEpochDay = dateEpochDay,
                    notes = notes
                )
            )
            SellGoodsResult.Success(id)
        }

    /** Total goods sale revenue (OUT transactions) in [startEpochDay]..[endEpochDay], in dollars. */
    suspend fun getGoodsRevenueForRange(startEpochDay: Long, endEpochDay: Long): Double =
        db.goodsTransactionDao().getForRange(startEpochDay, endEpochDay)
            .filter { it.type == GoodsTransactionType.OUT }
            .sumOf { it.quantity * it.unitSalePrice }

    /** Total cost-of-goods-sold in [startEpochDay]..[endEpochDay], in dollars. */
    suspend fun getGoodsCostForRange(startEpochDay: Long, endEpochDay: Long): Double =
        db.goodsTransactionDao().getForRange(startEpochDay, endEpochDay)
            .filter { it.type == GoodsTransactionType.OUT }
            .sumOf { it.quantity * it.unitCost }

    /** All goods sale (OUT) transactions in range, newest first - used for the Revenue entries list. */
    suspend fun getGoodsSalesForRange(startEpochDay: Long, endEpochDay: Long): List<GoodsTransactionEntity> =
        db.goodsTransactionDao().getForRange(startEpochDay, endEpochDay).filter { it.type == GoodsTransactionType.OUT }

    /** Goods currently at or below their own low-stock threshold. */
    suspend fun getLowStockGoods(): List<GoodsEntity> = db.goodsDao().getAll().filter { it.quantityOnHand <= it.lowStockThreshold }

    // ---- Settings ----
    fun observeSettings(): Flow<AppSettingsEntity> =
        db.appSettingsDao().observe().map { it ?: AppSettingsEntity() }
    suspend fun getSettings(): AppSettingsEntity = db.appSettingsDao().get() ?: AppSettingsEntity()
    suspend fun updateBookToDollarRate(rate: Double) =
        db.appSettingsDao().upsert(getSettings().copy(bookToDollarRate = rate))
    suspend fun updateMackerelToBookRate(rate: Double) =
        db.appSettingsDao().upsert(getSettings().copy(mackerelToBookRate = rate))
    suspend fun updateLanguage(languageCode: String) =
        db.appSettingsDao().upsert(getSettings().copy(language = languageCode))
    suspend fun updateThemeMode(mode: ThemeMode) =
        db.appSettingsDao().upsert(getSettings().copy(themeMode = mode))

    fun close() = db.close()
}
