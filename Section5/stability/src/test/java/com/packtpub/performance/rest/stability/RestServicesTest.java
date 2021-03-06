/*
 * Copyright (c) 2014-2018 Gregor Roth and others
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.packtpub.performance.rest.stability;

import static org.junit.Assert.*;
import static com.packtpub.performance.rest.stability.service.payment.PaymentMethod.*;

import java.io.File;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;

import org.apache.catalina.startup.Tomcat;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.packtpub.performance.rest.stability.service.payment.Payment;
import com.packtpub.performance.rest.stability.service.payment.PaymentMethod;
import com.packtpub.performance.rest.stability.service.scoring.Score;

public class RestServicesTest {
    private static Client client;
    private static Tomcat server;
    
    @BeforeClass
    public static void setUp() throws Exception {             
        server = new Tomcat();
        server.setPort(9080);
        server.addWebapp("/service", new File("src/main/resources/webapp").getAbsolutePath());
        server.start();

        ClientConfig clientConfig = new ClientConfig();                    // jersey specific
        clientConfig.connectorProvider(new ApacheConnectorProvider());     // jersey specific       
        RequestConfig reqConfig = RequestConfig.custom()                   // apache HttpClient specific
                                               .build();            
        
        clientConfig.property(ApacheClientProperties.REQUEST_CONFIG, reqConfig); // jersey specific

        client = ClientBuilder.newClient(clientConfig);
                  
        HttpClientBuilder.create()
                         .setMaxConnPerRoute(30)
                         .setMaxConnTotal(150)
                         .setDefaultRequestConfig(reqConfig).build();  
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        client.close();
    }

    /*
    @Test
    public void testResteasy() throws Exception {
        RequestConfig reqConfig = RequestConfig.custom()                   // apache HttpClient specific
                                               .setConnectTimeout(1000)
                                               .setSocketTimeout(1000)
                                               .setConnectionRequestTimeout(1000)
                                               .build();            

        CloseableHttpClient httpClient = HttpClientBuilder.create()        // RESTEasy specific
                                                          .setDefaultRequestConfig(reqConfig)
                                                          .setMaxConnPerRoute(11)
                                                          .build();  
        Client client = new ResteasyClientBuilder().httpEngine(new ApacheHttpClient4Engine(httpClient, true)).build();// RESTEasy specific
    }*/
    
    @Test
    public void testRetrievePayment() throws Exception {
        Payment payment = client.target("http://localhost:9080/service/rest/payments/123443")
                                .request()
                                .get(Payment.class);      
        assertNotNull(payment);
    }

    @Test
    public void testAddressScoreGood() throws Exception {
        Score score = client.target("http://localhost:9080/service/rest/creditscores")
                            .queryParam("addr", "Michael Smith, 5736 Richmond Ave 2 Wappingers FL, NY 12990-9103")
                            .request()
                            .get(Score.class);
        
        assertEquals(Score.POSITIVE, score);
    }

    @Test
    public void testAddressScoreNeutral() throws Exception {
        Score score = client.target("http://localhost:9080/service/rest/creditscores")
                            .queryParam("addr", "Michael Smith, 9424 Westend Ave 2 Wappingers FL, NY 12990-9103")
                            .request()
                            .get(Score.class);
        
        assertEquals(Score.NEUTRAL, score);
    }
    
    @Test
    public void testAddressScoreBad() throws Exception {
        Score score = client.target("http://localhost:9080/service/rest/creditscores")
                            .queryParam("addr", "Michael Smith, 2434 Baltin Ave 2 Wappingers FL, NY 12990-9103")
                            .request()
                            .get(Score.class);
        
        assertEquals(Score.NEGATIVE, score);
    }
    
    @Test
    public void testRetrievePaymentNotFound() throws Exception {
        
        try {
            client.target("http://localhost:9080/service/rest/payments/823443")
                  .request()
                  .get(Payment.class);

            Assert.fail("NotFoundException expected");
        } catch (NotFoundException expected) { }
    }
    
    @Test
    public void testRetrievePaymentNotFoundAsync() throws Exception {
        
        try {
            client.target("http://localhost:9080/service/rest/payments/823443")
                  .request()
                  .get(Payment.class);

            Assert.fail("NotFoundException expected");
        } catch (NotFoundException expected) { }
    }

    @Test
    public void testRetrievePaymentMethodNewUserBadAddress() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/paymentmethods")
                                                  .queryParam("addr", "Michael Smith, 2434 Baltin Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .get(new GenericType<Set<PaymentMethod>>() { });            
        assertEquals(1, paymentMethods.size());
        assertTrue(paymentMethods.contains(PREPAYMENT));
    }
    
    @Test
    public void testRetrievePaymentMethodNewUserGoodAddress() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/sync/paymentmethods")
                                                  .queryParam("addr", "Michael Smith, 5736 Richmond Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .header("X-Client", "Testapp")
                                                  .get(new GenericType<Set<PaymentMethod>>() { });    
        
        assertEquals(4, paymentMethods.size());
        assertTrue(paymentMethods.contains(PREPAYMENT));
        assertTrue(paymentMethods.contains(CREDITCARD));
        assertTrue(paymentMethods.contains(PAYPAL));
        assertTrue(paymentMethods.contains(INVOCE));
    }

    @Test
    public void testRetrievePaymentMethodNewUserGoodAddressAsync() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/paymentmethods")
                                                  .queryParam("addr", "Michael Smith, 5736 Richmond Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .header("X-Client", "Testapp")
                                                  .get(new GenericType<Set<PaymentMethod>>() { });    
        
        assertEquals(4, paymentMethods.size());
        assertTrue(paymentMethods.contains(PREPAYMENT));
        assertTrue(paymentMethods.contains(CREDITCARD));
        assertTrue(paymentMethods.contains(PAYPAL));
        assertTrue(paymentMethods.contains(INVOCE));
    }
    
    @Test
    public void testRetrievePaymentMethodNewUserGoodAddressBulk() throws Exception {
        for (int i = 0; i < 100; i++) {
            testRetrievePaymentMethodNewUserGoodAddress();
        }
    }
    
    @Test
    public void testRetrievePaymentMethodNewUserUnknownAddress() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/sync/paymentmethods")
                                                  .queryParam("addr", "Michael Smith, 9424 Westend Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .get(new GenericType<Set<PaymentMethod>>() { });    
        
        assertEquals(3, paymentMethods.size());
        assertTrue(paymentMethods.contains(PREPAYMENT));
        assertTrue(paymentMethods.contains(CREDITCARD));
        assertTrue(paymentMethods.contains(PAYPAL));
    }
    
    @Test
    public void testRetrievePaymentMethodKnownUserGood() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/sync/paymentmethods")
                                                  .queryParam("addr", "Tom Smith, 2434 Baltin Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .get(new GenericType<Set<PaymentMethod>>() { });
        assertEquals(4, paymentMethods.size());
        assertTrue(paymentMethods.contains(INVOCE));
        assertTrue(paymentMethods.contains(PREPAYMENT));
        assertTrue(paymentMethods.contains(CREDITCARD));
        assertTrue(paymentMethods.contains(PAYPAL));
    }
    
    @Test
    public void testRetrievePaymentMethodKnownUserBad() throws Exception {
        Set<PaymentMethod> paymentMethods = client.target("http://localhost:9080/service/rest/sync/paymentmethods")
                                                  .queryParam("addr", "John Smith, 2434 Baltin Ave 2 Wappingers FL, NY 12990-9103")
                                                  .request()
                                                  .get(new GenericType<Set<PaymentMethod>>() { });    
         
        assertEquals(1, paymentMethods.size());
        assertTrue(paymentMethods.contains(PREPAYMENT));
    }
}