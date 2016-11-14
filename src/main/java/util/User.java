package util;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

public class User {

    private enum Status {online, offline};
    private String name;
    private Status status;
    private String privateAddress = null;

    public User(String name) {
        this.name = name;
        this.status = Status.online;
    }

    public void setOnline() {
        status = Status.online;
    }

    public void setOffline() {
        status = Status.offline;
    }

    public boolean isOnline() {
        return status.equals(Status.online);
    }

    public String getName() {
        return name;
    }

    public void register(String privateAddress) {
        this.privateAddress = privateAddress;
    }

    public String getPrivateAddress() {
        return this.privateAddress;
    }

    public String toString() {
        return name + " " + status;
    }

    public boolean equals(Object o) {
        if (!(o instanceof User)) {
            return false;
        }
        User other = (User) o;
        if (other.name.equals(name) && other.status.equals(status)) {
            return true;
        }
        return false;
    }

}
