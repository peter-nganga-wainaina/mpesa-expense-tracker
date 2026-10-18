package com.example.expensetracker.scenarios

import com.example.expensetracker.categorize.Categorizer
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionWithCategory
import com.example.expensetracker.data.toEntity
import com.example.expensetracker.data.toStoredTransaction
import com.example.expensetracker.insights.MonthSummary
import com.example.expensetracker.insights.summarize
import com.example.expensetracker.manual.FormCheck
import com.example.expensetracker.manual.ManualEntry
import com.example.expensetracker.manual.ManualEntryForm
import com.example.expensetracker.manual.ManualEntryStore
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.manual.check
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.ReceivedSms
import com.example.expensetracker.sms.TransactionIngest
import com.example.expensetracker.sms.TransactionNotifier
import com.example.expensetracker.sms.TransactionStore
import com.example.expensetracker.unparsed.UnparsedMessage
import com.example.expensetracker.unparsed.entryCodeFor
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * End-to-end money scenarios for the seams between components: real-wording SMS go through the
 * parser, ingest, categorizing, manual entries and the monthly summary, and the totals must come out
 * as intended. Behavior inside a single component belongs in that component's own tests.
 */
class MoneyScenarioTest {

    private val october = YearMonth.of(2026, 10)
    private val phone = Phone("Food", "Transport", "Other")

    @Test
    fun `cash added by hand counts alongside M-Pesa spending`() {
        phone.receive(paid(code = "TQ1A2B3C4D", ksh = "500.00", to = "SUNRISE CAFE"))
        phone.file("TQ1A2B3C4D", "Food")
        phone.addCash("450", "Mama Mboga", "Food", at = LocalDateTime.of(2026, 10, 18, 9, 0))

        assertEquals(mapOf("Food" to 950_00L), phone.totals(october))
    }

    @Test
    fun `a deleted manual entry no longer counts`() {
        val code = phone.addCash("450", "Mama Mboga", "Food", at = LocalDateTime.of(2026, 10, 18, 9, 0))
        phone.deleteManual(code)

        assertEquals(emptyMap<String, Long>(), phone.totals(october))
    }

    @Test
    fun `an unreadable message typed in twice counts once, at its latest amount`() {
        val unreadable = "UA1B2C3D4E Confirmed. Your M-PESA Global transfer request has been received and is being processed."
        phone.receive(unreadable)
        val message = phone.unreadable.single()

        phone.addFromMessage(message, "450", "Other")
        phone.addFromMessage(message, "480", "Other")

        assertEquals(mapOf("Other" to 480_00L), phone.totals(october))
    }

    @Test
    fun `spending lands in the month it happened, either side of midnight`() {
        phone.receive(paid(code = "TQ1A2B3C4D", ksh = "500.00", to = "SUNRISE CAFE", on = "31/10/26 at 11:59 PM"))
        phone.receive(paid(code = "TQ2B3C4D5E", ksh = "300.00", to = "SUNRISE CAFE", on = "1/11/26 at 12:01 AM"))
        phone.file("TQ1A2B3C4D", "Food")
        phone.file("TQ2B3C4D5E", "Food")

        assertEquals(mapOf("Food" to 500_00L), phone.totals(october))
        assertEquals(mapOf("Food" to 300_00L), phone.totals(YearMonth.of(2026, 11)))
    }

    @Test
    fun `transaction fees are not part of the totals`() {
        phone.receive(
            "TQ3C4D5E6F Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 18/10/26 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
        )
        phone.file("TQ3C4D5E6F", "Other")

        assertEquals(mapOf("Other" to 1_100_00L), phone.totals(october))
    }

    @Test
    fun `a Fuliza drawdown with no date in its text counts in the month it arrived`() {
        // Wording from mpesa_samples/fuliza.txt: the drawdown message carries no date at all.
        phone.receive(
            "TQ4D5E6F7G Confirmed. Fuliza M-PESA amount is Ksh 948.44. Access Fee charged Ksh 9.49. " +
                "Total Fuliza M-PESA outstanding amount is Ksh 957.93 due on 29/11/26."
        )
        phone.file("TQ4D5E6F7G", "Other")

        assertEquals(mapOf("Other" to 948_44L), phone.totals(october))
    }

    // Wording follows the anonymized samples in mpesa_samples/paid.txt.
    private fun paid(code: String, ksh: String, to: String, on: String = "18/10/26 at 12:30 PM") =
        "$code Confirmed. Ksh$ksh paid to $to. on $on.New M-PESA balance is Ksh9,000.00. Transaction cost, Ksh0.00."
}

/**
 * The app's logic wired as on a phone, over an in-memory table that stores the same rows as Room and
 * follows the same rules: inserting an existing code is skipped, saving a manual entry replaces it.
 */
private class Phone(vararg categoryNames: String) : TransactionStore, ManualEntryStore {
    private val categories = categoryNames.mapIndexed { i, name -> Category(i + 1L, name) }
    private val rows = linkedMapOf<String, TransactionEntity>()
    private val unparsed = linkedMapOf<String, UnparsedMessage>()
    private val arrivedAt = LocalDateTime.of(2026, 10, 18, 12, 0)

    private val notifier = object : TransactionNotifier {
        override fun promptForCategory(transaction: MpesaTransaction, quickPicks: List<Category>) = Unit
        override fun dismiss(transactionCode: String) = Unit
    }
    private val ingest = TransactionIngest(this, suggester = { _, _ -> emptyList() }, notifier = notifier)
    private val categorizer = Categorizer(this, notifier)

    val unreadable: List<UnparsedMessage> get() = unparsed.values.toList()

    fun receive(body: String) = runBlocking { ingest.ingest(body, arrivedAt) }

    fun sweep(vararg bodies: String) = runBlocking { ingest.backfill(bodies.map { ReceivedSms(it, arrivedAt) }) }

    fun file(code: String, categoryName: String) = runBlocking { categorizer.categorize(code, category(categoryName).id) }

    /** Adds cash spending through the add form's own validation; returns the new entry's code. */
    fun addCash(amount: String, note: String, categoryName: String, at: LocalDateTime): String {
        val form = ManualEntryForm(amount, note, PaymentSource.CASH, at, category(categoryName).id)
        val entry = (form.check() as FormCheck.Ready).entry
        runBlocking { save(entry) }
        return entry.code
    }

    /** Types in an unreadable message the way its "Add as spending" / "Edit spending" button does. */
    fun addFromMessage(message: UnparsedMessage, amount: String, categoryName: String) {
        val form = ManualEntryForm(amount, source = PaymentSource.MPESA, occurredAt = message.receivedAt, categoryId = category(categoryName).id)
        runBlocking { save((form.check(existingCode = entryCodeFor(message)) as FormCheck.Ready).entry) }
    }

    fun deleteManual(code: String) = runBlocking { delete(code) }

    fun summary(month: YearMonth): MonthSummary = summarize(month, ledger())

    /** Category name to total cents, as Insights lists them. */
    fun totals(month: YearMonth): Map<String, Long> =
        summary(month).categories.associate { it.category.name to it.totalCents }

    private fun ledger(): List<StoredTransaction> = rows.values.map { row ->
        TransactionWithCategory(row, categories.firstOrNull { it.id == row.categoryId }?.name).toStoredTransaction()
    }

    private fun category(name: String) = categories.single { it.name == name }

    // TransactionStore, with Room's rules.
    override suspend fun insertIfNew(transaction: MpesaTransaction, sms: ReceivedSms): Boolean =
        rows.putIfAbsent(transaction.code, transaction.toEntity(sms)) == null

    override suspend fun saveUnparsed(transactionCode: String, sms: ReceivedSms) {
        unparsed.putIfAbsent(transactionCode, UnparsedMessage(transactionCode, sms.body, sms.receivedAt))
    }

    override suspend fun setCategory(transactionCode: String, categoryId: Long) {
        rows.computeIfPresent(transactionCode) { _, row -> row.copy(categoryId = categoryId) }
    }

    // ManualEntryStore, with Room's rules.
    override suspend fun save(entry: ManualEntry) {
        rows[entry.code] = entry.toEntity()
    }

    override suspend fun delete(code: String) {
        check(rows[code]?.type == "MANUAL") { "Only manual entries can be deleted" }
        rows.remove(code)
    }
}
