package iuh.igc.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CropInfo {
    private int x;
    private int y;
    private int width;
    private int height;
}
