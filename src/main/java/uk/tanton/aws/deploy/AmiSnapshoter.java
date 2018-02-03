package uk.tanton.aws.deploy;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import uk.tanton.aws.deploy.domain.Deployments;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AmiSnapshoter {

    public static void takeSnapshot(final String component) throws InterruptedException {
        final AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();

        Instance instance = InstanceFinder.findInstances("deploy-worker").stream().filter(i -> isRunning(i.getInstanceId(), amazonEC2)).collect(Collectors.toList()).get(0);

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

        createImage(instance.getInstanceId(), component, currentHighestVersion);


    }

    private static CreateImageResult createImage(final String instanceId, final String component, final int currentHighestVersion) throws InterruptedException {
        final String imageName = String.format("%s-%s", component, currentHighestVersion);

        CreateImageResult image = null;
        try {
            image = AmazonFactory.getAmazonEC2().createImage(new CreateImageRequest(instanceId, imageName).withDescription(imageName));
            blockUntilImageCreated(image.getImageId(), component, currentHighestVersion);
        } catch (AmazonClientException e) {
            System.out.println(String.format("An exception occurred whilst taking a snapshot (%s), we will try again", e.getMessage()));
            Thread.sleep(10000);
            createImage(instanceId, component, currentHighestVersion + 1);
        }
        return image;

    }

    private static void blockUntilImageCreated(final String imageId, final String component, int currentHighestVersion) throws InterruptedException {
        final DescribeImagesResult describeImagesResult = AmazonFactory.getAmazonEC2().describeImages(new DescribeImagesRequest().withImageIds(imageId));
        for (final Image i : describeImagesResult.getImages()) {
//            final StateReason stateReason = i.getStateReason();
//            if (stateReason == null) {
//                System.out.println(String.format("[%s] AMI has no state yet", new Date().toString()));
//                Thread.sleep(5000);
//                blockUntilImageCreated(imageId, component, currentHighestVersion);
//                return;
//            }
            final Deployments newDeployment = new Deployments(imageId, component, new Date(), i.getState(), currentHighestVersion);
            AmazonFactory.getDynamoMapper().save(newDeployment);

            switch (i.getState()) {
                case "pending":
                    System.out.println(String.format("[%s] AMI %s is pending", new Date().toString(), imageId));
                    Thread.sleep(10000);
                    blockUntilImageCreated(imageId, component, currentHighestVersion);
                    return;
                case "available":
                    System.out.println(String.format("[%s] AMI %s is available", new Date().toString(), imageId));
                    return;
                default:
                    throw new RuntimeException(String.format("image: %s is in a failure state of: %s", imageId, i.getState()));
            }
        }
    }

    private static boolean isRunning(String instanceId, AmazonEC2 amazonEC2) {
        InstanceState state = amazonEC2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId)).getReservations().get(0).getInstances().get(0).getState();
        return state.getName().toLowerCase().equals("running");
    }
}
