# Rocket League Map Manager
This app allows you to easily switch between Rocket League Workshop maps from within the comfort of your browser!  
The app can also automatically start/stop Rocket League, if so desired, and it has a number of other features to make your life easier.

In order to function, the app must be aware of the location of the steamapps folder on your computer, where Rocket League is installed.
Once that is configured, the app will automatically scan your Steam workshop folder for any custom maps you have downloaded, and fetch the latest information about those maps from their respective workshop pages.  
This is called Map Discovery and can take a few minutes if it runs for the first time, depending on the number of maps you have downloaded.

From that point on, everything should be fairly self-explanatory.

If you have encountered any errors or have any suggestions, feel free to open an issue on this repository.

## How to use
1. Make sure you have Java 8 installed. If not, you can download it [here](https://www.java.com/de/download/).
2. Go to the [Releases page](https://github.com/Yggdrasil128/RocketLeague-MapManager/releases) and download the .jar file from the 'Assets' of the latest release.
3. Launch the .jar file. A browser tab will open.
4. If you have Rocket League installed in `C:\Program Files (x86)\Steam\steamapps` you can skip this step.
Otherwise, go to the Configuration tab, click on 'Choose steamapps folder', and enter the path to the steamapps folder where Rocket League is installed.
5. Wait until Map Discovery has finished.
6. Enjoy!

## Planned Features
- Add more layout options to the map list (compact list and grid view)
- Add a dedicated button for starting/stopping Rocket League