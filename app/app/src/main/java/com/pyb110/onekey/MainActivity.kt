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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notes
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Item
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.shape.CouiShape
import com.itos.coui.switch.Switch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.itos.coui.topappbar.TopAppBar
import com.itos.coui.topappbar.TopAppBarButton

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
        "AzirX1337 CVE-2026-98260",
        "MIT 许可的参考实现",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/AzirX1337/CVE-2026-98260-OnePlus16-PYB110-Jailbreak"
    )
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

    /* UI-only state hoisted for the COUI top bar / dialogs. */
    private var aboutPage by mutableStateOf(AboutPage.HOME)
    private var aboutLicense by mutableStateOf<LicenseItem?>(null)
    private var showDonate by mutableStateOf(false)

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
            CouiTheme(darkTheme = isSystemInDarkTheme()) {
                AppRoot()
            }
        }
    }

    /* ===== Root chrome ===== */

    @Composable
    private fun AppRoot() {
        val dark = CouiTheme.isDark
        val overlayVisible = showManual || showExtras
        BackHandler(
            enabled = aboutLicense != null || showManual || showExtras ||
                (tab == AppTab.ABOUT && aboutPage == AboutPage.LICENSES)
        ) {
            when {
                aboutLicense != null -> aboutLicense = null
                showManual -> showManual = false
                showExtras -> showExtras = false
                tab == AppTab.ABOUT && aboutPage == AboutPage.LICENSES -> aboutPage = AboutPage.HOME
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                when {
                    showExtras -> AppTopBar("拓展功能", onBack = { showExtras = false })
                    showManual -> AppTopBar("使用手册", onBack = { showManual = false })
                    tab == AppTab.ABOUT && aboutPage == AboutPage.LICENSES ->
                        AppTopBar("开放源代码许可", onBack = { aboutPage = AboutPage.HOME })
                    else -> AppTopBar(if (tab == AppTab.HOME) "Shark" else tab.label)
                }
            },
            bottomBar = {
                if (!overlayVisible) {
                    AppBottomBar(selected = tab, onSelect = { tab = it })
                }
            }
        ) { inner ->
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
                ) { current ->
                    when (current) {
                        AppTab.HOME -> HomeScreen(inner)
                        AppTab.LOGS -> LogScreen(inner)
                        AppTab.HISTORY -> HistoryScreen(inner)
                        AppTab.ABOUT -> AboutScreen(inner)
                    }
                }
                AnimatedVisibility(
                    visible = showManual,
                    enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
                    exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CouiTheme.colorScheme.background)
                            .pointerInput(Unit) { detectTapGestures { } }
                    ) {
                        ManualScreen(inner)
                    }
                }
                AnimatedVisibility(
                    visible = showExtras,
                    enter = slideInHorizontally(tween(240)) { it } + fadeIn(tween(240)),
                    exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CouiTheme.colorScheme.background)
                            .pointerInput(Unit) { detectTapGestures { } }
                    ) {
                        ExtrasScreen(inner)
                    }
                }
            }
        }
        UpdateDialog()
        DroidspacesDialog()
        LicenseDetailDialog()
        DonateDialog()
    }

    @Composable
    private fun AppTopBar(title: String, onBack: (() -> Unit)? = null) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    kind = CouiTextKind.Toolbar,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            },
            navigationIcon = { if (onBack != null) CouiBackButton(onBack) }
        )
    }

    @Composable
    private fun CouiBackButton(onBack: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 34) {
            TopAppBarButton(onClick = onBack) {
                AppIcon(CouiIcons.Back, "返回", Modifier.size(24.dp))
            }
        } else {
            val dark = CouiTheme.isDark
            val source = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CouiShape(24.dp, 2f))
                    .couiButtonFeedback(dark, source, true)
                    .clickable(source, indication = null, role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "返回" },
                contentAlignment = Alignment.Center
            ) {
                AppIcon(CouiIcons.Back, null, Modifier.size(24.dp))
            }
        }
    }

    @Composable
    private fun AppBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
        val renderer = remember {
            if (Build.VERSION.SDK_INT >= 34) {
                runCatching { CouiFloatingRenderer() }.getOrNull()
            } else {
                null
            }
        }
        if (Build.VERSION.SDK_INT >= 34 && renderer != null) {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                renderer = renderer,
                items = AppTab.entries.map { CouiNavigationItem(it.label, it.icon, it.icon) },
                selectedIndex = selected.ordinal,
                dark = CouiTheme.isDark,
                onSelection = { onSelect(AppTab.entries[it]) }
            )
        } else {
            PlainBottomBar(selected = selected, onSelect = onSelect)
        }
    }

    /* API 33 上底栏浮动材质不可用，使用同一套 COUI 配色的平铺栏位。 */
    @Composable
    private fun PlainBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
        val dark = CouiTheme.isDark
        val scheme = CouiTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .height(64.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { entry ->
                val source = remember { MutableInteractionSource() }
                val tint = if (entry == selected) scheme.accent else scheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(percent = 50))
                        .couiNavigationFeedback(dark, source)
                        .selectable(
                            selected = entry == selected,
                            interactionSource = source,
                            indication = null,
                            enabled = true,
                            role = Role.Tab
                        ) { onSelect(entry) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(entry.icon, null, Modifier.size(24.dp), tint = tint)
                    Text(text = entry.label, kind = CouiTextKind.Category, color = tint)
                }
            }
        }
    }

    /* ===== Shared pieces ===== */

    @Composable
    private fun AppIcon(
        icon: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified
    ) {
        val color = tint.takeOrElse {
            LocalCouiContentColor.current.takeOrElse { CouiTheme.colorScheme.onSurface }
        }
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = ColorFilter.tint(color)
        )
    }

    private fun itemDividerIndent(): Dp =
        CouiPageDefaults.rowPadding + CouiPageDefaults.iconSize + CouiPageDefaults.iconSpacing

    @Composable
    private fun Chevron() {
        val colors = CouiPageDefaults.themeColors()
        AppIcon(CouiIcons.Next, null, Modifier.padding(start = 4.dp).size(18.dp), tint = colors.summary)
    }

    /**
     * 统一设置行：图标 + 标题 + 可选摘要 + 尾部内容。
     * onClick 为空的行走只读装配，交互由尾部控件负责。
     */
    @Composable
    private fun SettingItem(
        icon: ImageVector,
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        dark: Boolean = CouiTheme.isDark,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        trailing: @Composable () -> Unit = {}
    ) {
        val colors = CouiPageDefaults.themeColors()
        Item(
            modifier = modifier,
            icon = {
                AppIcon(
                    icon,
                    null,
                    Modifier.size(CouiPageDefaults.iconSize),
                    tint = if (enabled) colors.title else colors.disabled
                )
            },
            summary = if (summary == null) null else {
                {
                    Text(
                        text = summary,
                        kind = CouiTextKind.PreferenceSummary,
                        color = colors.summary,
                        textAlign = TextAlign.Start
                    )
                }
            },
            trailing = trailing,
            onClick = onClick,
            enabled = enabled,
            dark = dark
        ) {
            Text(
                text = title,
                kind = CouiTextKind.PreferenceTitle,
                color = if (enabled) colors.title else colors.disabled,
                textAlign = TextAlign.Start
            )
        }
    }

    @Composable
    private fun SettingSwitchItem(
        icon: ImageVector,
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        SettingItem(
            icon = icon,
            title = title,
            onClick = { onCheckedChange(!checked) },
            trailing = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    onDragCheckedChange = null
                )
            }
        )
    }

    @Composable
    private fun CouiProgressBar(progress: Float) {
        val scheme = CouiTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(scheme.onSurface.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(scheme.accent)
            )
        }
    }

    @Composable
    private fun DialogBody(text: String, monospace: Boolean = false) {
        val scheme = CouiTheme.colorScheme
        val style = rememberCouiTextStyle(CouiTextKind.Body).let {
            if (monospace) it.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp) else it
        }
        SelectionContainer {
            Box(modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                BasicText(
                    text = text,
                    style = style.copy(color = scheme.onSurface)
                )
            }
        }
    }

    @Composable
    private fun DialogLink(text: String, onClick: () -> Unit) {
        Text(
            text = text,
            kind = CouiTextKind.PreferenceTitle,
            color = CouiTheme.colorScheme.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp)
        )
    }

    /* ===== Dialogs ===== */

    @Composable
    private fun DroidspacesDialog() {
        val release = droidspacesRelease
        val savedPath = droidspacesSavedPath
        val scheme = CouiTheme.colorScheme
        val accent = scheme.accent
        val confirm = when {
            droidspacesDownloading -> CouiDialogAction(
                text = "下载中 ${(droidspacesProgress * 100).toInt()}%",
                onClick = {},
                enabled = false
            )
            droidspacesLoading -> CouiDialogAction(
                text = "关闭",
                onClick = { droidspacesDialogVisible = false }
            )
            release == null -> CouiDialogAction(
                text = "重试",
                onClick = { loadDroidspacesRelease() },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            )
            savedPath != null -> CouiDialogAction(
                text = "打开 KernelSU",
                onClick = {
                    droidspacesDialogVisible = false
                    launchManager()
                },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            )
            else -> CouiDialogAction(
                text = "下载",
                onClick = { downloadDroidspacesModule() },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            )
        }
        val dismiss = if (droidspacesDownloading || droidspacesLoading) null else {
            CouiDialogAction(text = "关闭", onClick = { droidspacesDialogVisible = false })
        }
        Dialog(
            onDismissRequest = { if (!droidspacesDownloading) droidspacesDialogVisible = false },
            title = "Droidspaces 模块",
            confirmAction = confirm,
            dismissAction = dismiss,
            visible = droidspacesDialogVisible
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
            ) {
                when {
                    droidspacesLoading -> Text(
                        text = "正在获取最新 release…",
                        kind = CouiTextKind.Body,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                    release == null -> Text(
                        text = droidspacesError ?: "获取最新 release 失败",
                        kind = CouiTextKind.Body,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                    else -> {
                        Text(
                            text = "最新版本 ${release.version}",
                            kind = CouiTextKind.PreferenceTitle,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${release.assetName} · ${formatSize(release.assetSize)}",
                            kind = CouiTextKind.PreferenceSummary,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(12.dp))
                        if (savedPath == null) {
                            DialogBody(release.changelog.ifBlank { "无更新说明" })
                        } else {
                            Text(
                                text = "已保存到：",
                                kind = CouiTextKind.PreferenceSummary,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                BasicText(
                                    text = savedPath,
                                    style = rememberCouiTextStyle(CouiTextKind.Body).copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = scheme.onSurface
                                    )
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "请到 KernelSU 管理器 → 模块 → 安装，选择该文件刷入。",
                                kind = CouiTextKind.Body,
                                color = scheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                        }
                        droidspacesError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                kind = CouiTextKind.Description,
                                color = Color(0xFFD32F2F),
                                textAlign = TextAlign.Start
                            )
                        }
                        if (droidspacesDownloading) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "正在下载 ${(droidspacesProgress * 100).toInt()}%",
                                kind = CouiTextKind.Body,
                                color = scheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                            Spacer(Modifier.height(6.dp))
                            CouiProgressBar(droidspacesProgress)
                        }
                    }
                }
            }
        }
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
    private fun UpdateDialog() {
        val info = pendingUpdate ?: return
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val scheme = CouiTheme.colorScheme
        val accent = scheme.accent
        Dialog(
            onDismissRequest = { if (!updateDownloading) updateDialogVisible = false },
            title = "发现新版本 ${info.version}",
            confirmAction = CouiDialogAction(
                text = when {
                    updateDownloading -> "下载中 ${(updateProgress * 100).toInt()}%"
                    pendingApk != null -> "安装"
                    else -> "立即更新"
                },
                onClick = {
                    val ready = pendingApk
                    if (ready != null) {
                        if (installApk(ready)) updateDialogVisible = false
                    } else {
                        updateDownloading = true
                        updateError = null
                        updateProgress = 0f
                        scope.launch {
                            val result = runCatching {
                                UpdateChecker.download(context, info) { p -> updateProgress = p }
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
                    }
                },
                enabled = !updateDownloading,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            ),
            dismissAction = CouiDialogAction(
                text = "稍后",
                onClick = { updateDialogVisible = false },
                enabled = !updateDownloading
            ),
            visible = updateDialogVisible
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
            ) {
                Text(
                    text = "当前 ${BuildConfig.VERSION_NAME}  ·  来源 ${info.source}",
                    kind = CouiTextKind.PreferenceSummary,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
                if (updateDownloading) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "正在下载 ${(updateProgress * 100).toInt()}%",
                        kind = CouiTextKind.Body,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(6.dp))
                    CouiProgressBar(updateProgress)
                } else {
                    Spacer(Modifier.height(12.dp))
                    DialogBody(info.changelog.ifBlank { "无更新说明" })
                    updateError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            kind = CouiTextKind.Description,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Start
                        )
                        DialogLink("打开浏览器下载") { uriHandler.openUri(info.pageUrl) }
                    }
                }
            }
        }
    }

    @Composable
    private fun DonateDialog() {
        if (!showDonate) return
        val accent = CouiTheme.colorScheme.accent
        Dialog(
            onDismissRequest = { showDonate = false },
            title = "支持开发者",
            confirmAction = CouiDialogAction(
                text = "关闭",
                onClick = { showDonate = false },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            ),
            visible = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
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
            }
        }
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
                kind = CouiTextKind.PreferenceSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun LicenseDetailDialog() {
        val item = aboutLicense ?: return
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val scheme = CouiTheme.colorScheme
        val accent = scheme.accent
        val contents = remember(item) {
            item.licenses.map { license ->
                val body = license.content?.takeIf { it.isNotBlank() }
                    ?: license.asset?.let { asset ->
                        runCatching {
                            context.assets.open(asset).bufferedReader().use { it.readText() }
                        }.getOrNull()
                    }
                license to body
            }
        }
        Dialog(
            onDismissRequest = { aboutLicense = null },
            title = item.name,
            confirmAction = CouiDialogAction(
                text = "关闭",
                onClick = { aboutLicense = null },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(accent)
            ),
            visible = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
            ) {
                if (item.licenses.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.licenses.forEach { LicenseChip(it.name) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                SelectionContainer {
                    Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (contents.isEmpty()) {
                                Text(
                                    text = "未声明许可证文本",
                                    kind = CouiTextKind.Body,
                                    color = scheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                            }
                            contents.forEach { (license, body) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = license.name,
                                        kind = CouiTextKind.PreferenceTitle,
                                        color = accent,
                                        textAlign = TextAlign.Start
                                    )
                                    BasicText(
                                        text = body ?: "未内置许可证文本",
                                        style = rememberCouiTextStyle(CouiTextKind.Body).copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = scheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                item.website?.let { url ->
                    DialogLink("访问主页") { uriHandler.openUri(url) }
                }
            }
        }
    }

    /* ===== Home ===== */

    @Composable
    private fun HomeScreen(inner: PaddingValues) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 4.dp,
                bottom = inner.calculateBottomPadding() + 8.dp
            )
        ) {
            item {
                StatusCard(granted = binderReady && authorized)
            }
            item {
                PrimaryActionCard()
            }
            item {
                HomeEntriesCard()
            }
            item {
                InfoCard(
                    kernelVersion = kernelVersion,
                    exploitLabel = exploit.fullLabel,
                    deviceModel = deviceModel,
                    fingerprint = Build.FINGERPRINT,
                    selinux = selinuxText
                )
            }
        }
    }

    @Composable
    private fun StatusCard(granted: Boolean) {
        val dark = CouiTheme.isDark
        val colors = CouiPageDefaults.themeColors(dark)
        val backendName = PrivService.pick()?.label ?: "Stellar"
        val green = if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        val greenTint = Color(0xFF36D167)
        Group(colors = colors.copy(card = if (granted) green else colors.card)) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        Text(
                            text = if (granted) "$backendName 已授权" else "$backendName 未授权",
                            kind = CouiTextKind.PageTitle,
                            color = colors.title,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (granted) "已连接到 $backendName，可尝试越狱"
                            else "未连接到 $backendName，请授予权限",
                            kind = CouiTextKind.Body,
                            color = colors.title,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(end = 20.dp, bottom = 12.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AppIcon(
                        Icons.Rounded.CheckCircleOutline,
                        null,
                        Modifier.size(100.dp),
                        tint = if (granted) greenTint else Color(0xFF9AA0A6)
                    )
                }
            }
        }
    }

    @Composable
    private fun PrimaryActionCard() {
        val colors = CouiPageDefaults.themeColors()
        val label = when {
            busy -> "运行中…"
            !binderReady -> "Stellar 未运行（点击打开）"
            !authorized -> "授权 Shizuku"
            ksuActive -> "校验 / 再次越狱"
            else -> "一键越狱"
        }
        Group {
            Item(
                onClick = {
                    if (!busy) {
                        when {
                            !binderReady -> {
                                addLog("Start Stellar or Shizuku first")
                                PrivService.openManager(applicationContext)
                            }
                            !authorized -> PrivService.request(REQ_SHIZUKU)
                            else -> runFlow()
                        }
                    }
                },
                enabled = !busy,
                trailing = { Chevron() }
            ) {
                Text(
                    text = label,
                    kind = CouiTextKind.SectionTitle,
                    color = if (busy) colors.disabled else colors.accent,
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    @Composable
    private fun HomeEntriesCard() {
        Group {
            DropdownItem(
                icon = Icons.Filled.Tag,
                title = "ROOT管理器",
                values = managerChoices(),
                selected = manager,
                label = { it.fullLabel }
            ) { selectManager(it) }
            Divider(startIndent = itemDividerIndent())
            SettingItem(
                icon = Icons.Filled.Tune,
                title = "拓展功能",
                onClick = { showExtras = true },
                trailing = { Chevron() }
            )
            Divider(startIndent = itemDividerIndent())
            SettingItem(
                icon = Icons.Filled.MenuBook,
                title = "使用手册",
                summary = "首次使用、功能说明与异常处理",
                onClick = { showManual = true },
                trailing = { Chevron() }
            )
        }
    }

    @Composable
    private fun <T> DropdownItem(
        icon: ImageVector,
        title: String,
        values: List<T>,
        selected: T,
        label: (T) -> String,
        onSelect: (T) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val anchor = rememberCouiMenuAnchor()
        val colors = CouiPageDefaults.themeColors()
        Box {
            SettingItem(
                icon = icon,
                title = title,
                modifier = Modifier.couiMenuAnchor(anchor),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label(selected),
                            kind = CouiTextKind.PreferenceValue,
                            color = colors.summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Chevron()
                    }
                },
                onClick = { anchor.open { expanded = true } }
            )
            Menu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                items = values.map { CouiMenuItem(value = it, text = label(it), selected = it == selected) },
                onItemClick = { item ->
                    onSelect(item.value)
                    expanded = false
                },
                anchor = anchor
            )
        }
    }

    @Composable
    private fun InfoCard(
        kernelVersion: String,
        exploitLabel: String,
        deviceModel: String,
        fingerprint: String,
        selinux: String
    ) {
        val colors = CouiPageDefaults.themeColors()
        Group {
            SettingItem(Icons.Filled.DeveloperBoard, "内核版本", summary = kernelVersion)
            Divider(startIndent = itemDividerIndent(), color = colors.divider)
            SettingItem(Icons.Filled.BugReport, "漏洞利用方式", summary = exploitLabel)
            Divider(startIndent = itemDividerIndent(), color = colors.divider)
            SettingItem(Icons.Filled.Smartphone, "设备型号", summary = deviceModel)
            Divider(startIndent = itemDividerIndent(), color = colors.divider)
            SettingItem(Icons.Filled.Fingerprint, "系统指纹", summary = fingerprint)
            Divider(startIndent = itemDividerIndent(), color = colors.divider)
            SettingItem(Icons.Filled.Security, "SELinux 状态", summary = selinux)
        }
    }

    /* ===== Extras ===== */

    @Composable
    private fun ExtrasScreen(inner: PaddingValues) {
        val colors = CouiPageDefaults.themeColors()
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 4.dp,
                bottom = inner.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                Group {
                    if (exploit != ExploitKind.CVE2505A) {
                        SettingSwitchItem(
                            icon = Icons.Filled.NotificationsOff,
                            title = "越狱后禁用安全弹窗",
                            checked = antiRootGuard,
                            onCheckedChange = { updateAntiRootGuard(it) }
                        )
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                        SettingSwitchItem(
                            icon = Icons.Filled.RestartAlt,
                            title = "越狱后软重启",
                            checked = softRebootAfter,
                            onCheckedChange = { updateSoftRebootAfter(it) }
                        )
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                        SettingSwitchItem(
                            icon = Icons.Filled.RemoveModerator,
                            title = "越狱后卸载内核安全防护",
                            checked = unloadGuard,
                            onCheckedChange = { updateUnloadGuard(it) }
                        )
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    }
                    SettingSwitchItem(
                        icon = Icons.Filled.Extension,
                        title = "SUSFS 支持",
                        checked = susfsSupport,
                        onCheckedChange = { updateSusfsSupport(it) }
                    )
                    if (exploit == ExploitKind.CVE98260) {
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                        SettingSwitchItem(
                            icon = Icons.Filled.AccountTree,
                            title = "VFS 支持",
                            checked = vfsSupport,
                            onCheckedChange = { updateVfsSupport(it) }
                        )
                    }
                    if (supportsUniversal() && universalLoaded) {
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                        DropdownItem(
                            icon = Icons.Filled.SwapHoriz,
                            title = "管理器热切换",
                            values = hotManagerChoices(),
                            selected = hotManager,
                            label = { it.fullLabel }
                        ) { target ->
                            hotManager = target
                            switchHotManager(target)
                        }
                    }
                }
            }
            item {
                hotSwitchError?.let {
                    Text(
                        text = it,
                        kind = CouiTextKind.Description,
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp)
                    )
                }
            }
            if (exploit == ExploitKind.CVE98260) {
                item {
                    Group {
                        SettingItem(
                            icon = Icons.Filled.Extension,
                            title = "Droidspaces 支持",
                            summary = "下载最新的 droidspaces_lkm 模块，保存到 Download/Shark，之后到 KernelSU 管理器刷入。"
                        )
                        Divider(startIndent = itemDividerIndent(), color = colors.divider)
                        SettingItem(
                            icon = Icons.Filled.Download,
                            title = "下载模块",
                            onClick = { openDroidspacesDialog() },
                            trailing = { Chevron() }
                        )
                    }
                }
            }
        }
    }

    /* ===== Manual ===== */

    @Composable
    private fun ManualScreen(inner: PaddingValues) {
        val context = LocalContext.current
        val markdown = remember {
            runCatching {
                context.assets.open("manual.md").bufferedReader().use { it.readText() }
            }.getOrElse { "# 使用手册\n\n手册内容读取失败。" }
        }
        val blocks = remember(markdown) { parseManualMarkdown(markdown) }
        val codeBackground = CouiTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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

    /* ===== Logs ===== */

    @Composable
    private fun LogScreen(inner: PaddingValues) {
        val scheme = CouiTheme.colorScheme
        val state = logs.toList()
        if (state.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无日志",
                    kind = CouiTextKind.Body,
                    color = scheme.onSurfaceVariant
                )
            }
            return
        }
        val listState = rememberLazyListState()
        LaunchedEffect(state.size) {
            listState.scrollToItem(state.lastIndex)
        }
        val logStyle = rememberCouiTextStyle(CouiTextKind.Description).copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = scheme.onSurface
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 12.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state) { line -> BasicText(text = line, style = logStyle) }
        }
    }

    /* ===== History ===== */

    @Composable
    private fun HistoryScreen(inner: PaddingValues) {
        LaunchedEffect(Unit) { refreshHistory() }
        val entries = historyEntries
        val scheme = CouiTheme.colorScheme
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无记录",
                    kind = CouiTextKind.Body,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = inner.calculateTopPadding() + 4.dp,
                    bottom = inner.calculateBottomPadding() + 12.dp
                )
            ) {
                items(entries) { e ->
                    HistoryEntryCard(
                        entry = e,
                        onClick = {
                            val name = e.fileName
                            lifecycleScope.launch(Dispatchers.IO) {
                                val text = HistoryStore.read(applicationContext, name)
                                withContext(Dispatchers.Main) {
                                    historyDetailText = text
                                    historyDetail = e
                                }
                            }
                        },
                        onExport = { shareHistory(e) }
                    )
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

    /* 条目右侧滑出导出操作；卡片本体使用 COUI 卡片配色与列表按压反馈。 */
    @Composable
    private fun HistoryEntryCard(entry: HistoryEntry, onClick: () -> Unit, onExport: () -> Unit) {
        val scheme = CouiTheme.colorScheme
        val colors = CouiPageDefaults.themeColors()
        val dark = CouiTheme.isDark
        val scope = rememberCoroutineScope()
        val offset = remember { Animatable(0f) }
        val density = LocalDensity.current
        val actionWidth = with(density) { 88.dp.toPx() }
        val source = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            val reveal = (-offset.value).coerceIn(0f, actionWidth)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(with(density) { reveal.toDp() })
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(88.dp)
                            .fillMaxHeight()
                            .clip(CouiPageDefaults.shape)
                            .background(scheme.accent)
                            .clickable {
                                scope.launch { offset.animateTo(0f, tween(200)) }
                                onExport()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "导出", kind = CouiTextKind.PreferenceTitle, color = scheme.onAccent)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = offset.value }
                    .clip(CouiPageDefaults.shape)
                    .background(colors.card)
                    .couiButtonFeedback(dark, source, list = true)
                    .clickable {
                        if (offset.value < -1f) {
                            scope.launch { offset.animateTo(0f, tween(200)) }
                        } else {
                            onClick()
                        }
                    }
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
                    }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.manager,
                            kind = CouiTextKind.PreferenceTitle,
                            color = colors.title,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = entry.exploit,
                            kind = CouiTextKind.PreferenceSummary,
                            color = colors.summary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = entry.startTime,
                            kind = CouiTextKind.PreferenceSummary,
                            color = colors.summary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.basicMarquee()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val tagColors = listOf(
                                scheme.accent to scheme.onAccent,
                                scheme.accent.copy(alpha = 0.14f) to scheme.accent,
                                scheme.onSurface.copy(alpha = 0.08f) to scheme.onSurfaceVariant
                            )
                            historyTags(entry).forEachIndexed { index, tag ->
                                val (bg, fg) = tagColors.getOrElse(index) { tagColors.last() }
                                CouiStatusTag(label = tag, backgroundColor = bg, contentColor = fg)
                            }
                        }
                    }
                    Text(
                        text = historyValue(entry),
                        kind = CouiTextKind.PreferenceSummary,
                        color = colors.summary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    AppIcon(
                        CouiIcons.Next,
                        null,
                        Modifier.padding(start = 4.dp).size(18.dp),
                        tint = colors.summary
                    )
                }
            }
        }
    }

    @Composable
    private fun CouiStatusTag(label: String, backgroundColor: Color, contentColor: Color) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                kind = CouiTextKind.Category,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
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
        val scheme = CouiTheme.colorScheme
        Dialog(
            onDismissRequest = onDismiss,
            title = "${displayEntry.startTime}  ${displayEntry.manager}",
            confirmAction = CouiDialogAction(
                text = "确定",
                onClick = onDismiss,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(scheme.accent)
            ),
            dismissAction = CouiDialogAction(text = "删除", onClick = onDelete),
            visible = entry != null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
            ) {
                SelectionContainer {
                    Box(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        BasicText(
                            text = text.ifEmpty { "暂无日志" },
                            style = rememberCouiTextStyle(CouiTextKind.Body).copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = scheme.onSurface
                            )
                        )
                    }
                }
            }
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

    /* ===== About ===== */

    @Composable
    private fun AboutScreen(inner: PaddingValues) {
        AnimatedContent(
            targetState = aboutPage,
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
                AboutHomeScreen(inner)
            } else {
                LicenseListScreen(inner)
            }
        }
    }

    @Composable
    private fun AboutHomeScreen(inner: PaddingValues) {
        val uriHandler = LocalUriHandler.current
        val colors = CouiPageDefaults.themeColors()
        val scheme = CouiTheme.colorScheme
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 4.dp,
                bottom = inner.calculateBottomPadding() + 12.dp
            )
        ) {
            item {
                Group {
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
                            kind = CouiTextKind.SectionTitle,
                            color = colors.title
                        )
                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            kind = CouiTextKind.PreferenceSummary,
                            color = colors.summary
                        )
                    }
                }
            }
            item {
                Group {
                    Item(
                        icon = {
                            AppIcon(Icons.Filled.Info, null, Modifier.size(20.dp), tint = scheme.accent)
                        },
                        onClick = { aboutPage = AboutPage.LICENSES }
                    ) {
                        Text(
                            text = "App 图标由本项目绘制；第三方组件版权归各自作者所有，完整许可见「开放源代码许可」。",
                            kind = CouiTextKind.PreferenceSummary,
                            color = colors.title,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            item {
                Group {
                    SettingItem(
                        icon = Icons.Filled.Code,
                        title = "查看源代码",
                        summary = "在 GitHub 上查看源代码",
                        onClick = { uriHandler.openUri(PROJECT_URL) },
                        trailing = { Chevron() }
                    )
                    Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    SettingItem(
                        icon = Icons.Filled.Forum,
                        title = "加入社区",
                        summary = "加入 QQ 群",
                        onClick = { uriHandler.openUri("https://qm.qq.com/q/HMg4ngAC8W") },
                        trailing = { Chevron() }
                    )
                    Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    SettingItem(
                        icon = Icons.Filled.BugReport,
                        title = "问题反馈",
                        summary = "在 GitHub Issues 提交问题",
                        onClick = { uriHandler.openUri("$PROJECT_URL/issues") },
                        trailing = { Chevron() }
                    )
                    Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    SettingItem(
                        icon = Icons.Filled.Refresh,
                        title = "检查更新",
                        summary = "当前 v${BuildConfig.VERSION_NAME}",
                        onClick = { checkForUpdatesManually() },
                        trailing = { Chevron() }
                    )
                    Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    SettingItem(
                        icon = Icons.Filled.Favorite,
                        title = "支持开发者",
                        summary = "微信 / 支付宝赞赏二维码",
                        onClick = { showDonate = true },
                        trailing = { Chevron() }
                    )
                    Divider(startIndent = itemDividerIndent(), color = colors.divider)
                    SettingItem(
                        icon = Icons.Filled.Copyright,
                        title = "开放源代码许可",
                        summary = "查看本应用所使用的第三方开源库及其许可证信息",
                        onClick = { aboutPage = AboutPage.LICENSES },
                        trailing = { Chevron() }
                    )
                }
            }
        }
    }

    @Composable
    private fun LicenseListScreen(inner: PaddingValues) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 4.dp,
                bottom = inner.calculateBottomPadding() + 12.dp
            )
        ) {
            item { SectionLabel("本项目与附加组件") }
            items(localItems) { item ->
                LicenseCard(item) { aboutLicense = item }
            }
            item {
                SectionLabel(
                    if (libraryItems.isEmpty()) "第三方依赖"
                    else "第三方依赖（${libraryItems.size}）"
                )
            }
            items(libraryItems) { item ->
                LicenseCard(item) { aboutLicense = item }
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        val colors = CouiPageDefaults.themeColors()
        Text(
            text = text,
            kind = CouiTextKind.GroupTitle,
            color = colors.summary,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 4.dp)
        )
    }

    @Composable
    private fun LicenseChip(text: String) {
        val scheme = CouiTheme.colorScheme
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(scheme.accent.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(text = text, kind = CouiTextKind.Category, color = scheme.accent)
        }
    }

    @Composable
    private fun LicenseCard(item: LicenseItem, onClick: () -> Unit) {
        val colors = CouiPageDefaults.themeColors()
        val hasSummary = !item.author.isNullOrBlank() || item.licenses.isNotEmpty()
        Group {
            Item(
                onClick = onClick,
                summary = if (!hasSummary) null else {
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.author?.takeIf { it.isNotBlank() }?.let { author ->
                                Text(
                                    text = author,
                                    kind = CouiTextKind.PreferenceSummary,
                                    color = colors.summary,
                                    textAlign = TextAlign.Start
                                )
                            }
                            if (item.licenses.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item.licenses.take(3).forEach { LicenseChip(it.name) }
                                }
                            }
                        }
                    }
                },
                trailing = {
                    item.version?.takeIf { it.isNotBlank() }?.let { version ->
                        Text(
                            text = version,
                            kind = CouiTextKind.PreferenceSummary,
                            color = colors.summary,
                            maxLines = 1
                        )
                    }
                }
            ) {
                Text(
                    text = item.name,
                    kind = CouiTextKind.PreferenceTitle,
                    color = colors.title,
                    textAlign = TextAlign.Start
                )
            }
        }
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
        LOGS("日志", Icons.Filled.Notes),
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
