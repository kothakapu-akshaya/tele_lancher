package com.telelauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.telelauncher.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telelauncher.BuildConfig
import com.telelauncher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class Screen {
    data object Dashboard : Screen()
    data object AppLauncher : Screen()
    data class Details(val type: String, val id: Int) : Screen()
    data object Search : Screen()
    data object Watchlist : Screen()
    data object TeleShield : Screen()
}

data class AppEntry(val label: String, val packageName: String?, val launchIntent: Intent? = null)

private fun Modifier.escapeTo(onBack: () -> Unit, allowBackspace: Boolean = true): Modifier = onPreviewKeyEvent { event ->
    val keyCode = event.nativeKeyEvent.keyCode
    if (event.type == KeyEventType.KeyDown &&
        (event.key == Key.Escape || keyCode == AndroidKeyEvent.KEYCODE_ESCAPE || keyCode == AndroidKeyEvent.KEYCODE_BACK ||
            (allowBackspace && keyCode == AndroidKeyEvent.KEYCODE_DEL))
    ) { onBack(); true } else false
}

@Composable
fun HomeScreen(startInAppLauncher: Boolean = false, launchApp: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var screen by remember(startInAppLauncher) { mutableStateOf<Screen>(if (startInAppLauncher) Screen.AppLauncher else Screen.Dashboard) }
    val watchlist = remember { WatchlistStore(context) }
    val dashboardListState = rememberLazyListState()

    when (val current = screen) {
        Screen.Dashboard -> {
            BackHandler { /* Tele Launcher dashboard is a stable Home screen */ }
            DashboardContent(dashboardListState, watchlist, onDetails = { type, id -> screen = Screen.Details(type, id) }, onSearch = { screen = Screen.Search }, onWatchlist = { screen = Screen.Watchlist }, onTeleShield = { screen = Screen.TeleShield })
        }
        Screen.AppLauncher -> {
            BackHandler { /* Do not leave the launcher accidentally */ }
            AppLauncherScreen(onOpenDashboard = { screen = Screen.Dashboard }, launchApp = launchApp)
        }
        is Screen.Details -> {
            BackHandler { screen = Screen.Dashboard }
            DetailScreen(type = current.type, id = current.id, onBack = { screen = Screen.Dashboard }, watchlist = watchlist)
        }
        Screen.Search -> {
            BackHandler { screen = Screen.Dashboard }
            SearchScreen(onBack = { screen = Screen.Dashboard }, onDetails = { type, id -> screen = Screen.Details(type, id) })
        }
        Screen.Watchlist -> {
            BackHandler { screen = Screen.Dashboard }
            WatchlistScreen(onBack = { screen = Screen.Dashboard }, onDetails = { type, id -> screen = Screen.Details(type, id) }, watchlist = watchlist)
        }
        Screen.TeleShield -> {
            BackHandler { screen = Screen.Dashboard }
            AdBlockScreen(onBack = { screen = Screen.Dashboard })
        }
    }
}

@Composable
fun TvButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "buttonScale")
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale).onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier)
    ) { Text(text) }
}

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "backButtonScale")
    Button(
        onClick = onBack,
        modifier = modifier
        .height(56.dp)
        .scale(scale)
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier),
        colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF1B2230),
        contentColor = Color.White
        )
    ) {
        Text("←", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_back), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun DashboardContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    watchlist: WatchlistStore,
    onDetails: (String, Int) -> Unit,
    onSearch: () -> Unit,
    onWatchlist: () -> Unit,
    onTeleShield: () -> Unit = {}
) {
    var movies by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var tv by remember { mutableStateOf<List<TmdbTv>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var popularTv by remember { mutableStateOf<List<TmdbTv>>(emptyList()) }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = mutableMapOf<String, AppEntry>()
            val liveIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            pm.queryIntentActivities(liveIntent, 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName && !installed.containsKey(pkg)) {
                    installed[pkg] = AppEntry(info.loadLabel(pm).toString(), pkg)
                }
            }
            installed.values.forEach { android.util.Log.d("TeleLauncher", "Discovered app: ${it.label} (${it.packageName})") }
            apps = installed.values.sortedBy { it.label.lowercase() }
        }
    }
    LaunchedEffect(Unit) {
        if (BuildConfig.TMDB_API_KEY.isBlank()) {
            error = "TMDB API key not configured. Add tmdb.api.key to local.properties."
            loading = false
        } else {
            scope.launch {
                val repo = TmdbRepository()
                val trendingMovies = async { runCatching { repo.trendingMovies() } }
                val trendingTv = async { runCatching { repo.trendingTv() } }
                val popularMovie = async { runCatching { repo.popularMovies("IN") } }
                val popularTvShows = async { runCatching { repo.popularTv("IN") } }
                val errs = mutableListOf<String>()
                trendingMovies.await().fold({ movies = it }, { errs.add(it.message ?: it.toString()) })
                trendingTv.await().fold({ tv = it }, { errs.add(it.message ?: it.toString()) })
                popularMovie.await().fold({ popularMovies = it }, { errs.add(it.message ?: it.toString()) })
                popularTvShows.await().fold({ popularTv = it }, { errs.add(it.message ?: it.toString()) })
                if (movies.isEmpty() && tv.isEmpty() && popularMovies.isEmpty() && popularTv.isEmpty()) {
                    error = if (errs.isNotEmpty()) errs.first() else "Entertainment discovery could not be loaded."
                }
                loading = false
            }
        }
    }

    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D14)), 
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_title), color = Color.White, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                TvButton(stringResource(R.string.nav_search), onClick = onSearch, modifier = Modifier.focusRequester(initialFocus))
                Spacer(Modifier.width(12.dp))
                TvButton(stringResource(R.string.nav_watchlist), onClick = onWatchlist)
                Spacer(Modifier.width(12.dp))
                TvButton("TeleShield", onClick = onTeleShield)
            }
            Spacer(Modifier.height(20.dp))
        }
        if (loading) item { CircularProgressIndicator(color = Color.White) }
        error?.let { item { Text(stringResource(R.string.unable_load, it), color = Color(0xFFCF6679)) } }

        val continueBrowsing = watchlist.list().take(10)
        if (continueBrowsing.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.continue_browsing)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(continueBrowsing) { e -> PosterCard(e.title, "https://image.tmdb.org/t/p/w342${e.poster ?: ""}") { onDetails(e.type, e.id) } } } }
        }
        item { SectionTitle("My TV Apps") }
        if (apps.isEmpty()) {
            item { Text("No installed TV apps discovered.", color = Color(0xFFAAAAAA)) }
        } else {
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(apps) { a ->
                var focused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "appScale2")
                Card(onClick = {
                    val intent = a.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
                    if (intent != null) context.startActivity(intent) else android.util.Log.w("TeleLauncher", "No launch intent for ${a.packageName}")
                }, modifier = Modifier.width(120.dp).aspectRatio(0.85f).scale(scale).then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier).onFocusChanged { focused = it.isFocused }) {
                    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (a.packageName != null) {
                            AsyncImage(model = runCatching { context.packageManager.getApplicationIcon(a.packageName) }.getOrNull(), contentDescription = a.label, modifier = Modifier.size(56.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(a.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            } } }
        }
        if (movies.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.trending_movies)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(movies) { m -> PosterCard(m.title, posterUrl(m.poster_path)) { onDetails("movie", m.id) } } } }
        }
        if (tv.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.trending_tv)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(tv) { t -> PosterCard(t.name, posterUrl(t.poster_path)) { onDetails("tv", t.id) } } } }
        }
        if (popularMovies.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.popular_movies)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(popularMovies) { m -> PosterCard(m.title, posterUrl(m.poster_path)) { onDetails("movie", m.id) } } } }
        }
        if (popularTv.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.popular_tv_shows)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(popularTv) { t -> PosterCard(t.name, posterUrl(t.poster_path)) { onDetails("tv", t.id) } } } }
        }
    }
}

private fun posterUrl(path: String?): String = if (path.isNullOrBlank()) "" else "https://image.tmdb.org/t/p/w342$path"

@Composable
fun AppLauncherScreen(onOpenDashboard: () -> Unit, launchApp: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val apps = remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = mutableMapOf<String, AppEntry>()

            installed[context.packageName] = AppEntry("Tele Launcher", context.packageName)
            appIfInstalled(pm, "com.android.vending", "Google Play Store")?.let { installed["com.android.vending"] = it }
            appIfInstalled(pm, "com.google.android.youtube", "YouTube")?.let { installed["com.google.android.youtube"] = it }
            appIfInstalled(pm, "com.google.android.apps.youtube.music", "YouTube Music")?.let { installed["com.google.android.apps.youtube.music"] = it }

            val liveIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            pm.queryIntentActivities(liveIntent, 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName && !installed.containsKey(pkg)) {
                    installed[pkg] = AppEntry(info.loadLabel(pm).toString(), pkg)
                }
            }
            val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo }
                .mapNotNull { info ->
                    val pkg = info.packageName
                    if (pkg == context.packageName || installed.containsKey(pkg)) null
                    else AppEntry(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(), pkg)
                }
                .associateBy { it.packageName ?: "" }
            apps.value = (installed.values.filterNotNull() + launchable.values).distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        }
        runCatching { initialFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090D14)).padding(40.dp).escapeTo(onBack = { /* stay in launcher */ })) {
        Text(stringResource(R.string.label_apps), color = Color.White, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps.value) { app ->
                var focused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "appScale")
                Card(onClick = {
                    if (app.packageName == context.packageName) onOpenDashboard()
                    else app.packageName?.let(launchApp)
                }, modifier = Modifier.aspectRatio(0.85f).scale(scale).then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier).onFocusChanged { focused = it.isFocused }.let { if (app.packageName == context.packageName) it.focusRequester(initialFocus) else it }) {
                    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (app.packageName != null) {
                            AsyncImage(model = context.packageManager.getApplicationIcon(app.packageName), contentDescription = app.label, modifier = Modifier.size(56.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(app.label, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun appIfInstalled(pm: PackageManager, packageName: String, label: String): AppEntry? = try {
    pm.getApplicationInfo(packageName, 0)
    AppEntry(label, packageName)
} catch (_: PackageManager.NameNotFoundException) { null }

@Composable
fun DetailScreen(type: String, id: Int, onBack: () -> Unit, watchlist: WatchlistStore) {
    var detail by remember { mutableStateOf<Any?>(null) }
    var providers by remember { mutableStateOf<TmdbWatchRegionProviders?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(type, id) {
        scope.launch {
            try {
                val repo = TmdbRepository()
                detail = if (type == "movie") repo.movieDetails(id) else repo.tvDetails(id)
                providers = if (type == "movie") repo.movieProviders(id, "IN") else repo.tvProviders(id, "IN")
            } catch (e: Exception) { error = e.message ?: "Unable to load details." }
            finally { loading = false }
        }
    }

    val title = when (val d = detail) { is TmdbMovieDetail -> d.title; is TmdbTvDetail -> d.name; else -> "" }
    val backdrop = when (val d = detail) { is TmdbMovieDetail -> d.backdrop_path; is TmdbTvDetail -> d.backdrop_path; else -> null }
    val poster = when (val d = detail) { is TmdbMovieDetail -> d.poster_path; is TmdbTvDetail -> d.poster_path; else -> null }
    val rating = when (val d = detail) { is TmdbMovieDetail -> d.vote_average; is TmdbTvDetail -> d.vote_average; else -> 0.0 }
    val overview = when (val d = detail) { is TmdbMovieDetail -> d.overview; is TmdbTvDetail -> d.overview; else -> null }
    val date = when (val d = detail) { is TmdbMovieDetail -> d.release_date; is TmdbTvDetail -> d.first_air_date; else -> null }
    val genres = when (val d = detail) { is TmdbMovieDetail -> d.genres; is TmdbTvDetail -> d.genres; else -> emptyList<TmdbGenre>() }
    var saved by remember(type, id) { mutableStateOf(watchlist.contains(type, id)) }
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(Color(0xFF090D14)).padding(40.dp).escapeTo(onBack)) {
        BackButton(onBack = onBack, modifier = Modifier.focusRequester(backFocus))
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
        if (loading) item { Spacer(Modifier.height(24.dp)); CircularProgressIndicator(color = Color.White) }
        error?.let { item { Spacer(Modifier.height(24.dp)); Text(stringResource(R.string.error_prefix, it), color = Color(0xFFCF6679)) } }
        if (!loading && error == null) item {
            Spacer(Modifier.height(16.dp))
            if (!backdrop.isNullOrBlank()) AsyncImage(model = "https://image.tmdb.org/t/p/w1280$backdrop", contentDescription = title, modifier = Modifier.fillMaxWidth().height(240.dp))
            Row(Modifier.padding(top = 16.dp)) {
                if (!poster.isNullOrBlank()) AsyncImage(model = "https://image.tmdb.org/t/p/w342$poster", contentDescription = title, modifier = Modifier.size(160.dp, 240.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.rating_date, rating.toString(), date ?: ""), color = Color.Gray)
                    Text(genres.joinToString { it.name }, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    TvButton(text = if (saved) stringResource(R.string.remove_from_watchlist) else stringResource(R.string.add_to_watchlist), onClick = { if (saved) watchlist.remove(type, id) else watchlist.add(type, id, title, poster); saved = !saved })
                }
            }
            Spacer(Modifier.height(16.dp)); Text(overview ?: "", color = Color.White)
        }

        item {
            Spacer(Modifier.height(24.dp)); SectionTitle(stringResource(R.string.where_watch_india))
            Text(stringResource(R.string.availability_note), color = Color(0xFFBFC7D5))
        }
        val subscription = providers?.flatrate.orEmpty().distinctBy { it.provider_id }
        val rental = providers?.rent.orEmpty().distinctBy { it.provider_id }
        val purchase = providers?.buy.orEmpty().distinctBy { it.provider_id }
        if (subscription.isEmpty() && rental.isEmpty() && purchase.isEmpty()) {
            item { Text(stringResource(R.string.no_provider_availability), color = Color(0xFFAAAAAA)) }
        } else {
            if (subscription.isNotEmpty()) item { ProviderRow(stringResource(R.string.subscription), subscription, context, providers?.link) }
            if (rental.isNotEmpty()) item { ProviderRow(stringResource(R.string.rent), rental, context, providers?.link) }
            if (purchase.isNotEmpty()) item { ProviderRow(stringResource(R.string.purchase), purchase, context, providers?.link) }
        }
        }
    }
}

@Composable
private fun ProviderRow(label: String, providers: List<TmdbWatchProvider>, context: Context, tmdbLink: String?) {
    Text(label, color = Color(0xFFBFC7D5), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(providers) { provider ->
            var focused by remember { mutableStateOf(false) }
            Card(onClick = { launchProvider(context, provider.provider_name, tmdbLink) }, modifier = Modifier.width(180.dp).height(100.dp).then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier).onFocusChanged { focused = it.isFocused }) {
                Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (!provider.logo_path.isNullOrBlank()) AsyncImage(model = "https://image.tmdb.org/t/p/w92${provider.logo_path}", contentDescription = provider.provider_name, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(4.dp)); Text(provider.provider_name, color = Color.White, maxLines = 2, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun launchProvider(context: Context, providerName: String, tmdbLink: String?) {
    val packageName = when {
        providerName.contains("Netflix", true) -> "com.netflix.mediaclient"
        providerName.contains("Prime", true) || providerName.contains("Amazon", true) -> "com.amazon.avod.thirdpartyclient"
        providerName.contains("Disney", true) || providerName.contains("Hotstar", true) -> "in.hotstar.android"
        providerName.contains("Jio", true) -> "com.jio.jiocinema"
        providerName.contains("YouTube", true) -> "com.google.android.youtube"
        providerName.contains("ZEE5", true) || providerName.contains("ZEE", true) -> "com.graymatrix.did"
        providerName.contains("Sony", true) -> "com.sonyliv"
        providerName.contains("Apple TV", true) -> "com.apple.atve.androidtv.appletv"
        else -> null
    }

    val installed = packageName?.let {
        try { context.packageManager.getApplicationInfo(it, 0); it } catch (_: PackageManager.NameNotFoundException) { null }
    }
    if (installed != null) {
        context.packageManager.getLaunchIntentForPackage(installed)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } else if (!packageName.isNullOrBlank()) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } else if (!tmdbLink.isNullOrBlank()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tmdbLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun SectionTitle(text: String) { Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp)) }

@Composable
fun PosterCard(title: String, url: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "posterScale")
    BoxWithConstraints {
        val cardWidth = if (maxWidth >= 1600.dp) 210.dp else if (maxWidth >= 1200.dp) 180.dp else 150.dp
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(cardWidth)
                .aspectRatio(2f / 3f)
                .scale(scale)
                .then(if (focused) Modifier.border(3.dp, Color(0xFF7DB7FF), RoundedCornerShape(12.dp)) else Modifier)
                .onFocusChanged { focused = it.isFocused },
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(Modifier.fillMaxSize().background(Color(0xFF20283A))) {
                AsyncImage(model = url, contentDescription = title, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onDetails: (String, Int) -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var results by remember { mutableStateOf<List<TmdbSearchItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
    var fieldFocused by remember { mutableStateOf(false) }
    val performSearch: () -> Unit = { scope.launch { loading = true; error = null; try { results = TmdbRepository().search(query.text) } catch (e: Exception) { error = e.message } finally { loading = false } } }

    Column(Modifier.fillMaxSize().background(Color(0xFF090D14)).padding(40.dp).escapeTo(onBack, allowBackspace = false)) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.nav_search)) }, modifier = Modifier.focusRequester(fieldFocus).onFocusChanged { fieldFocused = it.isFocused }.then(if (fieldFocused) Modifier.border(3.dp, Color(0xFF7DB7FF)) else Modifier).onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER || event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER || event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER)) { performSearch(); true } else false })
        Spacer(Modifier.height(8.dp)); TvButton(stringResource(R.string.nav_search), onClick = performSearch)
        if (loading) CircularProgressIndicator(color = Color.White)
        error?.let { Text(stringResource(R.string.error_prefix, it), color = Color(0xFFCF6679)) }
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(results.filter { it.media_type == "movie" || it.media_type == "tv" }) { r -> PosterCard(r.title ?: r.name ?: "", posterUrl(r.poster_path)) { onDetails(r.media_type ?: "movie", r.id) } }
        }
    }
}

@Composable
fun WatchlistScreen(onBack: () -> Unit, onDetails: (String, Int) -> Unit, watchlist: WatchlistStore) {
    var entries by remember { mutableStateOf(watchlist.list()) }
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
    Column(Modifier.fillMaxSize().background(Color(0xFF090D14)).padding(40.dp).escapeTo(onBack)) {
        BackButton(onBack = onBack, modifier = Modifier.focusRequester(backFocus)); Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.watchlist_title), color = Color.White, style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(24.dp))
        if (entries.isEmpty()) Text(stringResource(R.string.empty_state), color = Color(0xFFAAAAAA))
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(entries) { e -> Column(horizontalAlignment = Alignment.CenterHorizontally) { PosterCard(e.title, posterUrl(e.poster)) { onDetails(e.type, e.id) }; TvButton(stringResource(R.string.remove_action), onClick = { watchlist.remove(e.type, e.id); entries = watchlist.list() }) } }
        }
    }
}

class WatchlistStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("watchlist", Context.MODE_PRIVATE)
    private fun key(type: String, id: Int) = "$type:$id"
    fun contains(type: String, id: Int) = prefs.contains(key(type, id))
    fun add(type: String, id: Int, title: String, poster: String?) { prefs.edit().putString(key(type, id), "$title|${poster ?: ""}").apply() }
    fun remove(type: String, id: Int) { prefs.edit().remove(key(type, id)).apply() }
    data class Entry(val type: String, val id: Int, val title: String, val poster: String?)
    fun list(): List<Entry> = prefs.all.mapNotNull { (k, v) ->
        val parts = k.split(":")
        if (parts.size != 2) null else {
            val values = (v as? String)?.split("|", limit = 2).orEmpty()
            val id = parts[1].toIntOrNull()
            if (id == null) null else Entry(parts[0], id, values.getOrElse(0) { "" }, values.getOrElse(1) { "" }.ifEmpty { null })
        }
    }
}
