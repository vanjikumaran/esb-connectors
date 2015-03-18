copy wso2esb.4.8.1.zip file here...
Make sure your axis2.xml contains following entries.

    <messageFormatter contentType="multipart/form-data" class="org.apache.axis2.transport.http.MultipartFormDataFormatter"/>

    <messageBuilder contentType="multipart/form-data" class="org.wso2.carbon.relay.BinaryRelayBuilder"/>
