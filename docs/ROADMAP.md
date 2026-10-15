# Roadmap

## 最新の予定（2026-10-09）

以下の旧Phase記録より outputs/WORK_PLAN.md の状態・順序を優先する。

1. 設定画面の日本語対応（ユーザー実画面確認OK、完了）。
2. Embeddium互換対応（旧Phase 9、0.3.31でユーザー確認OK）。
3. Oculus／Shader互換対応（旧Phase 10、Oculus 1.8.0でShader ON独立PiPを試作実装。Complementary／Mellowで映像生成を検証、受け入れ確認待ち）。
4. Distant Horizons互換対応（旧Phase 11）。
5. 対応組み合わせの回帰確認・配布物と手順の更新。

Phase 1～8は現在の対象範囲で一区切り。複合照準器・倍率補間・レンズ内銃表示も実装済み。EOS／Maxstuffはユーザー確認範囲で良好。全モデル保証ではない。
初期最適化・通常使用用まとめは完了、Configured設定画面もユーザー確認OK。追加最適化は保留。急激な負荷増大・ワールド終了は原因未確定で、再発時に調査を優先する。MCS2個別対応とAWP特殊表示は対象外。

Embeddiumはユーザー実画面確認済み。OculusはShader ONの試作検証中、DHは未着手。安全策の単純解除だけで対応済みとはしない。

## 以下は旧計画・過去時点の状態

## Phase 1 — 開発環境構築

Minecraft 1.20.1、Forge 47.4.20、Java 17で再現可能なGradle環境とPrismLauncherへの配布手順を整える。

## Phase 2 — TaCZ検出

TaCZを外部必須依存としてロードし、起動ログで検出を確認する。

## Phase 3 — TaCZのADS / Scope処理調査

完了。公式1.20.1ソースを基準にADS、FOV、スコープ描画、attachmentデータの流れと介入候補を特定した。詳細は `ADS_FOV_SCOPE_RESEARCH.md` を参照。

## Phase 4 — 通常FOV固定

スコープ使用中もメインビューのFOVを通常値に保つ方法を、小さな変更範囲で実証する。

## Phase 5 — PiP用Framebuffer

狭いFOVの別ワールド描画を専用Framebufferへ出力する。最初はDistant Horizonsを対象外とする。

## Phase 6 — TaCZ標準スコープへの表示

Framebufferの内容を標準スコープのレンズ面へ合成する。

### Phase 6後の追加要件 — ADS時の銃本体表示

ユーザー承認により共通ADS方針の試作へ着手。全銃の背景ズーム停止、非ハイブリッドスコープの先頭倍率基準の構え固定、銃本体のStencil除外解除を実装。パック固有の非表示ボーンは対象外。実機受け入れは未完了。outputs/COMMON_ADS_TESTING.md参照。

## Phase 7 — Gun Pack Scope自動対応

スコープIDのハードコードを避け、TaCZのattachment / scopeデータを使って追加スコープを検出する。

## Phase 8 — 可変倍率対応

TaCZ側の倍率状態をPiP用FOVへ安全に同期する。

## Phase 9 — Embeddium対応

描画パイプライン、チャンク描画、性能への影響を検証し、互換処理を追加する。

## Phase 10 — Oculus / Shader対応

Shader有効時のFramebuffer、深度、描画状態の保存・復元を検証する。

## Phase 11 — Distant Horizons対応

通常画面ではDHを維持し、スコープ用FramebufferではまずDHなしを既定とする。既存のTaCZ + Oculus + Shader + DHの深度問題をPiPへ持ち込まない構成から始める。

Phase 5はユーザーによる基本動作確認済み。現在はPhase 6のレンズ内Stencil合成を試作し、実機検証を行う。ハイブリッドサイト対応、Shader/DH/Embeddium対応は未実装。
