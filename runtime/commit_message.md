Google Drive接続確認機能を追加

理由:
Windows版の食事設定とバックアップをAndroid側から取得できる接続経路を用意するため

変更点:
- Google認証でアプリ専用領域と表示バックアップ領域への権限を要求
- 同期ログ、画像、バックアップZIPの一覧を取得して接続状態を表示
- アクセストークンは端末へ保存せずメモリだけで保持
- Windows版の同期ファイル名、データ属性、フォルダ構成に合わせたDrive通信を追加

確認内容:
- gradlew.bat :app:compileDebugKotlin 成功
- gradlew.bat :app:assembleDebug 成功
- Windows版プロジェクトの作業ツリーに変更なし
- 単体テストは既存のFakeMealTemplateDao未実装によりコンパイル停止
