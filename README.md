# Rocket League Map Manager
This app allows you to easily switch between Rocket League Workshop maps from within the comfort of your browser!  
The app can also automatically start/stop Rocket League for you if you want, and it has a number of other features that make your life easier.

For example, the map list can be sorted in various ways, you can mark maps as favorite,
you can quickly go to the Steam workshop page of any map,
and the app even remembers the last time you loaded a map, so you can easily see which maps you don't play anymore,
to clear them out if you're low on hard drive space.

## How it works
In order to function, the app must be aware of the location of the steamapps folder on your computer, where Rocket League is installed.
If you have Rocket League installed in `C:\Program Files (x86)\Steam\steamapps` the app will detect that automatically during setup.  
Once the steamapps folder is configured, the app will scan your Steam workshop folder for any custom maps you have downloaded, and fetch the latest information about those maps from their respective workshop pages.  
This is called Map Discovery and can take a few minutes if it runs for the first time, depending on the number of maps you have downloaded.

From that point on, everything should be fairly self-explanatory.

When you click on the 'Load map' button in the web interface, the app simply copies the .udk file from the workshop folder to the .upk file in CookedPCConsole\\mods and overwrites it if necessary.  

## How to install
1. Make sure you have Java 8 installed. If not, you can download it [here](https://www.java.com/de/download/).
2. Go to the [Releases page](https://github.com/Yggdrasil128/RocketLeague-MapManager/releases) and download the .jar file of the latest release.
3. Launch the .jar file. A browser tab will open.
4. Follow the setup instructions.
5. Wait until installation and Map Discovery have finished.
6. Enjoy!

The app installs itself into the 'RL-MapManager' folder in your user directory.

If you encounter any errors or have any suggestions, feel free to open an issue on this repository.
