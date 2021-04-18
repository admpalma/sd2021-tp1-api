package tp1.api.service.util;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

import java.net.URI;
import java.util.List;

public class UsersImpl implements Users {
    URI uri;
    Client client;
    public UsersImpl(URI uri){
        this.uri = uri;
        client = ClientBuilder.newClient(new ClientConfig());
    }

    @Override
    public Result<String> createUser(User user) {
        return null;
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        return null;
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        return null;
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return null;
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        return null;
    }
}
