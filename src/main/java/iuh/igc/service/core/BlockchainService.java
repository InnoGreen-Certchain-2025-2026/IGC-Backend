package iuh.igc.service.core;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface BlockchainService {

    void init();

    TransactionReceipt issueCertificate(String certificateId, String documentHash);

    VerificationResult verifyCertificate(String certificateId);

    TransactionReceipt revokeCertificate(String certificateId);

    Web3j getWeb3j();

    /**
     * DTO cho kết quả xác thực từ blockchain
     */
    interface VerificationResult {
        String getCertificateId();
        String getDocumentHash();
        Long getIssueTimestamp();
        Boolean getIsValid();
    }
}