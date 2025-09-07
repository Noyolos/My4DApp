package com.example.s66

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.s66.ui.theme.S66Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.s66.data.AppDatabase
import com.example.s66.data.ReceiptEntity

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            S66Theme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        FirstPage(
                            onGoInput = { nav.navigate("input") },
                            onGoHistory = { nav.navigate("history") }
                        )
                    }
                    composable("input") {
                        inputScreen(
                            onNext = {
                                // 记录进入输出时的原始输入，供历史保存
                                BetDataStore.lastCommittedInput = BetDataStore.multiLineInput.value
                                nav.navigate("output")
                            },
                            onBack = { nav.popBackStack("home", inclusive = false) }
                        )
                    }
                    composable("output") {
                        outPutScreen(
                            onStart = { nav.navigate("input") }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            onBack = { nav.popBackStack() },
                            onNavigateToOutput = { nav.navigate("output") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FirstPage(onGoInput: () -> Unit, onGoHistory: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("S66", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            Button(onClick = onGoInput, modifier = Modifier.fillMaxWidth()) {
                Text("🧾 开新单（进入输入）")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onGoHistory, modifier = Modifier.fillMaxWidth()) {
                Text("📚 历史 / 查单")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToOutput: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).receiptDao() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ReceiptEntity>>(emptyList()) }

    LaunchedEffect(query) {
        val flow: Flow<List<ReceiptEntity>> =
            if (query.isBlank()) dao.getAll() else dao.searchBySequence(query)
        flow.collectLatest { items = it }
    }

    fun searchAndReuse() {
        scope.launch(Dispatchers.IO) {
            val list = if (query.isBlank()) dao.getAll().first()
            else dao.searchBySequence(query).first()
            val chosen = list.firstOrNull()
            if (chosen != null) {
                launch(Dispatchers.Main) {
                    // 回填历史原始输入 → 解析 → 自增序列号 → 跳输出
                    BetDataStore.multiLineInput.value = chosen.rawInput
                    BetDataStore.allBetGroups.clear()
                    BetDataStore.allBetGroups.addAll(parseAllGroupsFromMultilineInput(chosen.rawInput))
                    BetDataStore.lastCommittedInput = chosen.rawInput
                    CounterManager.increment(context)
                    onNavigateToOutput()
                }
            } else {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史 / 查单") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("输入序列号（例如 121）") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { searchAndReuse() }) { Text("搜索并出单") }
            }

            Spacer(Modifier.height(16.dp))

            Text("匹配结果（最新在上）", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth()) {
                items.forEach { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(row.timestamp))
                            Text(
                                "$timeText   GT=${row.gtTotal}   序列:${row.sequenceId}",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                BetDataStore.multiLineInput.value = row.rawInput
                                BetDataStore.allBetGroups.clear()
                                BetDataStore.allBetGroups.addAll(parseAllGroupsFromMultilineInput(row.rawInput))
                                BetDataStore.lastCommittedInput = row.rawInput
                                CounterManager.increment(context)
                                onNavigateToOutput()
                            }) { Text("出单") }
                        }
                    }
                }
            }
        }
    }
}

data class ParseOutcome(
    val groups: List<BetGroup>,
    val errors: List<String>
)

fun parseAllGroupsWithValidation(input: String): ParseOutcome {
    val lines = input.trim().lines()
    val errors = mutableListOf<String>()
    val allGroups = mutableListOf<BetGroup>()

    var currentPlaceDigits: String? = null
    val currentLines = mutableListOf<String>()

    fun flushGroupForValidationAndBuild() {
        val pd = currentPlaceDigits ?: return
        if (currentLines.isEmpty()) {
            errors += "没写地方 -$pd 没有任何号码行"
            return
        }

        var defaultB = 0
        var defaultS = 0
        var defaultA1 = 0
        var defaultFroze = false

        for (rawLine in currentLines) {
            val cleanline = rawLine.trim()
            val betType = detectType(cleanline, defaultB, defaultS, defaultA1, defaultFroze)
            if (betType == null) {
                val num = cleanline.substringBefore("-").trim()
                errors += "号码 $num 未写金额，请补写金额"
                continue
            }
            if (betType.updateDefault) {
                defaultB = betType.b; defaultS = betType.s; defaultA1 = betType.a1
                defaultFroze = false
            }
            if (betType.freezeDefault) defaultFroze = true

            val b = betType.b; val s = betType.s; val a1 = betType.a1
            if ((b + s + a1) == 0) {
                val num = cleanline.substringBefore("-").trim()
                errors += "号码 $num 请补写金额"
            }
        }
        if (errors.isEmpty()) {
            allGroups += parsePlaceandNumberList(pd, currentLines.toList())
        }
        currentLines.clear()
    }

    for ((idx, raw) in lines.withIndex()) {
        val clean = raw.trim()
        if (clean.isEmpty()) continue

        if (clean.startsWith("-")) {
            val pd = clean.removePrefix("-").trim()
            if (!pd.matches(Regex("^[1-9]+$"))) {
                errors += "第 ${idx + 1} 行组头非法：-$pd（仅允许 1–9）"
            }
            if (currentPlaceDigits != null) flushGroupForValidationAndBuild()
            currentPlaceDigits = pd
        } else {
            if (!clean.matches(Regex("^\\d{4,6}(-\\d+)?(-\\d+)?(-\\d+)?$"))) {
                errors += "第 ${idx + 1} 行号码/金额非法：$clean（号码须为 4–6 位数字；金额用 - 分隔）"
            } else {
                currentLines += clean
            }
        }
    }

    if (currentPlaceDigits != null) flushGroupForValidationAndBuild() else if (allGroups.isEmpty()) {
        errors += "缺少组头（以 - 开头的行）"
    }

    return if (errors.isEmpty()) ParseOutcome(allGroups, emptyList())
    else ParseOutcome(emptyList(), errors)
}

@Composable
fun inputScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val multiLineInput = BetDataStore.multiLineInput
    val context = LocalContext.current
    var errorMessages by remember { mutableStateOf<List<String>>(emptyList()) }

    if (multiLineInput.value.isBlank()) {
        multiLineInput.value = "-"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    BetDataStore.multiLineInput.value = ""
                    BetDataStore.allBetGroups.clear()
                    onBack()
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) { Text("🔙 返回起始页") }

            TextField(
                value = multiLineInput.value,
                onValueChange = { multiLineInput.value = it },
                label = { Text("请输入所有号码\nPlace 行以 - 开头") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                singleLine = false,
                maxLines = 100,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Default
                )
            )

            Button(
                onClick = {
                    val outcome = parseAllGroupsWithValidation(multiLineInput.value)
                    if (outcome.errors.isNotEmpty()) {
                        errorMessages = outcome.errors
                        return@Button
                    }
                    BetDataStore.allBetGroups.clear()
                    BetDataStore.allBetGroups.addAll(outcome.groups)
                    CounterManager.increment(context)
                    onNext()
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) { Text("✔") }
        }

        if (errorMessages.isNotEmpty()) {
            val preview = errorMessages.take(3).joinToString("\n")
            AlertDialog(
                onDismissRequest = { errorMessages = emptyList() },
                confirmButton = { TextButton(onClick = { errorMessages = emptyList() }) { Text("我知道了") } },
                title = { Text("输入有误") },
                text = {
                    val more = if (errorMessages.size > 3) "\n…共 ${errorMessages.size} 条" else ""
                    Text(preview + more)
                }
            )
        }
    }
}

@Composable
fun outPutScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).receiptDao() }
    val scope = rememberCoroutineScope()

    val output = remember { formatAllBetGroupsWithTotal(BetDataStore.allBetGroups, context) }

    var savedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(output) {
        if (!savedOnce) {
            val meta = parseMetaFromOutput(output)
            val entity = ReceiptEntity(
                sequenceId = meta.sequenceId,
                timestamp = meta.timestampMillis,
                gtTotal = meta.gt,
                outputText = output,
                rawInput = BetDataStore.lastCommittedInput
            )
            scope.launch(Dispatchers.IO) { dao.insert(entity) }
            savedOnce = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = output)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    BetDataStore.multiLineInput.value = ""
                    BetDataStore.allBetGroups.clear()
                    onStart()
                }) { Text("🔄 重新输入") }

                Button(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, output)
                        setPackage("com.whatsapp")
                    }
                    context.startActivity(sendIntent)
                }) { Text("📤 WhatsApp") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("🔙 Back") }
        }
    }
}

data class OutputMeta(val sequenceId: String, val timestampMillis: Long, val gt: Int)

fun parseMetaFromOutput(output: String): OutputMeta {

    val lines = output.lines().map { it.trim() }
    var dateStr = ""
    var timeStr = ""
    var seq = "00"
    var gtVal = 0

    if (lines.size >= 2) {
        val second = lines[1]
        val regex = Regex("""(\d{2}\.\d{2}\.\d{2})\s+(\d{4})\((\d{2})\)""")
        val m = regex.find(second)
        if (m != null) {
            dateStr = m.groupValues[1]
            timeStr = m.groupValues[2]
            seq = m.groupValues[3]
        }
    }
    lines.firstOrNull { it.startsWith("GT=") }?.let {
        gtVal = it.removePrefix("GT=").toIntOrNull() ?: 0
    }

    val sdf = SimpleDateFormat("dd.MM.yy HHmm", Locale.getDefault())
    val ts = try { sdf.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis() }
    catch (_: Throwable) { System.currentTimeMillis() }

    return OutputMeta(sequenceId = seq, timestampMillis = ts, gt = gtVal)
}

object BetDataStore {
    val allBetGroups = mutableStateListOf<BetGroup>()
    var multiLineInput = mutableStateOf("")
    var lastCommittedInput: String = ""
}

data class Bet(
    val number: String,
    val b: Int = 0,
    val s: Int = 0,
    val a1: Int = 0
)

data class BetGroup(
    val placeDigits: String,
    val betList: List<Bet>
)

data class BetType(
    val b: Int,
    val s: Int,
    val a1: Int,
    val updateDefault: Boolean,
    val freezeDefault: Boolean
)

fun mapToLetter(placeDigits: String): String {
    val mapping = mapOf(
        '1' to 'M', '2' to 'P', '3' to 'T',
        '4' to 'S', '5' to 'B', '6' to 'K',
        '7' to 'W', '8' to 'H', '9' to 'E'
    )
    return placeDigits.mapNotNull { mapping[it] }.joinToString("")
}

fun detectType(line: String, defaultB: Int, defaultS: Int, defaultA1: Int, defaultFroze: Boolean): BetType? {
    val parts = line.split("-")
    val b = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val s = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val a1 = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val count = listOf(b, s, a1).count { it != null }

    return when {
        count == 0 -> if (defaultFroze) null else BetType(defaultB, defaultS, defaultA1, false, false)
        count == 1 -> BetType(b ?: 0, s ?: 0, a1 ?: 0, true, false)
        count == 3 && b == s && s == a1 -> BetType(b ?: 0, s ?: 0, a1 ?: 0, true, false)
        else -> BetType(b ?: 0, s ?: 0, a1 ?: 0, false, true)
    }
}

fun parsePlaceandNumberList(placeDigits: String, inputLine: List<String>): BetGroup {
    val betList = mutableListOf<Bet>()
    var defaultB = 0
    var defaultS = 0
    var defaultA1 = 0
    var defaultFroze = false

    for (line in inputLine) {
        val cleanline = line.trim()
        val parts = cleanline.split("-")
        val number = parts[0].trim()
        if (number.isEmpty()) continue

        val betType = detectType(cleanline, defaultB, defaultS, defaultA1, defaultFroze) ?: continue

        if (betType.updateDefault) {
            defaultB = betType.b
            defaultS = betType.s
            defaultA1 = betType.a1
            defaultFroze = false
        }
        if (betType.freezeDefault) {
            defaultFroze = true
        }

        betList.add(Bet(number, betType.b, betType.s, betType.a1))
    }
    return BetGroup(placeDigits, betList)
}

fun parseAllGroupsFromMultilineInput(input: String): List<BetGroup> {
    val lines = input.trim().lines()
    val allGroups = mutableListOf<BetGroup>()
    var currentPlaceDigits: String? = null
    val currentLines = mutableListOf<String>()

    for (line in lines) {
        val clean = line.trim()
        if (clean.isEmpty()) continue

        if (clean.startsWith("-")) {
            if (currentPlaceDigits != null && currentLines.isNotEmpty()) {
                allGroups.add(parsePlaceandNumberList(currentPlaceDigits, currentLines))
                currentLines.clear()
            }
            currentPlaceDigits = clean.removePrefix("-").trim()
        } else {
            currentLines.add(clean)
        }
    }

    if (currentPlaceDigits != null && currentLines.isNotEmpty()) {
        allGroups.add(parsePlaceandNumberList(currentPlaceDigits, currentLines))
    }

    return allGroups
}

fun formatAllBetGroupsWithTotal(groups: List<BetGroup>, context: Context): String {
    val result = StringBuilder()
    val fullDate = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
    val shortDate = SimpleDateFormat("d/M", Locale.getDefault()).format(Date())
    val counter = CounterManager.getFormattedCounter(context)

    result.appendLine("(PPT)")
    result.appendLine("$fullDate  $currentTime($counter)")
    result.appendLine("tjp88")
    result.appendLine(shortDate)

    var grandTotal = 0

    for (group in groups) {
        val letters = mapToLetter(group.placeDigits)
        result.appendLine("*$letters")

        for (bet in group.betList) {
            val parts = mutableListOf<String>()
            if (bet.b > 0) parts.add("${bet.b}B")
            if (bet.s > 0) parts.add("${bet.s}S")
            if (bet.a1 > 0) parts.add("${bet.a1}A1")
            result.appendLine("${bet.number}=${parts.joinToString(" ")}")
        }

        val groupTotal = group.betList.sumOf { (it.b + it.s + it.a1) * letters.length }
        grandTotal += groupTotal
    }

    result.appendLine("GT=$grandTotal")
    return result.toString()
}

object CounterManager {
    private const val PREF_NAME = "CounterPrefs"
    private const val KEY_COUNT = "count"

    fun increment(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COUNT, 0)
        prefs.edit().putInt(KEY_COUNT, current + 1).apply()
    }

    fun getFormattedCounter(context: Context): String {
        val count = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_COUNT, 0)
        return String.format("%02d", count)
    }
}
