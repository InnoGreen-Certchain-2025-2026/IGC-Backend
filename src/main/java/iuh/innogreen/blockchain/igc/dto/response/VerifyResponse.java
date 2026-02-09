package iuh.innogreen.blockchain.igc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {

    private boolean exists;
    private boolean valid;
    private String certificateId;
    private String studentName;
    private String issuer;
    private Long issueTimestamp;
    private String documentHash;
    private String message;
}
