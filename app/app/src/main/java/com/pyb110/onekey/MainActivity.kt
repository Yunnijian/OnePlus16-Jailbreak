package com.pyb110.onekey

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType

class MainActivity : ComponentActivity() {
    private val logs = mutableStateListOf<String>()
    private val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    private var busy by mutableStateOf(false)
    private var authorized by mutableStateOf(false)
    private var binderReady by mutableStateOf(false)
    private var ksuActive by mutableStateOf(false)
    private var exploit by mutableStateOf(ExploitKind.CVE64560)
    private var manager by mutableStateOf(ManagerKind.KERNELSU)
    private var kernelVersion by mutableStateOf("未知")
    private var deviceModel by mutableStateOf("未知")
    private var selinuxText by mutableStateOf("未知")
    private var antiRootGuard by mutableStateOf(true)
    private var softRebootAfter by mutableStateOf(false)
    private var unloadGuard by mutableStateOf(false)
    private var showLogs by mutableStateOf(false)

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU) {
            authorized = result == PackageManager.PERMISSION_GRANTED
            addLog(if (authorized) "Shizuku 已授权" else "Shizuku 授权被拒绝")
            if (authorized) refreshKsuAsync()
        }
    }
    private val binderListener = Shizuku.OnBinderReceivedListener {
        binderReady = Shizuku.pingBinder()
        refreshAuth()
        refreshKsuAsync()
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
        binderReady = Shizuku.pingBinder()
        refreshAuth()
        antiRootGuard = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("antiroot_guard", true)
        softRebootAfter = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("soft_reboot_after", false)
        unloadGuard = getSharedPreferences("pyb110", MODE_PRIVATE).getBoolean("unload_guard", false)
        val savedExploit = getSharedPreferences("pyb110", MODE_PRIVATE).getString("exploit", null)
        exploit = runCatching { ExploitKind.valueOf(savedExploit ?: "") }.getOrDefault(ExploitKind.CVE64560)
        val savedManager = getSharedPreferences("pyb110", MODE_PRIVATE).getString("manager", null)
        manager = runCatching { ManagerKind.valueOf(savedManager ?: "") }.getOrDefault(ManagerKind.KERNELSU)
        kernelVersion = runCatching { android.system.Os.uname().release }.getOrDefault("未知")
        deviceModel = resolveDeviceName()
        refreshStaticStateAsync()
        refreshKsuAsync()

        setContent {
            val themeController = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }
            MiuixTheme(themeController) {
                if (showLogs) {
                    LogScreen(state = logs.toList(), onBack = { showLogs = false })
                } else {
                    val scrollBehavior = MiuixScrollBehavior()
                    Scaffold(
                        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
                        topBar = {
                            TopAppBar(
                                title = "OnePlus 16 Jailbreak",
                                scrollBehavior = scrollBehavior,
                                navigationIcon = {
                                    IconButton(onClick = { showLogs = true }) {
                                        Icon(
                                            imageVector = MiuixIcons.Notes,
                                            contentDescription = "日志",
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            )
                        }
                    ) { inner ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .padding(inner)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                StatusCard(granted = binderReady && authorized)
                            }
                            item {
                                TextButton(
                                    text = when {
                                        busy -> "运行中…"
                                        !binderReady -> "Shizuku 未运行（点击打开）"
                                        !authorized -> "授权 Shizuku"
                                        ksuActive -> "校验 / 再次越狱"
                                        else -> "一键越狱"
                                    },
                                    onClick = {
                                        if (busy) return@TextButton
                                        when {
                                            !binderReady -> {
                                                addLog("请先启动 Shizuku（无线调试）")
                                                openShizuku()
                                            }
                                            !authorized -> Shizuku.requestPermission(REQ_SHIZUKU)
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
                                            items = ManagerKind.entries.map { it.fullLabel },
                                            selectedIndex = manager.ordinal,
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = MiuixTheme.colorScheme.onBackground
                                                )
                                            },
                                            onSelectedIndexChange = { selectManager(ManagerKind.entries[it]) }
                                        )
                                        HorizontalDivider()
                                        WindowDropdownPreference(
                                            title = "漏洞利用方式",
                                            items = ExploitKind.entries.map { it.fullLabel },
                                            selectedIndex = exploit.ordinal,
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Security,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = MiuixTheme.colorScheme.onBackground
                                                )
                                            },
                                            onSelectedIndexChange = { selectExploit(ExploitKind.entries[it]) }
                                        )
                                        HorizontalDivider()
                                        SwitchPreference(
                                            title = "越狱后禁用安全弹窗",
                                            checked = antiRootGuard,
                                            onCheckedChange = { updateAntiRootGuard(it) },
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Security,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = MiuixTheme.colorScheme.onBackground
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
                                                    tint = MiuixTheme.colorScheme.onBackground
                                                )
                                            }
                                        )
                                        HorizontalDivider()
                                        SwitchPreference(
                                            title = "越狱后卸载内核安全防护",
                                            checked = unloadGuard,
                                            onCheckedChange = { updateUnloadGuard(it) },
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Filled.Security,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    tint = MiuixTheme.colorScheme.onBackground
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            item {
                                InfoCard(
                                    kernelVersion = kernelVersion,
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
    }

    @Composable
    private fun StatusCard(granted: Boolean) {
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
                            text = if (granted) "Shizuku 已授权" else "Shizuku 未授权",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (granted) "已连接到 Shizuku，可尝试越狱" else "未连接到 Shizuku，请授予权限",
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
                tint = MiuixTheme.colorScheme.onSurface
            )
            Column {
                Text(
                    text = title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = content,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    @Composable
    private fun InfoCard(
        kernelVersion: String,
        deviceModel: String,
        fingerprint: String,
        selinux: String,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.DeveloperBoard, "内核版本", kernelVersion)
                InfoText(Icons.Filled.Smartphone, "设备型号", deviceModel)
                InfoText(Icons.Filled.Fingerprint, "系统指纹", fingerprint)
                InfoText(Icons.Filled.Security, "SELinux 状态", selinux, bottomPadding = 0.dp)
            }
        }
    }

    @Composable
    private fun LogScreen(state: List<String>, onBack: () -> Unit) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = "日志",
                    actions = { TextButton(text = "返回", onClick = onBack) }
                )
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
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(state.size) {
                    listState.scrollToItem(state.lastIndex)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state) { line -> Text(line, fontSize = 11.sp) }
                }
            }
        }
    }


    private fun addLog(line: String) {
        runOnUiThread {
            logs.add("${timeFmt.format(java.util.Date())}  $line")
            if (logs.size > 4000) logs.removeRange(0, logs.size - 4000)
        }
    }

    private fun refreshAuth() {
        authorized = Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    private fun refreshKsuAsync() {
        if (!Shizuku.pingBinder()) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val binder = Shizuku.getBinder() ?: return@launch
                val service = IShizukuService.Stub.asInterface(binder) ?: return@launch
                val (active, se) = readDeviceState(service)
                withContext(Dispatchers.Main) {
                    ksuActive = active
                    selinuxText = se
                }
            } catch (t: Throwable) {
                // binder 未就绪时静默
            }
        }
    }

    private fun readDeviceState(service: IShizukuService): Pair<Boolean, String> {
        val p = service.newProcess(
            arrayOf("sh", "-c", "grep -qi kernelsu /proc/modules && echo YES || echo NO; getenforce"),
            shizukuEnv(), "/data/local/tmp"
        )
        val lines = ParcelFileDescriptor.AutoCloseInputStream(p.getInputStream())
            .bufferedReader().readLines()
        p.waitFor()
        val active = lines.getOrNull(0)?.trim() == "YES"
        val se = mapSelinux(lines.getOrNull(1)?.trim().orEmpty())
        return active to se
    }

    private fun updateAntiRootGuard(value: Boolean) {
        antiRootGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("antiroot_guard", value).apply()
        addLog("安全弹窗守卫: ${if (value) "开启（禁用 5 秒后自动恢复）" else "关闭"}")
    }

    private fun updateSoftRebootAfter(value: Boolean) {
        softRebootAfter = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("soft_reboot_after", value).apply()
        addLog("越狱后软重启: ${if (value) "开启" else "关闭"}")
    }

    private fun updateUnloadGuard(value: Boolean) {
        unloadGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("unload_guard", value).apply()
        addLog("越狱后卸载内核安全防护: ${if (value) "开启" else "关闭"}")
    }

    private fun selectExploit(kind: ExploitKind) {
        exploit = kind
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putString("exploit", kind.name).apply()
        addLog("已选择漏洞利用: ${kind.fullLabel}")
    }

    private fun selectManager(kind: ManagerKind) {
        manager = kind
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putString("manager", kind.name).apply()
        addLog("已选择 ROOT 管理器: ${kind.fullLabel}")
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

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) startActivity(intent) else addLog("未找到 Shizuku 应用")
    }

    private fun shizukuEnv(): Array<String> = arrayOf(
        "PATH=/system/bin:/system/xbin:/data/adb/ksu/bin",
        "HOME=/data/local/tmp",
        "TMPDIR=/data/local/tmp"
    )

    private fun runFlow() {
        busy = true
        val softReboot = softRebootAfter
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val binder = Shizuku.getBinder()
                if (binder == null) {
                    addLog("Shizuku binder 为空")
                    return@launch
                }
                val service = IShizukuService.Stub.asInterface(binder)
                if (service == null) {
                    addLog("Shizuku service 为空")
                    return@launch
                }
                val dir = deployDir(service)
                addLog("部署工具到 $dir …")
                stageAssets(service, dir, deployPlan())
                addLog("开始执行 ${exploit.script}")
                val rc = runStreaming(service, arrayOf("sh", "$dir/${exploit.script}"), dir)
                addLog("run.sh 退出码=$rc")
                val (active, se) = readDeviceState(service)
                withContext(Dispatchers.Main) {
                    ksuActive = active
                    selinuxText = se
                }
                addLog(if (active) "KernelSU 模块已加载" else "KernelSU 模块未加载")
                if (softReboot && rc == 0 && active) {
                    addLog("越狱完成，执行软重启（重启 zygote）")
                    val rebootRc = runCatching {
                        service.newProcess(
                            arrayOf("su", "-c", "setprop ctl.restart zygote"),
                            shizukuEnv(), "/data/local/tmp"
                        ).waitFor()
                    }.getOrDefault(-1)
                    if (rebootRc != 0) addLog("软重启命令失败 rc=$rebootRc")
                }
            } catch (t: Throwable) {
                addLog("ERROR: $t")
            } finally {
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    private fun deployPlan(): List<Pair<String, String>> {
        val common = when (exploit) {
            ExploitKind.CVE64560 -> listOf(
                "prep_solve", "prep_patch", "t64560_v36", "t64560_loader2", "t64560_w2", "forker", "run.sh"
            )
            ExploitKind.CVE98260 -> listOf(
                "brm_plant", "pc98260_su", "prep_solve", "prep_patch", "forker", "run98260.sh"
            )
        }
        val (ko, ksud) = when (manager) {
            ManagerKind.KERNELSU -> "ksu-base.ko" to "ksud_official"
            ManagerKind.NEXT -> "ksu-base-next.ko" to "ksud_next"
            ManagerKind.BAKASU -> "ksu-base-bakasu.ko" to "ksud_bakasu"
            ManagerKind.SUKISU -> "ksu-base-sukisu.ko" to "ksud_sukisu"
        }
        return common.map { it to it } + listOf(
            "syms.txt" to "syms.txt",
            "take_slide.sh" to "take_slide.sh",
            ko to "ksu-base.ko",
            ksud to "ksud_official"
        )
    }

    private fun deployDir(service: IShizukuService): String {
        val id = try {
            val p = service.newProcess(
                arrayOf("cat", "/proc/sys/kernel/random/boot_id"),
                shizukuEnv(), "/data/local/tmp"
            )
            val text = ParcelFileDescriptor.AutoCloseInputStream(p.getInputStream())
                .bufferedReader().readText().trim()
            p.waitFor()
            text.replace("-", "").take(12)
        } catch (t: Throwable) {
            ""
        }
        val tag = id.ifEmpty { "x" }
        return "/data/local/tmp/pyb110-$tag-${System.currentTimeMillis() / 1000}"
    }

    private fun stageAssets(service: IShizukuService, dir: String, files: List<Pair<String, String>>) {
        val stale = "killall -9 t64560_loader2 t64560_w2 t64560_v36 brm_plant 2>/dev/null"
        runCatching {
            service.newProcess(
                arrayOf("sh", "-c", stale),
                shizukuEnv(), "/data/local/tmp"
            ).waitFor()
        }
        val mk = service.newProcess(
            arrayOf("sh", "-c", "mkdir -p '$dir' && touch '$dir/.probe' && rm '$dir/.probe'"),
            shizukuEnv(), "/data/local/tmp"
        ).waitFor()
        if (mk != 0) {
            addLog("部署目录不可写: $dir rc=$mk")
            throw java.io.IOException("deploy dir not writable: $dir")
        }

        for ((assetName, target) in files) {
            val p = service.newProcess(
                arrayOf("sh", "-c", "cat > '$dir/$target'"),
                shizukuEnv(), "/data/local/tmp"
            )
            var copied = 0L
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(p.getOutputStream()).use { out ->
                    assets.open("pyb110/$assetName").use { copied = it.copyTo(out) }
                }
            } catch (t: java.io.IOException) {
                addLog("写入 $target 失败（目录/文件权限？）: ${t.message}")
                throw t
            }
            val rc = p.waitFor()
            if (rc != 0) addLog("部署失败 $target rc=$rc") else addLog("已部署 $target (${copied} B)")
        }

        service.newProcess(
            arrayOf("sh", "-c", "chmod 755 " + files.joinToString(" ") { "'$dir/${it.second}'" }),
            shizukuEnv(), "/data/local/tmp"
        ).waitFor()
    }

    private fun runStreaming(service: IShizukuService, cmd: Array<String>, dir: String): Int {
        val env = shizukuEnv() + "PYB110_DIR=$dir" + "PYB110_MANAGER=${manager.manager}" +
            "PYB110_LATE_LOAD=1" +
            "PYB110_UNLOAD_GUARD=${if (unloadGuard) "1" else "0"}" +
            "PYB110_DISABLE_ANTIROOT=${if (antiRootGuard) "1" else "0"}"
        val p = service.newProcess(cmd, env, dir)
        val out = ParcelFileDescriptor.AutoCloseInputStream(p.getInputStream())
        val err = ParcelFileDescriptor.AutoCloseInputStream(p.getErrorStream())
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
        super.onDestroy()
    }

    private enum class ExploitKind(val fullLabel: String, val script: String) {
        CVE64560("CVE-2026-64560", "run.sh"),
        CVE98260("CVE-2026-98260", "run98260.sh")
    }

    private enum class ManagerKind(val fullLabel: String, val pkg: String, val manager: String) {
        KERNELSU("KernelSU", "me.weishu.kernelsu", "kernelsu"),
        NEXT("KernelSU Next", "com.rifsxd.ksunext", "next"),
        BAKASU("BakaSU", "com.resukisu.resukisu", "bakasu"),
        SUKISU("SukiSU Ultra", "com.sukisu.ultra", "sukisu")
    }

    companion object {
        private const val REQ_SHIZUKU = 1001
    }
}
