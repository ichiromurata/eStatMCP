# e-Stat MCP Server

[政府統計の総合窓口(e-Stat)](https://www.e-stat.go.jp/)の[API機能](https://www.e-stat.go.jp/api/)を生成AIが扱えるようにするMCPサーバです。

## MCPとは
Model Context Protocol (MCP)は、アプリケーションが何を提供してくれるかを生成AIに教えるための様式です。Anthropicが2024年11月に提唱し、標準化を目指しています。生成AIからのリクエストを待つサーバとして実装したものを、MCPサーバと呼びます。既存のサービスを生成AIが扱えるようにするMCPサーバが数多く作られています。

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
Claude Desktopを使う場合は、`claude_desktop_config.json`に次の記述を加えてください。`claude_desktop_config.json`はMacの場合は`~/Library/Application\ Support/Claude/`、Windowsの場合は`%AppData%\Claude\`にあります。

#### ビルドしたjarファイルがある場合

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

#### Dockerを使う場合

```json
{
  "mcpServers": {
    "eStatMCP": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "ESTAT_API_KEY",
        "-e",
        "RESPONSE_SIZE",
        "ichiro21/estat-mcp"
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

### VS Codeの設定
VS Codeを使う場合は、`settings.json`に次の記述を加えてください。`settings.json`はVS Codeで`Ctrl + Shift + P`を押すと出てくるメニューから`Preferences: Open User Settings (JSON)`を選ぶと開くことができます。

#### Dockerを使う場合

```json
{
  "mcp": {
    "servers": {
      "eStatMCP": {
        "type": "stdio",
        "command": "docker",
        "args": [
          "run",
          "-i",
          "--rm",
          "-e",
          "ESTAT_API_KEY",
          "-e",
          "RESPONSE_SIZE",
          "ichiro21/estat-mcp"
        ],
        "env": {
          "ESTAT_API_KEY": "${env:ESTAT_API_KEY}",
          "RESPONSE_SIZE": "100"
        }
      }
    }
  }
}
```

VS Codeでは設定の中でOSの環境変数を`${env:ESTAT_API_KEY}`のように呼び出すことができます。OSの環境変数を使わない場合は直接記述するか、VS Codeに記憶させることができます。（[こちら](https://code.visualstudio.com/docs/copilot/chat/mcp-servers#_add-an-mcp-server-to-your-workspace)の記入例を参照）

### サーバプログラムのビルド
このMCPサーバをビルドするにはJava 17かそれ以降のバージョンとGradleが必要です。次のコマンドでjarファイルを作成します。

```shell
./gradlew clean build -x test
```


## 使用例
### 経済に関する統計調査を取得

[https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21](https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21)

調べたい調査名か決まっていないときは統計表を検索する前に統計調査を検索することができます。

### 人口に関する統計を取得

[https://claude.ai/share/5f6874e2-4fc6-42e5-a8dd-6e8feb6158df](https://claude.ai/share/5f6874e2-4fc6-42e5-a8dd-6e8feb6158df)

人口推計の最新のデータを取得してくれました。

### 東京都の家計調査の結果を取得（途中まで）

[https://claude.ai/share/d393e0cc-50ec-4770-9ae0-600740bb275d](https://claude.ai/share/d393e0cc-50ec-4770-9ae0-600740bb275d)

家計調査の結果表のメタデータが大きすぎてClaudeのコンテキスト長を超えてしまいました。

### 人口推計から年齢5歳階級別人口を取得

[https://claude.ai/share/e33bbc27-6094-4feb-984f-5c2cf6d5e5a2](https://claude.ai/share/e33bbc27-6094-4feb-984f-5c2cf6d5e5a2)

統計調査名、項目名まで指示したら的確に取得してくれました。人口推計の「参考表」を取得させないために、「統計表」で検索するように指定しています。

