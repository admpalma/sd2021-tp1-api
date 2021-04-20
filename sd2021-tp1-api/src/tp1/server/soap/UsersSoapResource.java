package tp1.server.soap;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;
import tp1.server.resources.Discovery;
import tp1.server.resources.UsersResource;

import java.util.List;

@WebService(serviceName=SoapUsers.NAME, targetNamespace=SoapUsers.NAMESPACE, endpointInterface=SoapUsers.INTERFACE)
public class UsersSoapResource implements SoapUsers {

    private final UsersResource usersResource;

    public UsersSoapResource(Discovery discovery) {
        usersResource = new UsersResource(discovery);
    }

    private <T> T extractResult(Result<T> result) throws UsersException {
        if (result.isOK()) {
            return result.value();
        } else {
            throw new UsersException(result.error().name());
        }
    }

    @Override
    public String createUser(User user) throws UsersException {
        return extractResult(usersResource.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) throws UsersException {
        return extractResult(usersResource.getUser(userId, password));
    }


    @Override
    public User updateUser(String userId, String password, User user) throws UsersException {
        return extractResult(usersResource.updateUser(userId, password, user));
    }


    @Override
    public User deleteUser(String userId, String password) throws UsersException {
        return extractResult(usersResource.deleteUser(userId, password));
    }


    @Override
    public List<User> searchUsers(String pattern) throws UsersException {
        return extractResult(usersResource.searchUsers(pattern));
    }

}
