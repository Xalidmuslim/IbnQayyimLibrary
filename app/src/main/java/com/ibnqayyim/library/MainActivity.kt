package com.ibnqayyim.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Paper = Color(0xFFF4F1EA)
private val CardWhite = Color(0xFFFFFEFC)
private val Ink = Color(0xFF2A302B)
private val Olive = Color(0xFF526658)
private val Muted = Color(0xFF777E74)
private val Line = Color(0xFFE2DDD4)

private val ThemeColors = lightColorScheme(
    primary = Olive,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEE9DF),
    outline = Line,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = ThemeColors) {
                LibraryApp()
            }
        }
    }
}

@Composable
private fun LibraryApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader_state", 0) }
    val scope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var currentId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    val bookmarkPages = remember {
        mutableStateMapOf<String, Int>().apply {
            Books.all.forEach { item ->
                val stored = prefs.getInt("bookmark_${item.id}", -1)
                if (stored >= 0) put(item.id, stored)
            }
        }
    }
    var busyIds by remember { mutableStateOf(emptySet<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val book = Books.all.find { it.id == currentId }

    fun startReader(bookToOpen: Book) {
        currentPage = prefs.getInt("page_${bookToOpen.id}", 0)
        currentId = bookToOpen.id
    }

    fun download(bookToDownload: Book) {
        if (bookToDownload.id in busyIds) return
        busyIds = busyIds + bookToDownload.id
        scope.launch {
            try {
                BookDownloader.download(context, bookToDownload)
                startReader(bookToDownload)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Не удалось скачать файл. Проверьте подключение."
            } finally {
                busyIds = busyIds - bookToDownload.id
            }
        }
    }

    fun openSource(bookToOpen: Book) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookToOpen.sourceUrl)))
    }

    BackHandler(enabled = book != null) { currentId = null }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            if (book == null) {
                NavigationBar(containerColor = CardWhite, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = currentTab == 0, onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Filled.MenuBook, null) }, label = { Text("Книги") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1, onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Filled.BookmarkBorder, null) }, label = { Text("Закладки") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2, onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Filled.Info, null) }, label = { Text("О библиотеке") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                book != null -> {
                    val file = BookDownloader.targetFile(context, book)
                    if (file.exists()) {
                        ReaderScreen(
                            book = book, file = file, page = currentPage,
                            bookmarkedPage = bookmarkPages[book.id] ?: -1,
                            onBack = { currentId = null },
                            onPageChange = { next ->
                                currentPage = next
                                prefs.edit().putInt("page_${book.id}", next).apply()
                            },
                            onBookmark = {
                                val existing = bookmarkPages[book.id] ?: -1
                                val next = if (existing == currentPage) -1 else currentPage
                                prefs.edit().putInt("bookmark_${book.id}", next).apply()
                                if (next >= 0) bookmarkPages[book.id] = next else bookmarkPages.remove(book.id)
                            },
                        )
                    } else {
                        LaunchedEffect(book.id) { currentId = null }
                    }
                }
                currentTab == 0 -> {
                    val owned = Books.all.filter {
                        it.title.contains(catalogQuery, ignoreCase = true) ||
                            it.description.contains(catalogQuery, ignoreCase = true) ||
                            it.category.contains(catalogQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text("БИБЛИОТЕКА", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = Olive, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Ибн Каййим", fontSize = 33.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("аль-Джаузийя", fontSize = 25.sp, color = Ink)
                            Spacer(Modifier.height(12.dp))
                            Text("Произведения на русском языке. Скачайте один раз — читайте без интернета.",
                                color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                            Spacer(Modifier.height(22.dp))
                            OutlinedTextField(
                                value = catalogQuery,
                                onValueChange = { catalogQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Найти книгу") },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CardWhite,
                                    unfocusedContainerColor = CardWhite,
                                    focusedBorderColor = Olive,
                                    unfocusedBorderColor = Line
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("КНИГИ · ${owned.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.7.sp, color = Muted)
                        }
                        items(owned, key = { it.id }) { item ->
                            BookCard(
                                book = item,
                                isDownloaded = BookDownloader.targetFile(context, item).exists(),
                                isBusy = item.id in busyIds,
                                onRead = { startReader(item) },
                                onDownload = { download(item) },
                                onSource = { openSource(item) }
                            )
                        }
                        if (owned.isEmpty()) {
                            item { Text("Нет книг по вашему запросу", color = Muted) }
                        }
                        item {
                            Text("Оригинальные PDF скачиваются с IslamHouse. Переводы и ссылки на источник указаны для каждого издания.",
                                style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
                currentTab == 1 -> {
                    val bookmarked = Books.all.filter { (bookmarkPages[it.id] ?: -1) >= 0 }
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Закладки", fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Отмеченные страницы", color = Muted)
                            Spacer(Modifier.height(16.dp))
                        }
                        if (bookmarked.isEmpty()) item {
                            EmptyPanel("Пока нет закладок", "Откройте книгу и нажмите значок закладки на нужной странице.")
                        }
                        items(bookmarked, key = { it.id }) { item ->
                            val savedPage = bookmarkPages[item.id] ?: 0
                            ElevatedCard(
                                onClick = {
                                    currentPage = savedPage
                                    currentId = item.id
                                }, colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Bookmark, null, tint = Olive)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.SemiBold)
                                        Text("Страница ${savedPage + 1}", color = Muted, fontSize = 12.sp)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Muted)
                                }
                            }
                        }
                    }
                }
                else -> AboutScreen(onSource = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/IslamHouse-API/multilingual-quran-hadith-islamic-content-database-api-hub"))) })
            }
            error?.let { message ->
                AlertDialog(
                    onDismissRequest = { error = null },
                    title = { Text("Не удалось открыть книгу") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = { error = null }) { Text("Понятно") } }
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    isDownloaded: Boolean,
    isBusy: Boolean,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onSource: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                MiniCover(book.id)
                Column(Modifier.weight(1f)) {
                    Text(book.category.uppercase(), color = Olive, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(book.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(7.dp))
                    Text(book.description, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("${book.pages} стр. · PDF", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(11.dp))
            Text(book.credit, fontSize = 11.sp, color = Muted, lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSource) { Text("Источник", fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = !isBusy,
                    onClick = if (isDownloaded) onRead else onDownload,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 17.dp, vertical = 10.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Загрузка")
                    } else {
                        Icon(if (isDownloaded) Icons.Filled.MenuBook else Icons.Filled.Download, null,
                            modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (isDownloaded) "Читать" else "Скачать")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCover(id: String) {
    val cover = when (id) {
        "rain" -> listOf(Color(0xFF637164), Color(0xFF35473D))
        "prayer" -> listOf(Color(0xFFA18D77), Color(0xFF6D594C))
        "complaint" -> listOf(Color(0xFF7F8790), Color(0xFF53616B))
        else -> listOf(Color(0xFFB39B76), Color(0xFF78654D))
    }
    Box(
        modifier = Modifier.width(88.dp).height(128.dp)
            .shadow(9.dp, RoundedCornerShape(topStart = 5.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 5.dp))
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 5.dp))
            .background(Brush.linearGradient(cover))
    ) {
        Box(Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.2f)))
        Column(Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", color = Color(0xFFEEE9DC), fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Text("ابن قيم", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center)
            Text("الجوزية", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(34.dp).height(1.dp).background(Color.White.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun ReaderScreen(
    book: Book,
    file: File,
    page: Int,
    bookmarkedPage: Int,
    onBack: () -> Unit,
    onPageChange: (Int) -> Unit,
    onBookmark: () -> Unit,
) {
    val count by produceState(initialValue = 0, file.path) {
        value = withContext(Dispatchers.IO) { runCatching { PdfPages.count(file) }.getOrDefault(0) }
    }
    val safePage = ReaderLogic.clampPage(page, count)
    var zoom by rememberSaveable(book.id) { mutableFloatStateOf(1f) }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file.path, safePage, count) {
        value = null
        value = if (count > 0) withContext(Dispatchers.IO) {
            runCatching { PdfPages.render(file, safePage) }.getOrNull()
        } else null
    }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(CardWhite).padding(start = 7.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            Column(Modifier.weight(1f)) {
                Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Страница ${safePage + 1} из ${count.coerceAtLeast(1)}", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = onBookmark) {
                Icon(if (bookmarkedPage == safePage) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    "Сохранить закладку", tint = Olive)
            }
        }
        LinearProgressIndicator(
            progress = { if (count == 0) 0f else (safePage + 1).toFloat() / count },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Olive,
            trackColor = Line,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFE8E3D9))) {
            val visualWidth = maxWidth * zoom
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Оригинальная страница PDF ${safePage + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.width(visualWidth).padding(9.dp)
                                .shadow(5.dp, RoundedCornerShape(2.dp))
                        )
                    } else {
                        CircularProgressIndicator(Modifier.padding(32.dp), color = Olive)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(CardWhite).padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onPageChange(ReaderLogic.previousPage(safePage)) }, enabled = safePage > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Предыдущая страница")
            }
            IconButton(onClick = { zoom = (zoom / 1.3f).coerceAtLeast(1f) }, enabled = zoom > 1f) {
                Icon(Icons.Filled.Remove, "Уменьшить")
            }
            Text("${(zoom * 100).toInt()}%", fontSize = 12.sp, color = Muted)
            IconButton(onClick = { zoom = (zoom * 1.3f).coerceAtMost(3.0f) }, enabled = zoom < 3f) {
                Icon(Icons.Filled.Add, "Увеличить")
            }
            IconButton(onClick = { onPageChange(ReaderLogic.nextPage(safePage, count)) }, enabled = count > 0 && safePage < count - 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Следующая страница")
            }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardWhite).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.BookmarkBorder, null, tint = Olive, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AboutScreen(onSource: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(23.dp)) {
        Spacer(Modifier.height(9.dp))
        Text("О библиотеке", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(15.dp))
        Text("Бесплатная читалка произведений Ибн Каййима аль-Джаузийи на русском языке. Без рекламы, регистрации и платных функций.",
            fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(22.dp))
        Text("Источники и права", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Исходные PDF скачиваются напрямую с IslamHouse.com и воспроизводятся без изменения. Условия повторного использования IslamHouse требуют сохранять текст и указывать автора, переводчика и источник.",
            color = Muted, lineHeight = 21.sp)
        Spacer(Modifier.height(13.dp))
        Books.all.forEach { item ->
            Text(item.title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(item.credit, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(11.dp))
        }
        Text("Приложение независимо от IslamHouse и не является его официальным продуктом.",
            color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onSource) { Text("Правила использования IslamHouse") }
        Spacer(Modifier.height(15.dp))
        Text("Версия 0.1 · Android 8+", color = Muted, fontSize = 12.sp)
    }
}
