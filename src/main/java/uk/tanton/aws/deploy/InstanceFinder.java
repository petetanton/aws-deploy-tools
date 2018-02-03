package uk.tanton.aws.deploy;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceFinder {

    public static List<Instance> findInstances(String find) {
        List<Instance> instances = new ArrayList<>();

        AmazonEC2 amazonEC2 = AmazonFactory.getAmazonEC2();
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
