package util;

public class User {

    private enum Status {online, offline};
    private String name;
    private Status status;

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
