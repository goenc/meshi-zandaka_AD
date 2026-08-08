SQLite外食カードのクイック記録表示対応

理由:
クイック記録画面で、名前文字列に依存せず、SQLite由来の外食カードを正しく選べるようにするため

変更点:
- 同期済みSQLite行のRecipeEntityとRecipeIngredientEntityから外食カードを抽出
- ビックマックサラダ、侍マックなどの画像・数量・栄養値を外食カードとして保存
- クイック記録画面を外食カードカタログから表示し、選択内容を入力欄へ反映
- 外食カードのキャッシュ保存・復元テストを追加

確認内容:
- `:app:testDebugUnitTest` 成功
- `:app:compileDebugAndroidTestKotlin` 成功
- `:app:assembleDebug` 成功
- `git diff --check` 成功
- 実機へ `adb install -r` 実施
- 外食カード4件、ビックマックサラダ、侍マックの表示を確認
- ビックマックサラダ選択時の534 kcal、P26.6g、F28.1g、C44.3g反映を確認
