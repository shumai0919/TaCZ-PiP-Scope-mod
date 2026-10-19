# 視覚効果まとめ版（2026-10-18）

追記：傷の描画は後続の[傷専用テクスチャ](SCRATCH_TEXTURE.md)で更新しました。以下の「傷の濃淡補助」はまとめ版時点の記録です。現在は専用画像の傷の束を描画します。

## 追加した切り替え

| 設定 | 内容 | 制約 |
| --- | --- | --- |
| 外周屈折 | レンズの縁だけ像を曲げる | 中央の照準を保護。既存のゆがみと併用可能 |
| 光のゴースト | 明るい映像から小さな光の像を作る | 光源を追跡する物理的な反射ではなく近似 |
| レンズ内の銃をぼかす | 透明な銃描画レイヤーをぼかす | 照準線は対象外。深度による焦点処理ではない。100%解像度でも追加負荷あり |
| 非ADS時のガラス反射 | 拡大映像を使わない淡いガラスの光沢 | 環境の鏡面反射ではない。ADS進行度0.2までに消える |
| 表面の質感テクスチャ | 汚れと傷に画像の微細な質感を加える | 傷の配置・密度・大きさは従来の計算式を維持 |

新しい効果は既定で有効です。強さのある4項目は0～1、0で無効。質感テクスチャをオフにすると従来の数式模様になります。既存の設定値は変更していません。表面の共通パターン番号も引き続き使えます。

設定は「基本 → 光学効果 → 表面表現 → 焦点・非ADS → パフォーマンス」の順です。既存の設定キーを維持し、Configuredの当MODの画面だけ表示順を変更します。効果のスイッチと強さは隣接させています。Configuredは必須依存ではなく、未導入時にそのクラスを直接参照しません。対応外の画面では通常の並びへフォールバックします。

## 確認手順

1. 新しい5項目をいったんオフにし、基準の表示を確認する。
2. 1項目ずつオンにして、強さ0・標準・最大を比較する。
3. 円形と四角形のスコープで、ADS、銃の揺れ、リロードを確認する。
4. シェーダーON/OFF、PiP解像度100%/75%で比較する。
5. 非ADS反射は銃を下ろした状態、ゴーストは明るい空などを見て確認する。
6. 銃のぼけは照準線までぼけていないか、輪郭に黒いにじみが出ないかを確認する。
7. 質感テクスチャのON/OFFと共通パターン番号を切り替え、中央の視認性を確認する。

既存の遮蔽・開口形状を置き換えず、追加のワールド描画は行いません。ただし画像サンプリングと銃レイヤーの合成負荷は増えます。性能の改善を保証する追加ではありません。

## 実施済みの検証

- Gradle build成功（ゲーム内テストは含まない）。
- Test-LensSurface、Test-ConfiguredComments、VisualEffectRulesTest成功。
- 隠しOpenGLコンテキストでscope_image / scope_gun / scope_mask_featherのコンパイル・リンク成功。
- Configured 2.2.3の対象メソッドを配布JARで確認。実際の設定画面・Mixin適用・Oculusとの組み合わせは起動確認待ち。

## 質感画像の出典

imagegenスキルと組み込み画像生成ツールで新規生成した画像を使用しています。外部写真や他MODの画像は使用していません。生成画像は無加工で `src/main/resources/assets/tacz_pip_scope/textures/effect/lens_surface.png` に保存しました。黒を清浄なガラスとして扱うデータ画像なので、背景は透明ではなく不透明な黒です。サンプラーはlinear/clampです。

生成プロンプト（原文）:

> Use case: photorealistic-natural. Asset type: game shader grayscale glass surface roughness/smudge mask, square 1024x1024 texture. Flat orthographic scan of realistic optical glass residue ONLY, on perfectly black opaque background: faint gray oily wiping arcs, uneven fine polishing marks, sparse very fine pale micro-abrasions, isolated dust specks. This is a data texture, no depicted lens, no border, no circle, no scene, no lighting gradient, no text, no labels, no watermark. Mostly black, delicate low-contrast gray marks across the square, balanced distribution, no large bright areas, no cracks or damage. The central area slightly cleaner but no circular boundary. Fine natural irregular texture not digital noise. Suitable for multiplicative masks; zero black is clean glass.
