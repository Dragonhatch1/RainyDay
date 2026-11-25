# RainyDay (GTNH / Minecraft 1.7.10)

It always rains. Every so often the weather randomly switches to a different **rain profile**: drizzle,
steady rain, heavy rain or a thunderstorm. Each profile has its own rain strength, thunder strength,
duration and chance of being picked.

Runs on the server: put the jar in the server's `mods/` folder, or your own `mods/` folder for
singleplayer. Players don't need it, except to hear the custom sounds (see "Profile sounds").

## Default profiles

Each time a profile starts, its rain and thunder strength are rolled at random between its min and max,
along with how long it lasts.

| Profile | Rain | Thunder | Lasts | Weight | Feel |
|---|---|---|---|---|---|
| drizzle | 0.3–0.5 | 0 | 10–30 min | 3 | Faint, quiet rain |
| steady | 0.8–1.0 | 0–0.1 | 10–40 min | 4 | Normal vanilla rain |
| heavy | 1.0 | 0.5–0.7 | 5–15 min | 2 | Dark sky, distant thunder, no lightning |
| storm | 1.0 | 1.0 | 3–13 min | 1 | Full thunderstorm with lightning |

Weight is a relative chance: steady (4) is picked twice as often as heavy (2). With these defaults it's
roughly half steady, a third drizzle, a tenth heavy and 5% storms by time.

## What the numbers do

- **rain (0.25–1):** how visible the rain is, how many splashes appear (so how often you hear rain), and
  how dark the sky is. 1 is normal vanilla rain. It can't go above vanilla's heaviest rain.
- **thunder (0–1):** darkens the sky further. Lightning only strikes when rain × thunder is above 0.9.
  Hostile mobs also only spawn in daylight during a full storm, so "heavy" doesn't cause daytime spawns.
  If you widen the storm's ranges, keep rainMin × thunderMin above 0.9 or some storms won't have lightning.

Changes fade in and out at vanilla's speed (about 5 seconds from nothing to full).

## Config: `config/rainyday.cfg`

- `general.debug`: `true` prints a line to the console and log each time a profile begins, e.g.
  `Starting heavy (rain 1.00, thunder 0.60, for 13.7 min) in dimension 0`. Default `false`.
- `general.indoorSoundVolume`: profile sounds are multiplied by this for players who can't see the sky
  (indoors, underground). Default `0.35`; `0` = silent indoors.
- `general.dimensions`: dimension IDs where it always rains (default `0`, the Overworld).
- `general.profiles`: list of profile names. Add a new name and restart, and a `profile_<name>` section
  is created for it with editable values. Remove a name to drop that profile.
- `profile_<name>`: `rainMin`, `rainMax`, `thunderMin`, `thunderMax`, `minMinutes`, `maxMinutes`,
  `weight` (0 = never picked). Set min = max for a fixed value.
- `profile_<name>` sound settings: `sound`, `soundVolume`, `soundPitchMin`, `soundPitchMax`,
  `soundIntervalMinSeconds`, `soundIntervalMaxSeconds`. See "Profile sounds" below.

Configs from the previous version upgrade automatically: an old single `rain` or `thunder` value becomes
both the min and the max.

## Profile sounds

Any profile can play an ambient sound near each player every so often. By default only **heavy** has one:
`rainyday:heavy_thunder`, a distant thunder rumble every 15–40 seconds, pitched 0.8–1.1.

- The sound comes from a random direction 8–16 blocks away and above the player, so it sounds distant.
- Players who can't see the sky hear it at `indoorSoundVolume`.
- `soundVolume` up to 1 sets loudness. Above 1 it's full loudness and carries further (16 blocks × volume).
  The default 2.0 carries 32 blocks.
- `sound` can also be any vanilla sound name, e.g. `ambient.weather.thunder`, which every player already has.

### Random files

`rainyday:heavy_thunder` is defined in `src/main/resources/assets/rainyday/sounds.json` with a list of 10 files.
Each time it plays, the player's game picks one of the files at random.

The 10 included `rumble1.ogg`–`rumble10.ogg` files are **placeholders**: synthesized low rumbles, free to
use. To use your own:

1. Put your files in `assets/rainyday/sounds/thunder/`. They must be **Ogg Vorbis (.ogg)** and **mono**.
   Stereo files play without direction or distance fade.
2. List each file in `sounds.json`, without `.ogg`, e.g. `"thunder/my_rumble"`. Use as many as you like.
3. To add a new sound group (say, for drizzle), add another entry to `sounds.json` and set that profile's
   `sound` to `rainyday:<entry name>`.

### Who hears them

The server only sends the sound's name, so each player's game needs the files. Two ways to provide them:

- **Players install the RainyDay jar.** The sounds are inside it.
- **Players use `RainyDay-Sounds.zip` as a resource pack.** It contains the same `assets/` folder plus a
  `pack.mcmeta`, so players don't need the mod. A dedicated server can offer it on join: host the zip at a
  direct download URL and set `resource-pack=<url>` in `server.properties`.

If you change the sounds, update both the jar (rebuild) and the resource pack. Players with neither just
don't hear the sound, apart from a harmless "unknown soundEvent" warning in their log.

## Profile sounds

Any profile can play a sound near each player every so often. By default only **heavy** does: vanilla's
thunder (`ambient.weather.thunder`) at about half volume and a lower pitch, every 15–40 seconds, so it
sounds like a storm rolling in the distance without any lightning.

- Each player gets their own random timing, and the sound comes from a random direction a few blocks
  away, so it pans left and right.
- Under a roof it plays quieter (`general.indoorSoundVolume`, default 0.4).
- It starts once the rain has faded in, and stops as soon as the profile changes.
- Players don't need anything installed: the server only sends the sound's name, and each player's game
  plays it. Which file plays comes from their resource packs, so a pack that replaces
  `ambient.weather.thunder` changes this sound too, along with real lightning strikes.

Per-profile settings: `sound` (empty = none), `soundMinSeconds`, `soundMaxSeconds`, `soundVolume` (0–1),
`soundPitchMin`, `soundPitchMax` (0.5–2, lower = deeper).

## Commands and sleeping

- Sleeping through the night, or `/weather clear`, rerolls a new random profile. It never stops raining.
- `/weather rain` and `/weather thunder` have no effect; RainyDay is in charge of the weather.
- The current profile, its rolled strengths and its remaining time are saved in the world (`data/rainyday.dat`).
- `/gamerule doWeatherCycle` should be left **true**. RainyDay relies on vanilla's weather update.

## How it works

Each tick, vanilla nudges rain and thunder strength 0.01 toward full or toward zero, depending on its
"raining" and "thundering" switches, and sends any change to players. RainyDay sets those two switches
just before that each tick, so vanilla nudges the strengths toward the current profile's targets.
Vanilla still does all the fading, lightning, snow and syncing to players, so the mod sends nothing extra.

## Building

Needs a **Java 25 JDK** (GTNH's build plugin requires it). The mod itself is compiled for Java 8.

```
gradlew build      (Windows cmd)
./gradlew build    (IntelliJ terminal, Git Bash, macOS, Linux)
```

The jar is in `build/libs/`. Use the plain one, not `-dev` or `-sources`.
