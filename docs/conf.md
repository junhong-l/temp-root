# 漏洞程序与内核配置

本 App 需要两个外部文件。它们都**不在**仓库里。

---

## 一、漏洞程序（约 2.1 MB）

一个 ARM64 Linux 可执行文件（ELF），负责真正的提权。

- 文件名通常就叫 `timer`
- 用 `file` 查看应显示：`ELF 64-bit LSB executable, ARM aarch64`
- App 内点「下载漏洞程序」可自动获取；也可手动下载后用「手动选择」指定

> 它必须能在 **shell 身份（uid 2000）** 下运行，所以 App 会先把它推进 `/data/local/tmp` 并 `chmod 755`。

---

## 二、内核配置（conf）

这个文件描述**当前内核的精确布局**（函数地址、结构体偏移等），**与内核版本一一对应**，换内核就失效。

### 怎么判断你需要哪一份

```bash
adb shell uname -r
```

输出类似：

```
6.18.21-android17-5-g805aa6adea19-abogki576585092-4k
```

**conf 的文件名就是这个字符串**（末尾 `-4k` 可带可不带），内容可以打开看，第一行通常写着 `release = "..."`。

### 情况 A：有人已经做好了

直接找同名 conf 文件即可。

### 情况 B：没有现成的，需要自己提取

1. **拿到本机固件的 `boot.img`**
   - 线刷包：`tar -xzf xxx_images_*.tgz images/boot.img`
   - 卡刷包：解出 `payload.bin`，再 `payload-dumper-go -p boot payload.bin`
2. **确认内核版本**
   ```bash
   strings -a boot.img | grep -m1 -oE '6\.[0-9]+\.[0-9]+-android[0-9]+-[0-9]+-g[0-9a-f]+(-abogki[0-9]+)?'
   ```
   应与 `uname -r` 完全一致（不一致说明拿错包了）
3. **恢复符号表**
   ```bash
   vmlinux-to-elf boot.img_kernel vmlinux.elf
   nm vmlinux.elf | grep -E 'selinux_state|panic_on_warn|init_user_ns'
   ```
   用几个已知符号交叉验证基址是否正确（与同代际现成 conf 对比）
4. **定位关键地址**
   - `posix_timers_cache`：在 `posixtimer_init` 里找 `kmem_cache_create` 后 `str x0, [x9, #imm]` 的落点
   - `timer_site`：`do_timer_create` 中 `ldr x0, [xN, #imm]` + `bl kmem_cache_alloc_noprof` 的**返回地址**
   - `copy_process_low21`：`dup_task_struct` 中 `bl kmem_cache_alloc_node_noprof` 返回地址的低 21 位
5. 其余字段（cred 布局、task_struct 大小、SELinux blob 偏移）通常可沿用同内核代际的值

> 这一步需要一定的逆向基础。做不了就等别人适配，**不要随便套用别的内核的 conf**——偏移错了会直接把内核写崩。

---

## 三、常见问题

**Q：日志里 `bad base` / `base calibration failed`**
A：内核状态不干净（缓存里已有空闲对象），**重启手机后立刻重试**。

**Q：`KSU_MISSING`**
A：没装 KernelSU 管理器，或它的 KMI 与内核对不上。安装 `android17-6.18` 对应的管理器。

**Q：执行完日志停住不动**
A：正常。漏洞进程会挂起以维持伪造凭据，**不要结束它**，用完重启即可。

**Q：重启后 root 还在吗**
A：不在。内核模块、伪造凭据、SELinux 状态全部随重启清除，只有 `/data/local/tmp` 下的文件会留下。

**Q：安装时报 `-124`**
A：APK 的 `resources.arsc` 被压缩了。用本仓库 `build.sh` 构建（脚本会正确对齐），不要手工重打包。
