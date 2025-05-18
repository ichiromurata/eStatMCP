# e-Stat MCP Server

[政府統計の総合窓口(e-Stat)](https://www.e-stat.go.jp/)の[API機能](https://www.e-stat.go.jp/api/)を呼び出すToolsを持つMCPサーバです。

## Tools

* get_tables
  - 統計表情報取得(URL: getStatsList)を行います。

* get_surveys
  - 統計表情報取得(URL: getStatsList)の中の統計調査名一覧取得を行います。

* get_metadata
  - メタ情報取得(URL: getMetaInfo)を行います。
  - 必須パラメータ
    - 統計表ID（statsDataId）

* get_data
  - 統計データ取得(URL: getStatsData)を行います。
  - 必須パラメータ
    - 統計表ID（statsDataId）

それぞれの関数ではe-StatのAPIレスポンスを簡略化したJSON形式に変換して返すように実装しています。

## 使用方法

### e-Stat用アプリケーションIDの取得

[利用ガイド](https://www.e-stat.go.jp/api/api-info/api-guide)に従ってe-Statのユーザ登録を行い、アプリケーションIDを取得してください。


### Claude Desktopの設定
Claude Desktopを使う場合は、`claude_desktop_config.json`に次の記述を加えてください。

```json
{
  "mcpServers": {
    "eStatMCP": {
      "command": "java",
      "args": [
        "-jar",
        "build/libs/<your-jar-name>.jar"
      ],
      "env": {
        "ESTAT_API_KEY": "<アプリケーションID>",
        "RESPONSE_SIZE": "100"
      }
    }
  }
}
```

`<アプリケーションID>`には、ご自身のe-StatのアプリケーションIDを入れてください。

`RESPONSE_SIZE`には、e-Stat APIから1度に受信するデータ数を指定します。AIチャットのコンテキスト長を制限したい場合に使用します。e-Stat API側のデフォルトは100,000です。

### サーバプログラムのビルド
このMCPサーバをビルドするにはJava 17かそれ以降のバージョンが必要です。次のコマンドでjarファイルを作成します。

```shell
./gradlew clean build -x test
```


## 使用例
### 経済に関する統計調査を取得

(https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21)[https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21]

統計表は数が多いため、調べたい調査名か決まっていないときは統計表を検索する前に統計調査を検索するとよいかも知れません。

### 東京都の家計調査の結果を取得（途中まで）

(https://claude.ai/share/d393e0cc-50ec-4770-9ae0-600740bb275d)[https://claude.ai/share/d393e0cc-50ec-4770-9ae0-600740bb275d]

家計調査の結果表のメタデータが大きすぎてClaudeのコンテキスト長を超えてしまいました。

### 人口推計から年齢5歳階級別人口を取得

(https://claude.ai/share/e33bbc27-6094-4feb-984f-5c2cf6d5e5a2)[https://claude.ai/share/e33bbc27-6094-4feb-984f-5c2cf6d5e5a2]

統計調査名、項目名まで指示するとなかなかよい動きをしてくれました。


