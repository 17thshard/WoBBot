# WoBBot [![Build Status](https://travis-ci.org/Palanaeum/WoBBot.svg?branch=master)](https://travis-ci.org/Palanaeum/WoBBot)
A discord bot which fetches Words of Brandon from Arcanum.

To run this bot: `env DISCORD_TOKEN=<TOKEN> [ARCANUM_TOKEN=<TOKEN>] mvn exec:java`

This bot requires the `Manage Messages` permission to control reactions. To grant this in your invite link: 
`https://discordapp.com/oauth2/authorize?&client_id=<CLID>&scope=bot&permissions=8192`

Alternatively, invoke `!wobabout` to get an invite link.

## Configuration

Additional environment variables can be used to configure the bot's various aspects.

| Name | Default | What? |
| ---- | ------- | ----- |
| DISCORD_TOKEN | n/a; Required variable | Discord token to connect to the bot |
| ARCANUM_TOKEN | | Token for unlimited API calls to the archive |
| BRANDONSANDERSON_URL | | The homepage URL to extract progress information |
| ARCANUM_URL | https://wob.coppermind.net | The base URL for API interactions |
| WIKI_URL | coppermind.net | The wiki URL |
| WIKI_COMMAND | coppermind|cm | The wiki interaction command |
| WOB_COMMAND | wob | The WoB interaction command |
| ARCANUM_ICON | ![](https://cdn.discordapp.com/emojis/373082865073913859.png?v=1) | The URL of the Icon to use for archive responses |
| WIKI_ICON | ![](https://cdn.discordapp.com/emojis/432391749550342145.png?v=1) | The URL of the Icon to use for wiki responses |
| ARCANUM_COLOR | ![#003A52](https://via.placeholder.com/15/003A52/000000?text=+) | The color to use in the WoB interactions |
| WIKI_COLOR | ![#CB6D51](https://via.placeholder.com/15/CB6D51/000000?text=+) | The color to use in the Wiki interactions |
