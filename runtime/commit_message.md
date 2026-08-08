グラフ凡例を一列に整理

理由:
Home画面の食事グラフで6区分の対応関係を横一列で確認できるようにするため

変更点:
- グラフ凡例を2行表示から1行表示へ変更
- 凡例の順序を朝・間朝・昼・間昼・夕・間全へ統一

確認内容:
- .\gradlew.bat testDebugUnitTest 成功
- .\gradlew.bat assembleDebug 成功
- git diff --check 成功
- 実機へadb install -rで更新
- 6つの凡例が同じ行に並び、指定順で表示されることを確認
