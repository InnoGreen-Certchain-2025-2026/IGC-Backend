package iuh.igc.entity.template;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateField {
    String id;
    String name;
    String type;

    Double x;
    Double y;
    Double w;
    Double h;

    Integer fontSize;
    String fontFamily;
    String align;
    String color;
}
