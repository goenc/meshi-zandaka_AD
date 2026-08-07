Google Sheetsの消費カロリー表示を追加

理由:
Home画面で体データシートの日別推定消費カロリーと平均値を確認できるようにするため

変更点:
- stepDailyRecordsのestimatedTotalKcalをGoogle Sheets APIから読み込む処理を追加
- 今日の消費カロリーと登録済み日次データの平均消費カロリーを計算
- Home画面の特別食回数表示の下へ2項目を追加
- 読み込み失敗または当日値未取得時の表示と単体テストを追加

確認内容:
- .\gradlew.bat testDebugUnitTest 成功
- .\gradlew.bat assembleDebug 成功
- git diff --check 成功
