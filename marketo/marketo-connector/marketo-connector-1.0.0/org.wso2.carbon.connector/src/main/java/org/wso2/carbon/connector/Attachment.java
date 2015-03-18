package main.java.org.wso2.carbon.connector;

import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.connector.core.*;
import org.apache.axiom.attachments.Attachments;
import org.apache.synapse.SynapseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.String;
import java.lang.System;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class Attachment extends AbstractConnector{

    private final String boundary = "----=_WebKitFormBoundary" + System.currentTimeMillis();

    OutputStream httpStream;

    DataOutputStream wr;

    HttpURLConnection httpURLConnection;
    URL apiUrl;

    final String LINE_FEED = "\r\n";

    public static final String API_URI = "marketo.apiURI";
    public static final String HTTP_METHOD = "marketo.httpMethod";
    public static final String URL_PARAMETERS = "marketo.apiParameters";
    public static final String FIELD_NAME = "marketo.fieldName";
    public static final String FILE_PATH = "marketo.filePath";

    private static final String UTF8 = "UTF-8";

//    public boolean mediate(MessageContext messageContext) {
//        try {
//            sendRequest(messageContext);
//        } catch (Exception e) {
//            throw new SynapseException(e);
//        }
//        return true;
//    }

    public void connect(MessageContext msgctx) throws ConnectException {

        String httpMethod = msgctx.getProperty(HTTP_METHOD).toString();
        String apiUri = msgctx.getProperty(API_URI).toString();
        String urlParameters = msgctx.getProperty(URL_PARAMETERS).toString();
        String fieldName = msgctx.getProperty(FIELD_NAME).toString();
        String filePath = msgctx.getProperty(FILE_PATH).toString();
        try {
            apiUrl = new URL(apiUri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            httpURLConnection = (HttpURLConnection) apiUrl.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            httpURLConnection.setRequestMethod(httpMethod);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        httpURLConnection.setRequestProperty("User-Agent", "Synapse-PT-HttpComponents-NIO");
        httpURLConnection.setRequestProperty("Host", apiUrl.getHost().toString());

        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        String response= null;
        try {
            wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            httpStream = httpURLConnection.getOutputStream();
            addFileToRequest(fieldName,filePath);
            response = readResponse(httpURLConnection);
        } catch (IOException e) {
            e.printStackTrace();
        }

       msgctx.setProperty("response",response.toString());
    }
    public void addFileToRequest(String fieldName, String filePath) throws IOException {

        File file = null;

        InputStream inputStream = null;
        try {

            file = new File(filePath);
            String contentType;

            inputStream = new FileInputStream(file);
            contentType = HttpURLConnection.guessContentTypeFromStream(inputStream);
            inputStream.close();

            addFileToRequest(fieldName, file, contentType,filePath);

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

        }
    }
    public void addFileToRequest(String fieldName, File file, String contentType, String fileName)
            throws IOException {

        FileInputStream inputStream = null;
        try {
            StringBuilder builder = new StringBuilder();

            builder.append("--").append(boundary).append(LINE_FEED);

            builder.append(
                    "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);

            builder.append("Content-Type: " + contentType).append(LINE_FEED);
            builder.append("Content-Transfer-Encoding: binary").append(LINE_FEED).append(LINE_FEED);

            httpStream.write(builder.toString().getBytes());

            httpStream.flush();

            // process File
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[10485760];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                httpStream.write(buffer, 0, bytesRead);
            }
            httpStream.flush();
            inputStream.close();
            httpStream.write(LINE_FEED.getBytes());
            httpStream.flush();

        } finally {
            if (inputStream != null)
                inputStream.close();
        }

    }
    private String readResponse(HttpURLConnection con) throws IOException {

        InputStream responseStream = null;
        String responseString = null;

        if (con.getResponseCode() >= 400) {
            responseStream = con.getErrorStream();
        } else {
            responseStream = con.getInputStream();
        }

        if (responseStream != null) {

            StringBuilder stringBuilder = new StringBuilder();
            byte[] bytes = new byte[1024];
            int len;

            while ((len = responseStream.read(bytes)) != -1) {
                stringBuilder.append(new String(bytes, 0, len));
            }

            if (!stringBuilder.toString().trim().isEmpty()) {
                responseString = stringBuilder.toString();
            }

        }

        return responseString;
    }

}
