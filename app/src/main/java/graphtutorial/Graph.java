// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

// <ImportSnippet>
package graphtutorial;

import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.nio.file.Files;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentCollectionResponse;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.UserCollectionPage;

import okhttp3.Request;
// </ImportSnippet>

public class Graph {
    // <UserAuthConfigSnippet>
    private static Properties _properties;
    private static DeviceCodeCredential _deviceCodeCredential;
    private static GraphServiceClient<Request> _userClient;

    public static void initializeGraphForUserAuth(Properties properties, Consumer<DeviceCodeInfo> challenge) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        _properties = properties;

        final String clientId = properties.getProperty("app.clientId");
        final String authTenantId = properties.getProperty("app.authTenant");
        final List<String> graphUserScopes = Arrays
            .asList(properties.getProperty("app.graphUserScopes").split(","));

        _deviceCodeCredential = new DeviceCodeCredentialBuilder()
            .clientId(clientId)
            .tenantId(authTenantId)
            .challengeConsumer(challenge)
            .build();

        final TokenCredentialAuthProvider authProvider =
            new TokenCredentialAuthProvider(graphUserScopes, _deviceCodeCredential);

        _userClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();
    }
    // </UserAuthConfigSnippet>

    // <GetUserTokenSnippet>
    public static String getUserToken() throws Exception {
        // Ensure credential isn't null
        if (_deviceCodeCredential == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        final String[] graphUserScopes = _properties.getProperty("app.graphUserScopes").split(",");

        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphUserScopes);

        final AccessToken token = _deviceCodeCredential.getToken(context).block();
        return token.getToken();
    }
    // </GetUserTokenSnippet>

    // <GetUserSnippet>
    public static User getUser() throws Exception {
        // Ensure client isn't null
        if (_userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        return _userClient.me()
            .buildRequest()
            .select("displayName,mail,userPrincipalName")
            .get();
    }
    // </GetUserSnippet>

    // <GetInboxSnippet>
    public static MessageCollectionPage getInbox() throws Exception {
        // Ensure client isn't null
        if (_userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        return _userClient.me()
            .mailFolders("inbox")
            .messages()
            .buildRequest()
            .select("from,isRead,receivedDateTime,subject")
            .top(25)
            .orderBy("receivedDateTime DESC")
            .get();
    }
    // </GetInboxSnippet>

    // <SendMailSnippet>
    public static void sendMail(String subject, String body, String recipient, File attachment) throws Exception {
        // Ensure client isn't null
        if (_userClient == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }

        // Create a new message
        String subjectUTF8 = new String(subject.getBytes("ISO-8859-1"), "UTF-8");
        String bodyUTF8 = new String(body.getBytes("ISO-8859-1"), "UTF-8");
        final Message message = new Message();
        message.subject = subjectUTF8;
        message.body = new ItemBody();
        message.body.content = bodyUTF8;
        message.body.contentType = BodyType.HTML;
        message.body.oDataType = "#microsoft.graph.itemBody";

        LinkedList<Recipient> toRecipientsList = new LinkedList<Recipient>();

        String[] dest = recipient.split(";");

        for (int i = 0; i < dest.length; i++) {
            Recipient toRecipient = new Recipient();
            toRecipient.emailAddress = new EmailAddress();
            toRecipient.emailAddress.address = dest[i];
            toRecipientsList.add(toRecipient);
        }

        message.toRecipients = toRecipientsList;

        LinkedList<Attachment> attachmentsList = new LinkedList<Attachment>();
        //File adjunto = new File("C:\\Users\\eduardo\\Documents\\Eduardo Pintos\\Captura-BigData.JPG");
        File adjunto = attachment;
        FileAttachment attachments = new FileAttachment();
        //attachments.name = "Captura-BigData.JPG";
        attachments.name = attachment.getName();
        //attachments.contentType = "image/jpg";
        attachments.contentType = "application/pdf";
        //attachments.contentLocation = "C:\\Users\\eduardo\\Documents\\Eduardo Pintos\\Captura-BigData.JPG";
        //attachments.contentBytes = Base64.getDecoder().decode("SGVsbG8gV29ybGQh");
        attachments.contentBytes = Base64.getDecoder().decode(encodeFileToBase64(adjunto));
        attachments.oDataType = "#microsoft.graph.fileAttachment";
        attachmentsList.add(attachments);
        AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
        attachmentCollectionResponse.value = attachmentsList;
        AttachmentCollectionPage attachmentCollectionPage = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        message.attachments = attachmentCollectionPage;

        // Send the message
        _userClient.me()
            .sendMail(UserSendMailParameterSet.newBuilder()
                .withMessage(message)
                    .withSaveToSentItems(null)
                .build())
            .buildRequest()
            .post();
    }
    // </SendMailSnippet>

    // <AppOnyAuthConfigSnippet>
    private static ClientSecretCredential _clientSecretCredential;
    private static GraphServiceClient<Request> _appClient;

    private static void ensureGraphForAppOnlyAuth() throws Exception {
        // Ensure _properties isn't null
        if (_properties == null) {
            throw new Exception("Properties cannot be null");
        }

        if (_clientSecretCredential == null) {
            final String clientId = _properties.getProperty("app.clientId");
            final String tenantId = _properties.getProperty("app.tenantId");
            final String clientSecret = _properties.getProperty("app.clientSecret");

            _clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .build();
        }

        if (_appClient == null) {
            final TokenCredentialAuthProvider authProvider =
                new TokenCredentialAuthProvider(
                    List.of("https://graph.microsoft.com/.default"), _clientSecretCredential);

            _appClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
        }
    }
    // </AppOnyAuthConfigSnippet>

    // <GetUsersSnippet>
    public static UserCollectionPage getUsers() throws Exception {
        ensureGraphForAppOnlyAuth();

        return _appClient.users()
            .buildRequest()
            .select("displayName,id,mail")
            .top(25)
            .orderBy("displayName")
            .get();
    }
    // </GetUsersSnippet>

    // <MakeGraphCallSnippet>
    public static void makeGraphCall() {
        // INSERT YOUR CODE HERE
        // Note: if using _appClient, be sure to call ensureGraphForAppOnlyAuth
        // before using it.
        // ensureGraphForAppOnlyAuth();
    }
    // </MakeGraphCallSnippet>

    private static byte[] encodeFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encode(fileContent);
        } catch (IOException e) {
            throw new IllegalStateException("could not read file " + file, e);
        }
    }
}
