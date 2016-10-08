package hwserver;

import de.mlessmann.common.annotations.IndexType;
import de.mlessmann.common.apparguments.AppArgument;
import de.mlessmann.hwserver.HWServer;
import org.reflections.adapters.JavassistAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Life4YourGames on 29.04.16.
 * @author Life4YourGames
 */
public class Main {


    public static void main (String[] args) throws IOException {

        try {
            JavassistAdapter.includeInvisibleTag = false;
            for (Annotation annotation : IndexType.class.getAnnotations()) {
                System.out.println(annotation.toString());
            }

            ArrayList<AppArgument> aa = AppArgument.fromArray(args);

            HWServer hwServer = new HWServer();

            hwServer.preInitialize();

            aa.forEach(hwServer::setArg);

            hwServer.initialize();

            hwServer.start();

        } catch (Exception e) {

            e.printStackTrace();
            try (PrintWriter w = new PrintWriter(new FileWriter("hwserver_crash.log"))) {
                w.println(e.toString());
                e.printStackTrace(w);
                Logger.getGlobal().severe("An unhandled exception occurred! The process is forced to halt!");
                Logger.getGlobal().severe("A crash log has been written to \"hwserver_crash.log\"");
            } catch (IOException e1) {

                Logger.getGlobal().severe("Unable to write crash log: " + e1.toString());
                e1.printStackTrace();

            }
        }

    }


}
