package uk.tanton.aws.deploy;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

public class Main {


    public static void main(final String[] args) throws InterruptedException {
        final AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();

        final String baseCmd = args[0];

        switch (baseCmd) {
            case "provision-worker":
                WorkerProvisioner.ProvisionWorker();
                break;
            case "get-instances":
                for (Instance instance : InstanceFinder.findInstances("deploy-worker")) {
                    System.out.println(instance.getPublicIpAddress());
                }
                break;
            case "terminate-workers":
                for (Instance instance : InstanceFinder.findInstances("deploy-worker")) {
                    final TerminateInstancesResult result = amazonEC2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instance.getInstanceId()));
                    result.getTerminatingInstances().forEach(x -> blockUntilTerminated(x.getInstanceId()));
                }
                break;
            case "take-snapshot":
                AmiSnapshoter.takeSnapshot(args[1]);
                break;
            default:
                throw new IllegalArgumentException("Unknown argument " + baseCmd);
        }
    }

    public static void blockUntilTerminated(final String instanceId) {
        final AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();
        boolean block = true;


        while (block) {
            block = false;
            final DescribeInstancesResult result = amazonEC2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
            for (Reservation reservation : result.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    if (!instance.getState().getName().equals("terminated")) {
                        System.out.println(String.format("waiting for %s to terminate, current state is: %s", instance.getInstanceId(), instance.getState().getName()));
                        block = true;
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println(instance.getInstanceId() + " terminated");
                    }
                }
            }


//                         /**
//                        * The current state of the instance.
//                        * <p>
//                        * <b>Constraints:</b><br/>
//                        * <b>Allowed Values: </b>pending, running, shutting-down, terminated, stopping, stopped
//                                */
////                        private String name;
//            String state = instanceStateChange.getCurrentState().getName();
//
//            while (!state.equalsIgnoreCase("terminated")) {
//                Thread.sleep(10000);
//            }
        }

    }



}
