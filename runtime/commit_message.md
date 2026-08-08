特別食サマリー表示を削除

理由:
Home画面の残高表示を必要な項目に絞り、特別食関連の2項目を非表示にするため

変更点:
- Home画面から「今月の特別食回数」を削除
- Home画面から「特別メシ差分合計」を削除
- 特別食の内部計算と記録機能は維持

確認内容:
- .\gradlew.bat testDebugUnitTest 成功
- .\gradlew.bat assembleDebug 成功
- git diff --check 成功
- 実機へadb install -rで更新
- 特別食2項目が表示されず、今週・今月の残高と消費カロリー表示が残ることを確認
