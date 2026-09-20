package app.orionmd.management.ui.schedule

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.RentalsRepository
import app.orionmd.management.data.SaveBookingResult
import app.orionmd.management.data.entity.BookingEntity
import app.orionmd.management.data.entity.PaymentStatus
import app.orionmd.management.ui.booking.BookingFormDialog
import app.orionmd.management.util.DateTimeUtils
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.Money
import app.orionmd.management.util.PdfExporter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class ScheduleMode { DAILY, WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val repository = remember { AppSession.repository!! }
    val scope = rememberCoroutineScope()

    // Weekly is the default landing view; Monthly gives the same "tap a day" calendar the old
    // Booking tab had, and Daily is kept for a single-day agenda.
    var mode by remember { mutableStateOf(ScheduleMode.WEEKLY) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var editingBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var pdfMessage by remember { mutableStateOf<String?>(null) }

    var selectedMonthDay by remember { mutableStateOf<LocalDate?>(null) }
    var showNewBookingDatePicker by remember { mutableStateOf(false) }
    var newBookingDate by remember { mutableStateOf<LocalDate?>(null) }

    val customers by repository.observeCustomers().collectAsState(initial = emptyList())
    val services by repository.observeServices().collectAsState(initial = emptyList())
    val rateTypes by repository.observeRateTypes().collectAsState(initial = emptyList())
    val paymentMethods by repository.observePaymentMethods().collectAsState(initial = emptyList())

    val rangeStart = when (mode) {
        ScheduleMode.DAILY -> anchorDate
        ScheduleMode.WEEKLY -> DateTimeUtils.weekStart(anchorDate)
        ScheduleMode.MONTHLY -> DateTimeUtils.monthGridStart(anchorDate)
    }
    val rangeEnd = when (mode) {
        ScheduleMode.DAILY -> anchorDate
        ScheduleMode.WEEKLY -> DateTimeUtils.weekEnd(anchorDate)
        ScheduleMode.MONTHLY -> DateTimeUtils.monthGridEnd(anchorDate)
    }

    val bookings by repository
        .observeBookingsForRange(rangeStart.toEpochDay(), rangeEnd.toEpochDay())
        .collectAsState(initial = emptyList())

    val periodLabel = when (mode) {
        ScheduleMode.DAILY -> DateTimeUtils.fullDate(anchorDate, strings.locale)
        ScheduleMode.WEEKLY -> DateTimeUtils.weekRangeLabel(DateTimeUtils.weekStart(anchorDate), strings.locale)
        ScheduleMode.MONTHLY -> DateTimeUtils.monthYear(anchorDate, strings.locale)
    }

    fun createBookingAt(date: LocalDate) {
        newBookingDate = date
        editingBooking = null
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val entries = bookings.sortedWith(compareBy({ it.dateEpochDay }, { it.startMinute ?: -1 })).map { booking ->
                PdfExporter.Entry(
                    listOf(
                        "${DateTimeUtils.mdy(LocalDate.ofEpochDay(booking.dateEpochDay), strings.locale)} — ${booking.customerName}",
                        DateTimeUtils.timeRangeLabel(
                            booking.startMinute, booking.endMinute, booking.timeNotApplicable,
                            notApplicableLabel = strings.notApplicable, dash = strings.noneDash, locale = strings.locale
                        ),
                        "${booking.serviceName} • ${booking.rateType} • ${booking.paymentMethod} — ${Money.formatEntered(booking.paymentAmount, booking.paymentUnit)} (${strings.statusLabel(booking.paymentStatus)})"
                    )
                )
            }
            val result = PdfExporter.export(
                context = context,
                uri = uri,
                title = strings.scheduleScreenTitle,
                subtitle = periodLabel,
                entries = entries,
                emptyMessage = strings.noBookingsScheduled
            )
            pdfMessage = if (result.isSuccess) strings.pdfSavedMessage else "${strings.pdfFailedPrefix}${result.exceptionOrNull()?.message}"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.scheduleScreenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newBookingDate = null
                showNewBookingDatePicker = true
            }) { Icon(Icons.Filled.Add, contentDescription = strings.addBookingDesc) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == ScheduleMode.DAILY,
                    onClick = { mode = ScheduleMode.DAILY },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text(strings.dailyLabel) }
                SegmentedButton(
                    selected = mode == ScheduleMode.WEEKLY,
                    onClick = { mode = ScheduleMode.WEEKLY },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text(strings.weekLabel) }
                SegmentedButton(
                    selected = mode == ScheduleMode.MONTHLY,
                    onClick = { mode = ScheduleMode.MONTHLY },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text(strings.monthLabel) }
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    anchorDate = when (mode) {
                        ScheduleMode.DAILY -> anchorDate.minusDays(1)
                        ScheduleMode.WEEKLY -> anchorDate.minusWeeks(1)
                        ScheduleMode.MONTHLY -> anchorDate.minusMonths(1)
                    }
                }) { Icon(Icons.Filled.ChevronLeft, contentDescription = strings.previous) }

                Text(periodLabel, style = MaterialTheme.typography.titleMedium)

                IconButton(onClick = {
                    anchorDate = when (mode) {
                        ScheduleMode.DAILY -> anchorDate.plusDays(1)
                        ScheduleMode.WEEKLY -> anchorDate.plusWeeks(1)
                        ScheduleMode.MONTHLY -> anchorDate.plusMonths(1)
                    }
                }) { Icon(Icons.Filled.ChevronRight, contentDescription = strings.next) }
            }
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                when (mode) {
                    ScheduleMode.DAILY -> DailyList(bookings = bookings, onEdit = { editingBooking = it })
                    ScheduleMode.WEEKLY -> WeeklyList(
                        weekStart = DateTimeUtils.weekStart(anchorDate),
                        bookings = bookings,
                        onEdit = { editingBooking = it }
                    )
                    ScheduleMode.MONTHLY -> {
                        val bookedDates = remember(bookings) { bookings.map { it.dateEpochDay }.toSet() }
                        Column {
                            WeekdayHeaderRow()
                            Spacer(Modifier.height(4.dp))
                            MonthGrid(
                                monthAnchor = anchorDate,
                                gridStart = rangeStart,
                                gridEnd = rangeEnd,
                                bookedDates = bookedDates,
                                onDayClick = { selectedMonthDay = it }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { pdfLauncher.launch("schedule_${anchorDate}.pdf") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(strings.saveAsPdf)
            }
            pdfMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    selectedMonthDay?.let { date ->
        DayBookingsDialog(
            date = date,
            repository = repository,
            onDismiss = { selectedMonthDay = null },
            onAddNew = { createBookingAt(date) },
            onEditBooking = { booking -> editingBooking = booking }
        )
    }

    if (showNewBookingDatePicker) {
        val initial = newBookingDate ?: anchorDate
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showNewBookingDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    newBookingDate = state.selectedDateMillis?.let { millisToLocalDate(it) } ?: initial
                    showNewBookingDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showNewBookingDatePicker = false }) { Text(strings.cancel) } }
        ) { DatePicker(state = state) }
    }

    (editingBooking to newBookingDate).let { (booking, newDate) ->
        val dateForForm = booking?.dateEpochDay ?: newDate?.toEpochDay()
        if (dateForForm != null) {
            BookingFormDialog(
                dateEpochDay = dateForForm,
                existing = booking,
                customerNames = customers.map { it.name },
                services = services,
                rateTypeNames = rateTypes.map { it.name },
                paymentMethodNames = paymentMethods.map { it.name },
                repository = repository,
                onDismiss = { editingBooking = null; newBookingDate = null },
                onAddCustomer = { name -> scope.launch { repository.getOrCreateCustomer(name) } },
                onAddService = { name -> scope.launch { repository.addService(name, requiresTimeSlot = true) } },
                onAddRateType = { name -> scope.launch { repository.addRateType(name) } },
                onAddPaymentMethod = { name -> scope.launch { repository.addPaymentMethod(name) } },
                onDelete = booking?.let {
                    {
                        scope.launch {
                            repository.deleteBooking(it)
                            editingBooking = null
                        }
                    }
                },
                onSave = { draft ->
                    scope.launch {
                        val customer = repository.getOrCreateCustomer(draft.customerName)
                        val result = repository.saveBooking(draft.copy(customerId = customer.id, customerName = customer.name))
                        if (result is SaveBookingResult.Success) {
                            editingBooking = null
                            newBookingDate = null
                        }
                    }
                }
            )
        }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun DailyList(bookings: List<BookingEntity>, onEdit: (BookingEntity) -> Unit) {
    if (bookings.isEmpty()) {
        EmptyState()
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bookings.sortedBy { it.startMinute ?: -1 }) { booking ->
            BookingDetailCard(booking = booking, onClick = { onEdit(booking) })
        }
    }
}

@Composable
private fun WeeklyList(weekStart: LocalDate, bookings: List<BookingEntity>, onEdit: (BookingEntity) -> Unit) {
    val strings = LocalStrings.current
    val byDate = bookings.groupBy { it.dateEpochDay }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items((0..6).map { weekStart.plusDays(it.toLong()) }) { date ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    "${DateTimeUtils.dayOfWeekLabel(date, strings.locale)} · ${DateTimeUtils.shortDate(date, strings.locale)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                val dayBookings = byDate[date.toEpochDay()].orEmpty().sortedBy { it.startMinute ?: -1 }
                if (dayBookings.isEmpty()) {
                    Text(
                        strings.noBookingsThisDay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayBookings.forEach { booking ->
                            BookingDetailCard(booking = booking, onClick = { onEdit(booking) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val strings = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth()) {
        strings.weekdayShortLabels.forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    monthAnchor: LocalDate,
    gridStart: LocalDate,
    gridEnd: LocalDate,
    bookedDates: Set<Long>,
    onDayClick: (LocalDate) -> Unit
) {
    val totalDays = (gridEnd.toEpochDay() - gridStart.toEpochDay() + 1).toInt()
    val weeks = totalDays / 7
    val today = LocalDate.now()

    Column {
        repeat(weeks) { weekIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIndex ->
                    val date = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                    val inMonth = date.month == monthAnchor.month
                    val hasBookings = bookedDates.contains(date.toEpochDay())
                    val isToday = date == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = if (inMonth) 0.55f else 0.15f)
                                }
                            )
                            .clickable { onDayClick(date) },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                date.dayOfMonth.toString(),
                                color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (hasBookings) {
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBookingsDialog(
    date: LocalDate,
    repository: RentalsRepository,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEditBooking: (BookingEntity) -> Unit
) {
    val strings = LocalStrings.current
    val bookings by repository.observeBookingsForDate(date.toEpochDay()).collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(DateTimeUtils.fullDate(date, strings.locale), style = MaterialTheme.typography.titleMedium)
                    FloatingActionButton(
                        onClick = onAddNew,
                        modifier = Modifier.size(40.dp)
                    ) { Icon(Icons.Filled.Add, contentDescription = strings.addBookingDesc) }
                }
                Spacer(Modifier.height(8.dp))
                if (bookings.isEmpty()) {
                    Text(
                        strings.noBookingsYet,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height((bookings.size.coerceAtMost(6) * 64).dp)) {
                        items(bookings) { booking ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditBooking(booking) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    DateTimeUtils.timeRangeLabel(
                                        booking.startMinute, booking.endMinute, booking.timeNotApplicable,
                                        notApplicableLabel = strings.notApplicable, dash = strings.noneDash, locale = strings.locale
                                    ),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text("${booking.customerName} • ${booking.serviceName}", style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailCard(booking: BookingEntity, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(booking.customerName, style = MaterialTheme.typography.titleMedium)
                StatusBadge(booking.paymentStatus)
            }
            Text(
                DateTimeUtils.timeRangeLabel(
                    booking.startMinute, booking.endMinute, booking.timeNotApplicable,
                    notApplicableLabel = strings.notApplicable, dash = strings.noneDash, locale = strings.locale
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text("${booking.serviceName} • ${booking.rateType}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${booking.paymentMethod} — ${Money.formatEntered(booking.paymentAmount, booking.paymentUnit)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (booking.notes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(booking.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PaymentStatus) {
    val strings = LocalStrings.current
    val color = when (status) {
        PaymentStatus.PAID -> MaterialTheme.colorScheme.secondary
        PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
    }
    Text(strings.statusLabel(status), color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun EmptyState() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            strings.noBookingsScheduled,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
