package de.mlessmann.hwserver.reflections;

import com.google.common.base.Predicate;
import de.mlessmann.hwserver.main.HWServer;
import de.mlessmann.hwserver.network.commands.ICommandHandler;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 28.06.16.
 */
public class CommandLoader {

    public static URLClassLoader loader = (URLClassLoader) CommandLoader.class.getClassLoader();

    private Logger myLogger = Logger.getGlobal();
    private CommHandProvider myProvider;
    private HWServer myMaster;

    public void loadAll() {

        if (myProvider == null) return;

        URL[] urls = loader.getURLs();

        //Set up filters
        //String partPkgName = "/^.*(commands).*$/ig";
        String partPkgName = "^.*(commands).*$*";

        @SuppressWarnings("Guava") Predicate<String> filter = new FilterBuilder().include(partPkgName);

        //Set up configuration builder

        ConfigurationBuilder cBuilder = new ConfigurationBuilder();

        cBuilder.filterInputsBy(filter);
        cBuilder.setUrls(urls);

        Reflections ref = new Reflections(cBuilder);

        Set<Class<?>> classes = ref.getTypesAnnotatedWith(HWCommandHandler.class);


        classes.stream().filter(c1 -> c1 != null)
                        .forEach(this::loadFromClass);

    }

    private void loadFromClass(Class<?> c) {

        if (!ICommandHandler.class.isAssignableFrom(c)) {

            myLogger.info("Class " + c.toString() + " does not implement ICommandHandler, but is annotated!");

            return;

        }

        try {

            myLogger.finest("Instantiating new Handler from " + c.toString());

            Object o = null;

            try {

                o = c.getDeclaredConstructor(HWServer.class).newInstance(myMaster);

            } catch (NoSuchMethodException ex1) {

                try {

                    o = c.getDeclaredConstructor().newInstance();

                } catch (NoSuchMethodException ex2) {
                    //Do not care
                }

            }

            if (o == null) o = c.newInstance();


            ICommandHandler h = (ICommandHandler) o;

            //myLogger.finest("Resulting class: " + v.getClass().toString());

            myProvider.registerCommand(h);

        } catch (Exception e) {

            myLogger.warning("Unable to create ICommandHandler from class \"" + c.toString() + "\": " + e.toString());
            e.printStackTrace();

        }

    }

    public void setLogger(Logger l) { myLogger = l; }

    public void setProvider(CommHandProvider p) { myProvider = p; }

    public void setMaster(HWServer m) { myMaster = m; }

}
