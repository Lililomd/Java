package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findFirstByLemmaAndSiteId(String lemma, int id);

    Integer countLemmasBySiteId(Integer id);
    void deleteBySiteId(Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE site_id = ?1", nativeQuery = true)
    void deleteAllBySiteId(Integer siteId);

    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.lemma = :lemma AND l.site.id IN (:siteIds)")
    Integer getFrequencySum(@Param("lemma") String url, @Param("siteIds") List<Integer> ids);

    @Query("SELECT l.id FROM Lemma l WHERE l.lemma = :lemma AND l.site.id IN (:siteIds)")
    List<Integer> findIdsByLemmaAndSiteIds(@Param("lemma") String lemma, @Param("siteIds") List<Integer> siteIds);

    @Query("SELECT l.id FROM Lemma l WHERE l.lemma = :lemma")
    List<Integer> findIdsByLemma(@Param("lemma") String lemma);


}
