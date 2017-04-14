package uk.tanton.aws.deploy.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Date;

//                          deployment-tools-Deployments-GP979QPSCC56
@DynamoDBTable(tableName = "deployment-tools-Deployments-1S0H7I96WGN9M")
public class Deployments {

    @DynamoDBHashKey
    private String component;

    @DynamoDBRangeKey
    private int version;

    private String status;

    private Date dateCreated;

    private String ami;

    public Deployments() {
    }

    public Deployments(String ami, String component, Date dateCreated, String status, int version) {
        this.ami = ami;
        this.component = component;
        this.dateCreated = dateCreated;
        this.status = status;
        this.version = version;
    }

    public String getAmi() {
        return ami;
    }

    public void setAmi(String ami) {
        this.ami = ami;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
