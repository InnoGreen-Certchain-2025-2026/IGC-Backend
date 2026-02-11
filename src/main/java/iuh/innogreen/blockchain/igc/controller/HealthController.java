package iuh.innogreen.blockchain.igc.controller;

import iuh.innogreen.blockchain.igc.service.core.impl.BlockchainServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {

    private final BlockchainServiceImpl blockchainServiceImpl;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();

        try {
            var web3j = blockchainServiceImpl.getWeb3j();
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            String chainId = web3j.ethChainId().send().getChainId().toString();
            String blockNumber = web3j.ethBlockNumber().send().getBlockNumber().toString();

            response.put("status", "UP");
            response.put("blockchain", Map.of(
                    "connected", true,
                    "client", clientVersion,
                    "chainId", chainId,
                    "blockNumber", blockNumber
            ));

        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("blockchain", Map.of(
                    "connected", false,
                    "error", e.getMessage()
            ));
        }

        return response;
    }
}
