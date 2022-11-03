/*
!!!NOTICE!!!
This is a modified version of Enaium's LaunchWrapper, specifically a modified version of the Launch class.
This class was modified because the URL finder on the unmodified version assumed that the user was using Microsoft Windows.
This is a problem for me since my operating system is Arch Linux, so I changed this so people who use macOS or Linux can use VulpesLoader.
Although it doesn't appear as if it's under a license, I decided to provide attribution anyway.
The original class can be found here: https://github.com/Enaium/LaunchWrapper/blob/master/src/main/java/net/minecraft/launchwrapper/Launch.java
 */

package net.minecraft.launchwrapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.List;

import org.apache.logging.log4j.Level;

import javax.swing.*;

public class Launch {
    private static final String DEFAULT_TWEAK = "sh.talonfox.vulpesloader.bootstrap.MinecraftClientBootstrap";
    public static File minecraftHome;
    public static File assetsDir;
    public static Map<String, Object> blackboard;

    public static void main(String[] args) {
        new Launch().launch(args);
    }

    public static LaunchClassLoader classLoader;

    private Launch() {
        //Get classpath
        List<URL> urls = new ArrayList<>();
        if (getClass().getClassLoader() instanceof URLClassLoader) {
            Collections.addAll(urls, ((URLClassLoader) getClass().getClassLoader()).getURLs());
        } else {
            for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
                try {
                    urls.add(new File(s).toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        classLoader = new LaunchClassLoader(urls.toArray(new URL[0]));
        blackboard = new HashMap<>();
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private void launch(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSpec<String> profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        final OptionSpec<File> gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        final OptionSpec<File> assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
        final OptionSpec<String> tweakClassOption = parser.accepts("tweakClass", "Tweak class(es) to load").withRequiredArg().defaultsTo(DEFAULT_TWEAK);
        final OptionSpec<String> nonOption = parser.nonOptions();

        final OptionSet options = parser.parse(args);
        minecraftHome = options.valueOf(gameDirOption);
        assetsDir = options.valueOf(assetsDirOption);
        final String profileName = options.valueOf(profileOption);
        final List<String> tweakClassNames = new ArrayList<>(options.valuesOf(tweakClassOption));

        final List<String> argumentList = new ArrayList<>();
        // This list of names will be interacted with through tweakers. They can append to this list
        // any 'discovered' tweakers from their preferred mod loading mechanism
        // By making this object discoverable and accessible it's possible to perform
        // things like cascading of tweakers
        blackboard.put("TweakClasses", tweakClassNames);

        // This argument list will be constructed from all tweakers. It is visible here so
        // all tweakers can figure out if a particular argument is present, and add it if not
        blackboard.put("ArgumentList", argumentList);

        // This is to prevent duplicates - in case a tweaker decides to add itself or something
        final Set<String> allTweakerNames = new HashSet<>();
        // The 'definitive' list of tweakers
        final List<ITweaker> allTweakers = new ArrayList<>();
        try {
            final List<ITweaker> tweakers = new ArrayList<>(tweakClassNames.size() + 1);
            // The list of tweak instances - may be useful for interoperability
            blackboard.put("Tweaks", tweakers);
            // The primary tweaker (the first one specified on the command line) will actually
            // be responsible for providing the 'main' name and generally gets called first
            ITweaker primaryTweaker = null;
            // This loop will terminate, unless there is some sort of pathological tweaker
            // that reinserts itself with a new identity every pass
            // It is here to allow tweakers to "push" new tweak classes onto the 'stack' of
            // tweakers to evaluate allowing for cascaded discovery and injection of tweakers
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    // Safety check - don't reprocess something we've already visited
                    if (allTweakerNames.contains(tweakName)) {
                        LogWrapper.log(Level.WARN, "Tweak class name %s has already been visited -- skipping", tweakName);
                        // remove the tweaker from the stack otherwise it will create an infinite loop
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LogWrapper.log(Level.INFO, "Loading tweak class name %s", tweakName);

                    // Ensure we allow the tweak class to load with the parent classloader
                    classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).newInstance();
                    tweakers.add(tweaker);

                    // Remove the tweaker from the list of tweaker names we've processed this pass
                    it.remove();
                    // If we haven't visited a tweaker yet, the first will become the 'primary' tweaker
                    if (primaryTweaker == null) {
                        LogWrapper.log(Level.INFO, "Using primary tweak class name %s", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                // Now, iterate all the tweakers we just instantiated
                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    LogWrapper.log(Level.INFO, "Calling tweak class %s", tweaker.getClass().getName());
                    tweaker.acceptOptions(options.valuesOf(nonOption), minecraftHome, assetsDir, profileName);
                    tweaker.injectIntoClassLoader(classLoader);
                    allTweakers.add(tweaker);
                    // again, remove from the list once we've processed it, so we don't get duplicates
                    it.remove();
                }
                // continue around the loop until there's no tweak classes
            } while (!tweakClassNames.isEmpty());

            // Once we're done, we then ask all the tweakers for their arguments and add them all to the
            // master argument list
            for (final ITweaker tweaker : allTweakers) {
                argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }

            // Finally, we turn to the primary tweaker, and let it tell us where to go to launch
            final String launchTarget = primaryTweaker.getLaunchTarget();
            if(launchTarget.endsWith("Applet")) {
                final Map<String, String> params = new HashMap<String, String>();

                String name = "Player" + System.currentTimeMillis() % 1000;
                if (args.length > 0) name = args[0];

                String sessionId = "-";
                if (args.length > 1) sessionId = args[1];

                params.put("username", name);
                params.put("sessionid", sessionId);

                final Class<?> clazz = Class.forName(launchTarget, false, classLoader);

                LogWrapper.info("Launching wrapped minecraft applet {%s}", launchTarget);
                Applet applet = (Applet)clazz.getDeclaredConstructor().newInstance();

                final Frame launcherFrameFake = new Frame();
                launcherFrameFake.setTitle("Minecraft");
                launcherFrameFake.setBackground(Color.BLACK);

                final JPanel panel = new JPanel();
                launcherFrameFake.setLayout(new BorderLayout());
                panel.setPreferredSize(new Dimension(854, 480));
                launcherFrameFake.add(panel, BorderLayout.CENTER);
                launcherFrameFake.pack();

                launcherFrameFake.setLocationRelativeTo(null);
                launcherFrameFake.setVisible(true);

                launcherFrameFake.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(1);
                    }
                });

                class LauncherFake extends Applet implements AppletStub {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void appletResize(int width, int height) {

                    }


                    @Override
                    public boolean isActive() {
                        return true;
                    }

                    @Override
                    public URL getDocumentBase() {
                        try {
                            return new URL("http://www.minecraft.net/game/");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public URL getCodeBase() {
                        try {
                            return new URL("http://www.minecraft.net/game/");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public String getParameter(String paramName) {
                        if (params.containsKey(paramName)) {
                            return params.get(paramName);
                        }
                        System.err.println("Client asked for parameter: " + paramName);
                        return null;
                    }
                }

                final LauncherFake fakeLauncher = new LauncherFake();
                applet.setStub(fakeLauncher);

                fakeLauncher.setLayout(new BorderLayout());
                fakeLauncher.add(applet, BorderLayout.CENTER);
                fakeLauncher.validate();

                launcherFrameFake.removeAll();
                launcherFrameFake.setLayout(new BorderLayout());
                launcherFrameFake.add(fakeLauncher, BorderLayout.CENTER);
                launcherFrameFake.validate();

                applet.init();
                applet.start();

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        applet.stop();
                    }
                });
            } else {
                final Class<?> clazz = Class.forName(launchTarget, false, classLoader);
                final Method mainMethod = clazz.getMethod("main", String[].class);

                LogWrapper.info("Launching wrapped minecraft {%s}", launchTarget);
                mainMethod.invoke(null, (Object) argumentList.toArray(new String[0]));
            }
        } catch (Exception e) {
            LogWrapper.log(Level.ERROR, e, "Unable to launch");
            System.exit(1);
        }
    }
}
