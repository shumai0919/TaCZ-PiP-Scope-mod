# 中央の白い斑点の除去

2026-10-18。imagegenスキルの組み込み画像編集を使用し、既存の `lens_surface.png` の中央付近の白い粒状の斑点を除去。周辺の擦れ・汚れと中央の薄い線傷を残す方針です。生成結果を目視し、中央の粒状斑点が取り除かれ、周辺に擦れが残っていることを確認しました。ピクセル単位での周辺一致を保証する編集ではありません。

保存先: `src/main/resources/assets/tacz_pip_scope/textures/effect/lens_surface.png`。シェーダーと設定値は変更していません。

編集プロンプト:

> Use case: precise-object-edit. Input image: edit target, existing grayscale optical glass surface data texture. Remove the isolated white dust specks, dots, spots and flecks in the central area, approximately the inner 60 percent of the image width and height, with a natural gradual transition. Keep the fine hairline scratches and faint wiping texture, especially preserve the existing peripheral abrasion arcs and peripheral grime. The center should be clean of white particulate spots, not a hard-edged black circular hole. Change only the central spots; preserve the overall layout, grayscale, black opaque background, square framing, and peripheral marks as closely as possible. Do not add scratches, repeating patterns, cracks, glow, borders or text.
