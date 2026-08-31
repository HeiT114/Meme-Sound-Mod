# Music Player Mod（音乐播放模组）

一个 **Minecraft 26.1.2 / Fabric** 模组，功能：

- 🕊️ **开始飞行时**（创造飞行/鞘翅）→ 循环播放 `中国人能飞`，**停止飞行立即停止**
- 💀 **死亡时** → 播放一次 `Tuco Get Out`（不循环）；点确认死亡并重生立即停止
- ⌨️ **同时按住 W + S** → 循环播放 `Du Bist Gut Genug`，**松开任意键立即停止**
- 🧨 **按 C 键** → 播放 `vine boom`（按住不会重复触发）
- 😴 **10 分钟无操作** → 播放一次 `A Few Moments Later`，屏幕中央显示「这个玩家睡着了」2 秒
- 🏆 **达成成就/挑战** → 播放 `Oh My God`（任意成就/进度/挑战，通过 Mixin 监听，不受原版成就界面影响）
- 🐱 **杀死猫** → 播放 `Sad Meow`，画面变黑白（约 25 秒，与音效同步），播放完恢复
- 🍔 **吃东西时** → 循环播放 `Gogogogogogo`，**停止/吃完立即停止**
- ⚙️ **按 K 键** → 打开设置界面（音量 + 各音效独立开关 + **语言选择** + 版本号 + **作者主页**）

音乐只在本机播放（不会广播给服务器上的其他玩家）。

## ⚙️ 设置界面

- 默认按 **K** 键打开
- **音量滑条**：0% ~ 100%，作用于所有音效
- **语言选择**：中文 / English 一键切换（界面文字、空闲提示文字跟随）
- **音效开关**：飞行 / 死亡 / W+S / C 键 / 空闲(10分钟) / 成就 / 杀猫 / 吃 可独立开启或关闭（两列布局）
- **作者主页**：界面最下方按钮，点击打开浏览器访问 B 站主页
- **重置按钮**：一键恢复默认（100% 音量 + 全部开启）
- **版本号显示**：读取自 fabric.mod.json
- 配置保存到 `.minecraft/config/music_player_mod.json`，点「保存并关闭」生效

---

## 环境要求

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.155.2+26.1.2 |
| Java | 25+（本机 JDK 26 可用） |
| Gradle | 9.5.1（由 wrapper 自动下载） |

> 26.1 是 Mojang 新的年份版本号（2026 年 1 月发布），游戏本体已**不再混淆**，
> 因此本模组直接使用 **Mojang 官方映射** 编写，无需 Yarn mappings。

## 如何构建

### 方式一：命令行（Windows）

```bat
gradlew.bat build
```

首次运行会自动下载 Gradle 9.5.1 和全部依赖（需联网，耗时数分钟）。
构建产物位于 `build/libs/music-player-mod-1.0.0.jar`。

> 注意：构建产物 **不需要** 重新映射（26.1 起 `jar` 即最终产物，无需 `remapJar`）。

### 方式二：IntelliJ IDEA

1. `File → Open` 选择本目录
2. 等待 Gradle 同步完成（首次会下载依赖）
3. 右侧 Gradle 面板执行 `build` 任务

## 安装与使用

1. 安装 [Fabric Loader](https://fabricmc.net/use/installer/)（选择 26.1.2）
2. 下载 [Fabric API](https://modrinth.com/mod/fabric-api)（`0.155.2+26.1.2`）放进 `mods` 文件夹
3. 把构建出的 `music-player-mod-1.0.0.jar` 也放进 `.minecraft/mods` 文件夹
4. 启动游戏：
   - 按两次空格进入飞行（创造模式/鞘翅）→ 听到《中国人能飞》
   - 死亡 → 听到《See You Again》

## 项目结构

```
src/main/java/com/example/musicplayermod/
├── MusicPlayerMod.java          # 主入口：注册两个声音事件
└── ModSound.java                # 声音注册辅助类

src/client/java/com/example/musicplayermod/client/
└── MusicPlayerModClient.java    # 客户端入口：每刻检测飞行/死亡并播放声音

src/main/resources/
├── fabric.mod.json              # 模组元数据
└── assets/music_player_mod/
    ├── sounds.json              # 声音定义（key → 文件）
    ├── lang/en_us.json          # 字幕翻译
    ├── icon.png                 # 模组图标
    └── sounds/                  # 音频文件
        ├── chinese_can_fly.mp3
        └── see_you_again.mp3
```

## 技术说明

- **检测飞行**：`LocalPlayer#getAbilities().flying`（客户端能力状态，创作/鞘翅飞行均有效）
- **检测死亡**：`LocalPlayer#isDeadOrDying()`
- **播放声音**：`Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(...))`
  —— 纯客户端 API，只影响本机
- **触发方式**：上升沿检测（`wasFlying` / `wasDead` 状态翻转），同一状态下只播放一次

## 自定义音乐

想换音乐？用 ffmpeg 把音频转成 **OGG（Vorbis，单声道）** 后放入
`src/main/resources/assets/music_player_mod/sounds/`，命名为
`chinese_can_fly.ogg` / `see_you_again.ogg`，重新构建即可：

```bat
ffmpeg -y -i 你的音乐.mp3 -vn -ac 1 -ar 44100 -c:a libvorbis -q:a 5 ^
  src\main\resources\assets\music_player_mod\sounds\chinese_can_fly.ogg
```

> ⚠️ **Minecraft 26.1 已移除 mp3/wav 支持，只支持 `.ogg`**！
> 本项目已把自带的 mp3 转好为 ogg。若换音乐请务必转成 ogg，否则无声。
> 单声道 + 44.1kHz 为推荐规格（立体声会强制下混，不影响播放）。
> 转换时加 `-vn` 忽略 mp3 内嵌封面，否则部分 ffmpeg 版本会崩溃。
