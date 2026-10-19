# 表面テクスチャの切り替え

2026-10-18。設定「表面テクスチャの種類」を追加しました。

|値|種類|画像|
|---|---|---|
|0|標準（既存の中央斑点除去版）|lens_surface.png|
|1|細かな擦れ|lens_surface_fine.png|
|2|拭き跡|lens_surface_wiped.png|
|3|薄い汚れ|lens_surface_smudged.png|

保存場所は `src/main/resources/assets/tacz_pip_scope/textures/effect/`。既定値0で従来の画像を維持します。「表面の質感テクスチャ」をオンにしてください。傷・汚れと比較表示で同じ選択画像を使い、共通パターン番号はその画像の向き・配置を変更します。設定保存後の描画から切り替わります。数式モードでは選択は見た目に影響しません。

追加画像は組み込み画像生成ツール（imagegenスキル）で新規生成し、目視確認して無加工で保存。中央を清潔に、周辺へ質感を寄せた連続画像です。傷の反復スタンプは使用しません。画像ごとの濃さは同一ではないため、強さで調整してください。密度の既存しきい値・検証用表示は今回変更していません。

通常表示の確認は比較表示0、密度4、大きさ1を基準にすると種類の差を見やすくなります。現在のユーザー設定は自動変更しません。シェーダーON/OFFで切り替え、0に戻した際の復元も確認してください。ゲーム内確認は未実施です。選択する画像が変わるだけで、描画パス・サンプル数は増えませんが、複数画像を使用した分のテクスチャメモリは増えます。

## 最終生成プロンプト

### 細かな擦れ

Use case: photorealistic-natural. Asset type: square 1024x1024 grayscale optical glass surface data texture, flat orthographic scan. Opaque pure black background; gray marks only. One continuous full-lens texture, not repeated stamps. Center inner 50 percent is almost clean with no white dots or particulate spots; gradually more detail in middle and periphery, no hard circular border. No lens frame, object, text, watermark, glow, cracks, claw marks or repeated identical scratch clusters. Variant FINE: delicate irregular hairline micro-abrasions, diverse short lengths and directions, lightly scattered across middle and outer glass. A few subdued sweeping polishing hairs; no thick scratches, no broad greasy patches. Clearly varied natural fine wear.

### 拭き跡

Use case: photorealistic-natural. Asset type: square 1024x1024 grayscale optical glass surface data texture, flat orthographic scan. Opaque pure black background; gray marks only. One continuous full-lens texture, not repeated stamps. Center inner 50 percent is almost clean with no white dots or particulate spots; gradually more detail in middle and periphery, no hard circular border. No lens frame, object, text, watermark, glow, cracks, claw marks or repeated identical scratch clusters. Variant WIPED: low-contrast broad delicate overlapping cloth wiping sweeps, irregular soft curved arcs with fine parallel streak detail, varying directions. Mostly black, medium-gray thin residue, not bright white. No isolated particulate flecks, no conspicuous straight gouges. Keep center black and clean with only faint continuous hairline traces.

### 薄い汚れ

Use case: photorealistic-natural. Asset type: square 1024x1024 grayscale optical glass surface data texture, flat orthographic scan. Opaque pure black background; gray marks only. One continuous full-lens texture, not repeated stamps. Center inner 50 percent is almost clean with no white dots or particulate spots; gradually more detail in middle and periphery, no hard circular border. No lens frame, object, text, watermark, glow, cracks, claw marks or repeated identical scratch clusters. Variant SMUDGED: translucent-looking faint irregular oily residue patches and delicate soft rubbed smears, concentrated toward outer thirds. Fine subtle streaks inside smudges, almost no distinct scratch lines. Soft uneven gray patches, not solid white blobs, no dust grains or white specks. Center completely clear black, naturally feathered transition with no ring edge. Flat data mask, not lighting or smoke.
