package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.* ;


@Entity
@Getter
@Setter
@Table(name = "lemma", indexes = @javax.persistence.Index(columnList = "lemma, site_id"))
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteTable site;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;
    @Column(nullable = false)
    private int frequency;

}
