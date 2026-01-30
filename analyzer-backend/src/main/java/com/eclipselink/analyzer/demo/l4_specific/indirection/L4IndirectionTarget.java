package com.eclipselink.analyzer.demo.l4_specific.indirection;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class L4IndirectionTarget {
    @Id
    private Long id;
    private String data;
}
