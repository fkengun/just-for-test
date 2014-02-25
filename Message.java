package p2pfilesharing;

import java.util.ArrayList;

public class Message {

    public static int MAX_TTL = 3;
    protected Command cmd;
    protected int messageId;
    protected int TTL;
    protected String originator;
    protected ArrayList<String> route;
    protected Object body;
    protected int VersionNO;
    protected String fileName;


    public Message(int VersionNO){
        this.VersionNO = VersionNO;
    }
    
    public Message() {
        this.TTL = MAX_TTL;
    }

    public Message(Command cmd, int messageId, Object body) {
        this.cmd = cmd;
        this.messageId = messageId;
        this.TTL = MAX_TTL;
        this.body = body;
    }

    public Message(Command cmd, int messageId, String originator, ArrayList<String> route, Object body) {
        this.cmd = cmd;
        this.messageId = messageId;
        this.originator = originator;
        this.TTL = MAX_TTL;
        this.route = route;
        this.body = body;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    
    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setRoute(ArrayList<String> route) {
        if (this.route == null)
            this.route = new ArrayList<String>();
        this.route.addAll(route);
    }

    public ArrayList<String> getRoute() {
        return route;
    }

    public String getOriginator() {
        return originator;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public void pushID(String my_ID) {
        if (route == null)
            route = new ArrayList<String>();
        route.add(my_ID);
    }

    public String popID() {
        if (route.isEmpty()) {
            return null;
        }
        String id = route.get(route.size() - 1);
        route.remove(route.size() - 1);
        return id;
    }

    public boolean decrementTTL() {
        return --this.TTL <= 0;
    }
    
    public String getSource() {
        if (route.isEmpty()) {
            return null;
        }
        return route.get(0);
    }

    public void setVersionNO(int VersionNO) {
        this.VersionNO = VersionNO;
    }

    public int getVersionNO() {
        return VersionNO;
    }
    
    @Override
    public String toString() {
        return this.cmd + ", " + this.messageId + ", " + this.TTL + ", " + this.body + ", " + this.route;
    }
}
