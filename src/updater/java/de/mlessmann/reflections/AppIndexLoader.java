package de.mlessmann.reflections;

import com.google.common.base.Predicate;
import de.mlessmann.updates.IAppUpdateIndex;
import de.mlessmann.updates.UpdateManager;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 07.07.16.
 */
public class AppIndexLoader {


    private Logger myLogger = Logger.getGlobal();
    private UpdIndexProvider myProvider;
    private UpdateManager myMaster;

    public void loadAll() {

        if (myProvider == null) return;

        ClassLoader loader = getClass().getClassLoader();

        URLClassLoader ucl;

        if (loader instanceof URLClassLoader) ucl = (URLClassLoader) loader;
        else throw new RuntimeException("HWUpdIndexLoader: Classloader is not an instance of URLClassLoader - Unable to load update indices");

        URL[] urls = ucl.getURLs();

        //Set up filters
        String partPkgName = "^.*(indices).*$*";

        @SuppressWarnings("Guava") Predicate<String> filter = new FilterBuilder().include(partPkgName);

        //Set up configuration builder

        ConfigurationBuilder cBuilder = new ConfigurationBuilder();

        cBuilder.filterInputsBy(filter);
        cBuilder.setUrls(urls);

        Reflections ref = new Reflections(cBuilder);

        Set<Class<?>> classes = ref.getTypesAnnotatedWith(AppUpdateIndex.class);


        classes.stream().filter(c1 -> c1 != null)
                .forEach(this::loadFromClass);

    }

    private void loadFromClass(Class<?> c) {

        if (!IAppUpdateIndex.class.isAssignableFrom(c)) {

            myLogger.info("Class " + c.toString() + " does not implement IAppUpdateIndex, but is annotated!");

            return;

        }

        try {

            myLogger.finest("Instantiating new UpdateIndex from " + c.toString());

            Object o = null;

            try {

                o = c.getDeclaredConstructor(UpdateManager.class).newInstance(myMaster);

            } catch (NoSuchMethodException ex1) {

                try {

                    o = c.getDeclaredConstructor().newInstance();

                } catch (NoSuchMethodException ex2) {
                    //Do not care
                }

            }

            if (o == null) o = c.newInstance();


            IAppUpdateIndex h = (IAppUpdateIndex) o;

            //myLogger.finest("Resulting class: " + v.getClass().toString());

            myProvider.registerIndex(h);

        } catch (Exception e) {

            myLogger.warning("Unable to create IAppUpdateIndex from class \"" + c.toString() + "\": " + e.toString());
            e.printStackTrace();

        }

    }

    public void setLogger(Logger l) { myLogger = l; }

    public void setProvider(UpdIndexProvider p) { myProvider = p; }

    public void setMaster(UpdateManager m) { myMaster = m; }

}
