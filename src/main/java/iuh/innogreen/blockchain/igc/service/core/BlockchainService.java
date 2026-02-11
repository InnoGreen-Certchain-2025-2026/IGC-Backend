package iuh.innogreen.blockchain.igc.service.core;

import iuh.innogreen.blockchain.igc.service.core.impl.BlockchainServiceImpl;
import jakarta.annotation.PostConstruct;
import org.web3j.protocol.core.Ethereum;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Admin 2/11/2026
 *
 **/
public interface BlockchainService {
    @PostConstruct
    void init();

    TransactionReceipt issueCertificate(String certificateId, String documentHash);

    BlockchainServiceImpl.VerificationResult verifyCertificate(String certificateId);

    TransactionReceipt revokeCertificate(String certificateId);

    Ethereum getWeb3j();
}
