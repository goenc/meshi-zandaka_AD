Google Drive外食カードのクイック記録表示対応

理由:
クイック記録画面で、ローカル初期テンプレートではなくGoogle Driveから取得した外食カードを選べるようにするため

変更点:
- 選択中のDrive計画から外食項目を抽出し、画像・数量・栄養値付きで表示
- Drive外食カードの選択内容をクイック記録欄へ反映
- テンプレートなしの外食記録保存と既存の昼食・夕食への追加を継続可能化
- Drive外食カードの新規保存に対する単体テストを追加

確認内容:
- `:app:testDebugUnitTest` 成功
- `:app:assembleDebug` 成功
- `git diff --check` 成功
- 実機へ `adb install -r` 実施し、Google Drive外食カードの表示・選択反映を確認
