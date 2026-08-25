package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility terpusat untuk memformat angka uang (Rupiah) dan angka teknis non-uang
 * di seluruh aplikasi SEJAHTERA BERSAMA dan dokumen PDF laporan.
 */
object FormatHelper {

    private val idLocale = Locale("id", "ID")

    private val idSymbols = DecimalFormatSymbols(idLocale).apply {
        currencySymbol = "Rp "
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val rupiahFormatter = DecimalFormat("Rp #,##0", idSymbols)
    private val rupiahWithDecimalsFormatter = DecimalFormat("Rp #,##0.00", idSymbols)

    private val numberFormatter = DecimalFormat("#,##0.##", idSymbols)
    private val integerFormatter = DecimalFormat("#,##0", idSymbols)
    private val oneDecimalFormatter = DecimalFormat("#,##0.0", idSymbols)
    private val twoDecimalFormatter = DecimalFormat("#,##0.00", idSymbols)

    /**
     * Format angka uang ke standar Rupiah Indonesia (contoh: Rp 500.000, Rp 1.500.000, Rp 0)
     */
    fun formatRupiah(amount: Double?): String {
        if (amount == null) return "Rp 0"
        return rupiahFormatter.format(amount)
    }

    fun formatRupiah(amount: Long?): String {
        if (amount == null) return "Rp 0"
        return rupiahFormatter.format(amount)
    }

    fun formatRupiah(amount: Int?): String {
        if (amount == null) return "Rp 0"
        return rupiahFormatter.format(amount.toLong())
    }

    /**
     * Format Rupiah jika ada desimal yang signifikan
     */
    fun formatRupiahDecimals(amount: Double?): String {
        if (amount == null) return "Rp 0"
        return if (amount % 1.0 == 0.0) {
            rupiahFormatter.format(amount)
        } else {
            rupiahWithDecimalsFormatter.format(amount)
        }
    }

    /**
     * Format angka umum non-uang dengan pemisah ribuan titik (contoh: 10.500, 1.250, 42)
     */
    fun formatNumber(number: Number?): String {
        if (number == null) return "0"
        return numberFormatter.format(number)
    }

    fun formatInteger(number: Number?): String {
        if (number == null) return "0"
        return integerFormatter.format(number.toLong())
    }

    fun formatOneDecimal(number: Double?): String {
        if (number == null) return "0,0"
        return oneDecimalFormatter.format(number)
    }

    fun formatTwoDecimals(number: Double?): String {
        if (number == null) return "0,00"
        return twoDecimalFormatter.format(number)
    }

    /**
     * Format spesifik domain peternakan (non-uang)
     */
    fun formatEkor(count: Int?): String = "${formatInteger(count ?: 0)} Ekor"

    fun formatKg(kg: Double?): String = "${formatNumber(kg ?: 0.0)} Kg"

    fun formatGram(gram: Double?): String = "${formatInteger((gram ?: 0.0).toInt())} Gram"

    fun formatLiter(liter: Double?): String = "${formatNumber(liter ?: 0.0)} Liter"

    fun formatSak(sak: Int?): String = "${formatInteger(sak ?: 0)} Sak"

    fun formatHari(days: Int?): String = "${days ?: 0} Hari"

    fun formatPersen(pct: Double?): String = "${formatTwoDecimals(pct ?: 0.0)} %"

    fun formatFcr(fcr: Double?): String = formatTwoDecimals(fcr ?: 0.0)

    fun formatIp(ip: Double?): String = formatOneDecimal(ip ?: 0.0)

    fun formatCelsius(temp: Double?): String = "${formatOneDecimal(temp ?: 0.0)} °C"

    fun formatMeter(meters: Double?): String = "${formatOneDecimal(meters ?: 0.0)} m"
}
