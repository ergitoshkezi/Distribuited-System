/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;


public final class App {

    // path to your test-network directory included, e.g.: Paths.get("..", "..", "test-network")
    private static final Path PATH_TO_TEST_NETWORK = Paths.get("..", "..", "test-network");

    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

    // Gateway peer end point.
    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(final String[] args) throws Exception {

        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                "organizations/peerOrganizations/org1.example.com/" +
                                        "peers/peer0.org1.example.com/tls/ca.crt"))
                        .toFile())
                .build();
        // The gRPC client connection should be shared by all Gateway connections to
        // this endpoint.
        ManagedChannel channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
                .overrideAuthority(OVERRIDE_AUTH)
                .build();

        Gateway.Builder builderOrg1 = Gateway.newInstance()
                .identity(new X509Identity("Org1MSP",
                        Identities.readX509Certificate(
                                Files.newBufferedReader(
                                        PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                                "organizations/peerOrganizations/org1.example.com/" +
                                                        "users/User1@org1.example.com/msp/signcerts/cert.pem"
                                        ))
                                )
                        )
                ))
                .signer(
                        Signers.newPrivateKeySigner(
                                Identities.readPrivateKey(
                                        Files.newBufferedReader(
                                                Files.list(PATH_TO_TEST_NETWORK.resolve(
                                                                Paths.get(
                                                                        "organizations/peerOrganizations/org1.example.com/" +
                                                                                "users/User1@org1.example.com/msp/keystore")
                                                        )
                                                ).findFirst().orElseThrow()
                                        )
                                )
                        )
                )
                .connection(channel)
                // Default timeouts for different gRPC calls
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        // notice that we can share the grpc connection since we don't use private date,
        // otherwise we should create another connection
        Gateway.Builder builderOrg2 = Gateway.newInstance()
                .identity(new X509Identity("Org2MSP",
                        Identities.readX509Certificate(Files.newBufferedReader(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                "organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/signcerts/cert.pem"))))))
                .signer(Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Files
                        .list(PATH_TO_TEST_NETWORK.resolve(Paths
                                .get("organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/keystore")))
                        .findFirst().orElseThrow()))))
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        try (Gateway gatewayOrg1 = builderOrg1.connect();
             Gateway gatewayOrg2 = builderOrg2.connect()) {

            Contract contractOrg1 = gatewayOrg1
                    .getNetwork(CHANNEL_NAME)
                    .getContract(CHAINCODE_NAME);

            Contract contractOrg2 = gatewayOrg2
                    .getNetwork(CHANNEL_NAME)
                    .getContract(CHAINCODE_NAME);

            Map<String, Contract> ORGS = new HashMap<>();
            ORGS.put("Org1MSP", contractOrg1);
            ORGS.put("Org2MSP", contractOrg2);

            while (true) {
                try {
                    String orgName = getOrgIndex(ORGS.keySet().toArray(new String[0]), scanner);
                    if (orgName == null) {
                        continue;
                    }

                    String[] TRANSACTIONS = getTransactionByOrg(orgName);
                    Integer txIndex = getTransactionIndex(TRANSACTIONS, scanner);
                    if (txIndex == null) {
                        continue;
                    }

                    String txName = TRANSACTIONS[txIndex];
                    Contract orgContract = ORGS.get(orgName);
                    byte[] result;

                    if (orgName.equals("Org1MSP")) {
                        switch (txName) {
                            case "CreatePlant":
                                System.out.print("Insert the QR Code: ");
                                String QRCode = scanner.next();
                                System.out.print("Insert the Extra info: ");
                                String extraInfo = scanner.next();
                                System.out.print("Insert GPS Position: ");
                                String gpsPosition = scanner.next();

                                if (QRCode != null && extraInfo != null && gpsPosition != null) {
                                    Basil basil = new Basil(QRCode, extraInfo, orgName);
                                    BasilLeg basilLeg = new BasilLeg(System.currentTimeMillis(), gpsPosition, basil);
                                    String BlSerialized = gson.toJson(basilLeg);
                                    result = orgContract.submitTransaction(txName, BlSerialized);
                                    System.out.println("result = " + prettyJson(result));

                                } else {
                                    throw new Exception("Missing parameters!\n");
                                }
                                break;

                            case "UpdatePlant":
                                System.out.print("What Plant you want to update? : ");
                                String qr = scanner.next();
                                System.out.print("Insert extraInfo: ");
                                String extraInfoUpdt = scanner.next();
                                System.out.print("Insert GPS Position: ");
                                String gpsPositionUpdate = scanner.next();

                                if (qr != null && extraInfoUpdt != null && gpsPositionUpdate != null) {
                                    Basil basil = new Basil(qr, extraInfoUpdt, orgName);
                                    BasilLeg basilLeg = new BasilLeg(System.currentTimeMillis(), gpsPositionUpdate, basil);
                                    String BlSerialized = gson.toJson(basilLeg);
                                    result = orgContract.submitTransaction(txName, BlSerialized);
                                    System.out.println("result = " + prettyJson(result));
                                } else {
                                    throw new Exception("Missing parameters!\n");
                                }

                                break;

                            case "DeletePlant":
                                System.out.print("What Plant you want to delete? : ");
                                String deleteQr = scanner.next();
                                System.out.println("I'm about to delete: " + deleteQr);

                                if (deleteQr != null) {
                                    orgContract.submitTransaction(txName, deleteQr);
                                    getBasilByOwner(orgContract, orgName);
                                } else {
                                    throw new Exception("Missing QRCode!");
                                }
                                break;

                            case "GetTheStateOfPlant":
                                System.out.print("Insert target plant's QRCode: ");
                                String targetQr = scanner.next();

                                if (targetQr != null) {
                                    System.out.println("Current State of " + targetQr);
                                    result = orgContract.submitTransaction(txName, targetQr);
                                    System.out.println("result = " + prettyJson(result));
                                } else {
                                    throw new Exception("Missing QRCode!");
                                }
                                break;

                            case "HistoryPlant":
                                System.out.print("Insert the plant qr: ");
                                String historyQr = scanner.next();

                                if (historyQr != null) {
                                    System.out.printf("Retrieving History of %s", historyQr);
                                    result = orgContract.submitTransaction(txName, historyQr);
                                    System.out.println("result = " + prettyJson(result));
                                } else {
                                    throw new Exception("Missing QRCode!");
                                }
                                break;

                            case "TransferOwnership":
                                System.out.print("Insert the plant qr: ");
                                String transferPlantQR = scanner.next();
                                System.out.print("Insert the buyer (new owner): ");
                                String newOwner = getOrgIndex(ORGS.keySet().toArray(new String[0]), scanner);

                                if (transferPlantQR != null && newOwner != null) {
                                    result = orgContract.submitTransaction(txName, transferPlantQR, newOwner);
                                    System.out.println("result = " + prettyJson(result));
                                } else {
                                    throw new Exception("Missing Parameters!");
                                }
                                break;
                        }
                    } else if (orgName.equals("Org2MSP")) {
                        switch (txName) {
                            case "HistoryPlant":
                                System.out.print("Insert the plant qr: ");
                                String historyQr = scanner.next();

                                if (historyQr != null) {
                                    System.out.printf("Retrieving History of %s", historyQr);
                                    result = orgContract.submitTransaction(txName, historyQr);
                                    System.out.println("result = " + prettyJson(result));
                                } else {
                                    throw new Exception("Missing QRCode!");
                                }
                                break;

                            case "DeletePlant":
                                System.out.print("What Plant you want to delete? : ");
                                String deleteQr = scanner.next();
                                System.out.println("I'm about to delete: " + deleteQr);

                                if (deleteQr != null) {
                                    orgContract.submitTransaction(txName, deleteQr);
                                    getBasilByOwner(orgContract, orgName);
                                } else {
                                    throw new Exception("Missing QRCode!");
                                }
                                break;
                        }
                    } else {
                        throw new Exception("No Organization found");
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static String[] getTransactionByOrg(String orgName) {

        String[] TRANSACTIONS;

        if (orgName.equals("Org1MSP")) {
            TRANSACTIONS = new String[]{"CreatePlant", "UpdatePlant", "DeletePlant", "GetTheStateOfPlant",
                    "HistoryPlant", "TransferOwnership"};
        } else {
            TRANSACTIONS = new String[]{"HistoryPlant", "DeletePlant"};
        }
        return TRANSACTIONS;
    }

    private static String getOrgIndex(String[] ORGS, Scanner scanner) {
        System.out.println("Choose an organization");
        for (int i = 0; i < ORGS.length; i++) {
            System.out.print(i + ": " + ORGS[i] + "\t");
        }
        System.out.println();
        System.out.print(">");
        int orgIndex = scanner.nextInt();
        if (orgIndex < 0 || orgIndex >= ORGS.length) {
            System.err.println("Organization Not Found");
            return null;
        }
        return ORGS[orgIndex];
    }

    private static Integer getTransactionIndex(String[] TRANSACTIONS, Scanner scanner) {
        System.out.println("Choose a transaction:");
        for (int i = 0; i < TRANSACTIONS.length; i++) {
            System.out.println(i + ": " + TRANSACTIONS[i] + "\t");
        }
        System.out.println();
        System.out.print(">");

        int txIndex = scanner.nextInt();
        if (txIndex < 0 || txIndex >= TRANSACTIONS.length) {
            System.err.println("Transaction Not Found");
            return null;
        }
        return txIndex;
    }

    private static void getBasilByOwner(Contract contract, String owner) throws GatewayException {
        System.out.printf("\n--> %s Remaining Plants <--\n", owner);

        var result = contract.evaluateTransaction("GetBasilByOwner", owner);

        System.out.println(prettyJson(result));
    }

    private static String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private static String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }
}
