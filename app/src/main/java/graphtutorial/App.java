// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

// <ImportSnippet>
package graphtutorial;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.Scanner;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.UserCollectionPage;
import utils.Configuracion;
import utils.SQLconnection;

import static java.lang.Thread.sleep;
// </ImportSnippet>

public class App {
    static SQLconnection conexion;
    static File registro;

    // <MainSnippet>
    public static void main(String[] args) {
        //Declaración de variables
        String identificador[];
        int iden[];
        String destinatario[];
        String usuario[];
        String password[];
        String rzSocial[];
        String carta;
        String resultados[][];
        int conteo=0;
        int aux;
        int estado=0;
        Date fechaEnvio;
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        System.out.println("Envío de correos TEyS");
        System.out.println();

        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
            return;
        }

        final Properties propiedades = new Configuracion().getProperties();

        if (propiedades != null) {

            initializeGraph(oAuthProperties);

            greetUser();

            if (propiedades.getProperty("entorno").equals("Produccion")) {
                conexion = new SQLconnection(propiedades.getProperty("dirServerPro"), propiedades.getProperty("userServer"), propiedades.getProperty("passServer"), propiedades.getProperty("bdServer"));
            } else {
                conexion = new SQLconnection(propiedades.getProperty("dirServerDes"), propiedades.getProperty("userServer"), propiedades.getProperty("passServer"), propiedades.getProperty("bdServer"));
            }

            try {
                conexion.connectar();

                //Se realiza un conteo de cuántos emails hay que enviar
                conexion.consultar("SELECT COUNT(*) FROM ZPRUEBAEMAIL WHERE COALESCE(emailMod,email) IS NOT NULL AND idTrimestre = 12 AND descCarga = 1 AND iden = 2");

                conexion.getResultSet().next();

                identificador = new String[conexion.getResultSet().getInt(1)];
                iden = new int[conexion.getResultSet().getInt(1)];
                destinatario  = new String[conexion.getResultSet().getInt(1)];
                carta = new String();
                resultados = new String[conexion.getResultSet().getInt(1)][6];
                usuario  = new String[conexion.getResultSet().getInt(1)];
                password = new String[conexion.getResultSet().getInt(1)];
                rzSocial = new String[conexion.getResultSet().getInt(1)];

                //Se obtienen y se guardan los datos de los emails a enviar
                conexion.consultar("SELECT M.iden, M.identificador, M.rzSocial, COALESCE(M.emailMod, M.email) AS emailMod, M.usuario, M.password FROM ZPRUEBAEMAIL M WHERE COALESCE(emailMod,email) IS NOT NULL AND idTrimestre = 12 AND descCarga = 1 AND iden = 2");

                while(conexion.getResultSet().next()){
                    identificador[conteo] = conexion.getResultSet().getString("identificador");
                    iden[conteo] = conexion.getResultSet().getInt("iden");
                    destinatario[conteo] = conexion.getResultSet().getString("emailMod");
                    rzSocial[conteo] = conexion.getResultSet().getString("rzSocial");
                    usuario[conteo] = conexion.getResultSet().getString("usuario");
                    password[conteo] = conexion.getResultSet().getString("password");

                    conteo++;
                }

                //Se inicia el envío
                for(aux = 0; aux < identificador.length ; aux++){
                    estado = 0;

                    try {
                        fechaEnvio = new Date();
                        carta = "C:\\Users\\eduardo\\Documents\\Eduardo Pintos\\msgraph-training-java\\graphtutorial\\app\\ES6333-CartaECE_4T2022_firmada.pdf";
                        System.out.println(aux + 1);
                        System.out.println(identificador[aux]);
                        sendMail(destinatario[aux], carta, rzSocial[aux], identificador[aux], usuario[aux], password[aux]);
                        sleep(1000);
                    } catch (Exception ex) {
                        System.out.println("Se ha producido un error durante el envío del identificador: " + identificador[aux]);
                        fechaEnvio = new Date();
                        estado = 1;
                    }

                    //Se guarda si ha funcionado o no el envío en un array
                    switch(estado){
                        //Correcto
                        case 0:{
                            resultados[aux][0]=identificador[aux];
                            resultados[aux][1]="1";
                            resultados[aux][2]=formato.format(fechaEnvio);
                            resultados[aux][3]=String.valueOf(iden[aux]);

                            break;
                        }
                        //Fallo
                        case 1:{
                            resultados[aux][0]=identificador[aux];
                            resultados[aux][1]="0";
                            resultados[aux][2]=formato.format(fechaEnvio);
                            resultados[aux][3]=String.valueOf(iden[aux]);

                            break;
                        }
                    }
                }

                for(int i=0 ; i < aux ; i++){
                    conexion.ejecutar("INSERT INTO zReenvioEmail SELECT '"
                            +resultados[i][2]+"', '"+resultados[i][0]+"', "
                            +resultados[i][3]+", "+resultados[i][1]);
                }

                conexion.desconectar();

            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
/*
        Scanner input = new Scanner(System.in);

        int choice = -1;

        while (choice != 0) {
            System.out.println("Please choose one of the following options:");
            System.out.println("0. Exit");
            System.out.println("1. Display access token");
            System.out.println("2. List my inbox");
            System.out.println("3. Send mail");
            System.out.println("4. List users (required app-only)");
            System.out.println("5. Make a Graph call");

            try {
                choice = input.nextInt();
            } catch (InputMismatchException ex) {
                // Skip over non-integer input
            }

            input.nextLine();

            // Process user choice
            switch(choice) {
                case 0:
                    // Exit the program
                    System.out.println("Goodbye...");
                    break;
                case 1:
                    // Display access token
                    displayAccessToken();
                    break;
                case 2:
                    // List emails from user's inbox
                    listInbox();
                    break;
                case 3:
                    // Send an email message
                    sendMail();
                    break;
                case 4:
                    // List users
                    listUsers();
                    break;
                case 5:
                    // Run any Graph code
                    makeGraphCall();
                    break;
                default:
                    System.out.println("Invalid choice");
            }
        }

        input.close();

 */
    }
    // </MainSnippet>

    // <InitializeGraphSnippet>
    private static void initializeGraph(Properties properties) {
        try {
            Graph.initializeGraphForUserAuth(properties,
                challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }
    // </InitializeGraphSnippet>

    // <GreetUserSnippet>
    private static void greetUser() {
        try {
            final User user = Graph.getUser();
            // For Work/school accounts, email is in mail property
            // Personal accounts, email is in userPrincipalName
            final String email = user.mail == null ? user.userPrincipalName : user.mail;
            System.out.println("Bienvenid@, " + user.displayName + "!");
            System.out.println("Email: " + email);
        } catch (Exception e) {
            System.out.println("Error getting user");
            System.out.println(e.getMessage());
        }
    }
    // </GreetUserSnippet>

    // <DisplayAccessTokenSnippet>
    /*private static void displayAccessToken() {
        try {
            final String accessToken = Graph.getUserToken();
            System.out.println("Access token: " + accessToken);
        } catch (Exception e) {
            System.out.println("Error getting access token");
            System.out.println(e.getMessage());
        }
    }*/
    // </DisplayAccessTokenSnippet>

    // <ListInboxSnippet>
    /*private static void listInbox() {
        try {
            final MessageCollectionPage messages = Graph.getInbox();

            // Output each message's details
            for (Message message: messages.getCurrentPage()) {
                System.out.println("Message: " + message.subject);
                System.out.println("  From: " + message.from.emailAddress.name);
                System.out.println("  Status: " + (message.isRead ? "Read" : "Unread"));
                System.out.println("  Received: " + message.receivedDateTime
                    // Values are returned in UTC, convert to local time zone
                    .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
            }

            final Boolean moreMessagesAvailable = messages.getNextPage() != null;
            System.out.println("\nMore messages available? " + moreMessagesAvailable);
        } catch (Exception e) {
            System.out.println("Error getting inbox");
            System.out.println(e.getMessage());
        }
    }*/
    // </ListInboxSnippet>

    // <SendMailSnippet>
    private static void sendMail(String destinatario, String carta, String rzSocial, String identificador, String usuario, String password) throws Exception {
        final String subject = "Encuesta de Coyuntura de la Exportación";
        final File attachment = new File(carta);

        final String body = "<html><head><meta charset=\"utf-8\"></head><br><br><b>Empresa: " + rzSocial +
                "</b> <br><br> Estimado/a Sr./Sra:"

                +"<br><br>El Ministerio de Industria, Comercio y Turismo, realiza anualmente "
                +"la <b><i>\"Encuesta de Coyuntura de la Exportación\"</i></b>. Esta encuesta está dirigida "
                +"a empresas (tanto personas físicas como jurídicas) ubicadas en el territorio "
                +"Español que realizan de forma continua operaciones "
                +"de exportación de mercancías."

                +"<br><br>El objetivo de esta encuesta es disponer de un conocimiento más profundo de la "
                +"situación del sector exportador español."

                +"<br><br>El Ministerio de Industria, Comercio y Turismo ha contratado con la empresa "
                +"<b>TYPSA Estadística y Servicios S.L. (TEyS)</b> la realización del trabajo de campo "
                +"y el tratamiento de la información de dicha Encuesta."

                +"<br><br>Su empresa <b>ha sido seleccionada</b> para participar en la citada encuesta, por lo que "
                +"adjunto se le remite el cuestionario correspondiente al tercer trimestre de 2022, "
                +"rogándole lo cumplimente con la información que se solicita, independientemente de cuál haya sido "
                +"la actividad de su empresa en dicho año, <b>en el plazo máximo de 5 días hábiles desde la recepción de este correo.</b>"

                +"<br><br>Puede cumplimentar el cuestionario a través de la página Web siguiendo el enlace:"
                +"<br><a href=\"https://www.testadistica.es/ECEX/\">https://www.testadistica.es/ECEX/</a>"
                +"<br><br>Una vez dentro de la Web, deberá introducir el <b>Usuario " + usuario + "</b> y <b>Contraseña " + password + "</b> correspondientes a su empresa."

                +"<br><br>La información suministrada está sometida al <b>\"Secreto Estadístico\"</b>, regulado por la "
                +"Ley 12/1989, de 9 de mayo, de la Función Estadística Pública, "
                +"que garantiza su utilización únicamente para fines estadísticos."
                +"<br><br>También quiero recordarle que, al ser una estadística incluida en el Plan Estadístico Nacional, "
                +"<b>su cumplimentación es obligatoria</b>, según se recoge en el Artículo 3 de dicho plan."
                +"<br><br>Facilitamos el siguiente teléfono de contacto, al que puede acudir para cualquier aclaración o "
                +"duda que pueda surgirle, <b>91 154 80 87</b>; asimismo puede dirigirse a la dirección de correo "
                +"electrónico: coyunturaexportacion@teys.eu"

                + "<br><br>Agradeciéndole de antemano su colaboración e interés, le saluda atentamente."
                + "<br><br>TYPSA Estadística y Servicios S.L. (TEyS). "
                + "</html>";

        Graph.sendMail(subject, body, destinatario, attachment);
        System.out.println("\nEmail enviado: " + identificador);
    }
    // </SendMailSnippet>

    // <ListUsersSnippet>
    /*private static void listUsers() {
        try {
            final UserCollectionPage users = Graph.getUsers();

            // Output each user's details
            for (User user: users.getCurrentPage()) {
                System.out.println("User: " + user.displayName);
                System.out.println("  ID: " + user.id);
                System.out.println("  Email: " + user.mail);
            }

            final Boolean moreUsersAvailable = users.getNextPage() != null;
            System.out.println("\nMore users available? " + moreUsersAvailable);
        } catch (Exception e) {
            System.out.println("Error getting users");
            System.out.println(e.getMessage());
        }
    }*/
    // </ListUsersSnippet>

    // <MakeGraphCallSnippet>
    /*private static void makeGraphCall() {
        try {
            Graph.makeGraphCall();
        } catch (Exception e) {
            System.out.println("Error making Graph call");
            System.out.println(e.getMessage());
        }
    }*/
    // </MakeGraphCallSnippet>
}
