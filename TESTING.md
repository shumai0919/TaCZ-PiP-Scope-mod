# PrismLauncherテスト手順

## 1. 専用インスタンスを作る

1. PrismLauncherで「インスタンスを追加」を選択します。
2. インスタンス名を `TaCZ-PiP-Dev-1.20.1` にします。
3. Minecraft `1.20.1` を選び、Forge `47.4.20` を導入します。
4. インスタンス設定のJavaでJava 17を選択します。

最初はEmbeddium、Oculus、Distant Horizons、Shaderを追加せず、最小構成を維持します。

## 2. TaCZを導入する

PrismLauncherの「Modをダウンロード」からTaCZを検索するか、CurseForgeからMinecraft 1.20.1 / Forge向けの `tacz-1.20.1-1.1.8-hotfix2.jar`（file ID `9037989`）を取得し、インスタンスの `.minecraft/mods` に入れます。異なるMinecraft版やNeoForge版と混同しないでください。

## 3. 開発版を配置する

プロジェクトルートで以下を実行すると、ビルド、旧開発JAR削除、新JAR配置を一度に行えます。

```powershell
$env:PRISM_INSTANCE_DIR = '<Prismのテスト用インスタンスのパス>'
.\tools\deploy-to-prism.ps1
```

または引数で指定できます。

```powershell
.\tools\deploy-to-prism.ps1 -PrismInstanceDir '<Prismのテスト用インスタンスのパス>'
```

指定するのは `.minecraft` または `minecraft` の親であるインスタンスディレクトリです。スクリプトがどちらの形式かを自動判定します。空白や日本語を含むパスも引用符で囲めば使用できます。ビルド済みJARだけを再配置する場合は `-SkipBuild` を追加します。

## 4. 起動確認

1. PrismLauncherでインスタンスを起動します。
2. Mods画面に `TaCZ PiP Scope` とTaCZが表示されることを確認します。
3. タイトル画面まで到達後、ログで次を検索します。

```text
[TaCZ PiP Scope] Mod loaded successfully
[TaCZ PiP Scope] TaCZ detected
```

## ログとクラッシュ情報

- 通常ログ: `<instance>/<game-dir>/logs/latest.log`
- クラッシュレポート: `<instance>/<game-dir>/crash-reports/`
- Forge早期起動ログ: `<instance>/<game-dir>/logs/debug.log`
- JVMクラッシュ: `<instance>/<game-dir>/hs_err_pid*.log`

`<game-dir>` はインスタンスに応じて `.minecraft` または `minecraft` です。

問題が起きた場合は、PrismLauncherのコンソールだけでなく `latest.log` と `debug.log`、生成されていれば最新のクラッシュレポートを確認します。まずTaCZと本MODだけで再現するかを切り分け、その後に互換性MODを1つずつ追加します。
