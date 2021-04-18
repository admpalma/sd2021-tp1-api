package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Singleton
public class UsersResource implements RestUsers {

    private final ConcurrentMap<String, User> users;
    private final Discovery discovery;

    private static final Logger Log = Logger.getLogger(Discovery.class.getName());

    public UsersResource(Discovery discovery) {
        users = new ConcurrentHashMap<>();
        this.discovery = discovery;
        discovery.startEmitting();
        discovery.startReceiving();
    }

    @Override
    public String createUser(User user) {
        Log.info("createUser : " + user);

        // Check if user is valid, if not return HTTP CONFLICT (409)
        if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null ||
                user.getEmail() == null) {
            Log.info("User object invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        //TODO concurrency
        // Check if userId does not exist exists, if not return HTTP CONFLICT (409)
        if (users.putIfAbsent(user.getUserId(), user) != null) {
            Log.info("User already exists.");
            throw new WebApplicationException(Status.CONFLICT);
        }

        //Add the user to the map of users
        ;

        return user.getUserId();
    }


    @Override
    public User getUser(String userId, String password) {
        Log.info("getUser : user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP CONFLICT (409)
//        if (userId == null || password == null) {
//            Log.info("UserId or password null.");
//            throw new WebApplicationException(Status.FORBIDDEN);
//        }

        User user = users.get(userId);
        // Check if user exists
        if (user == null) {
            Log.info("User does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        synchronized (user) {
            if (!users.containsKey(userId)) {
                Log.info("User does not exist.");
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            //Check if the password is correct
            if (!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                throw new WebApplicationException(Status.FORBIDDEN);
            }
            return user;
        }
    }


    @Override
    public User updateUser(String userId, String password, User user) {
        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
        // TODO Complete method
        if (userId == null || password == null) {
            Log.info("UserId or password null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        User userToUpdate = users.get(userId);

        // Check if user exists
        if (userToUpdate == null) {
            Log.info("User does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        synchronized (userToUpdate) {
            if (!users.containsKey(userId)) {
                Log.info("User does not exist.");
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            //Check if the password is correct
            if (!userToUpdate.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                throw new WebApplicationException(Status.FORBIDDEN);
            }

            if (user.getFullName() != null) {
                userToUpdate.setFullName(user.getFullName());
            }
            if (user.getEmail() != null) {
                userToUpdate.setEmail(user.getEmail());
            }
            if (user.getPassword() != null) {
                userToUpdate.setPassword(user.getPassword());
            }
            return userToUpdate;
        }
    }


    @Override
    public User deleteUser(String userId, String password) {
        Log.info("deleteUser : user = " + userId + "; pwd = " + password);
        // TODO Complete method
        // Check if user is valid, if not return HTTP CONFLICT (409)
        if (userId == null || password == null) {
            Log.info("UserId or password null.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        // Check if user exists
        User user = users.get(userId);
        if (user == null) {
            Log.info("User does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        synchronized (user) {
            //Check if the password is correct
            if (!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                throw new WebApplicationException(Status.FORBIDDEN);
            }
            users.remove(userId);
            return user;
        }
    }


    @Override
    public List<User> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);
        // TODO Complete method
        //Check if the password is correct
        if (pattern == null) {
            Log.info("Pattern is null.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        return users.values().stream()
                .filter(user -> user.getFullName().toLowerCase().contains(pattern.toLowerCase()))
                .map(user -> new User(user.getUserId(), user.getFullName(), user.getEmail(), ""))
                .collect(Collectors.toList());
    }

}
