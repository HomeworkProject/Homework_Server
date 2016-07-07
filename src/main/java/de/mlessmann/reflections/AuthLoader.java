package de.mlessmann.reflections;

import com.google.common.base.Predicate;
import de.mlessmann.authentication.IAuthMethod;
import de.mlessmann.hwserver.HWServer;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by magnus.lessmann on 06.07.2016.
 */
public class AuthLoader {

    private Logger myLogger = Logger.getGlobal();
    private AuthProvider myProvider;
    private HWServer myMaster;

    public void loadAll() {

        if (myProvider == null) return;

        ClassLoader loader = getClass().getClassLoader();

        URLClassLoader ucl;

        if (loader instanceof URLClassLoader) ucl = (URLClassLoader) loader;
        else throw new RuntimeException("HWCommandLoader: Classloader is not an instance of URLClassLoader - Unable to load auth methods");

        URL[] urls = ucl.getURLs();

        //Set up filters
        //String partPkgName = "/^.*(authentication).*$/ig";
        String partPkgName = "^.*(authentication).*$*";

        @SuppressWarnings("Guava") Predicate<String> filter = new FilterBuilder().include(partPkgName);

        //Set up configuration builder

        ConfigurationBuilder cBuilder = new ConfigurationBuilder();

        cBuilder.filterInputsBy(filter);
        cBuilder.setUrls(urls);

        Reflections ref = new Reflections(cBuilder);

        Set<Class<?>> classes = ref.getTypesAnnotatedWith(HWAuthMethod.class);


        classes.stream().filter(c1 -> c1 != null)
                .forEach(this::loadFromClass);

    }

    private void loadFromClass(Class<?> c) {

        if (!IAuthMethod.class.isAssignableFrom(c)) {

            myLogger.info("Class " + c.toString() + " does not implement IAuthMethod, but is annotated!");

            return;

        }

        try {

            myLogger.finest("Instantiating new AuthMethod from " + c.toString());

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


            IAuthMethod h = (IAuthMethod) o;

            //myLogger.finest("Resulting class: " + v.getClass().toString());

            myProvider.registerMethod(h);

        } catch (Exception e) {

            myLogger.warning("Unable to create IAuthMethod from class \"" + c.toString() + "\": " + e.toString());
            e.printStackTrace();

        }

    }

    public void setLogger(Logger l) { myLogger = l; }

    public void setProvider(AuthProvider p) { myProvider = p; }

    public void setMaster(HWServer m) { myMaster = m; }

}
