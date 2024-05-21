# DracoLoader

The DracoLoader is the core of Draco, it is responsible for loading mods and providing a backend for SpongePowered's Mixins. It is built to be version independent and can run with Java version 8 or higher (though 17 or higher is recommended), and will work from late beta versions of Minecraft all the way up to the latest snapshots.

## How does it work?

### LaunchWrapper

LaunchWrapper is the essential part of DracoLoader, it provides the ability to add URLs to the Java classpaths as well as providing class transformation functionality. This provides the absolute essential functionality of the loader

## DracoLoader

DracoLoader is essentially a frontend for LaunchWrapper, this program discovers and loads the mod's jar into it's classpath saving it's config and classpaths, it then initializes Mixins and MixinExtras, afterwards, all of the assigned Listener classes are initialized (the `<init>` function is called, the game is in a state we call `PRELOAD`, meaning Mixins are applied but the game hasn't ran yet). The Listeners are then saved so they can be indexed later by the DracoStandardLibrary. Afterwards, LaunchWrapper will pass control to the game, from there the DracoStandardLibrary will use it's hooks to load the game and other mods.