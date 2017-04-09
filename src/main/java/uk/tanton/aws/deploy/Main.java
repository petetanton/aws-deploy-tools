package uk.tanton.aws.deploy;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {


    public static void main(final String[] args) throws InterruptedException {
        final AmazonEC2 amazonEC2 = AmazonEC2Factory.getAmazonEC2();

        final String baseCmd = args[0];

        switch (baseCmd) {
            case "provision-worker":
                WorkerProvisioner.ProvisionWorker();
                break;
            case "get-instances":
                for (Instance instance : findInstances("deploy-worker")) {
                    System.out.println(instance.getPublicIpAddress());
                }
                break;
            default:
            throw new IllegalArgumentException("Unknown argument " + baseCmd);
        }
    }

    public static List<Instance> findInstances(String find) {
        List<Instance> instances = new ArrayList<>();

        AmazonEC2 amazonEC2 = AmazonEC2Factory.getAmazonEC2();
        for (Reservation reservation : amazonEC2.describeInstances().getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                instances.addAll(instance.getTags().stream().filter(
                        tag -> tag.getKey().equalsIgnoreCase("name") && tag.getValue().equalsIgnoreCase(find)
                ).map(tag -> instance).collect(Collectors.toList()));
            }
        }
        return instances;
    }

}
