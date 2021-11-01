/*
 * Copyright 2014-2020  [fisco-dev]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.fisco.bcos.sdk.precompiled;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.fisco.bcos.sdk.BcosSDKTest;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.SealerList;
import org.fisco.bcos.sdk.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.config.Config;
import org.fisco.bcos.sdk.config.ConfigOption;
import org.fisco.bcos.sdk.config.exceptions.ConfigException;
import org.fisco.bcos.sdk.contract.precompiled.bfs.BFSService;
import org.fisco.bcos.sdk.contract.precompiled.bfs.FileInfo;
import org.fisco.bcos.sdk.contract.precompiled.callback.PrecompiledCallback;
import org.fisco.bcos.sdk.contract.precompiled.cns.CnsInfo;
import org.fisco.bcos.sdk.contract.precompiled.cns.CnsService;
import org.fisco.bcos.sdk.contract.precompiled.consensus.ConsensusService;
import org.fisco.bcos.sdk.contract.precompiled.crud.TableCRUDService;
import org.fisco.bcos.sdk.contract.precompiled.crud.common.Condition;
import org.fisco.bcos.sdk.contract.precompiled.crud.common.Entry;
import org.fisco.bcos.sdk.contract.precompiled.sysconfig.SystemConfigService;
import org.fisco.bcos.sdk.contract.solidity.HelloWorld;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.ConstantConfig;
import org.fisco.bcos.sdk.model.PrecompiledRetCode;
import org.fisco.bcos.sdk.model.RetCode;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.network.NetworkException;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.fisco.bcos.sdk.utils.Numeric;
import org.fisco.bcos.sdk.utils.StringUtils;
import org.fisco.bcos.sdk.utils.ThreadPoolService;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PrecompiledTest {
    private static final String configFile =
            BcosSDKTest.class
                    .getClassLoader()
                    .getResource(ConstantConfig.CONFIG_FILE_NAME)
                    .getPath();
    public AtomicLong receiptCount = new AtomicLong();
    private static final String GROUP = "group";

    @Test
    public void test1ConsensusService()
            throws ConfigException, ContractException, NetworkException {
        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        ConsensusService consensusService = new ConsensusService(client, cryptoKeyPair);
        // get the current sealerList
        List<SealerList.Sealer> sealerList = client.getSealerList().getResult();

        // select the node to operate
        SealerList.Sealer selectedNode = sealerList.get(0);

        // addSealer
        //        Assert.assertThrows(
        //                ContractException.class,
        //                () -> {
        //                    consensusService.addSealer(selectedNode.getNodeID(), BigInteger.ONE);
        //                });

        // add the sealer to the observerList
        RetCode retCode = consensusService.addObserver(selectedNode.getNodeID());
        // query the observerList
        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            List<String> observerList = client.getObserverList().getResult();
            Assert.assertTrue(observerList.contains(selectedNode.getNodeID()));
            // query the sealerList
            sealerList = client.getSealerList().getResult();
            Assert.assertFalse(sealerList.contains(selectedNode));
            // add the node to the observerList again
            Assert.assertThrows(
                    ContractException.class,
                    () -> consensusService.addObserver(selectedNode.getNodeID()));
        }
        // add the node to the sealerList again
        retCode = consensusService.addSealer(selectedNode.getNodeID(), BigInteger.ONE);

        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            Assert.assertTrue(client.getSealerList().getResult().contains(selectedNode));
            Assert.assertFalse(
                    client.getObserverList().getResult().contains(selectedNode.getNodeID()));
        }

        // removeNode
        retCode = consensusService.removeNode(selectedNode.getNodeID());
        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            Assert.assertFalse(
                    client.getObserverList().getResult().contains(selectedNode.getNodeID()));
            Assert.assertFalse(client.getSealerList().getResult().contains(selectedNode));
        }

        // add the node to observerList again
        retCode = consensusService.addObserver(selectedNode.getNodeID());
        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            Assert.assertTrue(
                    client.getObserverList().getResult().contains(selectedNode.getNodeID()));
            Assert.assertFalse(client.getSealerList().getResult().contains(selectedNode));
        }

        // add the node to the sealerList again
        retCode = consensusService.addSealer(selectedNode.getNodeID(), BigInteger.ONE);
        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            Assert.assertTrue(client.getSealerList().getResult().contains(selectedNode));
            Assert.assertFalse(
                    client.getObserverList().getResult().contains(selectedNode.getNodeID()));
        }
    }

    @Test
    public void test2CnsService() throws ConfigException, NetworkException, ContractException {
        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        HelloWorld helloWorld = HelloWorld.deploy(client, cryptoKeyPair);
        String contractAddress = helloWorld.getContractAddress().toLowerCase();
        String contractName = "HelloWorld";
        String contractVersion = String.valueOf(Math.random());
        CnsService cnsService = new CnsService(client, cryptoKeyPair);
        RetCode retCode =
                cnsService.registerCNS(contractName, contractVersion, contractAddress, "");
        // query the cns information
        List<CnsInfo> cnsInfos = cnsService.selectByName(contractName);
        if (!cnsInfos.isEmpty()) {
            boolean containContractAddress = false;
            for (CnsInfo cnsInfo : cnsInfos) {
                if (cnsInfo.getAddress().equals(contractAddress)) {
                    containContractAddress = true;
                    break;
                }
            }
            Assert.assertTrue(containContractAddress);
        }

        Tuple2<String, String> cnsTuple =
                cnsService.selectByNameAndVersion(contractName, contractVersion);
        Assert.assertTrue(
                Numeric.cleanHexPrefix(cnsTuple.getValue1()).equals(contractAddress)); // address
        Assert.assertTrue(cnsTuple.getValue2().equals("")); // abi

        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            boolean containContractAddress = false;
            for (CnsInfo cnsInfo : cnsInfos) {
                if (cnsInfo.getAddress().equals(contractAddress)) {
                    containContractAddress = true;
                }
            }
            Assert.assertTrue(containContractAddress);
        }
        Assert.assertTrue(cnsInfos.get(0).getName().equals(contractName));

        // query contractAddress
        cnsService.getContractAddress(contractName, contractVersion);
        // insert another cns info
        String contractVersion2 = String.valueOf(Math.random());
        retCode = cnsService.registerCNS(contractName, contractVersion2, contractAddress, "");

        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            List<CnsInfo> cnsInfos2 = cnsService.selectByName(contractName);
            Assert.assertTrue(cnsInfos2.size() == cnsInfos.size() + 1);
            Assert.assertTrue(
                    Numeric.cleanHexPrefix(
                                    cnsService.getContractAddress(contractName, contractVersion))
                            .equals(contractAddress));
            Assert.assertTrue(
                    Numeric.cleanHexPrefix(
                                    cnsService.getContractAddress(contractName, contractVersion2))
                            .equals(contractAddress));
        }
        // insert anther cns for other contract
        HelloWorld helloWorld2 = HelloWorld.deploy(client, cryptoKeyPair);
        String contractAddress2 = helloWorld2.getContractAddress().toLowerCase();
        String contractName2 = "hello";
        retCode = cnsService.registerCNS(contractName2, contractVersion, contractAddress2, "");
        if (retCode.getCode() == PrecompiledRetCode.CODE_SUCCESS.getCode()) {
            String abc = cnsService.getContractAddress(contractName, "abc");
            Assert.assertTrue(abc.equals("0x0000000000000000000000000000000000000000"));
            Assert.assertTrue(
                    Numeric.cleanHexPrefix(
                                    cnsService.getContractAddress(contractName2, contractVersion))
                            .equals(contractAddress2));
            Assert.assertTrue(
                    Numeric.cleanHexPrefix(
                                    cnsService.getContractAddress(contractName, contractVersion))
                            .equals(contractAddress));
        }
    }

    @Test
    public void test3SystemConfigService()
            throws ConfigException, ContractException, NetworkException {
        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        SystemConfigService systemConfigService = new SystemConfigService(client, cryptoKeyPair);
        this.testSystemConfigService(client, systemConfigService, "tx_count_limit");
        this.testSystemConfigService(client, systemConfigService, "tx_gas_limit");
    }

    private void testSystemConfigService(
            Client client, SystemConfigService systemConfigService, String key)
            throws ContractException {
        BigInteger value =
                new BigInteger(client.getSystemConfigByKey(key).getSystemConfig().getValue());
        BigInteger updatedValue = value.add(BigInteger.valueOf(1000));
        String updatedValueStr = String.valueOf(updatedValue);
        systemConfigService.setValueByKey(key, updatedValueStr);

        BigInteger queriedValue =
                new BigInteger(client.getSystemConfigByKey(key).getSystemConfig().getValue());
        System.out.println("queriedValue: " + queriedValue);
        // Assert.assertTrue(queriedValue.equals(updatedValue));
        // Assert.assertTrue(queriedValue.equals(value.add(BigInteger.valueOf(1000))));
    }

    @Test
    public void test5CRUDService() throws ConfigException, ContractException, NetworkException {
        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        TableCRUDService tableCRUDService = new TableCRUDService(client, cryptoKeyPair);
        // create a user table
        String tableName = "test" + (int) (Math.random() * 1000);
        String key = "key";
        List<String> valueFields = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            valueFields.add(i, "field" + i);
        }
        RetCode code = tableCRUDService.createTable(tableName, key, valueFields);
        Assert.assertEquals(0, code.getCode());
        // desc
        List<Map<String, String>> desc = tableCRUDService.desc(tableName);
        Assert.assertEquals(desc.get(0).get("value_field"), "field0,field1,field2,field3,field4");

        // insert
        Map<String, String> fieldNameToValue = new HashMap<>();
        for (int i = 0; i < valueFields.size(); i++) {
            fieldNameToValue.put("field" + i, "value" + i);
        }
        fieldNameToValue.put(key, "key1");
        Entry fieldNameToValueEntry = new Entry(fieldNameToValue);
        tableCRUDService.insert(tableName, fieldNameToValueEntry);
        // select
        Condition condition = new Condition();
        condition.EQ(key, "key1");
        List<Map<String, String>> result = tableCRUDService.select(tableName, condition);
        // field value result + key result
        if (result.size() > 0) {
            Assert.assertEquals(result.get(0).size(), fieldNameToValue.size());
        }
        System.out.println("tableCRUDService select result: " + result);
        // update
        fieldNameToValue.clear();
        fieldNameToValue.put(key, "key1");
        fieldNameToValueEntry.setFieldNameToValue(fieldNameToValue);
        tableCRUDService.update(tableName, fieldNameToValueEntry, null);
        result = tableCRUDService.select(tableName, condition);
        if (result.size() > 0) {
            Assert.assertTrue(result.get(0).size() == valueFields.size() + 1);
        }
        System.out.println("tableCRUDService select result: " + result);

        // remove
        tableCRUDService.remove(tableName, condition);
        result = tableCRUDService.select(tableName, condition);
        Assert.assertTrue(result.size() == 0);
        System.out.println("testCRUDPrecompiled tableCRUDService.remove size : " + result.size());
    }

    // Note: Please make sure that the ut is before the permission-related ut
    @Test
    public void test51SyncCRUDService()
            throws ConfigException, ContractException, NetworkException {

        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        TableCRUDService crudService = new TableCRUDService(client, cryptoKeyPair);
        String tableName = "test_sync" + new Random().nextInt(100);
        List<String> valueFiled = new ArrayList<>();
        valueFiled.add("field");
        RetCode retCode = crudService.createTable(tableName, "key", valueFiled);
        System.out.println("tableName" + tableName);
        System.out.println(
                "createResult: " + retCode.getCode() + ", message: " + retCode.getMessage());
        // create a thread pool to parallel insert and select
        ExecutorService threadPool = Executors.newFixedThreadPool(50);

        BigInteger orgTxCount =
                new BigInteger(
                        client.getTotalTransactionCount()
                                .getTotalTransactionCount()
                                .getTransactionCount());
        for (int i = 0; i < 100; i++) {
            Integer index = i;
            threadPool.execute(
                    () -> {
                        try {
                            Map<String, String> value = new HashMap<>();
                            value.put("field", "field" + index);
                            value.put("key", "key" + index);
                            // insert
                            crudService.insert(tableName, new Entry(value));
                            // select
                            Condition condition = new Condition();
                            condition.EQ("key", "key" + index);
                            crudService.select(tableName, condition);
                            // update
                            value.clear();
                            value.put("field", "field" + index + 100);
                            crudService.update(tableName, new Entry(value), condition);
                            // remove
                            crudService.remove(tableName, condition);
                        } catch (ContractException e) {
                            System.out.println(
                                    "call crudService failed, error information: "
                                            + e.getMessage());
                        }
                    });
        }
        ThreadPoolService.stopThreadPool(threadPool);
        BigInteger currentTxCount =
                new BigInteger(
                        client.getTotalTransactionCount()
                                .getTotalTransactionCount()
                                .getTransactionCount());
        System.out.println("orgTxCount: " + orgTxCount + ", currentTxCount:" + currentTxCount);
        Assert.assertTrue(currentTxCount.compareTo(orgTxCount.add(BigInteger.valueOf(300))) >= 0);
    }

    class FakeTransactionCallback implements PrecompiledCallback {
        public TransactionReceipt receipt;

        // wait until get the transactionReceipt
        @Override
        public void onResponse(RetCode retCode) {
            this.receipt = retCode.getTransactionReceipt();
            PrecompiledTest.this.receiptCount.addAndGet(1);
        }
    }

    @Test
    public void test52AsyncCRUDService()
            throws ConfigException, NetworkException, ContractException, InterruptedException {

        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        TableCRUDService crudService = new TableCRUDService(client, cryptoKeyPair);
        // create table
        String tableName = "send_async" + new Random().nextInt(1000);
        List<String> valueFiled = new ArrayList<>();
        valueFiled.add("field");
        String key = "key";
        crudService.createTable(tableName, key, valueFiled);
        // create a thread pool to parallel insert and select
        ExecutorService threadPool = Executors.newFixedThreadPool(50);
        BigInteger orgTxCount =
                new BigInteger(
                        client.getTotalTransactionCount()
                                .getTotalTransactionCount()
                                .getTransactionCount());
        for (int i = 0; i < 100; i++) {
            int index = i;
            threadPool.execute(
                    () -> {
                        try {
                            Map<String, String> value = new HashMap<>();
                            value.put("field", "field" + index);
                            value.put("key", "key" + index);
                            // insert
                            FakeTransactionCallback callback = new FakeTransactionCallback();
                            crudService.asyncInsert(tableName, new Entry(value), callback);
                            // update
                            Condition condition = new Condition();
                            condition.EQ(key, "key" + index);
                            value.clear();
                            value.put("field", "field" + index + 100);
                            FakeTransactionCallback callback2 = new FakeTransactionCallback();
                            crudService.asyncUpdate(
                                    tableName, new Entry(value), condition, callback2);
                            // remove
                            FakeTransactionCallback callback3 = new FakeTransactionCallback();
                            crudService.asyncRemove(tableName, condition, callback3);
                        } catch (ContractException e) {
                            System.out.println(
                                    "call crudService failed, error information: "
                                            + e.getMessage());
                        }
                    });
        }
        while (this.receiptCount.get() != 300) {
            Thread.sleep(1000);
        }
        ThreadPoolService.stopThreadPool(threadPool);
        BigInteger currentTxCount =
                new BigInteger(
                        client.getTotalTransactionCount()
                                .getTotalTransactionCount()
                                .getTransactionCount());
        System.out.println("orgTxCount: " + orgTxCount + ", currentTxCount:" + currentTxCount);
        Assert.assertTrue(currentTxCount.compareTo(orgTxCount.add(BigInteger.valueOf(300))) >= 0);
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (StringUtils.isEmpty(hexString)) {
            return new byte[0];
        }
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++) {
            if (index > hexString.length() - 1) {
                return byteArray;
            }
            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return byteArray;
    }

    @Test
    public void test7BFSPrecompiled() throws ConfigException, NetworkException, ContractException {

        ConfigOption configOption = Config.load(configFile);
        Client client = Client.build(GROUP, configOption);

        CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
        BFSService bfsService = new BFSService(client, cryptoKeyPair);
        List<FileInfo> list = bfsService.list("/");
        System.out.println(list);

        String newDir = "local" + new Random().nextInt(1000);
        RetCode mkdir = bfsService.mkdir("/usr/" + newDir);
        Assert.assertEquals(mkdir.code, 0);
        List<FileInfo> list2 = bfsService.list("/usr");
        System.out.println(list2);
        boolean flag = false;
        for (FileInfo fileInfo : list2) {
            if (Objects.equals(fileInfo.getName(), newDir)) {
                flag = true;
                break;
            }
        }
        Assert.assertTrue(flag);
    }

    //    @Test
    //    public void test7ContractLifeCycleService() throws ConfigException {
    //        try {
    //            BcosSDK sdk = BcosSDK.build(configFile);
    //            Client client = sdk.getClientByGroupID("1");
    //            CryptoKeyPair cryptoKeyPair = client.getCryptoSuite().createKeyPair();
    //            ContractLifeCycleService contractLifeCycleService =
    //                    new ContractLifeCycleService(client, cryptoKeyPair);
    //            // deploy a helloWorld
    //            HelloWorld helloWorld = HelloWorld.deploy(client, cryptoKeyPair);
    //            String orgValue = helloWorld.get();
    //            contractLifeCycleService.freeze(helloWorld.getContractAddress());
    //            // call the contract
    //            TransactionReceipt receipt = helloWorld.set("Hello, Fisco");
    //
    //            // get contract status
    //            contractLifeCycleService.getContractStatus(helloWorld.getContractAddress());
    //
    //            // unfreeze the contract
    //            contractLifeCycleService.unfreeze(helloWorld.getContractAddress());
    //            String value = helloWorld.get();
    //            Assert.assertTrue(value.equals(orgValue));
    //
    //            helloWorld.set("Hello, Fisco1");
    //            value = helloWorld.get();
    //            System.out.println("==== after set: " + value);
    //            // Assert.assertTrue("Hello, Fisco1".equals(value));
    //            // grant Manager
    //            CryptoSuite cryptoSuite1 =
    //                    new CryptoSuite(client.getCryptoSuite().getCryptoTypeConfig());
    //            CryptoKeyPair cryptoKeyPair1 = cryptoSuite1.createKeyPair();
    //            ContractLifeCycleService contractLifeCycleService1 =
    //                    new ContractLifeCycleService(client, cryptoKeyPair1);
    //            // freeze contract without grant manager
    //            RetCode retCode =
    // contractLifeCycleService1.freeze(helloWorld.getContractAddress());
    //            Assert.assertTrue(retCode.equals(PrecompiledRetCode.CODE_INVALID_NO_AUTHORIZED));
    //            // grant manager
    //            contractLifeCycleService.grantManager(
    //                    helloWorld.getContractAddress(), cryptoKeyPair1.getAddress());
    //            // freeze the contract
    //            retCode = contractLifeCycleService1.freeze(helloWorld.getContractAddress());
    //            receipt = helloWorld.set("Hello, fisco2");
    //            //            Assert.assertTrue(
    //            //                    new BigInteger(receipt.getStatus().substring(2), 16)
    //            //                            .equals(BigInteger.valueOf(30)));
    //
    //            // unfreeze the contract
    //            contractLifeCycleService1.unfreeze(helloWorld.getContractAddress());
    //            helloWorld.set("Hello, fisco3");
    //            Assert.assertTrue("Hello, fisco3".equals(helloWorld.get()));
    //        } catch (ContractException | ClientException e) {
    //            System.out.println("testContractLifeCycleService failed, error info:" +
    // e.getMessage());
    //        }
    //    }
}
