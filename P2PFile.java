/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.util.Date;

/**
 *
 * @author fk
 */
public class P2PFile {

    private String fileName;
    private int validState;
    private int ttrState;
    private int versionNum;
    private Date lastModifiedTime;
    private String ownerId;

    public P2PFile(String fileName, int validState, int ttrState, int versionNum, String ownerId) {
        this.fileName = fileName;
        this.validState = validState;
        this.ttrState = ttrState;
        this.versionNum = versionNum;
        this.ownerId = ownerId;
    }

    public P2PFile(String fileName) {
        this.fileName = fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setValidState(int validState) {
        this.validState = validState;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public String getFileName() {
        return fileName;
    }

    public int getValidState() {
        return validState;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getTtrState() {
        return ttrState;
    }

    public void setTtrState(int ttrState) {
        this.ttrState = ttrState;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public void incrementVersionNum() {
        this.versionNum++;
    }
    
    @Override
    public boolean equals(Object obj) {
        P2PFile file = (P2PFile) obj;
        return this.fileName.equals(file.fileName);
    }
    
    @Override
    public String toString() {
        return fileName + ", " + validState + ", " + ttrState + ", " + versionNum + ", " + ownerId;
    }
}
