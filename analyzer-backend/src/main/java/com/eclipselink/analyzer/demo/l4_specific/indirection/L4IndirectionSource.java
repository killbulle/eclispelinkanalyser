package com.eclipselink.analyzer.demo.l4_specific.indirection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;

@Entity
public class L4IndirectionSource {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // Should use ValueHolder
    private L4IndirectionTarget target;
}
