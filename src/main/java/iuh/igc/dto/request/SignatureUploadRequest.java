package iuh.igc.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureUploadRequest {
    private String base64Image;
    private CropInfo crop;
}
