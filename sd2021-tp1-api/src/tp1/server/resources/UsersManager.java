package tp1.server.resources;

import tp1.api.User;
import tp1.api.service.util.Result;
import tp1.api.service.util.Users;
import tp1.server.resources.requester.Requester;
import tp1.server.resources.requester.RestRequester;
import tp1.server.resources.requester.SoapRequester;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UsersManager implements Users {

    protected final String serverSecret;
    private final ConcurrentMap<String, User> users;
    private final Discovery discovery;
    private final RestRequester restRequester;
    private final SoapRequester soapRequester;
    private final String domain;

    private static final Logger Log = Logger.getLogger(UsersManager.class.getName());

    public UsersManager(Discovery discovery, String domain,String serverSecret) {
        users = new ConcurrentHashMap<>();
        this.discovery = discovery;
        this.domain = domain;
        discovery.startEmitting();
        discovery.startReceiving();
        restRequester = new RestRequester();
        soapRequester = new SoapRequester();
        this.serverSecret = serverSecret;
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info("createUser : " + user);

        // Check if user is valid, if not return HTTP CONFLICT (409)
        if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null ||
                user.getEmail() == null) {
            Log.info("User object invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        //TODO concurrency
        // Check if userId does not exist exists, if not return HTTP CONFLICT (409)
        if (users.putIfAbsent(user.getUserId(), user) != null) {
            Log.info("User already exists.");
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        //Add the user to the map of users

        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String password) {
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
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        synchronized (user) {
            if (!users.containsKey(userId)) {
                Log.info("User does not exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            //Check if the password is correct
            //Insert turbo hacky backdoor to not require an entire new endpoint
            if (!user.getPassword().equals(password) && !serverSecret.equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
        // TODO Complete method
        if (userId == null || password == null) {
            Log.info("UserId or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User userToUpdate = users.get(userId);

        // Check if user exists
        if (userToUpdate == null) {
            Log.info("User does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        synchronized (userToUpdate) {
            if (!users.containsKey(userId)) {
                Log.info("User does not exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            //Check if the password is correct
            if (!userToUpdate.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
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
            return Result.ok(userToUpdate);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        Log.info("deleteUser : user = " + userId + "; pwd = " + password);
        // TODO Complete method
        // Check if user is valid, if not return HTTP CONFLICT (409)
        if (userId == null) {
            Log.info("UserId null.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        if (password == null) {
            Log.info("Password null.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        // Check if user exists
        User user = users.get(userId);
        if (user == null) {
            Log.info("User does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        synchronized (user) {
            //Check if the password is correct
            if (!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            users.remove(userId);
        }
        URI uri = discovery.knownUrisOf(domain + ":sheets")[0];
        requesterFromURI(uri).deleteUserSheets(uri, userId,serverSecret);

        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);
        // TODO Complete method
        //Check if the password is correct
        if (pattern == null) {
            Log.info("Pattern is null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        return Result.ok(users.values().stream()
                .filter(user -> user.getFullName().toLowerCase().contains(pattern.toLowerCase()))
                .map(user -> new User(user.getUserId(), user.getFullName(), user.getEmail(), ""))
                .collect(Collectors.toList()));
    }

    private Requester requesterFromURI(URI uri) {
        String serverType = uri.getPath().substring(1, 5);
        return switch (serverType) {
            case "soap" -> soapRequester;
            case "rest" -> restRequester;
            default -> throw new IllegalArgumentException("Unexpected value: " + serverType);
        };
    }
}
