package com.pyb110.onekey

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import rikka.shizuku.Shizuku
import roro.stellar.Stellar
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurColors
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.isRenderEffectSupported
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.ArrowRight
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.icon.extended.Notes
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.preference.WindowDropdownPreference
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ThemeController
import io.github.suqi8.coui.kmp.utils.PressFeedbackType

private const val PROJECT_URL = "https://github.com/Yunnijian/Shark"

private data class LicenseText(
    val name: String,
    val url: String?,
    val content: String? = null,
    val asset: String? = null,
)

private data class LicenseItem(
    val name: String,
    val version: String? = null,
    val author: String? = null,
    val website: String? = null,
    val licenses: List<LicenseText> = emptyList(),
)

private data class LocalLicense(
    val title: String,
    val summary: String,
    val licenseName: String,
    val asset: String,
    val url: String?,
)

private val localLicenses = listOf(
    LocalLicense(
        "Shark（本项目）",
        "项目自研代码",
        "GPL-3.0",
        "licenses/GPL-3.0.txt",
        "https://www.gnu.org/licenses/gpl-3.0.html"
    ),
    LocalLicense(
        "NOTICE",
        "第三方来源、署名与声明",
        "NOTICE",
        "licenses/NOTICE.txt",
        null
    ),
    LocalLicense(
        "SUSFS guard / Hybrid Mount",
        "随 App 分发的内核模块",
        "GPL-2.0",
        "licenses/GPL-2.0.txt",
        "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
    ),
    LocalLicense(
        "Stellar 修改部分",
        "MPL-2.0 覆盖的 Stellar 代码",
        "MPL-2.0",
        "licenses/MPL-2.0.txt",
        "https://www.mozilla.org/MPL/2.0/"
    ),
    LocalLicense(
        "AzirX1337 CVE-2026-98260",
        "MIT 许可的参考实现",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/AzirX1337/CVE-2026-98260-OnePlus16-PYB110-Jailbreak"
    ),
    LocalLicense(
        "COUI / Shizuku / androidx",
        "Apache-2.0 许可的组件",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://www.apache.org/licenses/LICENSE-2.0"
    ),
)

private fun Library.toLicenseItem(): LicenseItem = LicenseItem(
    name = name,
    version = artifactVersion,
    author = developers.joinToString(", ") { it.name.orEmpty() }
        .ifBlank { organization?.name.orEmpty() },
    website = website,
    licenses = licenses.map { LicenseText(it.name, it.url, it.licenseContent) },
)

private enum class AboutPage { HOME, LICENSES }

class MainActivity : ComponentActivity() {
    private val logs = mutableStateListOf<String>()
    private val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    private var busy by mutableStateOf(false)
    private var authorized by mutableStateOf(false)
    private var binderReady by mutableStateOf(false)
    private var ksuActive by mutableStateOf(false)
    private var exploit by mutableStateOf(ExploitKind.CVE98260)
    private var manager by mutableStateOf(ManagerKind.KERNELSU)
    private var kernelVersion by mutableStateOf("未知")
    private var deviceModel by mutableStateOf("未知")
    private var selinuxText by mutableStateOf("未知")
    private var antiRootGuard by mutableStateOf(true)
    private var softRebootAfter by mutableStateOf(false)
    private var unloadGuard by mutableStateOf(true)
    private var susfsSupport by mutableStateOf(false)
    private var vfsSupport by mutableStateOf(false)
    private var dynamicSwitching by mutableStateOf(false)
    private var universalLoaded by mutableStateOf(false)
    private var hotManager by mutableStateOf(ManagerKind.KERNELSU)
    private var hotSwitchBusy by mutableStateOf(false)
    private var hotSwitchError by mutableStateOf<String?>(null)
    private var tab by mutableStateOf(AppTab.HOME)
    private var showManual by mutableStateOf(false)
    private var showExtras by mutableStateOf(false)
    private var historyEntries by mutableStateOf(listOf<HistoryEntry>())
    private var historyDetail by mutableStateOf<HistoryEntry?>(null)
    private var historyDetailText by mutableStateOf("")
    private var pendingUpdate by mutableStateOf<UpdateInfo?>(null)
    private var updateDialogVisible by mutableStateOf(false)
    private var updateDownloading by mutableStateOf(false)
    private var updateProgress by mutableStateOf(0f)
    private var updateError by mutableStateOf<String?>(null)
    private var pendingApk by mutableStateOf<File?>(null)
    private var updateCheckedThisForeground = false
    private var droidspacesDialogVisible by mutableStateOf(false)
    private var droidspacesLoading by mutableStateOf(false)
    private var droidspacesRelease by mutableStateOf<DroidspacesRelease?>(null)
    private var droidspacesDownloading by mutableStateOf(false)
    private var droidspacesProgress by mutableStateOf(0f)
    private var droidspacesError by mutableStateOf<String?>(null)
    private var droidspacesSavedPath by mutableStateOf<String?>(null)

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU) onPermissionResult(result == PackageManager.PERMISSION_GRANTED)
    }
    private val stellarPermissionListener = Stellar.OnRequestPermissionResultListener { code, allowed, _ ->
        if (code == REQ_SHIZUKU) onPermissionResult(allowed)
    }
    private val binderListener = Shizuku.OnBinderReceivedListener {
        binderReady = PrivService.pick() != null
        refreshAuth()
        refreshKsuAsync()
    }
    private val stellarBinderListener = Stellar.OnBinderReceivedListener {
        binderReady = PrivService.pick() != null
        refreshAuth()
        refreshKsuAsync()
    }

    private fun onPermissionResult(ok: Boolean) {
        authorized = ok
        addLog(if (ok) "${PrivService.pick()?.label ?: "Backend"} authorized" else "permission denied")
        if (ok) refreshKsuAsync()
    }

    override fun onStart() {
        super.onStart()
        if (updateCheckedThisForeground) return
        updateCheckedThisForeground = true
        if (updateDialogVisible || updateDownloading) return
        lifecycleScope.launch {
            when (val result = runCatching {
                UpdateChecker.check(applicationContext)
            }.getOrNull()) {
                is UpdateCheckResult.Available -> {
                    if (!updateDialogVisible) {
                        pendingUpdate = result.info
                        pendingApk = null
                        updateError = null
                        updateDialogVisible = true
                    }
                }
                else -> Unit
            }
        }
    }

    private fun checkForUpdatesManually() {
        if (updateDownloading) return
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            when (val result = runCatching {
                UpdateChecker.check(applicationContext)
            }.getOrNull()) {
                is UpdateCheckResult.Available -> {
                    pendingUpdate = result.info
                    pendingApk = null
                    updateError = null
                    updateDialogVisible = true
                }
                UpdateCheckResult.UpToDate -> {
                    Toast.makeText(this@MainActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this@MainActivity, "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openDroidspacesDialog() {
        if (droidspacesDownloading) return
        droidspacesDialogVisible = true
        droidspacesRelease = null
        droidspacesSavedPath = null
        droidspacesError = null
        loadDroidspacesRelease()
    }

    private fun loadDroidspacesRelease() {
        droidspacesLoading = true
        droidspacesError = null
        lifecycleScope.launch {
            runCatching { DroidspacesModule.latest() }
                .onSuccess { droidspacesRelease = it }
                .onFailure {
                    droidspacesError = it.message ?: "获取最新 release 失败"
                }
            droidspacesLoading = false
        }
    }

    private fun downloadDroidspacesModule() {
        val release = droidspacesRelease ?: return
        if (droidspacesDownloading) return
        droidspacesDownloading = true
        droidspacesProgress = 0f
        droidspacesError = null
        lifecycleScope.launch {
            runCatching {
                DroidspacesModule.download(applicationContext, release) { progress ->
                    droidspacesProgress = progress
                }
            }.onSuccess {
                droidspacesSavedPath = it
            }.onFailure {
                droidspacesError = it.message ?: "下载失败"
            }
            droidspacesDownloading = false
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes <= 0 -> "未知大小"
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / 1024.0 / 1024.0)
        else -> "%.0f KB".format(bytes / 1024.0)
    }

    override fun onStop() {
        updateCheckedThisForeground = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Stellar.addRequestPermissionResultListener(stellarPermissionListener)
        Stellar.addBinderReceivedListenerSticky(stellarBinderListener)
        binderReady = PrivService.pick() != null
        refreshAuth()
        antiRootGuard = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("antiroot_guard", true)
        softRebootAfter = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("soft_reboot_after", false)
        unloadGuard = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("unload_guard", true)
        susfsSupport = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("susfs_support", false)
        vfsSupport = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("vfs_support", false)
        val savedManager = getSharedPreferences("pyb110", MODE_PRIVATE).getString("manager", null)
        manager = runCatching { ManagerKind.valueOf(savedManager ?: "") }.getOrDefault(ManagerKind.KERNELSU)
        kernelVersion = runCatching { android.system.Os.uname().release }.getOrDefault("未知")
        exploit = when {
            kernelVersion == V2505A_KERNEL_RELEASE &&
                Build.MANUFACTURER.equals("vivo", ignoreCase = true) -> ExploitKind.CVE2505A
            kernelVersion.contains("6.12.69") -> ExploitKind.CVE61269
            else -> ExploitKind.CVE98260
        }
        if (manager == ManagerKind.DYNAMIC && !supportsUniversal()) {
            manager = ManagerKind.KERNELSU
        }
        dynamicSwitching = manager == ManagerKind.DYNAMIC
        hotManager = if (dynamicSwitching) ManagerKind.KERNELSU else manager
        deviceModel = resolveDeviceName()
        refreshStaticStateAsync()
        refreshKsuAsync()

        setContent {
            val themeController = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }
            COUITheme(themeController) {
                /* Root scaffold: hosts OverlayDialog/popups (each tab's own
                 * Scaffold passes an empty popupHost, so the root is the only
                 * host for dialogs opened from the tab content). */
                val appBackdrop = rememberAppBackdrop()
                Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (slideInHorizontally(tween(240)) { it / 4 * dir } + fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(240)) { -it / 4 * dir } + fadeOut(tween(240)))
                        },
                        label = "tab",
                        modifier = Modifier.fillMaxSize()
                    ) { t ->
                if (t == AppTab.LOGS) {
                    LogScreen(state = logs.toList(), backdrop = appBackdrop)
                } else if (t == AppTab.HISTORY) {
                    HistoryScreen(backdrop = appBackdrop)
                } else if (t == AppTab.ABOUT) {
                    AboutScreen(backdrop = appBackdrop)
                } else {
                    val scrollBehavior = COUIScrollBehavior()
                    Scaffold(
                        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
                        topBar = {
                            BlurredBar(backdrop = appBackdrop) {
                                TopAppBar(
                                    title = "Shark",
                                    scrollBehavior = scrollBehavior,
                                    color = Color.Transparent
                                )
                            }
                        }
                    ) { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (appBackdrop != null) Modifier.layerBackdrop(appBackdrop) else Modifier
                                )
                        ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(
                                top = inner.calculateTopPadding() + 12.dp,
                                bottom = 12.dp + 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                StatusCard(granted = binderReady && authorized)
                            }
                            item {
                                TextButton(
                                    text = when {
                                        busy -> "运行中…"
                                        !binderReady -> "Stellar/Shizuku 未运行（点击打开）"
                                        !authorized -> "授权 Shizuku"
                                        ksuActive -> "校验 / 再次越狱"
                                        else -> "一键越狱"
                                    },
                                    onClick = {
                                        if (busy) return@TextButton
                                        when {
                                            !binderReady -> {
                                                addLog("Start Stellar or Shizuku first")
                                                PrivService.openManager(applicationContext)
                                            }
                                            !authorized -> PrivService.request(REQ_SHIZUKU)
                                            else -> runFlow()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColorsPrimary()
                                )
                            }
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        WindowDropdownPreference(
                                            title = "ROOT管理器",
                                            items = managerChoices().map { it.fullLabel },
                                            selectedIndex = managerChoices().indexOf(manager).coerceAtLeast(0),
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = COUITheme.colorScheme.onBackground
                                                )
                                            },
                                            onSelectedIndexChange = {
                                                selectManager(managerChoices()[it])
                                            }
                                        )
                                        HorizontalDivider()
                                        ArrowPreference(
                                            title = "拓展功能",
                                            onClick = { showExtras = true },
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Tune,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = COUITheme.colorScheme.onBackground
                                                )
                                            }
                                        )
                                        HorizontalDivider()
                                        ArrowPreference(
                                            title = "使用手册",
                                            summary = "首次使用、功能说明与异常处理",
                                            onClick = { showManual = true },
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.MenuBook,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = COUITheme.colorScheme.onBackground
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            item {
                                InfoCard(
                                    kernelVersion = kernelVersion,
                                    exploitLabel = exploit.fullLabel,
                                    deviceModel = deviceModel,
                                    fingerprint = Build.FINGERPRINT,
                                    selinux = selinuxText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        }
                    }
                }
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        BlurredBar(backdrop = appBackdrop) {
                            BottomBar(selected = tab, onSelect = { tab = it })
                        }
                    }
                    AnimatedVisibility(
                        visible = showManual,
                        enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
                        exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ManualScreen(onBack = { showManual = false })
                        }
                    }
                    AnimatedVisibility(
                        visible = showExtras,
                        enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
                        exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ExtrasScreen(onBack = { showExtras = false })
                        }
                    }
                    UpdateDialog()
                    DroidspacesDialog()
                }
                }
            }
        }
    }

    @Composable
    private fun DroidspacesDialog() {
        val release = droidspacesRelease
        val savedPath = droidspacesSavedPath
        OverlayDialog(
            show = droidspacesDialogVisible,
            title = "Droidspaces 模块",
            onDismissRequest = {
                if (!droidspacesDownloading) droidspacesDialogVisible = false
            },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    if (droidspacesLoading) {
                        Text(text = "正在获取最新 release…", fontSize = 13.sp)
                    } else if (release == null) {
                        Text(
                            text = droidspacesError ?: "获取最新 release 失败",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = "重试",
                                onClick = { loadDroidspacesRelease() }
                            )
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = "关闭",
                                onClick = { droidspacesDialogVisible = false },
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    } else {
                        Text(
                            text = "最新版本 ${release.version}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${release.assetName} · ${formatSize(release.assetSize)}",
                            fontSize = 12.sp,
                            color = COUITheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(Modifier.height(12.dp))
                        if (savedPath == null) {
                            SelectionContainer(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = release.changelog.ifBlank { "无更新说明" },
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = "已保存到：",
                                fontSize = 12.sp,
                                color = COUITheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = savedPath,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "请到 KernelSU 管理器 → 模块 → 安装，选择该文件刷入。",
                                fontSize = 13.sp
                            )
                        }
                        droidspacesError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        if (droidspacesDownloading) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "正在下载 ${(droidspacesProgress * 100).toInt()}%",
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        COUITheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(droidspacesProgress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(COUITheme.colorScheme.primary)
                                )
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (savedPath != null) {
                                    TextButton(
                                        modifier = Modifier.weight(1f),
                                        text = "打开 KernelSU",
                                        onClick = {
                                            droidspacesDialogVisible = false
                                            launchManager()
                                        }
                                    )
                                    TextButton(
                                        modifier = Modifier.weight(1f),
                                        text = "关闭",
                                        onClick = { droidspacesDialogVisible = false },
                                        colors = ButtonDefaults.textButtonColorsPrimary()
                                    )
                                } else {
                                    TextButton(
                                        modifier = Modifier.weight(1f),
                                        text = "关闭",
                                        onClick = { droidspacesDialogVisible = false }
                                    )
                                    TextButton(
                                        modifier = Modifier.weight(1f),
                                        text = "下载",
                                        onClick = { downloadDroidspacesModule() },
                                        colors = ButtonDefaults.textButtonColorsPrimary()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun UpdateDialog() {
        val info = pendingUpdate ?: return
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        OverlayDialog(
            show = updateDialogVisible,
            title = "发现新版本 ${info.version}",
            onDismissRequest = { if (!updateDownloading) updateDialogVisible = false },
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = "当前 ${BuildConfig.VERSION_NAME}  ·  来源 ${info.source}",
                        fontSize = 12.sp,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (updateDownloading) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "正在下载 ${(updateProgress * 100).toInt()}%",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(COUITheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(updateProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(COUITheme.colorScheme.primary)
                            )
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.changelog.ifBlank { "无更新说明" },
                                fontSize = 13.sp
                            )
                        }
                        updateError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = "稍后",
                                onClick = { updateDialogVisible = false }
                            )
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = if (pendingApk != null) "安装" else "立即更新",
                                onClick = {
                                    val ready = pendingApk
                                    if (ready != null) {
                                        if (installApk(ready)) updateDialogVisible = false
                                        return@TextButton
                                    }
                                    updateDownloading = true
                                    updateError = null
                                    updateProgress = 0f
                                    scope.launch {
                                        val result = runCatching {
                                            UpdateChecker.download(context, info) { p ->
                                                updateProgress = p
                                            }
                                        }
                                        updateDownloading = false
                                        result.onSuccess { file ->
                                            if (!UpdateChecker.verifySignature(context, file)) {
                                                file.delete()
                                                updateError = "安装包签名与当前应用不一致"
                                            } else {
                                                pendingApk = file
                                                if (installApk(file)) updateDialogVisible = false
                                            }
                                        }.onFailure {
                                            updateError = it.message ?: "下载失败"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                        if (updateError != null) {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = "打开浏览器下载",
                                onClick = { uriHandler.openUri(info.pageUrl) }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun installApk(file: File): Boolean {
        if (!packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "请允许安装未知应用后再次点击安装", Toast.LENGTH_LONG).show()
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            return false
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        }.getOrElse {
            updateError = "无法启动安装器：${it.message}"
            false
        }
    }

    @Composable
    private fun rememberAppBackdrop(): LayerBackdrop? {
        if (!isRenderEffectSupported()) return null
        val surfaceColor = COUITheme.colorScheme.surface
        return rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }

    @Composable
    private fun BlurredBar(backdrop: LayerBackdrop?, content: @Composable () -> Unit) {
        Box(
            modifier = if (backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f,
                    colors = BlurColors(
                        blendColors = listOf(
                            BlendColorEntry(color = COUITheme.colorScheme.surface.copy(0.87f))
                        )
                    )
                )
            } else {
                Modifier
            }
        ) {
            content()
        }
    }

    @Composable
    private fun ExtrasScreen(onBack: () -> Unit) {
        BackHandler(onBack = onBack)
        val backdrop = rememberAppBackdrop()
        LaunchedEffect(exploit) {
            if (!supportsUniversal()) {
                universalLoaded = false
                return@LaunchedEffect
            }
            val (rc, text) = withContext(Dispatchers.IO) {
                runRootCommand("cat /proc/kernelsu_manager", 20)
            }
            universalLoaded = rc == 0 && text.isNotEmpty()
            if (!universalLoaded) {
                addLog("Universal KO check failed: rc=$rc ${text.take(80)}")
            }
        }
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(
                        title = "拓展功能",
                        color = Color.Transparent,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = COUIIcons.Back,
                                    contentDescription = "返回",
                                    tint = COUITheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = inner.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                if (exploit != ExploitKind.CVE2505A) {
                                    SwitchPreference(
                                        title = "越狱后禁用安全弹窗",
                                        checked = antiRootGuard,
                                        onCheckedChange = { updateAntiRootGuard(it) },
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Filled.NotificationsOff,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = COUITheme.colorScheme.onBackground
                                            )
                                        }
                                    )
                                    HorizontalDivider()
                                    SwitchPreference(
                                        title = "越狱后软重启",
                                        checked = softRebootAfter,
                                        onCheckedChange = { updateSoftRebootAfter(it) },
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Filled.RestartAlt,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = COUITheme.colorScheme.onBackground
                                            )
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                if (exploit != ExploitKind.CVE2505A) {
                                    SwitchPreference(
                                        title = "越狱后卸载内核安全防护",
                                        checked = unloadGuard,
                                        onCheckedChange = { updateUnloadGuard(it) },
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Filled.RemoveModerator,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = COUITheme.colorScheme.onBackground
                                            )
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                SwitchPreference(
                                    title = "SUSFS 支持",
                                    checked = susfsSupport,
                                    onCheckedChange = { updateSusfsSupport(it) },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Extension,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                if (exploit == ExploitKind.CVE98260) {
                                    HorizontalDivider()
                                    SwitchPreference(
                                        title = "VFS 支持",
                                        checked = vfsSupport,
                                        onCheckedChange = { updateVfsSupport(it) },
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Filled.AccountTree,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = COUITheme.colorScheme.onBackground
                                            )
                                        }
                                    )

                                }
                                if (supportsUniversal() && universalLoaded) {
                                        HorizontalDivider()
                                        WindowDropdownPreference(
                                            title = "管理器热切换",
                                            items = hotManagerChoices().map { it.fullLabel },
                                            selectedIndex = hotManagerChoices().indexOf(hotManager).coerceAtLeast(0),
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.SwapHoriz,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = COUITheme.colorScheme.onBackground
                                                )
                                            },
                                            onSelectedIndexChange = {
                                                val target = hotManagerChoices()[it]
                                                hotManager = target
                                                switchHotManager(target)
                                            }
                                        )
                                        hotSwitchError?.let {
                                            Text(
                                                text = it,
                                                fontSize = 12.sp,
                                                color = Color(0xFFD32F2F)
                                            )
                                        }
                                    
                                }
                            }
                        }
                    }
                    if (exploit == ExploitKind.CVE98260) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Droidspaces 支持",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "下载最新的 droidspaces_lkm 模块，保存到 Download/Shark，之后到 KernelSU 管理器刷入。",
                                        fontSize = 13.sp,
                                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    TextButton(
                                        text = "下载模块",
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { openDroidspacesDialog() },
                                        colors = ButtonDefaults.textButtonColorsPrimary()
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
    private fun AboutScreen(backdrop: LayerBackdrop?) {
        var page by remember { mutableStateOf(AboutPage.HOME) }
        var selected by remember { mutableStateOf<LicenseItem?>(null) }
        BackHandler(enabled = selected != null || page == AboutPage.LICENSES) {
            if (selected != null) selected = null else page = AboutPage.HOME
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == AboutPage.LICENSES) {
                    (slideInHorizontally(tween(240)) { it } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(240)))
                } else {
                    (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240)))
                }
            },
            label = "aboutPage"
        ) { current ->
            if (current == AboutPage.HOME) {
                AboutHomeScreen(
                    backdrop = backdrop,
                    onLicenses = { page = AboutPage.LICENSES },
                    onCheckUpdate = { checkForUpdatesManually() }
                )
            } else {
                LicenseListScreen(
                    backdrop = backdrop,
                    onBack = { page = AboutPage.HOME },
                    onSelect = { selected = it }
                )
            }
        }
        LicenseDetailDialog(selected, onDismiss = { selected = null })
    }

    @Composable
    private fun AboutHomeScreen(
        backdrop: LayerBackdrop?,
        onLicenses: () -> Unit,
        onCheckUpdate: () -> Unit
    ) {
        val uriHandler = LocalUriHandler.current
        var showDonate by remember { mutableStateOf(false) }
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(title = "关于", color = Color.Transparent)
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                                        contentDescription = "Shark",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                                Text(
                                    text = "Shark",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLicenses)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = COUITheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "App 图标由本项目绘制；第三方组件版权归各自作者所有，完整许可见「开放源代码许可」。",
                                    fontSize = 13.sp,
                                    color = COUITheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                ArrowPreference(
                                    title = "查看源代码",
                                    summary = "在 GitHub 上查看源代码",
                                    onClick = { uriHandler.openUri(PROJECT_URL) },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Code,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "加入社区",
                                    summary = "加入 QQ 群",
                                    onClick = { uriHandler.openUri("https://qm.qq.com/q/HMg4ngAC8W") },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "问题反馈",
                                    summary = "在 GitHub Issues 提交问题",
                                    onClick = { uriHandler.openUri("$PROJECT_URL/issues") },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.BugReport,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "检查更新",
                                    summary = "当前 v${BuildConfig.VERSION_NAME}",
                                    onClick = onCheckUpdate,
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "支持开发者",
                                    summary = "微信 / 支付宝赞赏二维码",
                                    onClick = { showDonate = true },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                                HorizontalDivider()
                                ArrowPreference(
                                    title = "开放源代码许可",
                                    summary = "查看本应用所使用的第三方开源库及其许可证信息",
                                    onClick = onLicenses,
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Filled.Copyright,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = COUITheme.colorScheme.onBackground
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        DonateDialog(show = showDonate, onDismiss = { showDonate = false })
    }

    @Composable
    private fun DonateDialog(show: Boolean, onDismiss: () -> Unit) {
        OverlayDialog(
            show = show,
            title = "支持开发者",
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DonateQr(
                            label = "微信赞赏",
                            assetPath = "donate/wechat.png",
                            modifier = Modifier.weight(1f)
                        )
                        DonateQr(
                            label = "支付宝",
                            assetPath = "donate/alipay.png",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "关闭",
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        )
    }

    @Composable
    private fun DonateQr(label: String, assetPath: String, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val bitmap = remember(assetPath) {
            runCatching {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = COUITheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }

    @Composable
    private fun LicenseListScreen(
        backdrop: LayerBackdrop?,
        onBack: () -> Unit,
        onSelect: (LicenseItem) -> Unit
    ) {
        val context = LocalContext.current
        val libs by produceState(initialValue = Libs(emptyList(), emptySet()), context) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val json = context.resources
                        .openRawResource(R.raw.aboutlibraries)
                        .bufferedReader()
                        .use { it.readText() }
                    Libs.Builder().withJson(json).build()
                }.getOrElse { Libs(emptyList(), emptySet()) }
            }
        }
        val localItems = remember {
            localLicenses.map { entry ->
                LicenseItem(
                    name = entry.title,
                    author = entry.summary,
                    website = entry.url,
                    licenses = listOf(
                        LicenseText(entry.licenseName, entry.url, asset = entry.asset)
                    )
                )
            }
        }
        val libraryItems = remember(libs) { libs.libraries.map { it.toLicenseItem() } }
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(
                        title = "开放源代码许可",
                        color = Color.Transparent,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = COUIIcons.Back,
                                    contentDescription = "返回",
                                    tint = COUITheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            },
            popupHost = { }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp + 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SectionLabel("本项目与附加组件") }
                    items(localItems) { item ->
                        LicenseCard(item) { onSelect(item) }
                    }
                    item {
                        SectionLabel(
                            if (libraryItems.isEmpty()) "第三方依赖"
                            else "第三方依赖（${libraryItems.size}）"
                        )
                    }
                    items(libraryItems) { item ->
                        LicenseCard(item) { onSelect(item) }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }

    @Composable
    private fun LicenseChip(text: String) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(COUITheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                color = COUITheme.colorScheme.onSecondaryContainer
            )
        }
    }

    @Composable
    private fun LicenseCard(item: LicenseItem, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    item.author?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            color = COUITheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    if (item.licenses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.licenses.take(3).forEach { LicenseChip(it.name) }
                        }
                    }
                }
                item.version?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }

    @Composable
    private fun LicenseDetailDialog(item: LicenseItem?, onDismiss: () -> Unit) {
        var lastItem by remember { mutableStateOf(item) }
        if (item != null) lastItem = item
        val display = lastItem ?: return
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val contents = remember(display) {
            display.licenses.map { license ->
                val text = license.content?.takeIf { it.isNotBlank() }
                    ?: license.asset?.let { asset ->
                        runCatching {
                            context.assets.open(asset).bufferedReader().use { it.readText() }
                        }.getOrNull()
                    }
                license to text
            }
        }
        OverlayDialog(
            show = item != null,
            title = display.name,
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    if (display.licenses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            display.licenses.forEach { LicenseChip(it.name) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    SelectionContainer(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (contents.isEmpty()) {
                                Text("未声明许可证文本", fontSize = 13.sp)
                            }
                            contents.forEach { (license, body) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = license.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = COUITheme.colorScheme.primary
                                    )
                                    Text(
                                        text = body ?: "未内置许可证文本",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        display.website?.let { url ->
                            TextButton(
                                modifier = Modifier.weight(1f),
                                text = "访问主页",
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = "关闭",
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun ManualScreen(onBack: () -> Unit) {
        BackHandler(onBack = onBack)
        val context = LocalContext.current
        val markdown = remember {
            runCatching {
                context.assets.open("manual.md").bufferedReader().use { it.readText() }
            }.getOrElse { "# 使用手册\n\n手册内容读取失败。" }
        }
        val blocks = remember(markdown) { parseManualMarkdown(markdown) }
        Scaffold(popupHost = { }) { inner ->
            val codeBackground = COUITheme.colorScheme.onSurface.copy(alpha = 0.08f)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = inner.calculateTopPadding() + 16.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = inner.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(blocks) { _, block ->
                    MdBlockView(block, codeBackground)
                }
            }
        }
    }

    @Composable
    private fun StatusCard(granted: Boolean) {
        val backendName = PrivService.pick()?.label ?: "Stellar"
        val green = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        val greenTint = Color(0xFF36D167)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (granted) {
                CardDefaults.defaultColors(color = green)
            } else {
                CardDefaults.defaultColors()
            },
            showIndication = false,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(27.dp, 31.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = if (granted) greenTint else Color(0xFF9AA0A6)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        Text(
                            text = if (granted) "$backendName 已授权" else "$backendName 未授权",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (granted) "已连接到 $backendName，可尝试越狱" else "未连接到 $backendName，请授予权限",
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoText(
        icon: ImageVector,
        title: String,
        content: String,
        bottomPadding: Dp = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = COUITheme.colorScheme.onSurface
            )
            Column {
                Text(
                    text = title,
                    fontSize = COUITheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = COUITheme.colorScheme.onSurface
                )
                Text(
                    text = content,
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    color = COUITheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    @Composable
    private fun InfoCard(
        kernelVersion: String,
        exploitLabel: String,
        deviceModel: String,
        fingerprint: String,
        selinux: String,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.DeveloperBoard, "内核版本", kernelVersion)
                InfoText(Icons.Filled.BugReport, "漏洞利用方式", exploitLabel)
                InfoText(Icons.Filled.Smartphone, "设备型号", deviceModel)
                InfoText(Icons.Filled.Fingerprint, "系统指纹", fingerprint)
                InfoText(Icons.Filled.Security, "SELinux 状态", selinux, bottomPadding = 0.dp)
            }
        }
    }

    @Composable
    private fun LogScreen(state: List<String>, backdrop: LayerBackdrop?) {
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(title = "日志", color = Color.Transparent)
                }
            },
            popupHost = { }
        ) { inner ->
            if (state.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无日志",
                        fontSize = 14.sp,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(state.size) {
                    listState.scrollToItem(state.lastIndex)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 12.dp,
                        bottom = 12.dp + 100.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state) { line -> Text(line, fontSize = 11.sp) }
                }
                }
            }
        }
    }

    @Composable
    private fun BottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
        NavigationBar(color = Color.Transparent) {
            AppTab.entries.forEach { t ->
                NavigationBarItem(
                    selected = t == selected,
                    onClick = { onSelect(t) },
                    icon = t.icon,
                    label = t.label
                )
            }
        }
    }

    @Composable
    private fun HistoryScreen(backdrop: LayerBackdrop?) {
        LaunchedEffect(Unit) { refreshHistory() }
        Scaffold(
            topBar = {
                BlurredBar(backdrop = backdrop) {
                    TopAppBar(title = "历史", color = Color.Transparent)
                }
            },
            popupHost = { }
        ) { inner ->
            val entries = historyEntries
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无记录",
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = inner.calculateTopPadding() + 6.dp,
                        bottom = 12.dp + 100.dp
                    )
                ) {
                    items(entries) { e ->
                        HistoryEntryCard(entry = e, onClick = {
                            val name = e.fileName
                            lifecycleScope.launch(Dispatchers.IO) {
                                val text = HistoryStore.read(applicationContext, name)
                                withContext(Dispatchers.Main) {
                                    historyDetailText = text
                                    historyDetail = e
                                }
                            }
                        }, onExport = { shareHistory(e) })
                    }
                }
                }
            }
        }
        HistoryDetailDialog(
            entry = historyDetail,
            text = historyDetailText,
            onDismiss = { historyDetail = null },
            onDelete = {
                val name = historyDetail?.fileName
                if (name != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        HistoryStore.delete(applicationContext, name)
                        withContext(Dispatchers.Main) {
                            historyDetail = null
                            refreshHistory()
                        }
                    }
                }
            }
        )
    }

    /* Layout mirrors KernelSU Manager's sulog page (COUI renders it): entry card with
     * marquee title, description, timestamp, status tags, right-side status
     * and arrow; detail opens an OverlayDialog with monospace selectable text. */
    @Composable
    private fun HistoryEntryCard(entry: HistoryEntry, onClick: () -> Unit, onExport: () -> Unit) {
        val scope = rememberCoroutineScope()
        val offset = remember { Animatable(0f) }
        val actionWidth = with(LocalDensity.current) { 88.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            val reveal = (-offset.value).coerceIn(0f, actionWidth)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { reveal.toDp() })
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    Button(
                        onClick = {
                            scope.launch { offset.animateTo(0f, tween(200)) }
                            onExport()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(88.dp)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text("导出")
                    }
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = offset.value }
                    .pointerInput(entry.fileName) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val target = if (offset.value < -actionWidth / 2f) -actionWidth else 0f
                                scope.launch { offset.animateTo(target, tween(200)) }
                            },
                            onDragCancel = {
                                scope.launch { offset.animateTo(0f, tween(200)) }
                            },
                            onHorizontalDrag = { _, delta ->
                                scope.launch {
                                    offset.snapTo((offset.value + delta).coerceIn(-actionWidth, 0f))
                                }
                            }
                        )
                    },
                onClick = {
                    if (offset.value < -1f) {
                        scope.launch { offset.animateTo(0f, tween(200)) }
                    } else {
                        onClick()
                    }
                },
                showIndication = true,
                insideMargin = PaddingValues(16.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.manager,
                        modifier = Modifier.basicMarquee(),
                        fontWeight = FontWeight(550),
                        color = COUITheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = entry.exploit,
                        fontSize = 12.sp,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.startTime,
                        modifier = Modifier.basicMarquee(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = COUITheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val colors = listOf(
                            COUITheme.colorScheme.primary to COUITheme.colorScheme.onPrimary,
                            COUITheme.colorScheme.secondaryContainer to
                                COUITheme.colorScheme.onSecondaryContainer,
                            COUITheme.colorScheme.tertiaryContainer to
                                COUITheme.colorScheme.onTertiaryContainer,
                        )
                        historyTags(entry).forEachIndexed { index, tag ->
                            val (bg, fg) = colors.getOrElse(index) { colors.last() }
                            CouiStatusTag(label = tag, backgroundColor = bg, contentColor = fg)
                        }
                    }
                }
                Text(
                    text = historyValue(entry),
                    color = COUITheme.colorScheme.onSurfaceVariantActions,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Icon(
                    imageVector = COUIIcons.Basic.ArrowRight,
                    contentDescription = null,
                    tint = COUITheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(width = 10.dp, height = 16.dp)
                )
            }
            }
        }
    }

    @Composable
    private fun CouiStatusTag(label: String, backgroundColor: Color, contentColor: Color) {
        Box(
            modifier = Modifier.background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp)
            )
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                text = label,
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight(750),
                maxLines = 1,
                softWrap = false
            )
        }
    }

    private fun historyTags(e: HistoryEntry): List<String> = listOfNotNull(
        e.model.takeIf { it.isNotBlank() },
        e.kernel.takeIf { it.isNotBlank() }?.substringBefore('-')
    )

    /* Right-swipe export: copy the run log to cache/export and open the
     * system share sheet through FileProvider. */
    private fun shareHistory(entry: HistoryEntry) {
        lifecycleScope.launch(Dispatchers.IO) {
            val text = HistoryStore.read(applicationContext, entry.fileName)
            val dir = File(cacheDir, "export").apply { mkdirs() }
            val out = File(dir, entry.fileName)
            out.writeText(text)
            val uri = runCatching {
                FileProvider.getUriForFile(
                    applicationContext,
                    "$packageName.fileprovider",
                    out
                )
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (uri == null) {
                    addLog("Failed to prepare log for sharing")
                    return@withContext
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Shark log ${entry.startTime}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    startActivity(Intent.createChooser(send, "导出日志"))
                }.onFailure { addLog("Failed to open share sheet: $it") }
            }
        }
    }

    private fun historyValue(e: HistoryEntry): String {
        val dur = e.durationMs?.let { String.format(java.util.Locale.US, "%.1fs", it / 1000.0) }
        return when {
            e.result == "SUCCESS" && dur != null -> "成功 $dur"
            e.result == "SUCCESS" -> "成功"
            e.result == "FAIL" && dur != null -> "失败 $dur"
            e.result == "FAIL" -> "失败"
            else -> "进行中"
        }
    }

    @Composable
    private fun HistoryDetailDialog(
        entry: HistoryEntry?,
        text: String,
        onDismiss: () -> Unit,
        onDelete: () -> Unit
    ) {
        var lastEntry by remember { mutableStateOf(entry) }
        if (entry != null) lastEntry = entry
        val displayEntry = lastEntry ?: return
        OverlayDialog(
            show = entry != null,
            title = "${displayEntry.startTime}  ${displayEntry.manager}",
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    SelectionContainer(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = text.ifEmpty { "暂无日志" },
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = "删除",
                            onClick = onDelete
                        )
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = "确定",
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        )
    }


    private fun addLog(line: String) {
        Log.i("PYB110", line)
        val stamped = "${timeFmt.format(java.util.Date())}  $line"
        HistoryStore.append(stamped)
        runOnUiThread {
            logs.add(stamped)
            if (logs.size > 4000) logs.removeRange(0, logs.size - 4000)
        }
    }

    private fun refreshHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val entries = HistoryStore.list(applicationContext)
            withContext(Dispatchers.Main) { historyEntries = entries }
        }
    }

    private fun refreshAuth() {
        authorized = PrivService.granted()
    }

    private fun refreshKsuAsync() {
        if (PrivService.pick() == null) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (active, se) = readDeviceState()
                withContext(Dispatchers.Main) {
                    ksuActive = active
                    selinuxText = se
                }
            } catch (t: Throwable) {
                // binder 未就绪时静默
            }
        }
    }

    private fun readDeviceState(): Pair<Boolean, String> {
        val p = PrivService.newProcess(
            arrayOf("sh", "-c", "if grep -qi kernelsu /proc/modules; then echo YES; elif timeout 3 su -c id 2>/dev/null | grep -q uid=0; then echo YES; else echo NO; fi; getenforce"),
            shizukuEnv(), "/data/local/tmp"
        )
        val lines = p.getInputStream()
            .bufferedReader().readLines()
        p.waitFor()
        val active = lines.getOrNull(0)?.trim() == "YES"
        val se = mapSelinux(lines.getOrNull(1)?.trim().orEmpty())
        return active to se
    }

    private fun updateAntiRootGuard(value: Boolean) {
        antiRootGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("antiroot_guard", value).apply()
        addLog("Anti-root guard: ${if (value) "on (stop riskdetect HAL; restored on reboot)" else "off"}")
    }

    private fun updateSoftRebootAfter(value: Boolean) {
        softRebootAfter = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("soft_reboot_after", value).apply()
        addLog("Soft reboot after jailbreak: ${if (value) "on" else "off"}")
    }

    private fun updateUnloadGuard(value: Boolean) {
        unloadGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("unload_guard", value).apply()
        addLog("Unload kernel security guard after jailbreak: ${if (value) "on" else "off"}")
    }

    private fun updateSusfsSupport(value: Boolean) {
        susfsSupport = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("susfs_support", value).apply()
        addLog("SUSFS support: ${if (value) "on (load ko after jailbreak)" else "off"}")
    }

    private fun updateVfsSupport(value: Boolean) {
        vfsSupport = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("vfs_support", value).apply()
        addLog("VFS support: ${if (value) "on (load hybridmount ko after jailbreak)" else "off"}")
    }

    private fun selectManager(kind: ManagerKind) {
        manager = kind
        dynamicSwitching = kind == ManagerKind.DYNAMIC
        if (dynamicSwitching) {
            hotManager = ManagerKind.KERNELSU
        }
        getSharedPreferences("pyb110", MODE_PRIVATE).edit()
            .putString("manager", kind.name)
            .putBoolean("dynamic_switching", dynamicSwitching)
            .apply()
        addLog(
            if (dynamicSwitching) "ROOT manager selected: Dynamic Switching (universal LKM, initial KernelSU)"
            else "ROOT manager selected: ${kind.fullLabel}"
        )
    }

    /**
     * Prefer the App's own KernelSU grant; fall back to Shizuku/Stellar.
     * The hot-switch card must not disappear just because the privileged
     * backend is not running while the App itself already has root.
     */
    private fun runRootCommand(script: String, timeoutSeconds: Long = 8): Pair<Int, String> {
        var directFailure: Pair<Int, String>? = null
        runCatching {
            val p = ProcessBuilder("/system/bin/su", "-c", script).redirectErrorStream(true).start()
            if (p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                val out = p.inputStream.bufferedReader().readText().trim()
                val result = p.exitValue() to out
                if (result.first == 0) return result
                directFailure = result
            } else {
                p.destroyForcibly()
                directFailure = -2 to "su timeout"
            }
        }
        val fallback = runCatching {
            val p = PrivService.newProcess(arrayOf("su", "-c", script), shizukuEnv(), "/data/local/tmp")
            val out = p.getInputStream().bufferedReader().readText().trim()
            p.waitFor() to out
        }.getOrElse { -1 to (it.message ?: "root command failed") }
        return if (fallback.first == 0) fallback else (directFailure ?: fallback)
    }

    private fun switchHotManager(kind: ManagerKind) {
        if (hotSwitchBusy || kind == ManagerKind.DYNAMIC) return
        hotSwitchBusy = true
        lifecycleScope.launch(Dispatchers.IO) {
            val pkg = kind.pkg
            val appInfo = runCatching {
                packageManager.getPackageInfo(pkg, 0).applicationInfo
            }.getOrNull()
            if (appInfo == null) {
                withContext(Dispatchers.Main) {
                    hotSwitchBusy = false
                    addLog("Hot switch -> ${kind.fullLabel} failed: package not installed")
                }
                return@launch
            }
            val apk = appInfo.sourceDir
            val uid = appInfo.uid
            val script = "printf '%s %u %s' '$pkg' $uid '$apk' > /proc/kernelsu_manager"
            val (rc, out) = runRootCommand(script, 20)
            if (rc == 0) {
                runRootCommand("am force-stop '$pkg'", 10)
            }
            withContext(Dispatchers.Main) {
                hotSwitchBusy = false
                if (rc == 0) {
                    hotSwitchError = null
                    addLog("Hot switch -> ${kind.fullLabel} (rc=0)")
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        runCatching { startActivity(intent) }
                            .onFailure { addLog("Failed to open ${kind.fullLabel}: $it") }
                    } else {
                        addLog("Manager launch entry not found: $pkg")
                    }
                } else {
                    hotSwitchError = "切换失败：rc=$rc ${out.take(120)}"
                    addLog("Hot switch -> ${kind.fullLabel} failed rc=$rc ${out.take(120)}")
                }
            }
        }
    }

    private fun resolveDeviceName(): String {
        val marketName = when (Build.MANUFACTURER.orEmpty().lowercase()) {
            "xiaomi" -> getSystemProperty("ro.product.marketname")
            "oppo", "oneplus", "realme", "oplus" -> getSystemProperty("ro.vendor.oplus.market.name")
            "vivo" -> getSystemProperty("ro.vivo.market.name")
            "honor", "huawei" -> getSystemProperty("ro.config.marketing_name")
            "zte", "nubia" -> getSystemProperty("ro.vendor.product.ztename")
            else -> ""
        }.trim().takeIf {
            it.isNotEmpty() && !it.equals(Build.UNKNOWN, ignoreCase = true) && !it.equals("null", ignoreCase = true)
        }
        if (marketName != null) return marketName
        return buildString {
            append(Build.MANUFACTURER)
            if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) append(" ").append(Build.BRAND)
            append(" ").append(Build.MODEL)
        }
    }

    private fun getSystemProperty(key: String): String = try {
        val props = Class.forName("android.os.SystemProperties")
        props.getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
    } catch (t: Throwable) {
        ""
    }

    private fun refreshStaticStateAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val raw = getSELinuxStatusRaw()
            withContext(Dispatchers.Main) { selinuxText = mapSelinux(raw) }
        }
    }

    private fun getSELinuxStatusRaw(): String = try {
        val process = ProcessBuilder("getenforce").start()
        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText().trim()
        val ok = process.waitFor() == 0
        if (ok) {
            when (stdout) {
                "Enforcing", "Permissive", "Disabled" -> stdout
                else -> "Unknown"
            }
        } else if (stderr.endsWith("Permission denied")) {
            "Enforcing"
        } else {
            "Unknown"
        }
    } catch (t: Throwable) {
        "Unknown"
    }

    private fun mapSelinux(raw: String): String = when (raw) {
        "Enforcing" -> "强制执行 (Enforcing)"
        "Permissive" -> "宽容模式 (Permissive)"
        "Disabled" -> "已禁用"
        else -> "未知"
    }

    private fun shizukuEnv(): Array<String> = arrayOf(
        "PATH=/system/bin:/system/xbin:/data/adb/ksu/bin",
        "HOME=/data/local/tmp",
        "TMPDIR=/data/local/tmp"
    )

    private fun launchManager() {
        val intent = packageManager.getLaunchIntentForPackage(manager.pkg)
        if (intent == null) {
            addLog("Manager launch entry not found: ${manager.pkg}")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(intent) }
            .onFailure { addLog("Failed to open manager: $it") }
    }

    private fun runFlow() {
        busy = true
        val softReboot = softRebootAfter
        val startedAt = System.currentTimeMillis()
        HistoryStore.start(applicationContext, manager.fullLabel, exploit.fullLabel, kernelVersion, deviceModel)
        lifecycleScope.launch(Dispatchers.IO) {
            var success = false
            var exit = -1
            try {
                if (PrivService.pick() == null) {
                    addLog("No privileged backend (Stellar/Shizuku) running")
                    return@launch
                }
                val (alreadyActive, alreadySelinux) = readDeviceState()
                if (exploit == ExploitKind.CVE2505A && alreadyActive) {
                    exit = 0
                    success = true
                    withContext(Dispatchers.Main) {
                        ksuActive = true
                        selinuxText = alreadySelinux
                    }
                    addLog("ALREADY_LOADED: kernelsu present; skip exploit/load")
                    if (!softReboot) withContext(Dispatchers.Main) { launchManager() }
                    return@launch
                }
                val ksudPreflight = PrivService.newProcess(
                    arrayOf("sh", "-c",
                        "[ -n \"$(pm path '${manager.pkg}' 2>/dev/null)\" ] || [ -x /data/adb/ksu/bin/ksud ] || [ -x /data/adb/ksud ]"),
                    shizukuEnv(), "/data/local/tmp"
                ).waitFor()
                if (ksudPreflight != 0) {
                    addLog("Manager not installed: ${manager.pkg}. Install the manager APK first; ksud is executed from its APK, no bundled copy.")
                    return@launch
                }
                val nativeDir = applicationInfo.nativeLibraryDir
                addLog("Starting ${exploit.fullLabel}")
                val cmd = when (exploit) {
                    ExploitKind.CVE98260 -> arrayOf("$nativeDir/libt98260.so", "run")
                    ExploitKind.CVE61269 -> arrayOf("$nativeDir/libt61269.so", "run")
                    ExploitKind.CVE2505A -> arrayOf("$nativeDir/libv2505a.so", "run")
                }
                val rc = runStreaming(cmd, nativeDir)
                exit = rc
                addLog("exploit exit=$rc")
                val (active, se) = readDeviceState()
                success = rc == 0 && active
                withContext(Dispatchers.Main) {
                    ksuActive = active
                    selinuxText = se
                }
                addLog(if (active) "KernelSU module loaded" else "KernelSU module not loaded")
                if (active && !softReboot) {
                    withContext(Dispatchers.Main) { launchManager() }
                }
                if (softReboot && rc == 0 && active) {
                    addLog("Jailbreak complete; restarting zygote")
                    val rebootRc = runCatching {
                        PrivService.newProcess(
                            arrayOf("su", "-c", "setprop ctl.restart zygote"),
                            shizukuEnv(), "/data/local/tmp"
                        ).waitFor()
                    }.getOrDefault(-1)
                    if (rebootRc != 0) addLog("Soft reboot failed rc=$rebootRc")
                }
            } catch (t: Throwable) {
                addLog("ERROR: $t")
            } finally {
                HistoryStore.finish(success, exit, System.currentTimeMillis() - startedAt)
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    private fun runStreaming(cmd: Array<String>, dir: String): Int {
        val susfsKo = if (exploit == ExploitKind.CVE61269 || exploit == ExploitKind.CVE2505A) {
            "${applicationInfo.nativeLibraryDir}/libsusfs_guard_lkm_612.so"
        } else {
            "${applicationInfo.nativeLibraryDir}/libsusfs_guard_lkm.so"
        }
        val universal = dynamicSwitching && supportsUniversal()
        val initialManager = if (manager == ManagerKind.DYNAMIC) ManagerKind.KERNELSU else manager
        val env = shizukuEnv() + "PYB110_DIR=$dir" + "PYB110_MANAGER=${initialManager.manager}" +
            "PYB110_LATE_LOAD=1" +
            "PYB110_UNLOAD_GUARD=${if (unloadGuard) "1" else "0"}" +
            "PYB110_DISABLE_ANTIROOT=${if (antiRootGuard) "1" else "0"}" +
            "PYB110_SUSFS=${if (susfsSupport) "1" else "0"}" +
            "PYB110_VFS=${if (vfsSupport) "1" else "0"}" +
            "PYB110_UNIVERSAL=${if (universal) "1" else "0"}" +
            "PYB110_UNIVERSAL_KSUD=${applicationInfo.nativeLibraryDir}/libksud_universal.so" +
            "PYB110_SUSFS_KO=$susfsKo" +
            "PYB110_VFS_KO=${applicationInfo.nativeLibraryDir}/libhybridmount.so"
        val p = PrivService.newProcess(cmd, env, dir)
        val out = p.getInputStream()
        val err = p.getErrorStream()
        val t1 = Thread {
            try {
                out.bufferedReader().forEachLine { addLog(it) }
            } catch (t: Throwable) {
                // stream closed by waitFor() cleanup
            }
        }.apply { isDaemon = true; start() }
        val t2 = Thread {
            try {
                err.bufferedReader().forEachLine { addLog(it) }
            } catch (t: Throwable) {
                // stream closed by waitFor() cleanup
            }
        }.apply { isDaemon = true; start() }
        val rc = p.waitFor()
        runCatching { out.close() }
        runCatching { err.close() }
        t1.join(2000)
        t2.join(2000)
        return rc
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        Stellar.removeRequestPermissionResultListener(stellarPermissionListener)
        Stellar.removeBinderReceivedListener(stellarBinderListener)
        super.onDestroy()
    }

    private enum class AppTab(val label: String, val icon: ImageVector) {
        HOME("首页", Icons.Filled.Home),
        LOGS("日志", COUIIcons.Notes),
        HISTORY("历史", Icons.Filled.History),
        ABOUT("关于", Icons.Filled.Info)
    }

    private enum class ExploitKind(val fullLabel: String) {
        CVE98260("CVE-2026-98260-6.18"),
        CVE61269("CVE-2026-98260-6.12"),
        CVE2505A("CVE-2026-98260-vivo-V2505A")
    }

    private enum class ManagerKind(val fullLabel: String, val pkg: String, val manager: String) {
        KERNELSU("KernelSU", "me.weishu.kernelsu", "kernelsu"),
        FINASU("FinaSU", "me.yuki.fina", "finasu"),
        NEXT("KernelSU Next", "com.rifsxd.ksunext", "next"),
        BAKASU("BakaSU", "com.resukisu.resukisu", "bakasu"),
        SUKISU("SukiSU Ultra", "com.sukisu.ultra", "sukisu"),
        DYNAMIC("Dynamic Switching", "me.weishu.kernelsu", "kernelsu")
    }

    private fun supportsUniversal(): Boolean =
        exploit == ExploitKind.CVE98260 ||
            exploit == ExploitKind.CVE61269 ||
            exploit == ExploitKind.CVE2505A

    private fun managerChoices(): List<ManagerKind> =
        if (supportsUniversal()) ManagerKind.entries
        else ManagerKind.entries.filter { it != ManagerKind.DYNAMIC }

    private fun hotManagerChoices(): List<ManagerKind> =
        ManagerKind.entries.filter { it != ManagerKind.DYNAMIC }

    companion object {
        private const val REQ_SHIZUKU = 1001
        private const val V2505A_KERNEL_RELEASE =
            "6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k"
    }
}
