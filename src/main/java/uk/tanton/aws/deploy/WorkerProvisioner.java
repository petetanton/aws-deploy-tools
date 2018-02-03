package uk.tanton.aws.deploy;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.StringUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WorkerProvisioner {

    private static final String CENTOS7 = "ami-7abd0209";

    public static void ProvisionWorker() throws InterruptedException {
        final AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();
        final RequestSpotInstancesRequest request = new RequestSpotInstancesRequest();

        final SpotPrice cheapestInstance = new SpotPrice().withInstanceType(InstanceType.M3Medium);

        request.setInstanceCount(1);
        LaunchSpecification launchSpecification = new LaunchSpecification();
        Collection<GroupIdentifier> securityGroups = new ArrayList<>();
        securityGroups.add(new GroupIdentifier().withGroupId("sg-9547eff1"));     //ssh only

        launchSpecification.setAllSecurityGroups(securityGroups);
        Collection<BlockDeviceMapping> blockDeviceMappings = new ArrayList<>();
        final BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping();
        blockDeviceMapping.setDeviceName("/dev/sda1");
        blockDeviceMapping.setEbs(new EbsBlockDevice()
                .withDeleteOnTermination(true)
//                .withEncrypted(false)
                .withVolumeSize(8)
                .withVolumeType(VolumeType.Gp2)
        );
        blockDeviceMappings.add(blockDeviceMapping);
        launchSpecification.setBlockDeviceMappings(blockDeviceMappings);
//        launchSpecification.setEbsOptimized();
        launchSpecification.setImageId(CENTOS7);
        launchSpecification.setInstanceType(cheapestInstance.getInstanceType());
        launchSpecification.setKeyName("streamingrocket");

        launchSpecification.setSubnetId("subnet-5aaecf03");


        launchSpecification.setIamInstanceProfile(new IamInstanceProfileSpecification()
                        .withName("deployment-tools-DeploymentInstanceProfile-6VUJFOYO7VDE")
//                .withArn("arn:aws:iam::977503918776:role/deployment-role")
        );
        request.setLaunchSpecification(launchSpecification);

        request.setSpotPrice("0.015");
        request.setValidUntil(DateTime.now().plusMinutes(59).toDate());

        final RequestSpotInstancesResult result = amazonEC2.requestSpotInstances(request);

        List<String> instanceIds = new ArrayList<>();
        for (SpotInstanceRequest spotInstanceRequest : result.getSpotInstanceRequests()) {
            Collection<String> requestIds = new ArrayList<>();
            requestIds.add(spotInstanceRequest.getSpotInstanceRequestId());
            boolean isReady = false;
            while (!isReady) {

//                    System.out.println(spotInstanceRequest.toString());
                if (isSpotInstanceRequestReady(requestIds, amazonEC2)) {
                    instanceIds.addAll(amazonEC2.describeSpotInstanceRequests(new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(requestIds)).getSpotInstanceRequests().stream().map(SpotInstanceRequest::getSpotInstanceRequestId).collect(Collectors.toList()));
                    final DescribeSpotInstanceRequestsResult newResult = amazonEC2.describeSpotInstanceRequests(new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(requestIds));
                    for (SpotInstanceRequest instanceRequest : newResult.getSpotInstanceRequests()) {
                        instanceIds.add(instanceRequest.getInstanceId());
                    }
                    isReady = true;
                } else {

                    System.out.println("[" + DateTime.now().toString() + "] spot instance request is still processing");
                    Thread.sleep(10000);
                }
            }
        }

        Thread.sleep(10000);

        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("Name", "deploy-worker"));
        instanceIds.addAll(result.getSpotInstanceRequests().stream().map(SpotInstanceRequest::getInstanceId).collect(Collectors.toList()));
        final CreateTagsRequest createTagsRequest = new CreateTagsRequest(instanceIds, tags);
        amazonEC2.createTags(createTagsRequest);

        System.out.println("all instances have been started and tagged with the name: deploy-worker");
//        waitForInstances(instanceIds, amazonEC2);
        System.out.println("finished");

    }

    private static void waitForInstances(List<String> instanceIds, AmazonEC2 amazonEC2) throws InterruptedException {
        boolean done = true;
        DescribeInstanceStatusResult instanceStatus = amazonEC2.describeInstanceStatus(new DescribeInstanceStatusRequest().withInstanceIds(instanceIds));

        for (InstanceStatus status : instanceStatus.getInstanceStatuses()) {
            System.out.println(status.getInstanceState());
            if (!status.getInstanceState().equals(InstanceStateName.Running)) {
                done = false;
            }
        }

        if (!done) {
            Thread.sleep(10000L);
            waitForInstances(instanceIds, amazonEC2);
        }
    }

    private static boolean isSpotInstanceRequestReady(Collection<String> spotInstanceRequestIds, AmazonEC2 amazonEC2) {
        final DescribeSpotInstanceRequestsResult result = amazonEC2.describeSpotInstanceRequests(new DescribeSpotInstanceRequestsRequest().withSpotInstanceRequestIds(spotInstanceRequestIds));
        boolean ready = true;
        for (SpotInstanceRequest instanceRequest : result.getSpotInstanceRequests()) {
            if (instanceRequest.getStatus().getCode().equals("price-too-low")) {
                for (SpotInstanceRequest spotInstanceRequest : result.getSpotInstanceRequests()) {
                    amazonEC2.cancelSpotInstanceRequests(new CancelSpotInstanceRequestsRequest(Collections.singletonList(spotInstanceRequest.getSpotInstanceRequestId())));
                }
                System.out.println(instanceRequest.toString());
                throw new RuntimeException("Spot price too low");
            }
            if (StringUtils.isNullOrEmpty(instanceRequest.getInstanceId())) {
                ready = false;
            }
        }
        return ready;
    }
}
