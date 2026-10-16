# WoBBot [![Build Status](https://travis-ci.org/Palanaeum/WoBBot.svg?branch=master)](https://travis-ci.org/Palanaeum/WoBBot)
A Discord bot which fetches entries from a Palanaeum archive (by default, [Arcanum](https://wob.coppermind.net)).

To run this bot: `env DISCORD_TOKEN=<TOKEN> ./gradlew run`

> [!IMPORTANT]
> At the moment, the slash command-based rewrite only supports Palanaeum commands. The rest will be restored over time.

## Configuration

Additional environment variables can be used to configure the bot's various aspects.

| Name          | Default                                                           | What?                                                                        |
|---------------|-------------------------------------------------------------------|------------------------------------------------------------------------------|
| DISCORD_TOKEN | n/a; Required variable                                            | Discord token to connect to the bot                                          |
| TEST_GUILD    |                                                                   | If present, the bot will add commands to this guild instead of globally.[^1] |
| ARCANUM_TOKEN |                                                                   | Token for unlimited API calls to the Palanaeum archive                       |
| WOB_COMMAND   | wob                                                               | Archive interaction command                                                  |
| ARCANUM_URL   | https://wob.coppermind.net                                        | Base URL for the Palanaeum archive instance                                  |
| ARCANUM_ICON  | ![](https://cdn.discordapp.com/emojis/373082865073913859.png?v=1) | URL of icon to use in archive responses                                      |
| ARCANUM_COLOR | ![#003A52](https://via.placeholder.com/15/003A52/000000?text=+)   | Color to use in archive interactions                                         |
| ARCANUM_NAME  | Arcanum                                                           | Name to use in archive command descriptions                                  |
| WIKI_COMMAND  | cm                                                                | Wiki interaction command                                                     |
| WIKI_URL      | coppermind.net                                                    | Domain for the MediaWiki wiki instance                                       |
| WIKI_ICON     | ![](https://cdn.discordapp.com/emojis/432391749550342145.png?v=1) | URL of icon to use in wiki responses                                         |
| WIKI_COLOR    | ![#CB6D51](https://via.placeholder.com/15/CB6D51/000000?text=+)   | Color to use in wiki interactions                                            |
| WIKI_NAME     | Coppermind                                                        | Name to use in wiki command descriptions                                     |

[^1]: Global commands can take some time to propagate, while guild commands update instantly, so this makes for a faster testing loop.
