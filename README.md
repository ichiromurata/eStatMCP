# e-Stat MCP Server

[政府統計の総合窓口(e-Stat)](https://www.e-stat.go.jp/)の[API機能](https://www.e-stat.go.jp/api/)を呼び出すToolsを持つMCPサーバです。

## Tools

* get_tables
  - 統計表情報取得を行います。

* get_data
  - 統計データ取得を行います。


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
        "ESTAT_API_KEY": "<アプリケーションID>"
      }
    }
  }
}
```

### サーバプログラムのビルド
このMCPサーバをビルドするにはJava 17かそれ以降のバージョンが必要です。次のコマンドでjarファイルを作成します。

```shell
./gradlew clean build -x test
```



