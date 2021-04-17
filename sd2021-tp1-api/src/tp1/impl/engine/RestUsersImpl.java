package tp1.impl.engine;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class RestUsersImpl implements RestUsers {
    private final Map<String,User> users = new HashMap<String, User>();
    @Override
    public String createUser(User user) {
        if (users.containsKey(user.getUserId()))
            throw new WebApplicationException(Response.Status.CONFLICT);
        users.put(user.getUserId(),user);
        return user.getUserId();
    }

    @Override
    public User getUser(String userId, String password) {
        User user = users.get(userId);
        if (user == null )
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!user.getPassword().equals(password))
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        return user;
    }

    @Override
    public User updateUser(String userId, String password, User user) {
        User old_user = users.get(userId);
        if (old_user == null )
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!old_user.getPassword().equals(password))
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        if(user.getUserId() != null && !old_user.getUserId().equals(user.getUserId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        old_user.setEmail(user.getEmail()==null? old_user.getEmail() : user.getEmail());
        old_user.setFullName(user.getFullName()==null? old_user.getFullName() : user.getFullName());
        old_user.setPassword(user.getPassword()==null? old_user.getPassword() : user.getPassword());

        return old_user;
    }

    @Override
    public User deleteUser(String userId, String password) {
        User user = users.get(userId);
        if (user == null )
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!user.getPassword().equals(password))
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        return users.remove(user.getUserId());
    }

    @Override
    public List<User> searchUsers(String pattern) {
        List<User> res = new ArrayList<>();
        users.values().forEach(user -> {
            if (user.getFullName().contains(pattern))
                res.add(user);
        });

        return res;
    }
}
