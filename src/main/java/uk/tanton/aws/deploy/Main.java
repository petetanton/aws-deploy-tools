package uk.tanton.aws.deploy;

public class Main {

    public static void main(final String[] args) {

        final String baseCmd = args[0];

        if (baseCmd.equalsIgnoreCase("provision-baker")) {

        } else {
            throw new IllegalArgumentException("Unknown argument " + baseCmd);
        }
    }

}
