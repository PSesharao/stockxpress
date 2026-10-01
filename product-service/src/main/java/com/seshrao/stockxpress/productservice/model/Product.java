package com.seshrao.stockxpress.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(value = "product")
@AllArgsConstructor
@NoArgsConstructor
@Builder // lets you automatically produce the code required to have your class be instantiable (Constructing object step by step)
@Data  // @Data is a convenient shortcut annotation that bundles the features of @ToString ,
      // @EqualsAndHashCode , @Getter / @Setter and @RequiredArgsConstructor together
public class Product {
    @Id
    private String id ;
    private String name ;
    private String description;
    private BigDecimal price;
}
