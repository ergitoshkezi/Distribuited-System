/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(
        name = "basic",
        info = @Info(
                title = "My Smart contract",
                description = "The hyperlegendary asset transfer",
                version = "1.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public final class BasilContract implements ContractInterface {


    private final Genson genson = new Genson();

    private enum BasilErrors {
        BASIL_NOT_FOUND,
        BASIL_ALREADY_EXISTS,
        NOT_YOUR_BASIL
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetBasilByOwner(final Context ctx, String owner) {
        ChaincodeStub stub = ctx.getStub();

        List<BasilLeg> queryResults = new ArrayList<BasilLeg>();
        String queryString = String.format("{\"selector\":{\"basil\":{\"owner\":\"%s\"}}}", owner);

        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);

        for (KeyValue result : results) {
            BasilLeg basilLeg = genson.deserialize(result.getStringValue(), BasilLeg.class);
            queryResults.add(basilLeg);
        }

        final String response = genson.serialize(queryResults);
        return response;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String HistoryPlant(final Context ctx, final String qr) throws Exception {

        ChaincodeStub stub = ctx.getStub();

        List<BasilLeg> queryResults = new ArrayList<BasilLeg>();
        QueryResultsIterator<KeyModification> historyForKey = stub.getHistoryForKey(qr);

        for (KeyModification keyModification : historyForKey) {
            BasilLeg basilLeg = genson.deserialize(keyModification.getStringValue(), BasilLeg.class);
            queryResults.add(basilLeg);
        }
        //retrieving the time
        ctx.getStub().getTxTimestamp().getEpochSecond();
        final String response = genson.serialize(queryResults);

        return response;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean CheckBasil(final Context ctx, final String qr) {

        ChaincodeStub stub = ctx.getStub();

        String basilJSONSerialized = stub.getStringState(qr);
        return basilJSONSerialized != null && !basilJSONSerialized.isEmpty();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public BasilLeg CreatePlant(final Context ctx, final String basilLegJSON) throws Exception {

        ChaincodeStub stub = ctx.getStub();
        String ownerOrg = ctx.getClientIdentity().getMSPID();

        BasilLeg basilLeg = genson.deserialize(basilLegJSON, BasilLeg.class);
        Basil basil = basilLeg.getBasil();

        if (CheckBasil(ctx, basil.getQr())) {
            String errorMessage = String.format("Basil %s already exists", basil.getQr());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.BASIL_ALREADY_EXISTS.toString());
        }

        String basilJSONSerialized = genson.serialize(basilLeg);
        stub.putStringState(basil.getQr(), basilJSONSerialized);

        return basilLeg;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public BasilLeg UpdatePlant(final Context ctx, final String basilLegJSON) throws Exception {

        ChaincodeStub stub = ctx.getStub();
        BasilLeg basilLeg = genson.deserialize(basilLegJSON, BasilLeg.class);
        Basil basil = basilLeg.getBasil();

        if (!CheckBasil(ctx, basil.getQr())) {
            String errorMessage = String.format("Basil %s does not exists", basil.getQr());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.BASIL_NOT_FOUND.toString());
        }

        String ownerOrg = ctx.getClientIdentity().getMSPID();


        if (!basil.getOwner().equals(ownerOrg)) {
            String errorMessage = String.format("Your are not the owner of %s", basil.getQr());
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.NOT_YOUR_BASIL.toString());
        }

        String basilLegFromDbJSON = stub.getStringState(basil.getQr());
        BasilLeg basilLegFromDb = genson.deserialize(basilLegFromDbJSON, BasilLeg.class);

        basilLegFromDb.setBasil(basil);
        basilLegFromDb.setGpsPosition(basilLeg.getGpsPosition());
        basilLegFromDb.setTimestamp(basilLeg.getTimestamp());

        stub.putStringState(basil.getQr(), genson.serialize(basilLegFromDb));

        return basilLegFromDb;
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public BasilLeg GetTheStateOfPlant(final Context ctx, final String QRCode) throws Exception {

        ChaincodeStub stub = ctx.getStub();

        if (!CheckBasil(ctx, QRCode)) {
            String errorMessage = String.format("Basil %s does not exists", QRCode);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.BASIL_NOT_FOUND.toString());
        }

        String basilLegJSONSerialized = stub.getStringState(QRCode);
        BasilLeg deserializedBasilLeg = genson.deserialize(basilLegJSONSerialized, BasilLeg.class);

        return deserializedBasilLeg;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Basil TransferOwnership(final Context ctx, final String QRCode, final String buyer) throws Exception {

        ChaincodeStub stub = ctx.getStub();

        if (!CheckBasil(ctx, QRCode)) {
            String errorMessage = String.format("Basil %s does not exists", QRCode);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.BASIL_NOT_FOUND.toString());
        }

        String basilLegFromDbJSON = stub.getStringState(QRCode);
        BasilLeg basilLegFromDb = genson.deserialize(basilLegFromDbJSON, BasilLeg.class);
        Basil basil = basilLegFromDb.getBasil();

        String ownerOrg = ctx.getClientIdentity().getMSPID();

        if (!basil.getOwner().equals(ownerOrg)) {
            String errorMessage = String.format("Your are not the owner of %s", QRCode);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.NOT_YOUR_BASIL.toString());
        }

        basil.setOwner(buyer);
        stub.putStringState(QRCode, genson.serialize(basilLegFromDb));

        return basil;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeletePlant(final Context ctx, final String QRCode) throws Exception {

        ChaincodeStub stub = ctx.getStub();

        if (!CheckBasil(ctx, QRCode)) {
            String errorMessage = String.format("Basil %s does not exists", QRCode);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.BASIL_NOT_FOUND.toString());
        }

        String ownerOrg = ctx.getClientIdentity().getMSPID();
        String basilLegFromDbJSON = stub.getStringState(QRCode);
        BasilLeg basilLegFromDb = genson.deserialize(basilLegFromDbJSON, BasilLeg.class);
        Basil basil = basilLegFromDb.getBasil();

        if (!basil.getOwner().equals(ownerOrg)) {
            String errorMessage = String.format("Your are not the owner of %s", QRCode);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, BasilErrors.NOT_YOUR_BASIL.toString());
        }

        stub.delState(QRCode);
    }
}
