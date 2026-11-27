# 电脑端 adb 操作教程

不想装 Shizuku？插电脑用 adb 更直接——**不需要装 App，也不需要 Shizuku**，一条命令就能跑漏洞程序。

本文以 Windows 为主，macOS / Linux 命令相同，只是路径写法不同。

---

## 一、先在电脑装 adb

### Windows（推荐）

1. 下载官方 platform-tools：
   ```
   https://developer.android.com/tools/releases/platform-tools
   ```
2. 解压到 `C:\platform-tools`（路径别带中文）
3. 在该目录空白处 `Shift + 右键` → 「在此处打开 PowerShell 窗口」

验证：

```bat
.\adb version
```

看到 `Android Debug Bridge version 1.0.41` 之类就成功。

> 想在任何目录直接用 `adb`，可以把 `C:\platform-tools` 加进系统环境变量 PATH。

### macOS

```bash
brew install --cask android-platform-tools
adb version
```

### Linux

```bash
sudo apt install android-tools-adb      # Debian / Ubuntu
sudo dnf install android-tools          # Fedora
adb version
```

---

## 二、手机端准备

1. 设置 → 关于手机 → 连续点「版本号」7 次，开启开发者选项
2. 开发者选项里打开：
   - **USB 调试**
   - **USB 安装**（小米专有，不开发不了安装）
   - **USB 调试（安全设置）**（小米专有，改权限类命令需要）

> 小米这几个开关要求**登录小米账号 + 插 SIM 卡**，缺一个都会灰着。

3. 数据线连电脑，手机弹「允许 USB 调试吗？」→ 勾选「一律允许」→ 确定

验证：

```bat
adb devices
```

正常输出：

```
List of devices attached
4eff939a        device
```

- 显示 `unauthorized` → 手机上没点允许，重新插拔
- 列表为空 → 换线/换口，或装厂商驱动
- 显示 `offline` → 执行 `adb kill-server` 再 `adb devices`

---

## 三、开始操作（以小米 18 Pro 为例）

假设三个文件都在 `C:\platform-tools`：

```
temp-root.apk
timer
6.18.21-android17-5-g805aa6adea19-abogki576585092-4k.conf
```

### 1.（可选）清理上次残留

**第一次操作可以跳过这一步**，直接看第 2 步。

只有下面这些情况才需要清理：

- 上一次执行失败或中途 `Ctrl+C` 了
- 换了一份新的 conf 想覆盖
- 想确认环境干净、排除旧文件干扰

```bat
adb shell rm -rf /data/local/tmp/timer /data/local/tmp/timer.conf /data/local/tmp/ksu_stage.sh /data/local/tmp/timer.log
```

### 2. 把文件推进手机

```bat
adb push timer /data/local/tmp/timer
adb shell chmod 755 /data/local/tmp/timer
adb push 6.18.21-android17-5-g805aa6adea19-abogki576585092-4k.conf /data/local/tmp/timer.conf
```

### 3. 执行

```bat
adb shell /data/local/tmp/timer --conf /data/local/tmp/timer.conf fake
```

会看到类似输出：

```
[+] device=hongkong kernel=6.18.21-android17-5-g805aa6adea19-abogki576585092-4k
[ids] page_alloc=318 page_free=320 pairs=1029
[+] slab geometry confirmed by slabinfo: order=1 size=0x2000 obj=384 per=21
[+] plant1 pair pfn=0x9622b2 ... agree=1
[ksu] manager=... ksud=... version=ksud 3.3.1
[ksu] late-load rc=0
KSU_LOADED
```

**看到 `KSU_LOADED` 就是成功**，然后这个窗口会停住不动——正常，进程故意挂着维持凭据。

> 这个窗口别关、别 `Ctrl+C`、也别重启，一直到你把事情做完。

### 4. 验证

另开一个窗口：

```bat
adb shell su -c id
```

输出里出现：

```
uid=0(root) ... context=u:r:ksu:s0
```

就对了。

### 5. 然后呢

**别急着敲 `adb reboot`。** root 只存在于本次开机，一重启就没了——刚拿到的权限会立刻作废。

拿到 root 之后该干什么就干什么：

```bat
adb shell su -c id                     :: 确认权限
adb install KernelSU_Next_xxx.apk      :: 装管理器
adb shell su -c 'ls /data/adb/modules' :: 看模块
```

真正想还原的时候（用完了、或者要开网银了）再重启：

```bat
adb reboot
```

重启后 root、KernelSU 模块、SELinux 状态全部还原，只有 `/data/local/tmp` 里的文件会留下，想清就执行第 1 步。

> 顺序别搞反：**先拿 root → 做要做的事 → 最后才 reboot**。

---

## 四、日志与排错

| 现象 | 含义 | 处理 |
|---|---|---|
| `KSU_LOADED` | 成功 | 完事 |
| `bad base` / `base calibration failed` | 内核状态不干净，抓不到分配样本 | **重启手机后立刻重试** |
| `KSU_MISSING` | 没装 KernelSU 管理器，或 KMI 不匹配 | 装 `android17-6.18` 的管理器 |
| `late-load rc=非0` | LKM 加载被拒 | 重启再试；仍失败说明内核禁了未签名模块 |
| `timer: inaccessible or not found` | 文件没推进去 | 重做第 2 步 |
| 命令一直不返回 | 正常，进程挂起 | 别 Ctrl+C，另开窗口验证 |

`Ctrl+C` 会杀掉挂起的进程，可能导致内核崩溃——**要么留着，要么直接 `adb reboot`**。

---

## 五、adb 常用命令速查

```bat
adb devices                       :: 看设备
adb shell <命令>                   :: 在手机执行命令
adb push 本地 手机路径               :: 电脑 → 手机
adb pull 手机路径 本地               :: 手机 → 电脑
adb install -r app.apk            :: 安装/覆盖安装
adb uninstall 包名                 :: 卸载
adb reboot                        :: 重启
adb reboot bootloader             :: 进 fastboot
adb logcat -s TAG                 :: 看日志
adb shell pm list packages | findstr 关键字
```

---

## 六、安装 APK 时报错怎么办

| 错误 | 原因 | 解决 |
|---|---|---|
| `INSTALL_FAILED_USER_RESTRICTED` | 小米关了「USB 安装」 | 开发者选项里打开 |
| `INSTALL_FAILED_VERIFICATION_FAILURE` | 系统校验拦截 | 关闭「MIUI 优化」或允许未知来源 |
| `Failure [-124]` | APK 的 `resources.arsc` 被压缩 | 用仓库 `build.sh` 重新构建，别手工重打包 |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | 版本号比已装的小 | 加 `-d` 参数或先卸载 |

---

## 七、adb 和 Shizuku 怎么选

| | adb | Shizuku |
|---|---|---|
| 需要电脑 | 要 | 不要 |
| 需要装 App | 不要 | 要 |
| 每次重启后 | 插线即可 | 手机上重新配对无线调试 |
| 适合 | 调试、一次性操作 | 日常使用 |

两者本质一样：都是拿到 **shell 身份（uid 2000）** 来执行漏洞程序。
