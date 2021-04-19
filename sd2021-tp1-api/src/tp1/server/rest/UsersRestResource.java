package tp1.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.server.resources.Discovery;
import tp1.server.resources.UsersResource;

import java.util.List;


@Singleton
public class UsersRestResource implements RestUsers {

    private final UsersResource usersResource;

    public UsersRestResource(Discovery discovery) {
        usersResource = new UsersResource(discovery);
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
        return extractResult(usersResource.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) {
        return extractResult(usersResource.getUser(userId, password));
    }


    @Override
    public User updateUser(String userId, String password, User user) {
        return extractResult(usersResource.updateUser(userId, password, user));
    }


    @Override
    public User deleteUser(String userId, String password) {
        return extractResult(usersResource.deleteUser(userId, password));
    }


    @Override
    public List<User> searchUsers(String pattern) {
        return extractResult(usersResource.searchUsers(pattern));
    }

}
