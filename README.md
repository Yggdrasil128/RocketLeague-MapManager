# Rocket League Map Manager

This app allows you to easily switch between Rocket League Workshop maps from within the comfort of your browser!  
The app can also automatically start/stop Rocket League for you if you want, and it has a number of other features that
make your life easier.

For example, the map list can be sorted in various ways, you can mark maps as favorite, you can quickly go to the Steam
workshop page of any map, and the app even remembers the last time you loaded a map, so you can easily see which maps
you don't play anymore, to clear them out if you're low on hard drive space.  
You can see a full list of features down below.

## How it works

In order to function, the app must be aware of the location of the Rocket League installation on your computer. This
will be configured during setup, and this is where you can choose between Steam and Epic Games as your Rocket League
platform. The app will then try to auto-detect the location of your Rocket League installation. If that fails, you will
be asked to select the according folder manually.  
If you are using the Steam version, then the app can also automatically scan your Steam workshop folder for any maps
that you have subscribed to and downloaded. This is called Map Discovery and can take a few minutes if it runs for the
first time, depending on the number of maps you have downloaded.

You can import maps into the Map Manager with the "Import Maps" tab. You can choose whether you want to import maps from
the Steam workshop, from lethamyr.com, or from a file on your computer. If you import a map from the internet (either
Steam workshop or lethamyr.com) then the app will also fetch metadata (like title, author name, description, and map
image) from the map's respective web page and display it in the map list.

You can then click the 'Load map' button next to any map in the map list, and the app will then simply copy the
map's `.udk` file to the target `.upk` location in CookedPCConsole\\mods and overwrite it if necessary.

## How to install

A good friend of mine created [this tutorial video on YouTube](https://www.youtube.com/watch?v=9VmGahUXOAI) in which he
shows you step-by-step how to install and use the program.  
If you don't want to watch the video, here's the gist of it:

1. Make sure you have Java 8 installed. If not, you can download it [here](https://www.java.com/de/download/).
2. Go to the [Releases page](https://github.com/Yggdrasil128/RocketLeague-MapManager/releases) and download the .jar
   file of the latest release.
3. Launch the .jar file. A browser tab will open.
4. Follow the setup instructions.
5. Wait until installation (and Map Discovery if you have the Steam version) has finished.
6. Enjoy!

The app installs itself into the 'RL-MapManager' folder in your user directory.

## List of features

- A user-friendly web interface that allows you to easily switch between Rocket League maps
- Supports both the Steam and Epic Games version of Rocket League
- (If you are using the Steam version) Automatically detect which workshop maps are installed on your computer and
  download all relevant information from the Steam workshop page
- You can import maps from the Steam workshop (even if you are using the Epic Games version), from lethamyr.com and from
  a local file
- You can edit map metadata (title, author name, description, map image) and delete maps
- The app detects if the "Workshop Textures" are installed, and offers to download and install them. Some maps may
  require these files in order to work properly.
- Start/Stop Rocket League, either automatically when a map is loaded or manually with the click of a button
- The app remembers the date and time of when each map was added the Map Manager, and when it was last loaded
- 3 different map list layouts: Compact list, Detailed list, and Grid View
- Many map sorting options
- Mark maps as favorite
- A System Tray icon that allows you quickly switch between favorite maps, and start/stop Rocket League
- Optionally, the app can rename the original (real) `Labs_Underpass_P.upk` to something else when a map is loaded, to
  prevent the "Ambiguous package name" warning from showing up
- Autostart with Windows
- Automatic check for updates. Plus, you can install updates with just one click on a button.
- You can set which IP addresses (other than localhost) can have access to the web interface. This allows you to access
  the web interface on a different computer/phone over the local network.

If you encounter any errors or have any suggestions, feel free to open an issue or a discussion on this repository.
