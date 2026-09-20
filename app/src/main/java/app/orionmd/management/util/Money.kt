package app.orionmd.management.util

import app.orionmd.management.data.entity.PaymentUnit
import java.text.NumberFormat
import java.util.Locale

object Money {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun formatDollars(amount: Double): String = currencyFormat.format(amount)

    fun formatBooks(amount: Double): String {
        val trimmed = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)
        return if (amount == 1.0) "$trimmed Book" else "$trimmed Books"
    }

    fun formatMackerels(amount: Double): String {
        val trimmed = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)
        return if (amount == 1.0) "$trimmed Mackerel" else "$trimmed Mackerels"
    }

    /**
     * Converts a payment amount into a dollar figure. Books convert directly via
     * [bookToDollarRate]; Mackerels convert through Books first via [mackerelToBookRate]
     * (e.g. 4 Mackerels = 1 Book = $8 when bookToDollarRate is 8).
     */
    fun toDollars(amount: Double, unit: PaymentUnit, bookToDollarRate: Double, mackerelToBookRate: Double = 4.0): Double =
        when (unit) {
            PaymentUnit.BOOKS -> amount * bookToDollarRate
            PaymentUnit.MACKERELS -> (amount / mackerelToBookRate) * bookToDollarRate
            PaymentUnit.DOLLARS -> amount
        }

    fun formatEntered(amount: Double, unit: PaymentUnit): String = when (unit) {
        PaymentUnit.BOOKS -> formatBooks(amount)
        PaymentUnit.MACKERELS -> formatMackerels(amount)
        PaymentUnit.DOLLARS -> formatDollars(amount)
    }
}
