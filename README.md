CobraCorral
===========

A Horse management plugin for the Bukkit Minecraft server.

####Configuration:
* **max-horses** - Specify the maximum amount of horses any player can lock. *Default: 2*
* **immortality** - If true, locked horses will be invulnerable to all damage when not being ridden. *Default: true*
* **auto-lock** - If true, horses will automatically lock when tamed if a lock spot is available. *Default true*
* **immortal-cooldown** - If true, horses will become invulnerable only after a delay. *Default: false*
* **cooldown-time** - The time, in seconds, that a horse will be vulnerable after the rider gets off. *Default: 5*
* **protect-chests** - If false, players cannot lock Mules/Donkeys with chests or place chests on locked Mules/Donkeys. *Default: true*
* **stop-pvp** - If true, players can not damage a locked horse if someone is riding it. *Default: false*
* **eject-on-logoff** - If true, any non-owner player on a locked horse (test-driving or on ACL) will be ejected on logoff. *Default: true*
* **backend** - Sets the database used to store locked horse information. *Default: sqlite, see settings below.*
  * **type:** sqlite
  * **file:** ccorral.db
  * **or**
  * **type:** mysql
  * **hostname:** localhost
  * **port:** 3306
  * **database:** ccorral
  * **username:** root
  * **password:** password

####Commands:
* **/ccorral** - Show the CobraCorral command help pages.
  * **permission:** ccorral.help
  * **parameters:** \<command #**/**name\>
  * **admin-only:** /ccorral reload - reloads basic settings from file, does not change DB backend.
* **/corral** - Lock a horse so only the owner can ride/harm it.
  * **permission:** ccorral.lock
  * **aliases:** /horse-lock, /hlock
* **/uncorral** - Unlock a horse that you own allowing others to ride/harm it.
  * **permission:** ccorral.lock
  * **aliases:** /horse-unlock, /hunlock
  * **admin-only:** /uncorral \<player\> \<horseID**/**name\>
* **/testdrive** - Temporarily unlock a horse allowing someone to ride it.
  * **permission:** ccorral.lock
  * **aliases:** /horse-test, /htest
* **/horse-access** - View or modify a horse's access list.
  * **permission:** ccorral.lock
  * **parameters:** \<+**/**-\>\<username\>
  * **aliases:** /haccess, /hacl
* **/horse-name** - Nickname a locked horse.
  * **permission:** ccorral.lock
  * **parameters:** \<nickname\>
  * **aliases:** /hname
* **/horse-free** - Release a horse from your ownership.
  * **permission:** ccorral.free
  * **aliases:** /hfree
* **/horse-list** - List all the horses owned by a player
  * **permission:** ccorral.list
  * **parameters:** \<page\>
  * **aliases:** /hlist
  * **admin-only:** /horse-list \<playerName\> \<page\>
* **/horse-gps** - Locate a horse that you own.
  * **permission:** ccorral.gps
  * **parameters:** \<horseID/name\>
  * **aliases:** /hgps
  * **admin-only:** /horse-gps \<playerName\> \<horseID**/**name\>
* **/horse-tp** - Teleport a horse to your location
  * **permission:** ccorral.tp
  * **parameters:** \<playerName\> \<horseID**/**name\>
  * **aliases:** /htp
* **/horse-info** - List information for a specified horse.
  * **permission:** ccorral.info
  * **aliases:** /hinfo
* **/horse-bypass** - Toggle ability to bypass horse access restrictions.
  * **permission:** ccorral.bypass
  * **aliases:** /hbypass
* **/horse-tame** - Tame a horse to yourself or a player.
  * **permission:** ccorral.tame
  * **parameters:** \<playerName\>
  * **aliases:** /htame

####Permissions:
* **ccorral.admin:** Access to all CobraCorral commands.
  * default: op
* **ccorral.help:** Show command reference
  * default: true
* **ccorral.lock:** Lock a Horse entity
  * default: true
* **ccorral.free:** Set free an owned horse
  * default: true
* **ccorral.list:** List locked Horses
  * default: true
* **ccorral.list-all:** List any player's locked Horses
  * default: op
* **ccorral.gps:** Allows a player to locate a missing horse.
  * default: true
* **ccorral.gps-all:** Locate any horse and display precise coordinates.
  * default: op
* **ccorral.tp:** Teleport a specific horse to your location.
  * default: op
  * default: true
* **ccorral.bypass:** Allows someone to bypass all horse access restrictions.
  * default: op
* **ccorral.tame:** Tame a horse to yourself or specified player.
  * default: op