package iuh.innogreen.blockchain.igc.service.core.impl;

import iuh.innogreen.blockchain.igc.service.core.BlockchainService;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Ethereum;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BlockchainServiceImpl implements BlockchainService {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    @Value("${blockchain.admin-private-key}")
    private String privateKey;

    private Web3j web3j;
    private Credentials credentials;
    private StaticGasProvider gasProvider;

    @PostConstruct
    @Override
    public void init() {
        log.info("🔗 Initializing Blockchain connection...");

        web3j = Web3j.build(new HttpService(rpcUrl));
        credentials = Credentials.create(privateKey);
        gasProvider = new StaticGasProvider(
                BigInteger.valueOf(1_000_000_000L),
                BigInteger.valueOf(6_721_975L)
        );

        try {
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();

            log.info("✅ Connected to Besu: {}", clientVersion);
            log.info("📋 Contract Address: {}", contractAddress);
            log.info("🔑 Admin Address: {}", credentials.getAddress());
            log.info("📦 Current Block: {}", blockNumber);
        } catch (Exception e) {
            log.error("❌ Failed to connect to blockchain", e);
        }
    }

    @Override
    public TransactionReceipt issueCertificate(String certificateId, String documentHash) {
        try {
            log.info("📝 Issuing certificate: {}", certificateId);

            byte[] hashBytes = hexStringToByteArray(documentHash);

            Function function = new Function(
                    "issueCertificate",
                    Arrays.asList(
                            new Utf8String(certificateId),
                            new Bytes32(hashBytes)
                    ),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);

            EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(
                    createSignedTransaction(encodedFunction)
            ).send();

            if (transactionResponse.hasError()) {
                throw new RuntimeException(transactionResponse.getError().getMessage());
            }

            String txHash = transactionResponse.getTransactionHash();
            log.info("📤 Transaction sent: {}", txHash);

            TransactionReceipt receipt = waitForReceipt(txHash);

            log.info("✅ Certificate issued - Block: {}, Gas: {}",
                    receipt.getBlockNumber(), receipt.getGasUsed());

            return receipt;

        } catch (Exception e) {
            log.error("❌ Failed to issue certificate", e);
            throw new RuntimeException("Blockchain transaction failed: " + e.getMessage(), e);
        }
    }

    @Override
    public VerificationResult verifyCertificate(String certificateId) {
        try {
            log.info("🔍 Verifying certificate: {}", certificateId);

            Function function = new Function(
                    "verifyCertificate",
                    Arrays.asList(new Utf8String(certificateId)),
                    Arrays.asList(
                            new TypeReference<Utf8String>() {
                            },
                            new TypeReference<Bytes32>() {
                            },
                            new TypeReference<Uint256>() {
                            },
                            new TypeReference<Bool>() {
                            }
                    )
            );

            String encodedFunction = FunctionEncoder.encode(function);

            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            credentials.getAddress(),
                            contractAddress,
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            String certId = (String) results.get(0).getValue();
            byte[] hashBytes = (byte[]) results.get(1).getValue();
            BigInteger timestamp = (BigInteger) results.get(2).getValue();
            Boolean isValid = (Boolean) results.get(3).getValue();

            String hash = bytesToHex(hashBytes);

            log.info("✅ Verified - Valid: {}", isValid);

            return VerificationResult.builder()
                    .certificateId(certId)
                    .documentHash(hash)
                    .issueTimestamp(timestamp.longValue())
                    .isValid(isValid)
                    .build();

        } catch (Exception e) {
            log.error("❌ Verification failed", e);
            return VerificationResult.builder()
                    .certificateId(certificateId)
                    .isValid(false)
                    .build();
        }
    }

    @Override
    public TransactionReceipt revokeCertificate(String certificateId) {
        try {
            log.info("🚫 Revoking certificate: {}", certificateId);

            Function function = new Function(
                    "revokeCertificate",
                    Arrays.asList(new Utf8String(certificateId)),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);

            EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(
                    createSignedTransaction(encodedFunction)
            ).send();

            String txHash = transactionResponse.getTransactionHash();
            TransactionReceipt receipt = waitForReceipt(txHash);

            log.info("✅ Certificate revoked");
            return receipt;

        } catch (Exception e) {
            log.error("❌ Revocation failed", e);
            throw new RuntimeException("Failed to revoke certificate", e);
        }
    }

    private String createSignedTransaction(String encodedFunction) throws Exception {
        BigInteger nonce = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send().getTransactionCount();

        org.web3j.crypto.RawTransaction rawTransaction =
                org.web3j.crypto.RawTransaction.createTransaction(
                        nonce,
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        contractAddress,
                        encodedFunction
                );

        byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(
                rawTransaction,
                credentials
        );

        return org.web3j.utils.Numeric.toHexString(signedMessage);
    }

    private TransactionReceipt waitForReceipt(String txHash) throws Exception {
        for (int i = 0; i < 40; i++) {
            var receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
            if (receiptResponse.getTransactionReceipt().isPresent()) {
                return receiptResponse.getTransactionReceipt().get();
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("Transaction not mined");
    }

    private byte[] hexStringToByteArray(String s) {
        if (s.startsWith("0x")) s = s.substring(2);
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public Ethereum getWeb3j() {
        return web3j;
    }

    @Data
    @Builder
    public static class VerificationResult {
        private String certificateId;
        private String documentHash;
        private Long issueTimestamp;
        private Boolean isValid;
    }
}
