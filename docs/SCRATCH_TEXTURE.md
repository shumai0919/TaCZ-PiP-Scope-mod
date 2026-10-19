# 傷専用テクスチャ（2026-10-18）

## 後続修正：最初のガラス画像に統一

ユーザー確認で束の反復が不自然だったため、以下のスタンプ方式は廃止しました。その後、連続した1枚の画像をレンズに対応させる方式へ移行しました。現在の仕様は [SURFACE_NORMAL_USE.md](SURFACE_NORMAL_USE.md) を参照してください。未使用の `lens_scratches.png` と付随メタデータは2026-10-20の配布前整理でリソースから除外し、ローカルの `work/release-cleanup-backup-20261020-004819` に退避しました。以下は開発時点の記録で、現在の仕様ではありません。

画像使用時の密度は薄い模様の現れ方、大きさは傷画像全体の倍率になります。数式モードでは従来の本数と線の大きさです。中央保護・形状による切り取り・共通パターンを維持し、UVの繰り返しや傷の束の反復はしません。元画像が単一チャンネルなので、傷と汚れを意味的に完全分離した素材ではありません。

以下は廃止した試作の記録です。

従来は数式による線の濃淡だけを汚れ画像で調整していました。今回は傷専用の画像をスタンプとして描画します。汚れ画像とは別のサンプラーを使用し、汚れの濃さには依存しません。

- 「表面の質感テクスチャ」オン：画像の傷の束を描画。オフ：従来の数式の線。
- 「傷」オフ、強さ・密度・大きさのいずれか0：傷なし。
- 密度は傷の束の数、大きさは束の縦横寸法を変更。共通パターン番号で位置・向きを変更。
- 低密度で上下左右に分散する順序、中央保護、実際の開口形状による切り取りを維持。
- 画像の端はフェードし、縮小時は4サンプルと極小サイズの減衰でちらつきを抑制。完全なアンチエイリアスの保証ではありません。
- 最大48スタンプ。各スタンプ内4テクスチャサンプルの追加負荷があるため、密度最大時の性能はゲーム内で確認してください。

現在の設定値は変更しません。まず今の値で「表面の質感テクスチャ」を切り替えて比較してください。円形・四角形、ADS・リロード、解像度100%・75%で確認します。実ゲームでの視認性・ちらつき・性能は未確認です。

## 画像生成記録

imagegenスキルの組み込み画像生成ツールで新規作成。無加工で `src/main/resources/assets/tacz_pip_scope/textures/effect/lens_scratches.png` に保存。白い傷／黒い無傷部分の不透明なデータ画像です。画像を確認し、かすれた細い複数線と黒い余白を採用しました。

最終プロンプト:

> Use case: photorealistic-natural. Asset type: square grayscale shader decal data texture, 1024x1024. Pure opaque black background. In the central 70 percent, a small sparse horizontal cluster of realistic fine optical-glass abrasion scratches: several hairline nearly parallel gently irregular strokes, varying lengths, tapered broken ends, tiny gaps and uneven thickness, subtle short crossing micro scratches. Pale gray to white scratch pixels only, no smudges, no haze, no dust, no broad glow. Wide pure black empty margin on every side. Flat orthographic texture scan, no depicted lens or object, no lighting gradient, no border, no lettering, no watermark. Not cracks, not perfect circular arcs. This will be stamped repeatedly with random rotation and scale on a scope glass; black means no scratch.
