package edumsg.controller;

import java.util.Objects;

public class Host implements Comparable<Host> {

    private String ip;
    private String user;
    private String password;
    private int instancesCount;

    public Host(String ip, String user, String password) {
        this.ip = ip;
        this.user = user;
        this.password = password;
        this.instancesCount = 0;
    }

    public String getIp() {
        return ip;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getInstancesCount() {
        return instancesCount;
    }

    public void setInstancesCount(int instancesCount) {
        this.instancesCount = instancesCount;
    }

    public void incrementInstancesCount() {
        this.instancesCount++;
    }

    public void decrementInstancesCount() {
        this.instancesCount--;
    }

    @Override
    public int compareTo(Host host) {
        return this.getInstancesCount() - host.getInstancesCount();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Host)) return false;
        Host host = (Host) o;
        return this.getIp().equals(host.getIp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, user);
    }
}
