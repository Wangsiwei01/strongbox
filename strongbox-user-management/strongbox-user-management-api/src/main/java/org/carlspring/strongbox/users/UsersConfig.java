package org.carlspring.strongbox.users;

import org.carlspring.strongbox.config.DataServiceConfig;
import org.carlspring.strongbox.security.encryption.EncryptionAlgorithms;
import org.carlspring.strongbox.security.jaas.Credentials;
import org.carlspring.strongbox.security.jaas.Users;
import org.carlspring.strongbox.users.domain.User;
import org.carlspring.strongbox.users.service.UserService;
import org.carlspring.strongbox.xml.parsers.GenericParser;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Optional;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.orient.commons.repository.config.EnableOrientRepositories;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring configuration for all user-related code.
 *
 * @author Alex Oreshkevich
 */
@Configuration
@ComponentScan({ "org.carlspring.strongbox.users" })
@EnableOrientRepositories(basePackages = "org.carlspring.strongbox.users.repository")
@Import(DataServiceConfig.class)
public class UsersConfig
{

    private static final Logger logger = LoggerFactory.getLogger(UsersConfig.class);

    @Autowired
    OObjectDatabaseTx databaseTx;

    @Autowired
    UserService userService;

    @Autowired
    CacheManager cacheManager;

    private GenericParser<Users> parser = new GenericParser<>(Users.class);

    @PostConstruct
    @Transactional
    public void init()
    {
        logger.debug("\nStarting to configure users...\n");

        // register all domain entities
        databaseTx.getEntityManager().registerEntityClasses(User.class.getPackage().getName());

        // load users from xml file if schema do not exists
        boolean needToSaveInDb = userService.count() == 0;
        logger.warn("Load users from XML file...");
        Optional<Users> optionalUsers = loadUsersFromConfigFile();
        optionalUsers.ifPresent(
                users -> users.getUsers().stream().forEach(user -> {
                    obtainUser(user, needToSaveInDb);
                }));
    }

    @Transactional
    private void obtainUser(org.carlspring.strongbox.security.jaas.User user,
                            boolean needToSaveInDb)
    {

        User internalUser = toInternalUser(user);
        if (needToSaveInDb)
        {
            userService.save(internalUser);
        }

        logger.debug("Putting user " + internalUser.getUsername() + " to cache...");
        cacheManager.getCache("users").put(internalUser.getUsername(), internalUser);
    }

    @Transactional
    private Optional<Users> loadUsersFromConfigFile()
    {
        try
        {
            Users users = parser.parse(new File(new ClassPathResource(getUsersConfigFilePath()).getURI()));
            return Optional.of(users);
        }
        catch (Exception e)
        {
            logger.error("Unable to load users from configuration file.", e);
        }
        return Optional.empty();
    }

    @Transactional
    private User toInternalUser(org.carlspring.strongbox.security.jaas.User user)
    {
        User internalUser = new User();
        internalUser.setUsername(user.getUsername());

        Credentials credentials = user.getCredentials();
        EncryptionAlgorithms algorithms = EncryptionAlgorithms.valueOf(
                credentials.getEncryptionAlgorithm().toUpperCase());
        switch (algorithms)
        {
            case PLAIN:
                internalUser.setPassword(credentials.getPassword());
                break;
            // TODO process other cases
        }
        internalUser.setEnabled(true);
        internalUser.setRoles(user.getRoles());
        internalUser.setSalt(user.getSeed() + "");
        return internalUser;
    }

    @Transactional
    private String getUsersConfigFilePath()
    {
        // TODO How to receive path correctly ?
        return "users.xml";
    }

}
