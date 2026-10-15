# TaCZ PiP Scope

TaCZ向けPicture-in-Pictureスコープ機能を実装するForgeアドオンMODです。通常画面FOVを維持し、ADS中の別ワールド描画を専用Framebufferに出力します。client設定のenableWorldFovPrototype、enableFramebufferPrototypeでそれぞれ無効化できます。

TaCZのレンズ内Stencilへ映像を合成します。導入・テスト手順は [TESTING.md](TESTING.md) を参照してください。`docs` のPhase別資料は過去時点の作業記録であり、最新の互換性保証ではありません。

## 開発状況

開発中の `0.1.0-dev` です。日本語設定画面、PiP解像度・更新頻度の調整、レンズのゆがみ・周辺減光・色収差などを実装しています。Embeddium、Oculusのシェーダー、Distant Horizonsとの共存処理も含みますが、全構成での動作を保証するものではありません。

現在はスコープ形状・遮蔽・光学効果を調整中です。最新変更は開口形状に沿った周辺減光・ゆがみで、ビルドと計算テストは成功、ゲーム内の表示確認は未完了です。

このリポジトリにはソース・Gradle Wrapper・テスト用コードを含みます。外部MOD、ガンパック、シェーダーパック、ワールド、録画、開発キャッシュ、試験版JARは含みません。一部の手動テストは別途入手した外部パックを引数として必要とします。`gradlew build`だけでは`tools`配下の手動テストやGPU表示を検証しません。

## 対象環境

全TaCZ銃の背景ADSズームを停止し、光学倍率切替時の構え固定とレンズ内の銃本体表示に対応しています。標準構造のハイブリッド照準器では光学側をPiP表示し、補助サイト使用中は光学レンズを黒くします。EOS／Maxstuffはユーザーによる動作確認済みですが、全収録銃の保証ではありません。

- Minecraft: 1.20.1
- Forge: 47.4.20
- Java: 17
- TaCZ: 1.1.8-hotfix2（CurseForge file ID `9037989`）
- MOD ID: `tacz_pip_scope`
- Version: `0.1.0-dev`

TaCZはCurseMavenから `implementation fg.deobf(...)` で開発時・実行時の外部依存として読み込みます。成果物へshadeしません。`mods.toml`でもmod ID `tacz`を必須依存にしています。

開発クライアントではTaCZ配布JAR内のMixin refmapをParchment開発名へ変換するため、TaCZ公式プロジェクトと同じ `mixin.env.remapRefMap` / `mixin.env.refMapRemappingFile` runプロパティを設定しています。これは配布JARの動作には影響しません。

## ビルド

Java 17を使用します。Windowsでは、必要に応じてこのシェルだけJava 17へ切り替えてからビルドしてください。

```powershell
$env:JAVA_HOME = '<Java 17のインストール先>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

成果物は `build/libs/tacz-pip-scope-0.1.0-dev.jar` です。

## 開発クライアント

```powershell
.\gradlew.bat runClient
```

`runClient`はクラッシュ、Javaコード、ログ、Mixin、ブレークポイントの開発確認用です。PrismLauncherはユーザー環境に近い互換性・Shader・Distant Horizons・性能確認に使用します。

起動ログには次が出力されます。

```text
[TaCZ PiP Scope] Mod loaded successfully
[TaCZ PiP Scope] TaCZ detected
```

詳細なPrismLauncher手順は [TESTING.md](TESTING.md) を参照してください。
