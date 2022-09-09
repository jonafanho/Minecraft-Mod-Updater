# Minecraft Mod Updater

## Usage

### Clientside

1. Install the appropriate version of this mod to Minecraft. Currently, only Fabric versions are functional.
1. Edit `<Minecraft root folder>/config/minecraft-mod-updater.json` or create it if it doesn't exist. An example is shown below:
   ```json
   {
   	"synced": [
   		"https://example1.com/url.json",
   		"https://example2.com/url.json"
   	],
   	"local": [
   		{
   			"id": "123456",
   			"source": "curseforge"
   		},
   		{
   			"id": "another-mod-id",
   			"source": "modrinth"
   		},
   		{
   			"id": "My-Mod",
   			"url": "https://example.com/My-Mod.jar",
   			"sha1": "0123456789012345678901234567890123456789"
   		},
   		{
   			"id": "Another-Mod-1.0.0",
   			"url": "https://example.com/Another-Mod-1.0.0.jar",
   			"sha1": "0123456789012345678901234567890123456789",
   			"before": 1615338000000
   		},
   		{
   			"id": "Another-Mod-2.0.0",
   			"url": "https://example.com/Another-Mod-2.0.0.jar",
   			"sha1": "0123456789012345678901234567890123456789",
   			"after": 1615338000000
   		}
   	]
   }
   ```

* `synced` (Optional): If defined, the Minecraft Mod Updater will pull modpacks from each URL listed. Each URL should be
  a direct link to a JSON
  file. See [the section below](#distributing-a-modpack) for how this JSON file should be formatted.
* `local` (Optional): If defined, the Minecraft Mod Updater will pull mods from each mod listed.
    * `id` (Required): The mod identifier.
        * If `source` is also defined this is the mod ID for CurseForge or Modrinth.
        * If `url` and `sha1` are also defined, this can be any recognisable name for the mod.
    * `source`: (Required if `url` or `sha1` are not defined): The source of the mod, currently only `curseforge`
      and `modrinth` are supported.
    * `url` (Required if `source` is not defined): The direct download link to the mod jar file.
    * `sha1` (Required if `source` is not defined): The expected sha1 hash of the mod jar file.
    * `before` (Optional): If defined, only download the mod if the current time is before this value (in milliseconds since the UNIX epoch).
    * `after` (Optional): If defined, only download the mod if the current time is on or after this value (in milliseconds since the UNIX epoch).

### Distributing a Modpack

1. Create a JSON file with a publicly available direct download link.
1. Edit this JSON file. The format is the same as the `local` section of the client configuration file (without the parent key). An example is shown below:
   ```json
   [
   	{
   		"id": "123456",
   		"source": "curseforge"
   	},
   	{
   		"id": "another-mod-id",
   		"source": "modrinth"
   	},
   	{
   		"id": "My-Mod",
   		"url": "https://example.com/My-Mod.jar",
   		"sha1": "0123456789012345678901234567890123456789"
   	},
   	{
   		"id": "Another-Mod-1.0.0",
   		"url": "https://example.com/Another-Mod-1.0.0.jar",
   		"sha1": "0123456789012345678901234567890123456789",
   		"before": 1615338000000
   	},
   	{
   		"id": "Another-Mod-2.0.0",
   		"url": "https://example.com/Another-Mod-2.0.0.jar",
   		"sha1": "0123456789012345678901234567890123456789",
   		"after": 1615338000000
   	}
   ]
   ```
1. Add the JSON URL to the `synced` section of the client configuration file.

## License

This project is licensed with the [MIT License](https://opensource.org/licenses/MIT).

## Questions? Comments? Complaints?

Let's connect.

<a href="https://discord.gg/PVZ2nfUaTW" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/discord.png" alt="Discord" width=64></a>
&nbsp;
<a href="https://www.linkedin.com/in/jonathanho33" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/linked_in.png" alt="LinkedIn" width=64></a>
&nbsp;
<a href="mailto:jonho.minecraft@gmail.com" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/email.png" alt="Email" width=64></a>
&nbsp;
<a href="https://www.patreon.com/minecraft_transit_railway" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/patreon.png" alt="Patreon" width=64></a>
