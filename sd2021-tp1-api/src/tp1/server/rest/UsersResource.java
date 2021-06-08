package tp1.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.server.resources.Discovery;
import tp1.server.resources.UsersManager;

import java.util.List;


@Singleton
public class UsersResource implements RestUsers {

    private final UsersManager usersManager;

    public UsersResource(Discovery discovery, String domain,String serverSecret) {
        usersManager = new UsersManager(discovery, domain,serverSecret);
    }

    private <T> T extractResult(Result<T> result) {
        if (result.isOK()) {
            return result.value();
        } else {
            throw new WebApplicationException(Status.valueOf(result.error().name()));
        }
    }

    @Override
    public String createUser(User user) {
        return extractResult(usersManager.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) {
        return extractResult(usersManager.getUser(userId, password));
    }


    @Override
    public User updateUser(String userId, String password, User user) {
        return extractResult(usersManager.updateUser(userId, password, user));
    }


    @Override
    public User deleteUser(String userId, String password) {
        return extractResult(usersManager.deleteUser(userId, password));
    }


    @Override
    public List<User> searchUsers(String pattern) {
        return extractResult(usersManager.searchUsers(pattern));
    }

}
