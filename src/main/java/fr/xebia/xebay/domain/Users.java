package fr.xebia.xebay.domain;

import fr.xebia.xebay.domain.model.PublicUser;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static fr.xebia.xebay.domain.AdminUser.ADMIN_ROLE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

public class Users {
    private static final String CHARS_IN_KEY = "" +
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789" +
            "-_";

    private final Set<User> users;
    private final Random random;

    public Users() {
        this.random = new Random();
        this.users = new HashSet<>();
        this.users.add(new AdminUser());
    }

    public Set<PublicUser> getUsers() {
        return users.stream()
                .filter(user -> !user.isInRole(ADMIN_ROLE))
                .map(user -> user.toPublicUser())
                .sorted((user1, user2) -> Double.compare(user1.getBalance() + user1.getAssets(), user2.getBalance() + user2.getAssets()))
                .collect(toSet());
    }

    public User create(String name) throws BidException {
        if (name == null || name.isEmpty()) {
            throw new BidException("can't create user without name");
        }
        if (containsName(name)) {
            throw new BidException(format("\"%s\" is already registered", name));
        }
        String key;
        do {
            StringBuilder randomKey = new StringBuilder();
            range(0, 16).forEach((i) -> randomKey.append(CHARS_IN_KEY.charAt(random.nextInt(CHARS_IN_KEY.length()))));
            key = randomKey.toString();
        } while (containsKey(key));
        User newUser = new User(key, name);
        users.add(newUser);
        return newUser;
    }

    public User remove(String key) throws UserNotAllowedException, BidException {
        if (AdminUser.KEY.equals(key)) {
            throw new BidException("admin can't be removed");
        }
        User user = getUser(key);
        users.remove(user);
        return user;
    }

    public User getUser(String key) throws UserNotAllowedException {
        return this.users.stream()
                .filter((user) -> user.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new UserNotAllowedException(format("key \"%s\" is unknown", key)));
    }

    public Set<User> getUserSet() {
        return this.users;
    }

    private boolean containsKey(String key) {
        return users.stream().anyMatch((user) -> user.getKey().equals(key));
    }

    private boolean containsName(String name) {
        return users.stream().anyMatch((user) -> user.getName().equals(name));
    }
}
