package uk.tanton.aws.deploy;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.Instance;
import uk.tanton.aws.deploy.domain.Deployments;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AmiSnapshoter {

    public static void takeSnapshot(final String component) {
        final AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();

        Instance instance = InstanceFinder.findInstances("deploy-worker").get(0);

        final String volumeId = instance.getBlockDeviceMappings().get(0).getEbs().getVolumeId();
        System.out.println(volumeId);


        final DynamoDBMapper mapper = AmazonFactory.getDynamoMapper();

        Map<String, AttributeValue> expressionAttributeValues =
                new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":val", new AttributeValue().withS(component));

        final PaginatedScanList<Deployments> result = mapper.scan(Deployments.class, new DynamoDBScanExpression()
                .withFilterExpression("Component = :val")
                .withExpressionAttributeValues(expressionAttributeValues));

        int currentHighestVersion = 0;
        for (Deployments deployments : result) {
            if (deployments.getVersion() > currentHighestVersion) {
                currentHighestVersion = deployments.getVersion();
            }
        }


        currentHighestVersion++;

        final CreateImageResult image = amazonEC2.createImage(new CreateImageRequest(instance.getInstanceId(), String.format("%s-%s", component, currentHighestVersion)));

        final Deployments newDeployment = new Deployments(image.getImageId(), component, new Date(), "CREATED", currentHighestVersion);

        mapper.save(newDeployment);

        System.out.println(newDeployment.toString());


    }
}
