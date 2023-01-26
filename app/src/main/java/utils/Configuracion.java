package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JPB
 */
public class Configuracion {
    public Properties getProperties(){
        Properties configuracion=new Properties();

        try {
            //configuracion.load(getClass().getResourceAsStream("/config/config.properties"));
            //configuracion.load(new FileInputStream(new File("C:\\Users\\eduardo\\Documents\\Eduardo Pintos\\msgraph-training-java\\graphtutorial\\app\\src\\main\\java\\config\\config.properties")));
            configuracion.load(new FileInputStream(new File("C:\\Users\\AGR\\IdeaProjects\\graphtutorial\\app\\src\\main\\java\\config\\config.properties")));

            if(!configuracion.isEmpty()){
                return configuracion;
            } else {
                return null;
            }

        } catch (IOException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("No se ha podido encontrar el fichero con los datos de la configuración.");
            return null;
        }
    }
}