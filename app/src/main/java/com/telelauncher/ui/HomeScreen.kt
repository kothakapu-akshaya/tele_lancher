package com.telelauncher.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telelauncher.data.TmdbMovie
import com.telelauncher.data.TmdbMovieDetail
import com.telelauncher.data.TmdbRepository
import com.telelauncher.data.TmdbSearchItem
import com.telelauncher.data.TmdbTv
import com.telelauncher.data.TmdbTvDetail
import kotlinx.coroutines.launch

data class AppEntry(val label: String, val packageName: String)

private sealed class Screen { object Dashboard : Screen(); data class Details(val type: String, val id: Int) : Screen(); object Search : Screen(); object Watchlist : Screen() }

@Composable
fun HomeScreen(launchApp: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val watchlist = remember { WatchlistStore(context) }
    val dashboardListState = rememberLazyListState()

    when (screen) {
        is Screen.Dashboard -> {
            BackHandler { /* stay */ }
            DashboardContent(context, launchApp, dashboardListState, onDetails = { t, id -> screen = Screen.Details(t, id) }, onSearch = { screen = Screen.Search }, onWatchlist = { screen = Screen.Watchlist })
        }
        is Screen.Details -> {
            val d = screen as Screen.Details
            BackHandler { screen = Screen.Dashboard }
            DetailScreen(type = d.type, id = d.id, onBack = { screen = Screen.Dashboard }, watchlist = watchlist)
        }
        Screen.Search -> {
            BackHandler { screen = Screen.Dashboard }
            SearchScreen(onBack = { screen = Screen.Dashboard }, onDetails = { t, id -> screen = Screen.Details(t, id) })
        }
        Screen.Watchlist -> {
            BackHandler { screen = Screen.Dashboard }
            WatchlistScreen(onBack = { screen = Screen.Dashboard }, onDetails = { t, id -> screen = Screen.Details(t, id) }, watchlist = watchlist)
        }
    }
}

@Composable
fun DashboardContent(context: android.content.Context, launchApp: (String) -> Unit, listState: androidx.compose.foundation.lazy.LazyListState, onDetails: (String, Int) -> Unit, onSearch: () -> Unit, onWatchlist: () -> Unit) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var movies by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var tv by remember { mutableStateOf<List<TmdbTv>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { info -> pm.getLaunchIntentForPackage(info.packageName)?.let { AppEntry(pm.getApplicationLabel(info).toString(), info.packageName) } }
                .sortedBy { it.label.lowercase() }
        }
        scope.launch {
            try { val r = TmdbRepository(); movies = r.trendingMovies(); tv = r.trendingTv() } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(40.dp)) {
        item { Text("Tele Launcher", color = Color.White, style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(24.dp)) }
        item { Row { Button(onClick = onSearch) { Text("Search") }; Spacer(Modifier.width(12.dp)); Button(onClick = onWatchlist) { Text("Watchlist") } }; Spacer(Modifier.height(24.dp)) }
        if (loading) item { CircularProgressIndicator(color = Color.White) }
        error?.let { item { Text("Error: $it", color = Color.Red) } }
        if (movies.isNotEmpty()) {
            item { SectionTitle("Trending Movies") }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(movies) { m -> PosterCard(m.title, "https://image.tmdb.org/t/p/w342${m.poster_path}") { onDetails("movie", m.id) } } } }
        }
        if (tv.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)); SectionTitle("Trending TV") }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(tv) { t -> PosterCard(t.name, "https://image.tmdb.org/t/p/w342${t.poster_path}") { onDetails("tv", t.id) } } } }
        }
        item { Spacer(Modifier.height(24.dp)); SectionTitle("Apps") }
        item {
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.height(400.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                gridItems(apps) { app ->
                    var focused by remember { mutableStateOf(false) }
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.08f else 1f, label = "s")
                    Card(onClick = { launchApp(app.packageName) }, colors = CardDefaults.cardColors(containerColor = if (focused) Color(0xFF3A3A5C) else Color(0xFF1E1E2E)), modifier = Modifier.aspectRatio(0.85f).scale(scale).onFocusChanged { focused = it.isFocused }) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            AsyncImage(model = context.packageManager.getApplicationIcon(app.packageName), contentDescription = app.label, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(app.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(type: String, id: Int, onBack: () -> Unit, watchlist: WatchlistStore) {
    var detail by remember { mutableStateOf<Any?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(type, id) {
        scope.launch {
            try { detail = if (type == "movie") TmdbRepository().movieDetails(id) else TmdbRepository().tvDetails(id) } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    val title = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).title; is TmdbTvDetail -> (detail as TmdbTvDetail).name; else -> "" }
    val backdrop = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).backdrop_path; is TmdbTvDetail -> (detail as TmdbTvDetail).backdrop_path; else -> null }
    val poster = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).poster_path; is TmdbTvDetail -> (detail as TmdbTvDetail).poster_path; else -> null }
    val rating = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).vote_average; is TmdbTvDetail -> (detail as TmdbTvDetail).vote_average; else -> 0.0 }
    val overview = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).overview; is TmdbTvDetail -> (detail as TmdbTvDetail).overview; else -> null }
    val date = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).release_date; is TmdbTvDetail -> (detail as TmdbTvDetail).first_air_date; else -> null }
    val genres = when (detail) { is TmdbMovieDetail -> (detail as TmdbMovieDetail).genres; is TmdbTvDetail -> (detail as TmdbTvDetail).genres; else -> emptyList() }
    val saved = watchlist.contains(type, id)

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Button(onClick = onBack) { Text("Back") }
        if (loading) { CircularProgressIndicator(color = Color.White); return@Column }
        error?.let { Text("Error: $it", color = Color.Red); return@Column }
        AsyncImage(model = "https://image.tmdb.org/t/p/w1280$backdrop", contentDescription = title, modifier = Modifier.fillMaxWidth().height(240.dp))
        Row(Modifier.padding(top = 16.dp)) {
            AsyncImage(model = "https://image.tmdb.org/t/p/w342$poster", contentDescription = title, modifier = Modifier.size(160.dp, 240.dp))
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Rating: $rating  •  $date", color = Color.Gray)
                Text(genres.joinToString { it.name }, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (saved) watchlist.remove(type, id) else watchlist.add(type, id, title, poster) }) { Text(if (saved) "Remove from Watchlist" else "Add to Watchlist") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(overview ?: "", color = Color.White)
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onDetails: (String, Int) -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var results by remember { mutableStateOf<List<TmdbSearchItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                loading = true; error = null
                try { results = TmdbRepository().search(query.text) } catch (e: Exception) { error = e.message } finally { loading = false }
            }
        }) { Text("Search") }
        if (loading) CircularProgressIndicator(color = Color.White)
        error?.let { Text("Error: $it", color = Color.Red) }
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            gridItems(results.filter { it.media_type == "movie" || it.media_type == "tv" }) { r ->
                PosterCard(r.title ?: r.name ?: "", "https://image.tmdb.org/t/p/w342${r.poster_path}") { onDetails(r.media_type ?: "movie", r.id) }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
fun PosterCard(title: String, url: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.08f else 1f, label = "s")
    Card(onClick = onClick, modifier = Modifier.width(140.dp).aspectRatio(2f/3f).scale(scale).onFocusChanged { focused = it.isFocused }) {
        AsyncImage(model = url, contentDescription = title, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun WatchlistScreen(onBack: () -> Unit, onDetails: (String, Int) -> Unit, watchlist: WatchlistStore) {
    val entries = watchlist.list()
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Watchlist", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        if (entries.isEmpty()) Text("Empty", color = Color.Gray)
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            gridItems(entries) { e ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PosterCard(e.title, "https://image.tmdb.org/t/p/w342${e.poster}") { onDetails(e.type, e.id) }
                    Button(onClick = { watchlist.remove(e.type, e.id) }) { Text("Remove") }
                }
            }
        }
    }
}

class WatchlistStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("watchlist", Context.MODE_PRIVATE)
    fun key(type: String, id: Int) = "$type:$id"
    fun contains(type: String, id: Int) = prefs.contains(key(type, id))
    fun add(type: String, id: Int, title: String, poster: String?) { prefs.edit().putString(key(type, id), "$title|${poster ?: ""}").apply() }
    fun remove(type: String, id: Int) { prefs.edit().remove(key(type, id)).apply() }
    data class Entry(val type: String, val id: Int, val title: String, val poster: String?)
    fun list(): List<Entry> = prefs.all.mapNotNull { (k, v) ->
        val parts = k.split(":"); if (parts.size != 2) null else {
            val vparts = (v as String).split("|", limit = 2)
            Entry(parts[0], parts[1].toIntOrNull() ?: return@mapNotNull null, vparts.getOrElse(0) { "" }, vparts.getOrElse(1) { "" }.ifEmpty { null })
        }
    }
}