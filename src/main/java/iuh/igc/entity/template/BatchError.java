package iuh.igc.entity.template;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchError {
    Integer row;
    Long studentId;
    String reason;
}
