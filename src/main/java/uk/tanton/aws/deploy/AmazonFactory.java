package uk.tanton.aws.deploy;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class AmazonFactory {

    public static AmazonEC2 getAmazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(
//                new ProfileCredentialsProvider("pete-work"),
                new ClientConfiguration()
                        .withConnectionTimeout(5000)
        );
        amazonEC2.setRegion(Region.getRegion(Regions.EU_WEST_1));
        return amazonEC2;
    }

    //    deployment-tools-Deployments-GP979QPSCC56
    public static DynamoDBMapper getDynamoMapper() {
        return new DynamoDBMapper(
                new AmazonDynamoDBClient(
//                        new ProfileCredentialsProvider("pete-work")
                )
                .withRegion(Region.getRegion(Regions.EU_WEST_1))
        );
    }
}
