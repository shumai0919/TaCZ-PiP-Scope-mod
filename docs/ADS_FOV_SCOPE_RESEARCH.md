# TaCZ ADS / FOV / Scope rendering research

調査日: 2026-10-06  
対象: TaCZ `1.20.1` branch, commit `b482eff8c94a733ac8d0910193fca3893954027c`  
配布版: TaCZ `1.1.8-hotfix2`

## 結論

TaCZの現在のスコープはPiPではない。ADS進捗に応じてメイン世界のFOV全体を狭め、同時に一人称銃モデルをスコープの`scope_view`ノードへ移動する方式である。レンズ、遮光部、レティクルは`BedrockAttachmentModel`がメインRenderTargetのStencilを一時的に使って描画する。

将来のPiP実装では、次の分離が妥当である。

1. TaCZのADS状態、倍率、装着スコープ情報は既存APIから読む。
2. 通常世界のFOV変更だけをForgeイベントで抑止する。
3. 狭いFOVの世界を専用RenderTargetへ描く。
4. TaCZがレンズStencilを作った直後にPiPテクスチャを合成する。

スコープIDの固定表は不要である。`AttachmentType.SCOPE`、`ClientAttachmentIndex.isScope()`、`zoom[]`、`views[]`、`views_fov[]`、NBTの`ZoomNumber`で標準スコープと適切に作られたGun Packスコープを判定できる。

## ADS状態の流れ

### 入力

`AimKey.onAimPress`が右クリックを受け取り、`IClientPlayerGunOperator.aim(boolean)`を呼ぶ。hold/toggleの違いもここで処理される。

主要箇所:

- `client/input/AimKey.java:33-53`
- `api/client/gameplay/IClientPlayerGunOperator.java:56-58`

### クライアント状態

`LocalPlayerAim.aim`は現在のアイテムが`IGun`で、クライアントGun Indexが存在するときだけ`clientIsAiming`を更新し、`ClientMessagePlayerAim`をサーバーへ送る。

`LocalPlayerAim.tickAimingProgress`は毎tick、銃の`aimTime`または配件適用後の`AdsModifier`を使って`clientAimingProgress`を0〜1へ補間する。描画側は`getClientAimingProgress(partialTicks)`で前tickとの補間値を得る。

主要箇所:

- `client/gameplay/LocalPlayerAim.java:24-39`
- `client/gameplay/LocalPlayerAim.java:46-94`
- `network/message/ClientMessagePlayerAim.java:25-36`

PiPの表示開始・フェード係数にはこの補間済み`clientAimingProgress`をそのまま利用できる。

### サーバー状態

サーバーは`ClientMessagePlayerAim`を受け、`IGunOperator.fromLivingEntity(player).aim(...)`を呼ぶ。`LivingEntityAim`側にも独立した`aimingProgress`があり、同期データには`IS_AIMING_KEY`と`AIMING_PROGRESS_KEY`がある。

ローカル描画ではサーバー同期値より`IClientPlayerGunOperator.getClientAimingProgress`を優先すべきである。入力に対して即時で、既存の一人称モデルと同じ進捗になるためである。

## 倍率と可変倍率

`IGun.getAimingZoom(ItemStack)`の実装は`GunItemDataAccessor.getAimingZoom`にある。

1. 装着済み`AttachmentType.SCOPE`のIDを取得する。
2. 空なら内蔵スコープIDを試す。
3. 外付けスコープならNBT `ZoomNumber`を読む。
4. `TimelessAPI.getClientAttachmentIndex(scopeId).getZoom()`の配列から `zoomNumber % length` を選ぶ。
5. スコープがなければ銃Displayの`ironZoom`を使う。

主要箇所:

- `api/item/nbt/GunItemDataAccessor.java:328-346`
- `api/item/nbt/AttachmentItemDataAccessor.java:15-17,37-49`
- `entity/shooter/LivingEntityAim.java:33-51`
- `client/input/ZoomKey.java:31-68`

倍率切替キーはクライアントから空の`ClientMessagePlayerZoom`を送り、サーバーが銃内のスコープNBTの`ZoomNumber`を加算する。したがってPiP用FOVは独自の倍率状態を持たず、毎frameまたは変更検出時に`iGun.getAimingZoom(stack)`を読むのが安全である。

例として標準LPVOは`zoom: [6.25, 1.25]`、`views: [2, 2]`、`views_fov: [30, 60]`を持つ。`zoom`は世界倍率、`views_fov`は一人称銃モデル用FOVであり、PiP世界FOVには`zoom`を使う。

## 世界FOV

`CameraSetupEvent.applyScopeMagnification(ViewportEvent.ComputeFov)`が画面全体のズームを実装する。

- `usedConfiguredFov() == true`の世界描画だけを処理する。
- `zoom = iGun.getAimingZoom(stack)`を取得する。
- `1 + (zoom - 1) * aimingProgress`を実効倍率とする。
- `MathUtil.magnificationToFov(...)`でFOVへ変換する。
- `WORLD_FOV_DYNAMICS`で平滑化して`event.setFOV(...)`する。

主要箇所: `client/event/CameraSetupEvent.java:94-120`

### Phase 4の推奨方法

最初はMixinを使わずForgeの同じ`ViewportEvent.ComputeFov`を使う。

- `HIGHEST`で世界描画イベントのTaCZ適用前FOVを保存する。
- `LOWEST`で、ローカルプレイヤーがTaCZ銃をADS中かつPiP対象スコープ使用中なら保存FOVへ戻す。
- `usedConfiguredFov()`がfalseの手モデルFOVは変更しない。
- ADS進捗、銃モデル位置、マウス感度、反動補正には介入しない。

この方式ならTaCZの`CameraSetupEvent`へ直接Mixinせず、まず通常FOV固定を単独検証できる。ただし他MODも同イベントを変更するため、イベント優先度と保存値の扱いはEmbeddium/Oculus導入時に再検証する。

## 一人称銃モデルFOVと位置

世界FOVとは別に、`CameraSetupEvent.applyGunModelFovModifying`が手・銃モデル用FOVを変更する。スコープDisplayの`views_fov[]`または`fov`を使い、ADS進捗で補間する。

主要箇所: `client/event/CameraSetupEvent.java:122-164`

`FirstPersonRenderGunEvent.applyFirstPersonGunTransform`はADS進捗を再度平滑化し、銃モデルを次のノードへ移動する。

- スコープなし: 銃の`iron_view`
- スコープあり: 銃の`scope_pos` + 配件の`scope_view[_N]`
- `views[]`と`ZoomNumber`で可変倍率・複合照準器のviewを選ぶ

主要箇所: `client/event/FirstPersonRenderGunEvent.java:119-130,140-210`

PiP実装でもこの位置合わせは維持すべきである。世界FOVだけを通常値へ戻し、銃モデルFOVと`scope_view`位置合わせはTaCZに任せる。

## レンズとレティクル描画

`BedrockAttachmentModel`はモデルノード名を意味的に解釈する。

- `scope_view[_N]`: ADS時の視点位置
- `ocular`, `ocular_scope`, `ocular_sight`: レンズ開口Stencil形状
- `division[_N]`: レティクル／区画
- `scope_body`: スコープ本体
- `ocular_ring`: 接眼部外周

`renderScope`の処理順は次の通り。

1. メインRenderTargetでStencilを有効化する。
2. Stencilをclearする。
3. `ocular`形状を色・深度書込みなしでStencilへ記録する。
4. `scope_body`をレンズ外側へ描く。
5. ADS進捗に応じた円形マスクとレティクルを描く。
6. Stencilテストを無効化する。
7. 残りのモデルを描く。

主要箇所:

- `client/model/BedrockAttachmentModel.java:213-295`
- `client/model/BedrockAttachmentModel.java:357-386`
- `util/RenderHelper.java:46-71`

重要点: TaCZのStencilは一人称アイテム描画中に一時的に作成され、その場で無効化される。通常の`RenderLevelStageEvent`やGUIオーバーレイから後描きするだけでは、正確なレンズ領域を再利用できない。

### Phase 6で必要になる可能性が高い介入

`BedrockAttachmentModel.renderScope`の、`renderOcularStencil(...)`完了後かつレンズ遮光・レティクル描画前にPiPテクスチャを描く、小さなMixin injectionが第一候補である。対象メソッドはprivateなので、公開Forgeイベントだけで同じ位置へ挿入する経路は現在のソースにはない。

Mixinは次の責務だけに限定する。

- 現在のスコープがPiP対象か確認する。
- 既に用意したPiP RenderTargetのcolor textureを、現在のStencil値に従って描く。
- TaCZのレティクル、scope body、残りのモデル描画をそのまま続行する。
- GL状態を保存・復元し、TaCZのStencil clear/disable責務を奪わない。

## Gun Pack自動対応

ハードコードせず、現在の銃ItemStackから以下を読む。

1. `IGun.getIGunOrNull(stack)`
2. `getAttachmentId(stack, AttachmentType.SCOPE)`
3. 空なら`getBuiltInAttachmentId(...)`
4. `TimelessAPI.getClientAttachmentIndex(scopeId)`
5. `ClientAttachmentIndex.isScope()`、`isSight()`、`getZoom()`、`getViews()`、`getViewsFov()`
6. 外付けなら`getAttachmentTag(...)`の`ZoomNumber`

`scope == true`かつ有効な`zoom[]`があるものを初期PiP対象とするのが安全である。`sight == true`だけのドットサイトは通常PiP対象外にする。`scope && sight`の複合照準器は`views[]`とモデルの`ocular_scope[_N]`を考慮する必要があるため、Phase 7で個別に検証する。

Gun PackがTaCZのDisplay仕様とモデルノード規約に従っていれば、この判定はnamespaceに依存しない。独自レンダラーや規約外モデルは自動対応を保証できない。

## マウス感度と反動

`MouseHandlerMixin`は倍率とADS進捗からマウス感度を下げる。世界FOVを通常に戻しても、この処理はTaCZ倍率を使い続ける。光学倍率に対応する操作感として妥当なので、初期PiPでは変更しない。

`GunItemRendererWrapper`と`CameraSetupEvent.initialCameraRecoil`も倍率を使ってカメラアニメーション／反動を減衰する。これもPiPの倍率と整合するため維持する。

## 実装順序の提案

1. 読み取り専用の`ScopeContext`を作り、現在の銃、scope ID、isScope/isSight、倍率、ZoomNumber、ADS進捗を取得する。
2. デバッグログまたは画面表示で標準固定倍率・LPVO・内蔵照準器の値を確認する。
3. Forge FOVイベントだけでメイン世界FOVを通常値へ戻す。
4. PiP RenderTargetを追加し、まず固定倍率・DHなしで別描画する。
5. TaCZレンズStencil内へ合成する最小Mixinを追加する。
6. `ZoomNumber`変化、複合照準器、Gun Packを順に対応する。
7. 最後にEmbeddium、Oculus、Shader、Distant Horizonsを段階的に検証する。

## リスク

- `BedrockAttachmentModel.renderScope`はprivateで、TaCZ更新によりMixin注入点が変わる可能性がある。
- Accelerated Rendering使用時は`renderScopeAccelerated`へ別経路で入るため、通常経路だけへのMixinでは不十分。
- TaCZ自身がメインRenderTargetのStencilをclearするため、PiP側が独自Stencilを長時間保持してはいけない。
- Shader環境ではメインRenderTarget、深度、Stencil、buffer flushが通常環境と異なる可能性がある。
- Minecraftのワールド再描画は単純な`LevelRenderer.renderLevel`再呼出しでは再入性・frustum・camera・particle・hand描画に問題が出得る。Phase 5では状態境界を明示して小さく実証する必要がある。
- DHは最初のPiP passから除外し、通常画面だけで有効にする方針を維持する。

## Phase 3判定

ADS、倍率、可変倍率、世界FOV、銃モデルFOV、視点位置、レンズStencilの責務と主要呼出経路を特定できた。次のPhase 4「通常FOV固定」は、TaCZソースへ直接変更を加えずForgeイベント方式で試作可能である。
