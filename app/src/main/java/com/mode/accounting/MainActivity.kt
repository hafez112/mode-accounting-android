package com.mode.accounting

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF167D68)
private val Red = Color(0xFFB54747)
private val Ink = Color(0xFF202A27)
private val Muted = Color(0xFF697572)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AccountingDatabase(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Color(0xFFF5F7F6), surface = Color.White, onSurface = Ink)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AccountingScreen(database)
                }
            }
        }
    }
}

private data class Entry(val id: Long, val type: String, val cents: Long, val category: String, val note: String, val createdAt: Long)

private class AccountingDatabase(context: Context) : SQLiteOpenHelper(context, "mode_accounting.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL CHECK(type IN ('income','expense')), amount_cents INTEGER NOT NULL CHECK(amount_cents > 0), category TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun readAll(): List<Entry> {
        val result = mutableListOf<Entry>()
        readableDatabase.query("transactions", null, null, null, null, null, "created_at DESC, id DESC").use { c ->
            while (c.moveToNext()) result += Entry(c.getLong(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("type")), c.getLong(c.getColumnIndexOrThrow("amount_cents")), c.getString(c.getColumnIndexOrThrow("category")), c.getString(c.getColumnIndexOrThrow("note")), c.getLong(c.getColumnIndexOrThrow("created_at")))
        }
        return result
    }

    fun add(type: String, cents: Long, category: String, note: String) {
        val values = ContentValues().apply {
            put("type", type); put("amount_cents", cents); put("category", category); put("note", note); put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow("transactions", null, values)
    }

    fun remove(id: Long) { writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString())) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountingScreen(db: AccountingDatabase) {
    val entries = remember { mutableStateListOf<Entry>().apply { addAll(db.readAll()) } }
    var showDialog by remember { mutableStateOf(false) }
    val income = entries.filter { it.type == "income" }.sumOf { it.cents }
    val expense = entries.filter { it.type == "expense" }.sumOf { it.cents }

    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("Mode المحاسبي", fontWeight = FontWeight.Bold); Text("إدارة الإيرادات والمصروفات", color = Muted, style = MaterialTheme.typography.labelMedium) } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }, containerColor = Green, contentColor = Color.White) { Text("+", style = MaterialTheme.typography.headlineMedium) } },
        containerColor = Color(0xFFF5F7F6)
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("الرصيد الحالي", color = Color.White.copy(alpha = .8f))
                        Text(money(income - expense), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Summary("الإيرادات", income, Modifier.weight(1f))
                            Summary("المصروفات", expense, Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Text("العمليات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (entries.isEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("لا توجد عمليات بعد", fontWeight = FontWeight.Bold)
                        Text("اضغط + لإضافة إيراد أو مصروف", color = Muted)
                    }
                }
            } else items(entries, key = { it.id }) { item ->
                EntryCard(item) {
                    db.remove(item.id)
                    entries.removeAll { it.id == item.id }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showDialog) AddDialog(
        onDismiss = { showDialog = false },
        onSave = { type, cents, category, note ->
            db.add(type, cents, category, note)
            entries.clear(); entries.addAll(db.readAll()); showDialog = false
        }
    )
}

@Composable
private fun Summary(title: String, cents: Long, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .16f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelMedium)
            Text(money(cents), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EntryCard(entry: Entry, onDelete: () -> Unit) {
    val isIncome = entry.type == "income"
    val tint = if (isIncome) Green else Red
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.size(40.dp), color = if (isIncome) Color(0xFFE7F4EF) else Color(0xFFFFEEEE), shape = CircleShape) {
                Text(if (isIncome) "+" else "−", modifier = Modifier.padding(top = 5.dp), color = tint, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.category, fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) Text(entry.note, color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date(entry.createdAt)), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (isIncome) "+" else "−"}${money(entry.cents)}", color = tint, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete) { Text("حذف", color = Red) }
            }
        }
    }
}

@Composable
private fun AddDialog(onDismiss: () -> Unit, onSave: (String, Long, String, String) -> Unit) {
    var type by remember { mutableStateOf("income") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عملية", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { type = "income" }, modifier = Modifier.weight(1f)) { Text(if (type == "income") "✓ إيراد" else "إيراد") }
                    Button(onClick = { type = "expense" }, modifier = Modifier.weight(1f)) { Text(if (type == "expense") "✓ مصروف" else "مصروف") }
                }
                OutlinedTextField(amount, { amount = it; error = "" }, label = { Text("المبلغ بالريال") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(category, { category = it; error = "" }, label = { Text("الفئة / الوصف") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("ملاحظات اختيارية") }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = Red, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.trim().replace(',', '.').toBigDecimalOrNull()
                if (category.isBlank() || value == null || value <= java.math.BigDecimal.ZERO) error = "أدخل فئة ومبلغاً صحيحاً أكبر من صفر"
                else {
                    val cents = value.multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    if (cents <= 0) error = "المبلغ صغير جداً" else onSave(type, cents, category.trim(), note.trim())
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun money(cents: Long): String {
    val value = NumberFormat.getNumberInstance(Locale("ar", "SA")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(cents / 100.0)
    return "$value ر.س"
}
