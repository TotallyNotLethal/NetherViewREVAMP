![Nether View](art/meltpoint-banner2.png)

# Nether View
A plugin that makes it possible to see the nether through nether portals.  
This was inspired by SethBling's [Magic Nether Portal](https://www.youtube.com/watch?v=xewQL6CkMWI).

![yep, it's pretty cool](art/nether-view-demo.gif)

### Installation
The latest releases can be downloaded here: [spigotmc.org](https://www.spigotmc.org/resources/nether-view.78885/)

### Compiling
Nether View uses Maven 3 to manage dependencies.

Required libraries:
- Paper/Spigot API 1.21.x
- ProtocolLib 5.x

Build requirements:
- Java 21
- Maven 3

Build command:
```bash
mvn clean package
```

The plugin jar will be created at:
`target/netherview-3.0.0.jar`

### IntelliJ build notes (important)
If you build with IntelliJ's **Build Artifacts** feature, do **not** use an "exploded" artifact for Paper plugins.
Paper expects a real `.jar` file that contains `plugin.yml` (or `paper-plugin.yml`) at the root of the archive.

Recommended IntelliJ workflow:
1. Open the Maven tool window.
2. Run **Lifecycle -> clean**.
3. Run **Lifecycle -> package**.
4. Copy `target/netherview-3.0.0.jar` into your server `plugins/` folder.
5. Delete any stale remapped directory for this plugin if it exists (for example `plugins/.paper-remapped/netherview-3.0.0.jar/` when it is a folder).
6. Start the server again.

Quick validation command (from repo root):
```bash
jar tf target/netherview-3.0.0.jar | rg "plugin.yml|paper-plugin.yml"
```
This should print `plugin.yml`.

### Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

### bStats
[![bStats Graph Data](https://bstats.org/signatures/bukkit/NetherView.svg)](https://bstats.org/plugin/bukkit/NetherView/7571)
