package org.carlspring.strongbox.rest;

import org.carlspring.strongbox.client.RestClient;
import org.carlspring.strongbox.configuration.ProxyConfiguration;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.testing.AssignedPorts;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/strongbox-*-context.xml",
                                    "classpath*:/META-INF/spring/strongbox-*-context.xml" })
public class ConfigurationManagementRestletTest
{

    @Autowired
    private AssignedPorts assignedPorts;

    private RestClient client = new RestClient();


    @Test
    public void testSetAndGetPort()
            throws Exception
    {
        int newPort = 18080;

        int status = client.setListeningPort(newPort);

        assertEquals("Failed to set port!", 200, status);
        assertEquals("Failed to get port!", newPort, client.getListeningPort());
    }

    @Test
    public void testSetAndGetBaseUrl()
            throws Exception
    {
        String baseUrl = "http://localhost:" + 40080 + "/newurl";

        int status = client.setBaseUrl(baseUrl);

        assertEquals("Failed to set baseUrl!", 200, status);

        String b = client.getBaseUrl();

        assertEquals("Failed to get baseUrl!", baseUrl, b);
    }

    @Test
    public void testSetAndGetGlobalProxyConfiguration()
            throws Exception
    {
        ProxyConfiguration proxyConfiguration = createProxyConfiguration();

        int status = client.setProxyConfiguration(proxyConfiguration);

        assertEquals("Failed to set proxy configuration!", 200, status);

        ProxyConfiguration pc = client.getProxyConfiguration();

        assertNotNull("Failed to get proxy configuration!", pc);
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getHost(), pc.getHost());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPort(), pc.getPort());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getUsername(), pc.getUsername());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPassword(), pc.getPassword());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getType(), pc.getType());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getNonProxyHosts(), pc.getNonProxyHosts());
    }

    @Test
    public void testAddGetStorage()
            throws Exception
    {
        String storageId = "storage1";

        Storage storage1 = new Storage("storage1");

        final int response = client.addStorage(storage1);

        assertEquals("Failed to create storage!", 200, response);

        Repository r1 = new Repository("repository0");
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage1);

        Repository r2 = new Repository("repository1");
        r2.setAllowsForceDeletion(true);
        r2.setTrashEnabled(true);
        r2.setStorage(storage1);
        r2.setProxyConfiguration(createProxyConfiguration());

        client.addRepository(r1);
        client.addRepository(r2);

        Storage storage = client.getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository0").allowsRedeployment());
        assertTrue("Failed to get storage (" + storageId + ")!", storage.getRepositories().get("repository0").isSecured());
        assertTrue("Failed to get storage (" + storageId + ")!", storage.getRepositories().get("repository1").allowsForceDeletion());
        assertTrue("Failed to get storage (" + storageId + ")!", storage.getRepositories().get("repository1").isTrashEnabled());

        assertNotNull("Failed to get storage (" + storageId + ")!",
                      storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
        assertEquals("Failed to get storage (" + storageId + ")!",
                     "localhost",
                     storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
    }

    @Test
    public void testCreateAndDeleteStorage()
            throws IOException, JAXBException
    {
        final String storageId = "storage2";
        final String repositoryId = "repository0";

        Storage storage2 = new Storage(storageId);

        int response = client.addStorage(storage2);

        assertEquals("Failed to create storage!", 200, response);

        Repository r1 = new Repository(repositoryId);
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage2);

        client.addRepository(r1);

        Storage storage = client.getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());

        response = client.deleteRepository(storageId, repositoryId, false);

        assertEquals("Failed to delete repository " + storageId + ":" + repositoryId + "!", 200, response);

        final Repository r = client.getRepository(storageId, repositoryId);

        assertNull(r);

        response = client.deleteStorage(storageId);

        assertEquals("Failed to delete storage " + storageId + "!", 200, response);

        final Storage s = client.getStorage(storageId);

        assertNull("Failed to delete storage " + storageId + "!", s);
    }

    @Test
    public void testAddGetDeleteRepository()
            throws Exception
    {
        String storageId = "storage0";
        String repositoryId = "releases";

        final Repository repository = new Repository();

        final Repository r = client.getRepository(storageId, repositoryId);

        assertNotNull("Failed to get r (" + storageId + ":" + ")!", r);
        assertEquals("Failed to get r (" + storageId + ":" + repositoryId + ")!", repositoryId, r.getId());


    }

    /*
    @Test
    public void testAddGetAndDeleteRepository()
            throws Exception
    {
        Client client = ClientBuilder.newClient();

        WebTarget resource = client.target(BASE_URL + "/strongbox/baseUrl/" + baseUrl);

        Response response = resource.request(MediaType.TEXT_PLAIN)
                                    .put(Entity.entity(baseUrl, MediaType.TEXT_PLAIN));

        int status = response.getStatus();

        assertEquals("Failed to set baseUrl!", 200, status);

        resource = client.target(BASE_URL + "/strongbox/baseUrl");

        String b = resource.request(MediaType.TEXT_PLAIN).get(String.class);

        assertEquals("Failed to get baseUrl!", 200, status);
        assertEquals("Failed to get baseUrl!", baseUrl, b);
    }
    */

    private ProxyConfiguration createProxyConfiguration()
    {
        List<String> nonProxyHosts = new ArrayList<>();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");

        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.setHost("localhost");
        proxyConfiguration.setPort(8080);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("HTTP");
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        return proxyConfiguration;
    }

}