# 導入・配布前の確認

## 導入

1. Minecraft 1.20.1 / Forge 47.4.20 / Java 17のテスト用インスタンスを用意する。
2. TaCZ 1.1.8-hotfix2と、使うパックに必要な依存MODを導入する。
3. Minecraft終了後、旧PiP JARをmods外へバックアップし、新JARをmodsへ置く。重複させない。
4. Mods画面でTaCZ PiP Scopeを確認する。成功ログの出力は不要として削除済み。

設定は `config/tacz_pip_scope-client.toml`。Configuredがあれば設定画面から編集可能です。まず最小構成、その後シェーダー・軽量化MODとの組み合わせを確認します。

## 回帰確認

- 円形・四角形スコープのADS、揺れ、リロード、倍率切替。
- 全効果オフと個別オン・オフ。表面画像0～3の切り替え。
- 画像モードで密度0/4に差がなく、数式モードでは本数が変わること。
- 傷の強さ0で傷が消えること。明所・暗所の視認性。
- 解像度100%/75%、更新上限0/30、水面の動きと反射。
- シェーダーON/OFF、持ち替え、ワールド再入場、画面サイズ変更。
- EOS「ヘレナの釘」の標準HUD表示と倍率切替、その後通常スコープのPiP。
- 任意MODなしの起動。Configuredなしでも起動できること。

ユーザー確認済み: 表面4種類、調整値、ヘレナの釘の黒画面解消、確認範囲の通常スコープ。全項目・全構成を網羅した保証ではありません。

## 自動・コード検証

- `./gradlew build`
- `./tools/Test-LensSurface.ps1`
- `./tools/Test-ConfiguredComments.ps1`
- `./tools/Test-SurfaceVariants.ps1`
- `./tools/Test-DefaultsMatch.ps1 -ConfigPath <保存済みTOML>`（指定ファイルと初期値を比較するため、設定変更後の不一致は正常）
- toolsのJava単体テストはjavac/javaで別途実行。CompileVisualShadersは非表示OpenGLコンテキストでのコンパイル・リンク検証であり、実ゲームの表示テストではない。

旧試作のテスト・資料には過去の仕様の記録も含まれます。描画検証用の自動ワールド操作は配布環境で無効です。CPU/GPU計測は通常版で無効です。

## 不具合報告

`logs/latest.log`、必要に応じて`logs/debug.log`・`crash-reports`、使用MOD/パック/シェーダーの版、銃・照準器、再現手順、画像を添えてください。公開するログには個人情報が含まれないか確認してください。

性能比較は同じ場所・視点・設定で、読み込みや爆発後の更新が落ち着いてから実施します。
