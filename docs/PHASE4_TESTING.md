# Phase 4 FOV試作

通常の筒型スコープ（scope=true, sight=false）を装着したTaCZ銃を一人称で構えると、世界FOVをTaCZの倍率適用前の値へ戻します。ADSアニメーション、手・銃モデル用FOV、倍率状態、感度補正はTaCZの処理を維持します。PiP画像はまだないため、レンズ内の世界も拡大されません。

## 確認手順

1. Minecraftを終了してPrismLauncherの `tacz-addon-pip` を起動し直します。
2. テストワールドで銃に通常の筒型スコープ（例: ACOG）を装着します。
3. ADS開始・維持・解除を繰り返し、背景の建物などが画面全体で拡大しないことを確認します。銃モデル自体の移動・大きさの変化は正常です。
4. LPVOの倍率を切り替えても背景の世界FOVが変わらないことを確認します。
5. 銃の切替、三人称への切替、ワールド退出を試します。切替直後のFOVの跳ねやズーム残りがあれば報告してください。
6. アイアンサイト、ドットサイト、複合照準器は本試作の対象外です。従来のTaCZズームが残ります。

比較する場合はゲーム終了後、インスタンスの `minecraft/config/tacz_pip_scope-client.toml` の `enableWorldFovPrototype` を `false` にして再起動します（設定ファイルは初回起動で生成）。

ログ: `minecraft/logs/latest.log` に `[TaCZ PiP Scope] World FOV prototype active` / `inactive` が対象切替時に出ます。毎frameの出力はありません。

## 実装と検証範囲

Forge ViewportEvent.ComputeFovをHIGHESTで取得しLOWESTで復元します。イベント単位で保存するため入れ子イベントの値を混同しません。手モデル用イベントや別カメラ・三人称には適用しません。

この試作では同じイベントの途中で他MODが加えたFOV変更も復元され得ます。最小構成で評価し、Shader/DH対応は後の段階で検証します。実プレイでの見え方は上記手順による確認が必要です。
