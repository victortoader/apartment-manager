package com.apartmentmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection_rows")
public class InspectionRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String detail;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String status;

    private String costShare;

    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    @JsonIgnore
    private InspectionSection section;

    @OneToMany(mappedBy = "row", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<InspectionRowPhoto> photos = new ArrayList<>();

    public InspectionRow() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCostShare() { return costShare; }
    public void setCostShare(String costShare) { this.costShare = costShare; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public InspectionSection getSection() { return section; }
    public void setSection(InspectionSection section) { this.section = section; }

    public List<InspectionRowPhoto> getPhotos() { return photos; }
    public void setPhotos(List<InspectionRowPhoto> photos) { this.photos = photos; }
}
