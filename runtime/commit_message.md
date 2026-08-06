Google Driveのプラン選択表示を追加

理由:
Windows版で作成した複数の食事プランをAndroid側のテンプレート管理で選択し、6枠へ反映するため

変更点:
- Windows版の同期ログをRevision順に復元し、DailyPlanと6枠の食事スナップショットをAndroidモデルへ変換
- Windows側の選択プランを初期選択として読み込み、Android側のプラン選択で6枠を切り替え
- 食品・レシピ・主菜指定・数量単位・メモ・画像ハッシュを表示用データへ反映
- Drive画像を必要分だけAndroid内部へキャッシュ
- 既存のテンプレート管理画面を読み取り専用のWindowsプラン選択画面へ変更

確認内容:
- gradlew.bat :app:compileDebugKotlin 成功
- gradlew.bat :app:assembleDebug 成功
- git diff --check 成功
- Windows版プロジェクトの作業ツリーに変更なし
- 単体テストは既存のFakeMealTemplateDao未実装によりコンパイル停止
