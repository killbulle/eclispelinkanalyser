package com.eclipselink.analyzer.demo.l4_specific.batch;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import java.util.List;

@Entity
public class L4BatchLine {
    @Id
    private Long id;

    @OneToMany // mappedBy removed for brevity or need target
    @BatchFetch(BatchFetchType.JOIN)
    private List<L4Detail> details;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
