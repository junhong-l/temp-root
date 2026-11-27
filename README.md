# 临时ROOT（temp-root）

一个**不含任何漏洞代码**的 Android 壳应用：通过 [Shizuku](https://shizuku.rikka.app/) 以 shell 身份执行你提供的**漏洞程序**与**内核配置**，从而在本次开机内获得 root。

> 适用于「不想解锁 Bootloader、只想临时用一下 root」的场景。**重启即恢复原状。**

---

## 文档

| 文档 | 内容 |
|---|---|
| [电脑端 adb 教程](docs/adb-tutorial.md) | 不装 App、不用 Shizuku，插线就能跑，含排错表 |
| [漏洞程序与内核配置](docs/conf.md) | conf 怎么来、没有现成的怎么自己提取 |

---

## 它不是什么

- ❌ 不是 Magisk / KernelSU 的管理器
- ❌ 不含任何提权代码（漏洞程序由你自己提供）
- ❌ 不修改系统分区、不写入 boot
- ❌ 不保证任何机型可用

## 它是什么

一个遥控器。它做四件事：

1. 连接 Shizuku，拿到 shell 身份（uid 2000）
2. 把你选的文件推进 `/data/local/tmp`
3. 执行 `漏洞程序 --conf <你的配置> fake`
4. 把执行日志实时显示在界面上

---

## 前置条件

| 项 | 说明 |
|---|---|
| **Shizuku** | 必须安装并运行（用「无线调试」启动即可，不需要电脑） |
| **漏洞程序** | 一个 ARM64 可执行文件，详见 [docs/conf.md](docs/conf.md) |
| **内核配置（conf）** | 必须与当前内核版本严格匹配，详见 [docs/conf.md](docs/conf.md) |

App 内置了漏洞程序的一键下载（约 2.1 MB），但 **conf 必须自己准备**——它和内核版本一一对应，没有通用版。

---

## 使用

1. 安装 APK，打开
2. 点「连接 Shizuku」→ 在 Shizuku 里授权
3. 点「下载漏洞程序」（或用「手动选择」指定本地文件）
4. 点「选择 conf 文件」→ 选中与当前内核匹配的 `.conf`
5. 点「获取临时 Root」→ 阅读警告 → 确认
6. 下方「运行日志」会实时显示过程

> 不想装 App？插电脑用 adb 也能跑，见 [电脑端 adb 教程](docs/adb-tutorial.md)。

成功后验证：

```bash
adb shell su -c id
# uid=0(root) ... context=u:r:ksu:s0
```

---

## 验证成功与否

| 现象 | 含义 |
|---|---|
| 日志出现 `KSU_LOADED` | KernelSU 已加载，`su` 可用 |
| 日志出现 `bad base` / `base calibration failed` | 内核状态不干净，**重启后再试** |
| 日志出现 `KSU_MISSING` | 没装 KernelSU 管理器，或 KMI 不匹配 |
| 日志刷完就停住不退出 | 正常，漏洞进程会「挂起」以维持凭据 |

> 每次开机**只能执行一次**。失败请重启，不要连点。

---

## 构建

```bash
# 依赖：JDK 11+、Android SDK build-tools 34+、platforms/android-34+
./build.sh                 # 输出 dist/temp-root.apk
```

脚本会自动从 Maven Central 拉取 Shizuku API（`dev.rikka.shizuku:api`），首次构建会生成签名密钥 `keystore.jks`（口令 `temproot`）。

---

## 目录结构

```
temp-root/
├── app/
│   ├── AndroidManifest.xml
│   ├── res/mipmap-*/           应用图标
│   └── src/com/kpgc/troot/
│       └── MainActivity.java   全部界面与逻辑（约 500 行）
├── docs/
│   ├── adb-tutorial.md         电脑端 adb 操作教程
│   └── conf.md                 漏洞程序与内核配置怎么准备
├── build.sh
└── LICENSE
```

---

## 安全与免责

- 本工具**仅在 小米 18 Pro（hongkong，内核 6.18.21-android17-5-g805aa6adea19-abogki576585092）** 上验证成功
- 其他机型、其他内核版本**不保证可用**；错误的 conf 可能导致内核崩溃、重启甚至数据丢失
- 使用后 **SELinux 可能处于宽容模式**，期间不要安装来源不明的应用
- 请仅在**你自己的设备**上使用；使用本工具造成的一切后果由使用者承担
- 本项目不含漏洞利用代码，仅提供执行框架

## 许可

MIT
